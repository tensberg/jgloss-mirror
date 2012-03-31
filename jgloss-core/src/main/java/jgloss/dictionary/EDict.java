/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
 *
 * This file is part of JGloss.
 *
 * JGloss is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JGloss is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGloss; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * $Id$
 *
 */

package jgloss.dictionary;

import static java.util.logging.Level.SEVERE;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeMapper;
import jgloss.dictionary.attribute.AttributeSet;
import jgloss.dictionary.attribute.AttributeValue;
import jgloss.dictionary.attribute.Attributes;
import jgloss.dictionary.attribute.DefaultAttributeSet;
import jgloss.dictionary.attribute.Priority;

/**
 * Dictionary implementation for dictionaries in EDICT format 
 * based on {@link FileBasedDictionary}. For a documentation of the format see
 * <a href="http://www.csse.monash.edu.au/~jwb/edict_doc.html">
 * http://www.csse.monash.edu.au/~jwb/edict_doc.html</a>.
 *
 * @author Michael Koch
 */
public class EDict extends FileBasedDictionary {
	private static final Logger LOGGER = Logger.getLogger(EDict.class.getPackage().getName());

    /**
     * Object describing this implementation of the <CODE>Dictionary</CODE> interface. The
     * Object can be used to register this class with the <CODE>DictionaryFactory</CODE>, or
     * test if a descriptor matches this class.
     *
     * @see DictionaryFactory
     */
	public static final DictionaryFactory.Implementation<EDict> IMPLEMENTATION_EUC = 
		initImplementation("EDICT", "EUC-JP");
	public static final DictionaryFactory.Implementation<EDict> IMPLEMENTATION_UTF8 = 
		initImplementation("EDICT (Unicode)", "UTF-8");

