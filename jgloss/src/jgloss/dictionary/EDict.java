/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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
import java.util.*;

/**
 * Dictionary implementation for dictionaries in Edict format with
 * associated xjdx index.
 *
 * @author Michael Koch
 */
public class EDict implements Dictionary {
    /**
     * Path to the dictionary file.
     */
    private String dicfile;
    /**
     * Name of the dictionary. This will be the filename of the dictionary file.
     */
    private String name;
    /**
     * Array containing the content of the dictionary file.
     */
    private byte[] dictionary;
    /**
     * Array containing the content of the xjdx file associated with the dictionary file.
     */
    private int[] index;

    /**
     * Creates a new dictionary from a dictionary file in EDict format and the associated
     * xjdx index file.
     *
     * @param dicfile Path to a file in EDict format.
     * @param indexfile Path to the associated index file in XJDX format.
     * @exception IOException if the dictionary or the index file cannot be read.
     */
    public EDict( String dicfile, String indexfile) throws IOException {
        this.dicfile = dicfile;
        File f = new File( dicfile);
        name = f.getName();
        dictionary = new byte[(int) f.length()];
        InputStream is = new FileInputStream( f);
        int off = 0;
        int len = dictionary.length;
        int read = 0;
        while (read!=-1 && len>0) {
            read = is.read( dictionary, off, len); // this call might not read all data at once
            off += read;
            len -= read;
        }
        is.close();
      
        /* The xjdx files are created by a platform-dependent C program. They
           consist of an array of unsigned longs with indexes into the EDict file.
           On an ix86 box, the format of an array element is 4-byte
           least significant byte first. The following code tries to detect
           endianness by assuming that the MSB is always zero. This works as
           long as the dictionary size does not exceed 2^24 bytes.
           The first entry in the xjdx file contains the size of the dictionary
           file plus the version number of the index format.
        */
        // Test the first 4 entries. On a LSB first architecture it is very impropable that all 4 entries
        // are multiples of 256 and thus have a first byte of 0.
        RandomAccessFile r = new RandomAccessFile( indexfile, "r");
        byte b[] = new byte[16];
        r.readFully( b);
        boolean msbFirst = (b[0]==0) && (b[4]==0) && (b[8]==0) && (b[12]==0);
        r.close();

        // read the index
        f = new File( indexfile);
        index = new int[(int) f.length()/4-1];
        is = new FileInputStream( indexfile);
        byte[] ib = new byte[4];
        int[] ii = new int[4];
        for ( int i=0; i<index.length; i++) {
            is.read( ib);
            for ( int j=0; j<4; j++)
                ii[j] = (ib[j]>=0) ? ib[j] : 256 + ib[j];
            
            if (msbFirst)
                index[i] = ii[0]<<24 | ii[1]<<16 | ii[2]<<8 | ii[3];
            else
                index[i] = ii[3]<<24 | ii[2]<<16 | ii[1]<<8 | ii[0];
        }
        is.close();
    }

