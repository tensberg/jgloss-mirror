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

import jgloss.util.StringTools;
import jgloss.dictionary.attribute.*;

import java.io.*;
import java.nio.*;
import java.nio.charset.CharacterCodingException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;

/**
 * Implementation for dictionaries in WadokuJT.txt format. 
 * WadokuJT is a Japanese-German dictionary directed by Ulrich Apel.
 * The WadokuJT.txt file of the dictionary is maintained by Hans-Joerg Bibiko and available
 * from <a href="http://www.bibiko.com/dlde.htm">http://www.bibiko.com/dlde.htm</a>.
 *
 * @author Michael Koch
 */
public class WaDokuJT extends FileBasedDictionary {
    public static void main( String[] args) throws Exception {
        System.err.println( "Creating WadokuJT");
        IndexedDictionary d = new WaDokuJT( new java.io.File( args[0]));
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

        java.util.Iterator r = d.search( ExpressionSearchModes.ANY,
                                         new Object[] { args[1], f });
        System.err.println( "Matches:");
        while (r.hasNext())
            System.err.println( r.next().toString());
        //r.next();
        d.dispose();
    }

    /**
     * Name of the dictionary format.
     */
    public static final String FORMAT_NAME = "WadokuJT";

    protected final static AttributeMapper mapper = initMapper();

