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
import java.nio.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Dictionary implementation for dictionaries in EDICT format 
 * based on {@link FileBasedDictionary} and using the J2 1.4 NIO API. For a documentation of the format see
 * <a href="http://www.csse.monash.edu.au/~jwb/edict_doc.html">
 * http://www.csse.monash.edu.au/~jwb/edict_doc.html</a>.
 *
 * @author Michael Koch
 */
public class EDictNIO extends FileBasedDictionary {
    public static void main( String[] args) throws Exception {
        EDictNIO dict = new EDictNIO( new File( "/usr/share/edict/edict"), false);
        BufferedReader in = new BufferedReader( new InputStreamReader
            ( new FileInputStream( "/usr/share/edict/edict"), "EUC-JP"));
        BufferedWriter out = new BufferedWriter( new OutputStreamWriter
            ( new FileOutputStream( "/home/michael/japan/dictionaries/edict.gdt"), "UTF-8"));
        out.write( "Japanisch|Lesung|Wortart|Deutsch|Kommentar|Eintragsebene\n");
        String line;
        while ((line = in.readLine()) != null) {
            List l = new java.util.ArrayList( 1);
            dict.parseEntry( l, line, 0, 0, null, null, (short) 0, (short) 0);
            DictionaryEntry e = (DictionaryEntry) l.get( 0);
            StringBuffer entry = new StringBuffer( 128);
            entry.append( e.getWord());
            entry.append( '|');
            if (e.getReading() != null)
                entry.append( e.getReading());
            entry.append( "||");
            for ( java.util.Iterator i=e.getTranslations().iterator(); i.hasNext(); ) {
                entry.append( i.next().toString());
                if (i.hasNext())
                    entry.append( "; ");
            }
            entry.append( ".||\n");
            out.write( entry.toString());
        }
        in.close();
        out.close();
        System.exit( 0);

        //EDict eo = new EDict( "/home/michael/testdic", true);
        //EDictNIO en = new EDictNIO( new File( "/home/michael/testdic"), true);
        EDictNIO en = new EDictNIO( new File( "/home/michael/japan/dictionaries/edict"), true);
        EDict eo = new EDict( "/home/michael/japan/dictionaries/edict2", true);
        //EDictNIO en = new EDictNIO( new File( "/usr/share/edict/edict"), true);
        //EDict eo = new EDict( "/usr/share/edict/edict", true);
        //test( "regular (stops at every station) Jouetsu-line shinkansen", en, eo, SEARCH_ANY_MATCHES);
        
        /*Matcher word = Pattern.compile( "^(\\S+)").matcher( "");
        Matcher reading = Pattern.compile( "\\[(.*?)\\]").matcher( "");
        Matcher translation = Pattern.compile( "/(.*?)/").matcher( "");
        String line;

        BufferedReader in = new BufferedReader( new InputStreamReader
            ( new FileInputStream( "/home/michael/annotations"), "EUC-JP"));
        while ((line=in.readLine()) != null) {
            word.reset( line);
            word.find();
            test( word.group( 1), en, eo);

            reading.reset( line);
            if (reading.find())
                test( reading.group( 1), en, eo);

            translation.reset( line);
            while (translation.find())
                test( translation.group( 1), en, eo);
        }
        System.err.println( "Time (NIO): " + (t1/1000.0));
        System.err.println( "Time (old): " + (t2/1000.0));*/
    }

    private final static void test( String word, Dictionary d1, Dictionary d2) 
        throws SearchException {
        test( word, d1, d2, SEARCH_EXACT_MATCHES);
        test( word, d1, d2, SEARCH_STARTS_WITH);
        test( word, d1, d2, SEARCH_ENDS_WITH);
        test( word, d1, d2, SEARCH_ANY_MATCHES);
    }

    private static long t1 = 0;
    private static long t2 = 0;