    /**
     * Searches for entries in the dictionary.
     *
     * @param expression The string to search for.
     * @param mode The search mode. One of <CODE>SEARCH_EXACT_MATCHES, SEARCH_STARTS_WITH,
     *             SEARCH_ENDS_WITH</CODE> or <CODE>SEARCH_ANY_MATCHES</CODE>.
     * @return A list of dictionary entries which match the expression given the search modes.
     *         Items in the list are instances of 
     *         <CODE>DictionaryEntry</CODE>. If no match is found, the empty list will be returned.
     * @exception SearchException if there was an error during the search.
     * @see Dictionary
     * @see DictionaryEntry
     */
    public List search( String expression, short mode) throws SearchException {
        List result = new LinkedList();

        // do a binary search through the index file
        try {
            byte[] expr_euc = expression.getBytes( "EUC-JP");
            byte[] euc = expression.getBytes( "EUC-JP"); // EDict uses EUC. We only need the length
                                                         // and not the data of the array.

            // do a binary search
            int from = 0;
            int to = index.length-1;
            int match = -1;
            int curr;
            // search matching entry
            do {
                curr = (to-from)/2 + from;

                // read entry
                for ( int i=0; i<euc.length; i++) try {
                    euc[i] = dictionary[index[curr]-1+i];
                } catch (ArrayIndexOutOfBoundsException ex) { // end of dictionary
                    euc[i] = 0;
                }
                // A simple compareToIgnoreCase does not work, because Kstrcmp, which was used
                // by the index creation program behaves somewhat differently.
                // It also does not have the overhead of creating a new string for the search.
                int c = Kstrcmp( expr_euc, euc);
                if (c > 0)
                    from = curr+1;
                else if (c < 0)
                    to = curr-1;
                else
                    match = curr;
            } while (from<=to && match==-1);

            if (match != -1) {
                // search backwards for the first matching entry
                curr = match - 1;
                while (curr >= from) {
                    // read entry
                    for ( int i=0; i<euc.length; i++) try {
                        euc[i] = dictionary[index[curr]-1+i];
                    } catch (ArrayIndexOutOfBoundsException ex) { // end of dictionary
                        euc[i] = 0;
                    }
                    int c = Kstrcmp( expr_euc, euc);
                    if (c != 0)
                        curr = from - 1; // First non-matching entry. End search.
                    else {
                        match = curr;
                        curr--;
                    }
                }

                // read all matching entries
                do {
                    // find beginning of entry line
                    int i = index[match]-1;
                    while (i>0 && dictionary[i-1]!=0x0a)
                        i--;
                    
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while (i<dictionary.length && dictionary[i]!=0x0a) {
                        bos.write( dictionary[i]);
                        i++;
                    }
                    String entry = bos.toString( "EUC-JP").trim();
                    if (entry.indexOf( expression) == -1)
                        match = index.length; // first non-matching entry: end search
                    else {
                        match++;
                        int j, k;
                        // word:
                        String word;
                        i = entry.indexOf( ' ');
                        if (i == -1) {
                            System.err.println( "WARNING: " + dicfile +
                                                "\nMalformed dictionary entry: " + entry);
                            word = "";
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
                                reading = null;
                            }
                            else
                                reading = entry.substring( i+1, j);
                        } // else: no reading
                        // translations
                        List translationlist = new ArrayList( 10);
                        i = entry.indexOf( '/', i);
                        if (i == -1) {
                            System.err.println( "WARNING: " + dicfile +
                                                "\nMalformed dictionary entry: " + entry);
                        }
                        else {
                            j = entry.lastIndexOf( '/');
                            while (i < j) {
                                k = entry.indexOf( '/', i+1);
                                translationlist.add( entry.substring( i+1, k));
                                i = k;
                            }
                        }
                        String[] translation = new String[translationlist.size()];
                        translation = (String[]) translationlist.toArray( translation);
                        
                        // test if this entry matches the search mode
                        boolean addEntry = false;
                        switch (mode) {
                        case SEARCH_EXACT_MATCHES:
                            if (expression.equalsIgnoreCase( word) ||
                                expression.equalsIgnoreCase( reading)) {
                                addEntry = true;
                            }
                            else {
                                for ( i=0; i<translation.length; i++) {
                                    if (expression.equalsIgnoreCase( translation[i])) {
                                        addEntry = true;
                                        break;
                                    }
                                }
                            }
                            break;

                        case SEARCH_STARTS_WITH:
                            if (word.startsWith( expression) ||
                                reading!=null && reading.startsWith( expression)) {
                                addEntry = true;
                            }
                            else {
                                for ( i=0; i<translation.length; i++) {
                                    if (translation[i].startsWith( expression)) {
                                        addEntry = true;
                                        break;
                                    }
                                }
                            }
                            break;

                        case SEARCH_ENDS_WITH:
                            if (word.endsWith( expression) ||
                                reading!=null && reading.endsWith( expression)) {
                                addEntry = true;
                            }
                            else {
                                for ( i=0; i<translation.length; i++) {
                                    if (translation[i].endsWith( expression)) {
                                        addEntry = true;
                                        break;
                                    }
                                }
                            }
                            break;

                        case SEARCH_ANY_MATCHES:
                            addEntry = true;
                            break;
                        default:
                            throw new IllegalArgumentException( "Invalid search mode");
                        }
                        
                        if (addEntry)
                            result.add( new DictionaryEntry( word, reading, translation, this));
                    }
                } while (match < index.length);
            }
        } catch (IOException ex) {
            throw new SearchException( "IOException: " + ex.getMessage());
        }

        return result;
    }

    /**
     * Lexicographic comparison of two Strings encoded as EUC-JP byte-arrays. 
     * Adapted from Jim Breen's xjdic.
     * Effectively does a equalsIgnoreCase on two "strings" within the dictionary,
     * except it will make katakana and hirgana match (EUC A4 & A5).
     *
     * @param str1 A string encoded as EUC-JP.
     * @param str2 A string encoded as EUC-JP.
     */
    private int Kstrcmp( byte[] str1, byte[] str2) {
        int c1 = 0, c2 = 0;

        for ( int i = 0; i<Math.min( str1.length, str2.length); i++) {
            c1 = (str1[i]>=0) ? str1[i] : 256 + str1[i];
            c2 = (str2[i]>=0) ? str2[i] : 256 + str2[i];
            if ((i % 2) == 0) {
                if (c1 == 0xA5) {
                    c1 = 0xA4;
                }
                if (c2 == 0xA5) {
                    c2 = 0xA4;
                }
            }
            if ((c1 >= 'A') && (c1 <= 'Z')) c1 |= 0x20;
            if ((c2 >= 'A') && (c2 <= 'Z')) c2 |= 0x20;
            if (c1 != c2 ) break;
        }

        if (c1 == c2)
            return (str2.length - str1.length);
        
        return(c1-c2);
    }

    /**
     * Returns the path to the dictionary file.
     *
     * @return The path to the dictionary file.
     */
    public String getDictionaryFile() {
        return dicfile;
    }

    /**
     * Returns the name of this dictionary. This is the filename of the dictionary file.
     *
     * @return The name of this dictionary.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this dictionary. This will be the name of is.
     *
     * @return A string representation of this dictionary.
     */
    public String toString() {
        return name;
    }
} // class Edict