    private static AttributeMapper initMapper() {
        try {
            Reader r = new InputStreamReader( EDict.class.getResourceAsStream( "/resources/wadokujt.map"),
                                              "UTF-8");
            AttributeMapper mapper = new AttributeMapper( new LineNumberReader( r));
            r.close();
            return mapper;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
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
     * which recognizes UTF-8 encoded WaDoku dictionaries. Used to initialize the
     * {@link #implementation implementation} final member because the constructor has to
     * be wrapped in a try/catch block.
     * 
     */
    private static DictionaryFactory.Implementation initImplementation() {
        try {
            // Dictionary entries are of the form
            // japanese|reading|part of speech|translation|comment|reference
            // reading,part of speech, comment and reference may be empty.
            // At least four of the fields must be present in the first line of the file for
            // the match to be successful.
            return new FileBasedDictionary.Implementation
                ( FORMAT_NAME, "UTF-8", true, Pattern.compile
                  ( "\\A([^\\|]*\\|){3,}[^\\|]*$", Pattern.MULTILINE),
                  1.0f, 4096, WaDokuJT.class.getConstructor( new Class[] { File.class }));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Matches each word entry with alternatives. The word match is stored in group 1, the
     * alternatives are stores as single string in group 2.
     */
    protected final Pattern WORD_PATTERN = Pattern.compile
        ( "(\\S+)" + // word text
          "(?:(?:\\s\\[\\w+\\])|(?:\\s\\{.+?\\}))*" + // remarks, cross references
          "(?:\\s\\((.+?)\\))?" + // alternative spellings
          "(?:(?:\\s\\[\\w+\\])|(?:\\s\\{.+?\\}))*" + // remarks, cross references
          "(?:;\\s|$)"); // end of word
    protected Matcher WORD_MATCHER = WORD_PATTERN.matcher( "");
    /**
     * Matches semicolon-separated alternatives. The separator is a semicolon followed by a single
     * whitespace. The matched alternative is stored in group 1. Semicolons in brackets are ignored.
     * If an opening bracket is not matched by a closing bracket, everything to the end of the
     * pattern is matched.
     */
    protected final Pattern ALTERNATIVES_PATTERN = Pattern.compile
        ( "((?:[^(\\{]|" + // normal text
          "(?:\\(.*?[)|$])|" + // text in (), ignore "; "
          "(?:\\{.*?[}|$]))+?)" + // text in {}, ignore "; "
          "(?:\\s\\[\\w+\\])?" + // optional comment (ignored)
          "(?:\\s\\{.+?\\})?" + // optional comment (ignored)
          "(?:;\\s|$)"); // separation marker
    protected Matcher ALTERNATIVES_MATCHER = ALTERNATIVES_PATTERN.matcher( "");
    /**
     * Matches translation ranges of meaning. Group 1 contains the number of the range written in 
     * brackets at the beginning of the entry (or <code>null</code> if there is no such number), 
     * group 2 contains a string of all the meanings in the range.
     */
    protected final Pattern TRANSLATIONS_PATTERN = Pattern.compile
        ( "(?:\\[(\\d+)\\]\\s|//\\s)?(.+?)\\.?\\s?(?=\\[\\d+\\]|//|$)");
    protected Matcher TRANSLATIONS_MATCHER = TRANSLATIONS_PATTERN.matcher( "");

    protected final Pattern CATEGORY_PATTERN = Pattern.compile
        ( "(.+?)(?:[,;]\\s?|\\z)");
    protected final Matcher CATEGORY_MATCHER = CATEGORY_PATTERN.matcher( "");

    public WaDokuJT( File dicfile) throws IOException {
        super( dicfile);
    }

    protected EncodedCharacterHandler createCharacterHandler() {
        return new UTF8CharacterHandler();
    }

    protected boolean isFieldStart( ByteBuffer entry, int location, DictionaryEntryField field) {
        if (location == 0)
            return true;

        try {
            byte b = entry.get( location-1);
            if (b==';' || b=='|' || b==10 || b==13)
                return true;
            if (b == ' ') {
                byte b2 = entry.get( location-2);
                return (b2 == ';' || b2 == ']');
            }
            if (b=='(' && field==DictionaryEntryField.WORD)
                // ( followed by a 3-byte encoded character is assumed to be an alternative
                // spelling in the word field
                return true;

            return false;
        } catch (IndexOutOfBoundsException ex) {
            return true;
        }
    }

    protected boolean isFieldEnd( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( location);
            if (b==';' || b=='|' || b==10 || b==13)
                return true;
            if (b == '.') {
                // end of translation if followed by field end marker '|' or new range of meaning " [..."
                byte b2 = entry.get( location+1);
                if (b2 == '|')
                    return true;
                else if (b2 == ' ') 
                    return (entry.get( location+2) == '[');
            }
            else if ((b==' ' || b==')') && 
                     field==DictionaryEntryField.WORD)
                return true;
            return false;
        } catch (IndexOutOfBoundsException ex) {
            return true;
        }
    }

    protected DictionaryEntryField moveToNextField( ByteBuffer buf, int character,
                                                    DictionaryEntryField field) {
        if (field == null) {
            // first call to moveToNextField
            // skip first (comment) line
            while (!isEntrySeparator( buf.get()))
                ; // buf.get() advances the loop
            return DictionaryEntryField.WORD;
        }

        if (character == '|') {
            if (field==DictionaryEntryField.WORD) {
                field = DictionaryEntryField.READING;
            } 
            else if (field==DictionaryEntryField.READING) {
                // skip to translation field
                field = DictionaryEntryField.TRANSLATION;
                byte c;
                do {
                    c = buf.get();
                    if (isEntrySeparator( c)) { // fallback for error in dictionary
                        field = DictionaryEntryField.WORD;
                        break;
                    }
                } while (c != '|');
            } 
            else if (field==DictionaryEntryField.TRANSLATION) {
                // skip fields to next entry
                while (!isEntrySeparator( buf.get()))
                    ; // buf.get() advances the loop
                field = DictionaryEntryField.WORD;
            }
            else
                throw new IllegalArgumentException();
        } else if (character==10 || character==13) {
            // broken dictionary entry; reset for error recovery
            field = DictionaryEntryField.WORD;
        }

        return field;
    }

    protected DictionaryEntryField getFieldType( ByteBuffer buf, int entryStart, int entryEnd,
                                                 int position) {
        // count field delimiters from location to entry start or end (whatever is closer)
        // note: entryEnd is the first position not to be read
        int fields = 0;
        byte c;
        if (position-entryStart <= entryEnd-position-1) {
            // read from start to location
            buf.position( entryStart);
            while (buf.position() <= position) {
                if (buf.get() == '|')
                    fields++;
            }
            switch (fields) {
            case 0:
                return DictionaryEntryField.WORD;
            case 1:
                return DictionaryEntryField.READING;
            case 3:
                return DictionaryEntryField.TRANSLATION;
            default:
                return DictionaryEntryField.OTHER;
            }
        }
        else {
            // read from location to end
            buf.position( position);
            while (buf.position() < entryEnd) {
                if (buf.get() == '|')
                    fields++;
            }
            switch (fields) {
            case 2:
                return DictionaryEntryField.TRANSLATION;
            case 4:
                return DictionaryEntryField.READING;
            case 5:
                return DictionaryEntryField.WORD;
            default:
                return DictionaryEntryField.OTHER;
            }
        }
    }

    protected DictionaryEntry parseEntry( String entry) throws SearchException {
        try {
            DictionaryEntry out = null; 
            List wordlist = new ArrayList( 10);
            String reading;
            List rom = new ArrayList( 10);
            DefaultAttributeSet generalA = new DefaultAttributeSet( null);
            DefaultAttributeSet wordA = new DefaultAttributeSet( generalA);
            List wordsA = new ArrayList( 10);
            DefaultAttributeSet translationA = new DefaultAttributeSet( generalA);
            List romA = new ArrayList( 10);

            int start = 0;
            int end = entry.indexOf( '|');
            String words = entry.substring( start, end);

            start = end+1;
            end = entry.indexOf( '|', start);
            reading = entry.substring( start, end);
            // cut off [n] marker
            int bracket = reading.lastIndexOf( '[');
            if (bracket != -1)
                // the [ must always be preceeded by a single space, therefore bracket-1
                reading = unescape( reading.substring( 0, bracket-1));
            if (reading.length() == 0)
                reading = null;

            // skip part of speech
            start = end+1;
            end = entry.indexOf( '|', start);

            // translations
            start = end+1;
            end = entry.indexOf( '|', start);
            String translations = entry.substring( start, end);

            synchronized (WORD_MATCHER) {
                // split words
                WORD_MATCHER.reset( words);
                while (WORD_MATCHER.find()) {
                    wordlist.add( unescape( WORD_MATCHER.group( 1)));
                    wordsA.add( null);
                    if (WORD_MATCHER.group( 2) != null) {
                        // word with alternatives
                        ALTERNATIVES_MATCHER.reset( WORD_MATCHER.group( 2));
                        while (ALTERNATIVES_MATCHER.find()) {
                            wordlist.add( unescape( ALTERNATIVES_MATCHER.group( 1)));
                            wordsA.add( null);
                        }
                    }
                }
                
                // split translations
                TRANSLATIONS_MATCHER.reset( translations);
                while (TRANSLATIONS_MATCHER.find()) {
                    String crm = TRANSLATIONS_MATCHER.group( 2);
                    // handle categories (marked at beginning of translation enclosed in {})
                    if (crm.charAt( 0) == '{') {
                        int endb = crm.indexOf( '}');
                        // Test if the attributes apply to all translations. This is the case if
                        // they are written before the first ROM marker, or if there are no ROM
                        // markers. In both cases, group(1) is null. If allTranslations is
                        // false, attributes apply only to the current ROM.
                        boolean allTranslations = TRANSLATIONS_MATCHER.group( 1) == null;
                        // attribute strings unrecognized by mapping
                        StringBuffer unrecognized = null;
                        // attributes of this rom, create only if needed
                        DefaultAttributeSet thisRomA = null;

                        CATEGORY_MATCHER.reset( crm.substring( 1, endb));
                        while (CATEGORY_MATCHER.find()) {
                            String cat = CATEGORY_MATCHER.group( 1);
                            AttributeMapper.Mapping mapping = mapper.getMapping( cat);
                            if (mapping != null) {
                                Attribute att = mapping.getAttribute();
                                if (allTranslations) {
                                    if (att.appliesTo
                                        ( DictionaryEntry.AttributeGroup.GENERAL))
                                        generalA.addAttribute( att, mapping.getValue());
                                    else if (att.appliesTo
                                             ( DictionaryEntry.AttributeGroup.TRANSLATION))
                                        translationA.addAttribute( att, mapping.getValue());
                                    else // should not happen
                                        throw new SearchException
                                            ( "wrong attribute type for " + cat);
                                }
                                else {
                                    if (att.appliesTo
                                        ( DictionaryEntry.AttributeGroup.TRANSLATION)) {
                                        if (thisRomA == null) {
                                            thisRomA = new DefaultAttributeSet( translationA);
                                            romA.add( thisRomA);
                                        }
                                        thisRomA.addAttribute( att, mapping.getValue());
                                    }
                                    else if (att.appliesTo
                                             ( DictionaryEntry.AttributeGroup.GENERAL))
                                        generalA.addAttribute( att, mapping.getValue());
                                    else // should not happen
                                        throw new SearchException
                                            ( "wrong attribute type for " + cat);
                                }
                            }
                            else {
                                // unrecognized category
                                if (unrecognized == null) {
                                    unrecognized = new StringBuffer( cat.length() + 128);
                                    unrecognized.append( '{');
                                    unrecognized.append( cat);
                                }
                                else {
                                    unrecognized.append( "; ");
                                    unrecognized.append( cat);
                                }
                            }
                        }

                        // cut off categories
                        if (crm.charAt( endb + 1) == ' ')
                            endb++;
                        crm = crm.substring( endb + 1);
                        if (crm.length() == 0) {
                            // just attributes, no translations
                            continue;
                        }
                        if (unrecognized != null) {
                            // restore unrecognized categories
                            unrecognized.append( "} ");
                            unrecognized.append( crm);
                            crm = unrecognized.toString();
                        }
                    }

                    List crml = new ArrayList( 10);
                    rom.add( crml);
                    ALTERNATIVES_MATCHER.reset( crm);
                    while (ALTERNATIVES_MATCHER.find()) {
                        crml.add( unescape( ALTERNATIVES_MATCHER.group( 1)));
                        if (romA.size() < rom.size()) {
                            // no rom-specific attributes found previously
                            romA.add( null);
                        }
                    }
                }
            }

            if (wordlist.size() == 1) {
                out = new SingleWordEntry( (String) wordlist.get( 0), reading, rom, generalA,
                                           wordA, translationA, romA, this);
            }
            else {
                out = new MultiWordEntry( wordlist, reading, rom, generalA,
                                          wordA, wordsA, translationA, romA, this);
            }

            return out;
        } catch (StringIndexOutOfBoundsException ex) {
            throw new SearchException( "WadokuJT warning: malformed dictionary entry \"" + entry + "\"");
        }
    }

    public String toString() {
        return FORMAT_NAME + " " + getName();
    }

    /**
     * Escape all dictionary special characters.
     */
    protected boolean escapeChar( char c) {
        switch (c) {
        case 10:
        case 13:
        case '|':
        case ';':
            return true;
        }

        return false;
    }
} // class GDict