    private final static void test( String word, Dictionary d1, Dictionary d2, short mode) 
        throws SearchException {
        long t = System.currentTimeMillis();
        List r1 = d1.search( word, mode, RESULT_DICTIONARY_ENTRIES);
        t1 += System.currentTimeMillis() - t;
        t = System.currentTimeMillis();
        List r2 = d2.search( word, mode, RESULT_DICTIONARY_ENTRIES);
        t2 += System.currentTimeMillis() - t;
        //System.err.println( "Results: " + r1.size() + "/" + r2.size());
        java.util.Set s1 = new java.util.HashSet( r1.size()*2+1);
        for ( java.util.Iterator i=r1.iterator(); i.hasNext(); ) {
            s1.add( i.next().toString());
        }

        java.util.Set s2 = new java.util.HashSet( r2.size()*2+1);
        for ( java.util.Iterator i=r2.iterator(); i.hasNext(); ) {
            s2.add( i.next().toString());
        }

        boolean testPassed = true;
        boolean printHeader = true;
        for ( java.util.Iterator i=r2.iterator(); i.hasNext(); ) {
            String next = i.next().toString();
            if (!s1.contains( next)) {
                if (printHeader) {
                    printHeader = false;
                    System.err.println( "Results in r2 but not in r1:");
                }
                System.err.println( next);
                testPassed = false;
            }
        }
        printHeader = true;
        for ( java.util.Iterator i=r1.iterator(); i.hasNext(); ) {
            String next = i.next().toString();
            if (!s2.contains( next)) {
                if (printHeader) {
                    System.err.println( "Results in r1 but not in r2:");
                    printHeader = false;
                }
                System.err.println( next);
                testPassed = false;
            }
        }
        
        if (!testPassed) {
            System.out.println( "Test failed for \"" + word + "\", search mode " + mode);
            System.out.println( "Dictionary 1 results: " + r1.size());
            for ( java.util.Iterator i=r1.iterator(); i.hasNext(); ) {
                System.out.println( i.next());
            }
            System.out.println( "Dictionary 2 results: " + r2.size());
            for ( java.util.Iterator i=r2.iterator(); i.hasNext(); ) {
                System.out.println( i.next());
            }
            System.exit( 1);
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
                ( "EDICT", "EUC-JP", Pattern.compile
                  ( "\\A\\S+?(\\s\\[.+?\\])?\\s/\\P{InCJKUnifiedIdeographs}.*/$", Pattern.MULTILINE),
                  1.0f, 1024, EDictNIO.class.getConstructor( new Class[] { File.class }));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public EDictNIO( File dicfile) throws IOException {
        this( dicfile, true);
    }

    public EDictNIO( File dicfile, boolean createindex) throws IOException {
        super( dicfile, createindex);
    }

    public String getEncoding() { return "EUC_JP"; }

    protected boolean isEntryStart( int offset) {
        byte b = dictionary.get( offset-1);
        return (b=='[' || b=='/' || b==10 || b==13);
    }
    
    protected boolean isEntryEnd( int offset) {
        byte b = dictionary.get( offset);
        if (b==']' || b=='/' || b==10 || b==13)
            return true;
        else if (b == ' ') { // end of word if followed by [ or /
            b = dictionary.get( offset+1);
            return (b=='[' || b=='/');
        }
        else
            return false;
    }

    /**
     * Parses an EDICT formatted entry. The format is
     * <CODE>word [reading] /translation 1/translation2/...</CODE> with the reading
     * being optional.
     */
    protected void parseEntry( List result, String entry, int entrystart, int where, String expression,
                               byte[] exprBytes, short searchmode, short resultmode) {
        int j, k;
        // word:
        String word;
        try {
            int i = entry.indexOf( ' ');
            word = entry.substring( 0, i);

            // reading:
            String reading = null;
            i = entry.indexOf( '[');
            if (i != -1) {
                j = entry.indexOf( ']', i+1);
                reading = entry.substring( i+1, j);
            } // else: no reading
        
            // translations
            i = entry.indexOf( '/', i);
            // count number of translations
            int slashes = 1;
            for ( int x=i+1; x<entry.length(); x++)
                if (entry.charAt( x)=='/')
                    slashes++;
            String[] translation = new String[slashes-1];
            slashes = 0;
            j = entry.lastIndexOf( '/');
            while (i < j) {
                k = entry.indexOf( '/', i+1);
                translation[slashes++] = entry.substring( i+1, k);
                i = k;
            }
            result.add( new DefaultDictionaryEntry( word, reading, translation, this));
        } catch (StringIndexOutOfBoundsException ex) {
            System.err.println( "EDICT warning: " + dicfile +
                                "\nMalformed dictionary entry: " + entry);
        }

    }
    
    protected int readNextCharacter( boolean inWord) {
        int c = byteToUnsignedByte( dictionary.get());
        if (c > 127) { // multibyte character in EUC-JP encoding
            // skip second byte
            dictionary.get();
            if (c >= 0xb0) // kanji
                return 0;
            if (c == 0x8f) { // JIS X 0212 3-Byte kanji {
                dictionary.get(); // skip third byte
                return 0;
            }
            // otherwise kana
            return 1;
        }
        else { // ASCII character
            if (c>='a' && c<='z' ||
                c>='A' && c<='Z' ||
                c>='0' && c<='9' ||
                (inWord && (c=='-' || c=='.')))
                return 3; // word character
            else
                return -1; // not in index word
        }
    }

    protected static class EDictState implements ByteConverterState {
        /**
         * Multibyte char is 1 byte long.
         */
        public final static int ONE_BYTE = 1;
        /**
         * Multibyte char is 2 bytes long.
         */
        public final static int TWO_BYTES = 2;
        /**
         * Multibyte char is 3 bytes long.
         */
        public final static int THREE_BYTES = 3;
        /**
         * Byte in multibyte character
         */
        public int byteInChar;
        /**
         * Number of bytes in multibyte character
         */
        public int type;

        public EDictState() {
            reset();
        }

        public void reset() {
            byteInChar = 1;
            type = ONE_BYTE;
        }
    }

    protected int convertByteInChar( int c, boolean last, ByteConverterState bstate) {
        EDictState state = (EDictState) bstate;
        if (state.byteInChar == 1) { // first byte in multibyte character
            if (c < 128) { // ASCII char
                state.type = EDictState.ONE_BYTE;
                // uppercase -> lowercase conversion
                if ((c >= 'A') && (c <= 'Z')) c |= 0x20;
            }
            else {
                // convert katakana to hiragana
                if (c == 0xA5)
                    c = 0xA4;
                state.type = EDictState.TWO_BYTES;
                if (c == 0x8f) // JIS X 0212 3-Byte Kanji
                    state.type = EDictState.THREE_BYTES;
            }
        }
        if (last) {
            state.byteInChar++;
            if (state.byteInChar > state.type)
                state.byteInChar = 1;
        }

        return c;
    }

    protected ByteConverterState newByteConverterState() {
        return new EDictState();
    }

    public String toString() {
        return "EDICT " + getName();
    }
} // class EDictNIO
