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

import java.io.*;
import java.nio.BufferUnderflowException;
import java.util.List;
import java.util.regex.*;

/**
 * Implementation for dictionaries in GDict format. GDict is a Japanese-German dictionary
 * available from <a href="http://www.bibiko.com/dlde.htm">http://www.bibiko.com/dlde.htm</a>.
 *
 * @author Michael Koch
 */
public class GDict extends FileBasedDictionary {
    public static void main( String[] args) throws Exception {
        /*GDict g = new GDict( new File( "/home/michael/japan/dictionaries/gdict/gdictutf.txt"), true);
          g.search( "\u6f22\u5b57", SEARCH_ANY_MATCHES);*/
        test( WORD_PATTERN, "word1");
        test( WORD_PATTERN, "word1 (alternative1)");
        test( WORD_PATTERN, "word1; word2");
        test( WORD_PATTERN, "word1 (alternative1; alternative2)");
        test( WORD_PATTERN, "word1 (alternative1); word2");
        test( WORD_PATTERN, "word1 (alternative1); word2 (alternative2)");
        test( WORD_PATTERN, "word1 (alternative1; alternative3); word2 (alternative2)");
        test( WORD_PATTERN, "word1 (alternative1; alternative3); word2 (alternative2; alternative4)");
    }

    private static void test( Pattern p, String test) throws Exception {
        System.err.println( "Testing " + test);
        Matcher m = p.matcher( test);
        int c = m.groupCount();
        //System.err.println( c + " groups");
        while (m.find()) {
            System.err.println( "Found one");
            for ( int i=1; i<=c; i++)
                System.err.println( m.group( i));
        }
    }

    /**
     * Name of the dictionary format.
     */
    public static final String FORMAT_NAME = "GDICT";
    /**
     * Encoding used by dictionary instances.
     */
    public static final String ENCODING = "UTF-8";

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
     * which recognizes UTF-8 encoded GDICT dictionaries. Used to initialize the
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
                ( FORMAT_NAME, ENCODING, Pattern.compile
                  ( "\\A([^\\|]*\\|){3,}[^\\|]*$", Pattern.MULTILINE),
                  1.0f, 1024, GDict.class.getConstructor( new Class[] { File.class }));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected static Pattern WORD_PATTERN = Pattern.compile
        ( "([^(;\\[]+)(?:\\s\\[\\d+\\])?(?:\\s\\((.+?)\\))?(?:;\\s)?");
    protected static Pattern SEMICOLON_PATTERN = Pattern.compile
        ( "((?:[^(]+?(?:\\(.*?\\))?)+)(?:;\\s)?");

    public GDict( File dicfile) throws IOException {
        this( dicfile, true);
    }

    public GDict( File dicfile, boolean createindex) throws IOException {
        super( dicfile, createindex);
    }

    public String getEncoding() { return "UTF-8"; }

    protected boolean isEntryStart( int offset) {
        byte b = dictionary.get( offset-1);
        return (b==';' || b==' ' || b=='|' || b==10 || b==13);
    }
    
    protected boolean isEntryEnd( int offset) {
        byte b = dictionary.get( offset);
        return (b==';' || b=='|' || b==10 || b==13);
    }

    protected void parseEntry( List result, String entry) {
        System.err.println( entry);

        try {
            int start = 0;
            int end = entry.indexOf( '|');
            String words = entry.substring( start, end);
            System.out.println( words);

            start = end+1;
            end = entry.indexOf( '|', start);
            String readings = entry.substring( start, end);
            System.out.println( readings);

            // skip part of speech
            start = end+1;
            end = entry.indexOf( '|', start);

            // translations
            start = end+1;
            end = entry.indexOf( '|', start);
            String translations = entry.substring( start, end);
            System.out.println( translations);
        } catch (StringIndexOutOfBoundsException ex) {
            System.err.println( "GDICT warning: malformed dictionary entry \"" + entry + "\"");
        }
    }

    private final static int INDEX_FIRST_LINE = 0;
    private final static int INDEX_IN_WORD = 1;
    private final static int INDEX_IN_READING = 2;
    private final static int INDEX_IN_TRANSLATION = 3;

    /**
     * Character position type used during index creation.
     */
    private int indexPosition = INDEX_FIRST_LINE;

    protected int readNextCharacter( boolean inWord) throws BufferUnderflowException {
        // skip the first line in the dictionary since it is a description of the index format and
        // not an entry
        if (indexPosition == INDEX_FIRST_LINE) {
            byte c;
            do {
                c = dictionary.get();
            } while (!isEntrySeparator( c)); // search end of line
            indexPosition = INDEX_IN_WORD;
        }

        byte b = dictionary.get();
        int c;
        // the dictionary is UTF-8 encoded; if the highest bit is set, more than one byte must be read
        if ((b&0x80) == 0) {
            // ASCII character
            c = b;
        }
        else if ((b&0xe0) == 0xc0) { // 2-byte encoded char
            byte b2 = dictionary.get();
            if ((b2&0xc0) == 0x80) // valid second byte
                c = ((b&0x1f) << 6) | (b&0x3f);
            else {
                System.err.println( "GDICT warning: invalid 2-byte character");
                c = '?';
            }
        }
        else if ((b&0xf0) == 0xe0) { // 3-byte encoded char
            byte b2 = dictionary.get();
            byte b3 = dictionary.get();
            if (((b2&0xc0) == 0x80) &&
                ((b3&0xc0) == 0x80)) { // valid second and third byte
                c = ((b&0x0f) << 12) | ((b2&0x3f) << 6) | (b3&0x3f);
            }
            else {
                c = '?';
                System.err.println( "GDICT warning: invalid 3-byte character");
            }
        }
        else { // 4-6 byte encoded char or invalid char
            System.err.println( "GDICT warning: invalid character");
            c = '?';
        }

        // adjust index position
        if (c == '|') {
            switch (indexPosition) {
            case INDEX_IN_WORD:
                indexPosition = INDEX_IN_READING;
                break;
                
            case INDEX_IN_READING:
                // skip over part of speech
                do {
                    c = dictionary.get();
                } while (c!='|' && !isEntrySeparator( (byte) c));
                indexPosition = INDEX_IN_TRANSLATION;
                break;
            
            case INDEX_IN_TRANSLATION:
                // skip to next entry
                do {
                    c = dictionary.get();
                } while (!isEntrySeparator( (byte) c));
                // indexPosition will be adjusted in the following if statement
                break;
            }
        }

        // adjust for new entry
        if (isEntrySeparator( (byte) c)) // note that c might have been changed while skipping 
                                         // in previous switch block
            indexPosition = INDEX_IN_WORD;

        if (c>=0x4e00 && c<0xa000)
            return 0; // kanji
        else if (c>=3000 && c<3100)
            return 1; // katakana, hiragana
        else if (Character.isLetterOrDigit( (char) c)) // any other characters
            return 3;
        else
            return -1; // not in word
    }

    public String toString() {
        return FORMAT_NAME + " " + getName();
    }
} // class GDict
