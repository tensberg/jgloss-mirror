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

import jgloss.JGloss;

import java.io.*;
import java.util.*;

/**
 * Dictionary which uses SKK dictionary files to match kanji words to their readings.
 * The <a href="http://openlab.ring.gr.jp/skk/dic-ja.html">SKK (Simple Kana-Kanji converter)</a>
 * dictionaries are usually used to match hiragana readings to kanji words, for example by the
 * <a href="http://member.nifty.ne.jp/Tatari_SAKAMOTO/">skkinput</a> Japanese X input method.
 * This implementation uses it in the other direction.
 */
public class SKKDictionary implements Dictionary {
    public static void main( String args[]) throws Exception {
        System.out.println( "start loading " + args[0]);
        long time = System.currentTimeMillis();
        Dictionary d = new SKKDictionary( args[0]);
        System.out.println( (System.currentTimeMillis()-time)/1000.0);
        time = System.currentTimeMillis();
        List r = d.search( args[1], Dictionary.SEARCH_EXACT_MATCHES);
        System.out.println( (System.currentTimeMillis()-time)/1000.0);
        for ( Iterator i=r.iterator(); i.hasNext(); )
            System.out.println( i.next());
    }

    /**
     * Path to the dictionary file.
     */
    protected String dicfile;
    /**
     * Name of the dictionary. This will be the filename of the dictionary file.
     */
    protected String name;

    /**
     * Map from a word to an entry of list of entries. Contains a key for all kanjis,
     * readings and translations.
     */
    protected Map entries;

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
                            // , where the reading is optional.
                            // An entry in the SKK dictionary has the form
                            // reading /word1/word2/.../
                            // To distinguish between the two formats I test if the
                            // first character after the '/' is ISO-8859-1 or not.
                            if (i!=-1 && i<l.length()-1 && 
                                l.charAt( i+1)=='/' &&  l.charAt( i+2)>255)
                                return getMaxConfidence();
                        }
                    } catch (Exception ex) {}
                    return ZERO_CONFIDENCE;
                }
                
                public float getMaxConfidence() { return 1.0f; }
                
                public Dictionary createInstance( String descriptor) throws IOException {
                    return new SKKDictionary( descriptor);
                }

                public String getName() { return "SKK"; }
            };

    /**
     * Straightforward implementation of the <CODE>WordReadingPair</CODE> interface.
     */
    protected class SKKWordReadingPair implements WordReadingPair {
        protected String word;
        protected String reading;

        /**
         * Creates a new word/reading pair with the given word and reading.
         */
        public SKKWordReadingPair( String word, String reading) {
            this.word = word;
            this.reading = reading;
        }

        public String getWord() { return word; }
        public String getReading() { return reading; }
        public Dictionary getDictionary() { return SKKDictionary.this; }

        /**
         * Tests for equality. o is equal to this object if it is an instance of
         * <CODE>WordReadingPair</CODE> and the word and reading are equal.
         */
        public boolean equals( Object o) {
            return (o instanceof WordReadingPair &&
                    ((WordReadingPair) o).getWord().equals( word) &&
                    ((WordReadingPair) o).getReading().equals( reading));
        }

        /**
         * Returns a string representation of the word/reading pair.
         */
        public String toString() { return word + " (" + reading + ")"; }
    } // class SKKWordReadingPair

    /**
     * Creates a new <CODE>SKKDictionary</CODE> by reading the dictionary entries from
     * the given file.
     *
     * @param dicfile Path to the file containing the dictionary entries.
     * @exception IOException if the dictionary file cannot be read.
     */
    public SKKDictionary( String dicfile) throws IOException {
        entries = new HashMap( 25000);
        this.dicfile = dicfile;
        File dic = new File( dicfile);
        name = dic.getName();
        System.err.println( JGloss.messages.getString( "dictionary.load",
                                                       new String[] { name }));

        BufferedReader in = new BufferedReader( new InputStreamReader
            ( new FileInputStream( dic), "EUC-JP"));
        String line;
        while ((line = in.readLine()) != null) {
            // characters <= 127 are treated as comment
            if (line.length()>0 && line.charAt( 0)>127) {
                int i = line.indexOf( ' ');
                if (i > 0) {
                    String reading;
                    // A reading may end with an ASCII character hinting at the next
                    // character in the word. This entry is skipped.
                    if (line.charAt( i-1) < 128)
                        reading = line.substring( 0, i-1);
                    else
                        reading = line.substring( 0, i);
                    
                    // i+1 must be a '/', j is the '/' following after it
                    i++;
                    int j = line.indexOf( '/', i+1);
                    while (j != -1) {
                        String word = line.substring( i+1, j);
                        // word entries may have a comment, separated by a ';'
                        int k = word.indexOf( ';');
                        if (k != -1)
                            word = word.substring( 0, k);
                        addEntry( word, reading);

                        // move to the next word
                        i = j;
                        j = line.indexOf( '/', i+1);
                    }
                }
                else {
                    System.out.println( "WARNING: malformed dictionary entry " + line);
                }
            }
        }
    }

    /**
     * Adds an entry to the map of entries. If the map does not contain the key,
     * the entry will be put directly, otherwise all entries for this key are stored in
     * a list. If the entry has already been placed in the map for this key, it will be
     * ignored (no duplicates in the list).
     *
     * @param key The key under which to store the entry.
     * @param entry The entry which will be stored.
     */
    protected void addEntry( String key, String entry) {
        Object o = entries.get( key);
        if (entry.equals( o))
            return;
        if (o == null) {
            entries.put( key, entry);
        }
        else if (o instanceof List) {
            List l = (List) o;
            if (!l.contains( entry))
                l.add( entry);
        }
        else {
            List l = new LinkedList();
            l.add( o);
            l.add( entry);
            entries.put( key, l);
        }
    }

    /**
     * Searches for entries in the dictionary. Currently only exact match search is supported,
     * the search mode is ignored. The dictionary only provides lookups from words to readings,
     * not from readings to words.
     *
     * @param expression The string to search for.
     * @param mode The search mode.
     * @return A list of dictionary entries which match the expression given the search modes.
     *         Items in the list are instances of 
     *         <CODE>WordReadingPair</CODE>. If no match is found, the empty list will be returned.
     * @exception SearchException if there was an error during the search.
     * @see Dictionary
     * @see WordReadingPair
     */
    public List search( String expression, short mode) throws SearchException {
        List result = new LinkedList();

        Object o = entries.get( expression);
        if (o != null) {
            if (o instanceof String) // single reading
                result.add( new SKKWordReadingPair( expression, (String) o));
            else { // list of readings
                for ( Iterator i=((List) o).iterator(); i.hasNext(); ) {
                    result.add( new SKKWordReadingPair( expression, (String) i.next()));
                }
            }
        }

        return result;
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
     * Returns a string representation of this dictionary.
     *
     * @return A string representation of this dictionary.
     */
    public String toString() {
        return "SKK " + name;
    }

    public void dispose() {}
} // class SKKDictionary
