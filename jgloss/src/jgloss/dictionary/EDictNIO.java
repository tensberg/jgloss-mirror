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
import java.util.regex.*;

/**
 * Dictionary implementation for dictionaries in EDICT format 
 * based on {@link FileBasedDictionary}. For a documentation of the format see
 * <a href="http://www.csse.monash.edu.au/~jwb/edict_doc.html">
 * http://www.csse.monash.edu.au/~jwb/edict_doc.html</a>.
 *
 * @author Michael Koch
 */
public class EDictNIO extends FileBasedDictionary {
    public static void main( String[] args) throws Exception {
        EDictNIO en = new EDictNIO( new File( "/home/michael/japan/dictionaries/edict"), true);
        //EDict eo = new EDict( "/home/michael/japan/dictionaries/edict", true);
        EDict eo = new EDict( "/usr/share/edict/edict", true);
        //test( "end", en, eo, SEARCH_STARTS_WITH);


        Matcher word = Pattern.compile( "^(\\S+)").matcher( "");
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
        System.err.println( "Time (old): " + (t2/1000.0));
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
        List r1 = d1.search( word, mode);
        t1 += System.currentTimeMillis() - t;
        t = System.currentTimeMillis();
        List r2 = d2.search( word, mode);
        t2 += System.currentTimeMillis() - t;
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
        new DictionaryFactory.Implementation() {
                public float isInstance( String descriptor) {
                    try {
                        BufferedReader r = new BufferedReader( new InputStreamReader( new FileInputStream
                            ( descriptor), "EUC-JP"));
                        String l;
                        int lines = 0;
                        do {
                            l = r.readLine();
                            lines++;
                            // skip empty lines and comments
                        } while (l!=null && (l.length()==0 || l.charAt( 0)<128) && lines<100);
                        r.close();
                        if (l!=null && lines<100) {
                            int i = l.indexOf( ' ');
                            // An entry in EDICT has the form
                            // word [reading] /translation1/translation2/.../
                            // ([reading] is optional).
                            // An entry in the SKK dictionary has the form
                            // reading /word1/word2/.../
                            // To distinguish between the two formats I test if the
                            // first character after the '/' is ISO-8859-1 or not.
                            if (i!=-1 && i<l.length()-2 && 
                                (l.charAt( i+1)=='[' ||
                                 l.charAt( i+1)=='/' && l.charAt( i+2)<256))
                                return getMaxConfidence();
                        }
                    } catch (Exception ex) {}
                    return ZERO_CONFIDENCE;
                }
                
                public float getMaxConfidence() { return 1.0f; }
                
                public Dictionary createInstance( String descriptor) 
                    throws DictionaryFactory.InstantiationException {
                    try {
                        return new EDictNIO( new File( descriptor), true);
                    } catch (IOException ex) {
                        throw new DictionaryFactory.InstantiationException( ex.getLocalizedMessage(), ex);
                    }
                }

                public String getName() { return "EDICT"; }
            };


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
    protected void parseEntry( List result, String entry) {
        int j, k;
        // word:
        String word;
        int i = entry.indexOf( ' ');
        if (i == -1) {
            System.err.println( "WARNING: " + dicfile +
                                "\nMalformed dictionary entry: " + entry);
            return;
        }
        else {
            word = entry.substring( 0, i);
        }
        // reading:
        String reading = null;
        i = entry.indexOf( '[');
        if (i != -1) {
            j = entry.indexOf( ']', i+1);
            if (j == -1) {
                System.err.println( "WARNING: " + dicfile +
                                    "\nMalformed dictionary entry: " + entry);
                return;
            }
            else
                reading = entry.substring( i+1, j);
        } // else: no reading
        
        // translations
        i = entry.indexOf( '/', i);
        if (i == -1) {
            System.err.println( "WARNING: " + dicfile +
                                "\nMalformed dictionary entry: " + entry);
            return;
        }
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
        result.add( new DictionaryEntry( word, reading, translation, this));
    }
    
    protected int readNextCharacter() {
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
                c=='-' || c=='.')
                return 3; // word character
            else
                return -1; // not in index word
        }
    }

    /**
     * Comparison which takes into account the EUC-JP encoding format and
     * converts katakana to hiragana and uppercase characters to lowercase.
     */
    protected int compare( int i1, int i2) {
        int len1 = (int) dictionary.capacity() - i1;
        int len2 = (int) dictionary.capacity() - i2;
        // compare at most 30 bytes
        int length = Math.min( 30, Math.min( len1, len2));

        int b = 1; // byte in multibyte character
        final int ONE_BYTE = 1;
        final int TWO_BYTES = 2;
        final int THREE_BYTES = 3;
        int type = ONE_BYTE;

        for ( int i=0; i<length; i++) {
            int c1 = byteToUnsignedByte( dictionary.get( i1+i));
            int c2 = byteToUnsignedByte( dictionary.get( i2+i));

            if (b == 1) { // first byte in possible multibyte character
                // convert katakana to hiragana
                if (c1 == 0xA5) {
                    c1 = 0xA4;
                }
                if (c2 == 0xA5) {
                    c2 = 0xA4;
                }
                // Determine number of bytes in multibyte char
                // We only need to test the type of c1, because if c1 and c2 are different,
                // the equality test ends.
                if (c1 < 127) {
                    type = ONE_BYTE; // single-byte ASCII
                    // uppercase -> lowercase conversion
                    if ((c1 >= 'A') && (c1 <= 'Z')) c1 |= 0x20;
                    if ((c2 >= 'A') && (c2 <= 'Z')) c2 |= 0x20;
                }
                else {
                    if (c1 == 0x8f) // JIS X 0212 3-Byte Kanji
                        type = THREE_BYTES;
                    else
                        type = TWO_BYTES;
                    b++;
                }
            }
            else // second or third byte in multibyte char
                if (b == type) // last byte in char, reset counter
                    b = 1;
                else 
                    b++; // only reached for 3-byte char

            if (c1 < c2)
                return -1;
            else if (c1 > c2)
                return 1;
        }
        // equal along the length
        if (len1 < len2)
            return -1;
        else if (len1 > len2)
            return 1;
        else
            return 0; // equality
    }

    /**
     * Comparison which takes into account the EUC-JP encoding format and
     * converts katakana to hiragana and uppercase characters to lowercase.
     */
    protected int compare( byte[] str, int off) {
        int b = 1; // byte in multibyte character
        final int ONE_BYTE = 1;
        final int TWO_BYTES = 2;
        final int THREE_BYTES = 3;
        int type = ONE_BYTE;

        dictionary.position( off);
        for ( int i=0; i<str.length; i++) {
            int c1 = byteToUnsignedByte( str[i]);
            int c2;
            try {
                c2 = byteToUnsignedByte( dictionary.get());
            } catch (BufferUnderflowException ex) {
                // end of dictionary file, dictionary string is prefix of str
                return 1;
            }

            if (b == 1) { // first byte in possible multibyte character
                // convert katakana to hiragana
                if (c1 == 0xA5) {
                    c1 = 0xA4;
                }
                if (c2 == 0xA5) {
                    c2 = 0xA4;
                }
                // Determine number of bytes in multibyte char
                // We only need to test the type of c1, because if c1 and c2 are different,
                // the equality test ends.
                if (c1 < 127) {
                    type = ONE_BYTE; // single-byte ASCII
                    // uppercase -> lowercase conversion
                    if ((c1 >= 'A') && (c1 <= 'Z')) c1 |= 0x20;
                    if ((c2 >= 'A') && (c2 <= 'Z')) c2 |= 0x20;
                }
                else {
                    if (c1 == 0x8f) // JIS X 0212 3-Byte Kanji
                        type = THREE_BYTES;
                    else
                        type = TWO_BYTES;
                    b++;
                }
            }
            else // second or third byte in multibyte char
                if (b == type) // last byte in char, reset counter
                    b = 1;
                else
                    b++;

            if (c1 < c2)
                return -1;
            else if (c1 > c2)
                return 1;
        }
        return 0; // equality
    }

    public String toString() {
        return "EDICT " + getName();
    }
} // class EDictNIO
