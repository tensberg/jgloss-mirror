/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeMapper;
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
    public static void main( String[] args) throws Exception {
        System.err.println( "Creating edict");
        IndexedDictionary d = new EDict( new java.io.File( args[0]));
        System.err.println( "Loading index");
        if (!d.loadIndex()) {
            System.err.println( "Building index");
            d.buildIndex();
        }
        System.err.println( "Successfully loaded index");
        SearchFieldSelection f = new SearchFieldSelection();
        f.select( DictionaryEntryField.WORD, true);
        f.select( DictionaryEntryField.READING, true);
        f.select( DictionaryEntryField.TRANSLATION, true);
        f.select( MatchMode.WORD, true);

        ResultIterator r = d.search( ExpressionSearchModes.ANY,
                                     new Object[] { args[1], f });
        System.err.println( "Matches:");
        while (r.hasNext())
            System.err.println( r.next().toString());
        //r.next();
        d.dispose();
    }

    /**
     * Object describing this implementation of the <CODE>Dictionary</CODE> interface. The
     * Object can be used to register this class with the <CODE>DictionaryFactory</CODE>, or
     * test if a descriptor matches this class.
     *
     * @see DictionaryFactory
     */
    public final static DictionaryFactory.Implementation implementation = 
        initImplementation();

    /**
     * Returns a {@link FileBasedDictionary.Implementation FileBasedDictionary.Implementation}
     * which recognizes EUC-JP encoded EDICT dictionaries. Used to initialize the
     * {@link #implementation implementation} final member because the constructor has to
     * be wrapped in a try/catch block.
     * 
     */
    private static DictionaryFactory.Implementation initImplementation() {
        try {
            // Explanation of the pattern:
            // The EDICT format is "word [reading] /translation/translation/.../", with
            // the reading being optional. The dictionary has to start with a line of this form,
            // therefore the pattern starts with a \A and ends with $
            // To distinguish an EDICT dictionary from a SKK dictionary, which uses a similar format,
            // it is tested that the first char in the translation is not a Kanji
            // (InCJKUnifiedIdeographs)
            return new FileBasedDictionary.Implementation
                ( "EDICT", "EUC-JP", true, Pattern.compile
                  ( "\\A\\S+?(\\s\\[.+?\\])?\\s/\\P{InCJKUnifiedIdeographs}.*/$", Pattern.MULTILINE),
                  1.0f, 4096, EDict.class.getConstructor( new Class[] { File.class }));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected final static AttributeMapper mapper = initMapper();

    private static AttributeMapper initMapper() {
        try {
            Reader r = new InputStreamReader( EDict.class.getResourceAsStream( "/resources/edict.map"));
            AttributeMapper mapper = new AttributeMapper( new LineNumberReader( r));
            r.close();
            return mapper;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Match an EDICT entry. Group 1 is the word, group 2 the (optional) reading and group 3
     * the translations.
     */
    protected final Pattern ENTRY_PATTERN = Pattern.compile
        ( "(\\S+)(?:\\s\\[(.+?)\\])?\\s/(.+)/");
    protected final Matcher ENTRY_MATCHER = ENTRY_PATTERN.matcher( "");

    /**
     * Match a string in brackets at the beginning of a string.
     */
    protected final Pattern BRACKET_PATTERN = Pattern.compile( "\\G\\((.+?)\\)\\s");
    protected final Matcher BRACKET_MATCHER = BRACKET_PATTERN.matcher( "");

    protected final static String PRIORITY_MARKER = "(P)";

    protected final Priority PRIORITY_VALUE = new Priority() {
            public String getPriority() { return "_P_"; }
            public int compareTo( Priority p) {
                if (p == PRIORITY_VALUE)
                    return 0;
                else
                    throw new IllegalArgumentException();
            }
            public String toString() { return "_P_"; }
        };

    public EDict( File dicfile) throws IOException {
        super( dicfile);
    }
    
    protected void initSupportedAttributes() {
        super.initSupportedAttributes();
        
        supportedAttributes.putAll( mapper.getAttributes());
        supportedAttributes.put( Attributes.PRIORITY, Collections.singleton( PRIORITY_VALUE));
    }

    protected EncodedCharacterHandler createCharacterHandler() {
        return new EUCJPCharacterHandler();
    }

    protected boolean isFieldStart( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( --location);
            if (field==DictionaryEntryField.READING && b=='['
                || field==DictionaryEntryField.TRANSLATION && b=='/'
                || b==10 || b==13)
                return true;

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
                        return true;
                    // if b is now anything other than a space, in which case there could be
                    // another marker, the while loop will terminate and false will be returned
                }
            }

            return false;
        } catch (IndexOutOfBoundsException ex) {
            return true; // start of entry buffer
        }
    }

    protected boolean isFieldEnd( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( location);
            if (field==DictionaryEntryField.WORD && b==' ' ||
                field==DictionaryEntryField.READING && b==']' ||
                field==DictionaryEntryField.TRANSLATION && b=='/' 
                || b==10 || b==13)
                return true;
            else
                return false;
        } catch (IndexOutOfBoundsException ex) {
            return true; // end of entry buffer
        }
    }

    protected DictionaryEntryField moveToNextField( ByteBuffer buf, int character,
                                                    DictionaryEntryField field) {
        if (field == null) {
            // first call to moveToNextField
            return DictionaryEntryField.WORD;
        }

        if (field==DictionaryEntryField.WORD && character==' ') {
            byte b = buf.get();
            if (b == '[')
                field = DictionaryEntryField.READING;
            else
                field = DictionaryEntryField.TRANSLATION;
        } else if (field==DictionaryEntryField.READING && character==']') {
            buf.get(); // skip the ' '
            field = DictionaryEntryField.TRANSLATION;
        } else if (character==10 || character==13) {
            field = DictionaryEntryField.WORD;
        }

        return field;
    }

    protected DictionaryEntryField getFieldType( ByteBuffer buf, int entryStart, int entryEnd,
                                                 int location) {
        buf.position( location);
        byte b;
        try {
            do {
                b = buf.get();
            } while (b!=' ' && b!='/' && b!=']');
            if (b == '/')
                return DictionaryEntryField.TRANSLATION;
            else if (b == ']')
                return DictionaryEntryField.READING;
            else {
                // word or translation
                b = buf.get();
                if (b=='/' || b=='[')
                    return DictionaryEntryField.WORD;
                else
                    return DictionaryEntryField.TRANSLATION;
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
    protected DictionaryEntry parseEntry( String entry, int startOffset) throws SearchException {
        //System.err.println( entry);
        ENTRY_MATCHER.reset( entry);
        if (!ENTRY_MATCHER.matches())
            throw new MalformedEntryException( this, entry);

        String word = ENTRY_MATCHER.group( 1);
        String reading = ENTRY_MATCHER.group( 2);
        if (reading == null) // no reading in entry string
            reading = word;
        String translations = ENTRY_MATCHER.group( 3);
        List rom = new ArrayList( 10);
        List crm = new ArrayList( 10);
        rom.add( crm);

        DefaultAttributeSet generalA = new DefaultAttributeSet();
        DefaultAttributeSet wordA = new DefaultAttributeSet( generalA);
        DefaultAttributeSet translationA = new DefaultAttributeSet( generalA);
        List roma = new ArrayList( 10);
        DefaultAttributeSet translationromA = new DefaultAttributeSet( translationA);
        roma.add( translationromA);

        // ROM markers and entry attributes are written in brackets before the translation text.
        // Attributes which are placed in the first translation before the first ROM marker apply
        // to the whole entry, the other attributes only apply to the ROM.
        boolean seenROM = false;
        int start = 0;
        do {
            int end = translations.indexOf( '/', start);
            if (end == -1)
                end = translations.length();

            String translation = translations.substring( start, end);
            if (translation.equals( PRIORITY_MARKER)) {
                generalA.addAttribute( Attributes.PRIORITY, PRIORITY_VALUE);
            }
            else {
                BRACKET_MATCHER.reset( translation);
                int matchend = 0;
                StringBuffer unrecognized = null;
                
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
                        if (crm.size() > 0) {
                            crm = new ArrayList( 10);
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
                            if (endc == -1)
                                endc = att.length();
                            String attsub = att.substring( startc, endc);
                            AttributeMapper.Mapping mapping = mapper.getMapping( attsub);
                            if (mapping != null) {
                                Attribute a = mapping.getAttribute();
                                if (a.appliesTo( DictionaryEntry.AttributeGroup.GENERAL) &&
                                    (!seenROM || 
                                     !a.appliesTo( DictionaryEntry.AttributeGroup.TRANSLATION))) {
                                    generalA.addAttribute( a, mapping.getValue());
                                }
                                else if (a.appliesTo( DictionaryEntry.AttributeGroup.WORD)) {
                                    wordA.addAttribute( a, mapping.getValue());
                                }
                                else if (a.appliesTo( DictionaryEntry.AttributeGroup.TRANSLATION)) {
                                    if (seenROM)
                                        translationromA.addAttribute( a, mapping.getValue());
                                    else
                                        translationA.addAttribute( a, mapping.getValue());
                                }
                                else {
                                    // should not happen, edict does not support READING attributes
                                    System.err.println( "EDICT warning: illegal attribute type");
                                }
                            }
                            else {
                                // Not a recognized attribute. Since the whole bracket expression
                                // will be cut off from the translation, store it seperately and
                                // prepend it.
                                if (unrecognized == null)
                                    unrecognized = new StringBuffer();
                                if (!hasUnrecognized) {
                                    unrecognized.append( '(');
                                    hasUnrecognized = true;
                                }
                                unrecognized.append( attsub);
                            }
                            
                            startc = endc + 1;
                        } while (startc < att.length());
                        if (hasUnrecognized)
                            unrecognized.append( ") ");
                    }
                }

                if (matchend > 0)
                    translation = translation.substring( matchend, translation.length());
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
    
    public String toString() {
        return "EDICT " + getName();
    }

    /**
     * Escape LF/CR, '/' and all characters except ASCII, kanji and kana.
     */
    protected boolean escapeChar( char c) {
        // some special characters need escaping
        if (c==10 || c==13 || c=='/')
            return true;

        // ASCII and Kanji/Kana characters don't need escaping
        // (BUG: The range covered may be too large)
        if (c<128 || c>=0x2e80 && c<0xa000)
            return false;

        // escape all other characters
        return true;
    }
} // class EDict
