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
import java.util.List;

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
        EDictNIO g = new EDictNIO( new File( "/home/michael/japan/dictionaries/edict"), true);
        g.search( "\u6f22\u5b57", SEARCH_EXACT_MATCHES);
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
        return (b==' ' || b==']' || b=='/' || b==10 || b==13);
    }

    protected void parseEntry( List result, String entry) {
        System.err.println( entry);

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

    public String toString() {
        return "EDICT " + getName();
    }
} // class EDictNIO