    /**
     * Returns a {@link FileBasedDictionary.Implementation FileBasedDictionary.Implementation}
     * which recognizes EUC-JP encoded EDICT dictionaries. Used to initialize the
     * {@link #IMPLEMENTATION implementation} final member because the constructor has to
     * be wrapped in a try/catch block.
     * 
     */
    private static DictionaryFactory.Implementation<EDict> initImplementation(String name, String encoding) {
        try {
            // Explanation of the pattern:
            // The EDICT format is "word [reading] /translation/translation/.../", with
            // the reading being optional. The dictionary has to start with a line of this form,
            // therefore the pattern starts with a \A and ends with $
            // To distinguish an EDICT dictionary from a SKK dictionary, which uses a similar format,
            // it is tested that the first char in the translation is not a Kanji
            // (InCJKUnifiedIdeographs)
            return new FileBasedDictionary.Implementation<EDict>
                ( name, encoding, true, Pattern.compile
                  ( "\\A\\S+?(\\s\\[.+?\\])?(\\s/)|/\\P{InCJKUnifiedIdeographs}.*/$", Pattern.MULTILINE),
                  1.0f, 4096, EDict.class.getConstructor( new Class[] { File.class, String.class })) {
                        @Override
						protected Object[] getConstructorParameters(String descriptor) {
                            return new Object[] { new File(descriptor), this.encoding };
                        }
                  };
        } catch (Exception ex) {
            LOGGER.log(SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

    protected static final AttributeMapper MAPPER = initMapper();

    private static AttributeMapper initMapper() {
        try {
            Reader r = new InputStreamReader( EDict.class.getResourceAsStream( "/edict.map"));
            AttributeMapper mapper = new AttributeMapper( new LineNumberReader( r));
            r.close();
            return mapper;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Match an EDICT entry. Group 1 is the word, group 2 the (optional) reading and group 3
     * the translations.
     */
    protected static final Pattern ENTRY_PATTERN = Pattern.compile
        ( "(\\S+)(?:\\s\\[(.+?)\\])?\\s/(.+)/");
    protected static final Matcher ENTRY_MATCHER = ENTRY_PATTERN.matcher( "");

    /**
     * Match a string in brackets at the beginning of a string.
     */
    protected static final Pattern BRACKET_PATTERN = Pattern.compile( "\\G\\((.+?)\\)\\s");
    protected static final Matcher BRACKET_MATCHER = BRACKET_PATTERN.matcher( "");

    protected static final String PRIORITY_MARKER = "(P)";

    protected static final Priority PRIORITY_VALUE = new Priority() {
            @Override
			public String getPriority() { return "_P_"; }
            @Override
			public int compareTo( Priority p) {
                if (p == PRIORITY_VALUE) {
	                return 0;
                } else {
	                throw new IllegalArgumentException();
                }
            }
            @Override
			public String toString() { return "_P_"; }
        };

    public EDict( File dicfile, String encoding) throws IOException {
        super( dicfile, encoding);
    }
    
    @Override
	protected void initSupportedAttributes() {
        super.initSupportedAttributes();
        
        supportedAttributes.putAll( MAPPER.getAttributes());
        supportedAttributes.put( Attributes.PRIORITY, Collections.<AttributeValue> singleton( PRIORITY_VALUE));
    }

    @Override
	protected boolean isFieldStart( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( --location);
            if (field==DictionaryEntryField.READING && b=='['
                || field==DictionaryEntryField.TRANSLATION && b=='/'
                || b==10 || b==13) {
	            return true;
            }

            if (field == DictionaryEntryField.TRANSLATION) {
                // EDICT translation fields support multiple senses, which are marked 
                // as (1),(2)... , and also POS markers in the form (pos), which are all
                // at the start of the translation field

                while (b==' ' && entry.get( --location)==')') {
                    // assume that a POS marker or sense marker was found, and skip it
                    do {
                        b = entry.get( --location);
                    } while (b!='/' && b!='(');
                    if (b=='/' || 
                        b=='(' && (b=entry.get( --location))=='/')
					 {
	                    return true;
                    // if b is now anything other than a space, in which case there could be
                    // another marker, the while loop will terminate and false will be returned
                    }
                }
            }

            return false;
        } catch (IndexOutOfBoundsException ex) {
            return true; // start of entry buffer
        }
    }

    @Override
	protected boolean isFieldEnd( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( location);
            if (field==DictionaryEntryField.WORD && b==' ' ||
                field==DictionaryEntryField.READING && b==']' ||
                field==DictionaryEntryField.TRANSLATION && b=='/' 
                || b==10 || b==13) {
	            return true;
            } else {
	            return false;
            }
        } catch (IndexOutOfBoundsException ex) {
            return true; // end of entry buffer
        }
    }

    @Override
	protected DictionaryEntryField moveToNextField( ByteBuffer buf, int character,
                                                    DictionaryEntryField field) {
        if (field == null) {
            // first call to moveToNextField
            return DictionaryEntryField.WORD;
        }

        if (field==DictionaryEntryField.WORD && character==' ') {
            byte b = buf.get();
            if (b == '[') {
	            field = DictionaryEntryField.READING;
            } else {
	            field = DictionaryEntryField.TRANSLATION;
            }
        } else if (field==DictionaryEntryField.READING && character==']') {
            buf.get(); // skip the ' '
            field = DictionaryEntryField.TRANSLATION;
        } else if (character==10 || character==13) {
            field = DictionaryEntryField.WORD;
        }

        return field;
    }

    @Override
	protected DictionaryEntryField getFieldType( ByteBuffer buf, int entryStart, int entryEnd,
                                                 int location) {
        buf.position( location);
        byte b;
        try {
            do {
                b = buf.get();
            } while (b!=' ' && b!='/' && b!=']');
            if (b == '/') {
	            return DictionaryEntryField.TRANSLATION;
            } else if (b == ']') {
	            return DictionaryEntryField.READING;
            } else {
                // word or translation
                b = buf.get();
                if (b=='/' || b=='[') {
	                return DictionaryEntryField.WORD;
                } else {
	                return DictionaryEntryField.TRANSLATION;
                }
            }
        } catch (BufferUnderflowException ex) {
            // reached end of entry, must be translation
            return DictionaryEntryField.TRANSLATION;
        }
    }

    /**
     * Parses an EDICT formatted entry. The format is
     * <CODE>word [reading] /translation 1/translation 2/...</CODE> with the reading
     * being optional.
     */
    @Override
	protected DictionaryEntry parseEntry( String entry, int startOffset) throws SearchException {
        ENTRY_MATCHER.reset( entry);
        if (!ENTRY_MATCHER.matches()) {
	        throw new MalformedEntryException( this, entry);
        }

        String word = ENTRY_MATCHER.group( 1);
        String reading = ENTRY_MATCHER.group( 2);
        if (reading == null) {
	        reading = word;
        }
        String translations = ENTRY_MATCHER.group( 3);
        List<List<String>> rom = new ArrayList<List<String>>( 10);
        List<String> crm = new ArrayList<String>( 10);
        rom.add( crm);

        DefaultAttributeSet generalA = new DefaultAttributeSet();
        DefaultAttributeSet wordA = new DefaultAttributeSet( generalA);
        DefaultAttributeSet translationA = new DefaultAttributeSet( generalA);
        List<AttributeSet> roma = new ArrayList<AttributeSet>( 10);
        DefaultAttributeSet translationromA = new DefaultAttributeSet( translationA);
        roma.add( translationromA);

        // ROM markers and entry attributes are written in brackets before the translation text.
        // Attributes which are placed in the first translation before the first ROM marker apply
        // to the whole entry, the other attributes only apply to the ROM.
        boolean seenROM = false;
        int start = 0;
        do {
            int end = translations.indexOf( '/', start);
            if (end == -1) {
	            end = translations.length();
            }

            String translation = translations.substring( start, end);
            if (translation.equals( PRIORITY_MARKER)) {
                generalA.addAttribute( Attributes.PRIORITY, PRIORITY_VALUE);
            }
            else {
                BRACKET_MATCHER.reset( translation);
                int matchend = 0;
                StringBuilder unrecognized = null;
                
                while (BRACKET_MATCHER.find()) {
                    matchend = BRACKET_MATCHER.end();
                    
                    String att = BRACKET_MATCHER.group( 1);
                    
                    boolean isNumber = true;
                    for ( int i=0; i<att.length(); i++) {
                        if (att.charAt( i)<'1' ||
                            att.charAt( i)>'9') {
                            isNumber = false;
                            break;
                        }
                    }
                    if (isNumber) {
                        // ROM marker, start new ROM unless this is the first ROM
                        if (!crm.isEmpty()) {
                            crm = new ArrayList<String>( 10);
                            rom.add( crm);
                            translationromA = new DefaultAttributeSet( translationA);
                            roma.add( translationromA);
                        }
                        seenROM = true;
                    }
                    else {
                        // attribute list separated by ','
                        int startc = 0;
                        boolean hasUnrecognized = false;
                        do {
                            int endc = att.indexOf( ',', startc);
                            if (endc == -1) {
	                            endc = att.length();
                            }
                            String attsub = att.substring( startc, endc);
                            AttributeMapper.Mapping<?> mapping = MAPPER.getMapping( attsub);
                            if (mapping != null) {
                                Attribute<?> a = mapping.getAttribute();
                                if (a.appliesTo( DictionaryEntry.AttributeGroup.GENERAL) &&
                                    (!seenROM || 
                                     !a.appliesTo( DictionaryEntry.AttributeGroup.TRANSLATION))) {
                                    generalA.addAttribute(mapping);
                                }
                                else if (a.appliesTo( DictionaryEntry.AttributeGroup.WORD)) {
                                    wordA.addAttribute(mapping);
                                }
                                else if (a.appliesTo( DictionaryEntry.AttributeGroup.TRANSLATION)) {
                                    if (seenROM) {
	                                    translationromA.addAttribute(mapping);
                                    } else {
	                                    translationA.addAttribute(mapping);
                                    }
                                }
                                else {
                                    // should not happen, edict does not support READING attributes
                                    LOGGER.warning( "EDICT warning: illegal attribute type");
                                }
                            }
                            else {
                                // Not a recognized attribute. Since the whole bracket expression
                                // will be cut off from the translation, store it seperately and
                                // prepend it.
                                if (unrecognized == null) {
	                                unrecognized = new StringBuilder();
                                }
                                if (!hasUnrecognized) {
                                    unrecognized.append( '(');
                                    hasUnrecognized = true;
                                }
                                unrecognized.append( attsub);
                            }
                            
                            startc = endc + 1;
                        } while (startc < att.length());
                        if (hasUnrecognized) {
	                        unrecognized.append( ") ");
                        }
                    }
                }

                if (matchend > 0) {
	                translation = translation.substring( matchend, translation.length());
                }
                if (unrecognized != null) {
                    unrecognized.append( translation);
                    translation = unrecognized.toString();
                }

                crm.add( translation);
            }

            start = end+1;
        } while (start < translations.length());

        return new SingleWordEntry( startOffset, word, reading, rom, generalA, wordA, translationA,
                                    roma, this);
    }
    
    @Override
	public String toString() {
        return "EDICT " + getName();
    }

    /**
     * Escape LF/CR, '/' and all characters not supported by the encoding.
     */
    @Override
	protected boolean escapeChar( char c) {
        // some special characters need escaping
        if (c==10 || c==13 || c=='/') {
	        return true;
        }

        return !characterHandler.canEncode(c);
    }
} // class EDict
