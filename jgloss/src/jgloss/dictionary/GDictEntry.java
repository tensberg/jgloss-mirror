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

import java.util.List;
import java.util.Iterator;

/**
 * Entry in a GDICT-formatted dictionary. GDICT has more verbose information than EDICT and the
 * {@link DictionaryEntry DictionaryEntry} class support.
 *
 * @author Michael Koch
 */
public class GDictEntry extends DefaultDictionaryEntry {
    /**
     * Creates a dictionary entry which uses data from the <code>GDictEntry</code> and
     * returns a specified alternative as word.
     */
    protected class DictionaryEntryWrapper implements DictionaryEntry {
        /**
         * Stores the word of the entry. Storing a string reference should be more
         * memory efficient than storing two ints (word/alternative).
         */
        protected String word;

        public DictionaryEntryWrapper( int word, int alternative) {
            this.word = words[word][alternative];
        }

        public Dictionary getDictionary() { return dictionary; }

        public String getWord() { return word; }

        public String getReading() { return reading; }
        public List getTranslations() { return translations; }

        public String toString() { return DefaultDictionaryEntry.toString( this); }
    } // class DictionaryEntryWrapper

    /**
     * Array of words with alternative writings. All words and alternative writings have the same
     * readings. A gdict word entry of the form "word1 (alternative1); word2 (alternative2)" is stored
     * as <code>{ {"word1","alternative1"} {"word2","alternative2"} }</code>.
     */
    protected String[][] words;
    /**
     * Marks where distinct ranges of meaning end in the translation array. If there is only
     * one range of meaning for all translations, <code>rangesOfMeaning</code> has length 0,
     * otherwise the <code>rangesOfMeaning[i]<code> is the first index of the i+2-th range of meaning
     * int the <code>translations</code> array
     */
    protected int[] rangesOfMeaning;
    /**
     * Part of speech of this entry.
     */
    protected String partOfSpeech;
    protected String comment;
    protected String reference;

    /**
     * Creates a new dictionary entry.
     *
     * @param wordlist List of lists which each hold a word with alternative spellings.
     * @param reading The reading of the words. May be <code>null</code>.
     * @param translations List of translations.
     * @param rangesOfMeaning Indexes in the translation list for the start of new ranges of meaning
     *        for translations. The beginning index of the second range of meaning is stored in
     *        <code>rangesOfMeaning[0]</code>. If there is only one range of meaning, <code>null</code>
     *        may be passed.
     * @param partOfSpeech Part of speech of this entry. May be <code>null</code>.
     * @param comment An explanation of the entry. May be <code>null</code>.
     * @param reference A reference to other entries. May be <code>null</code>.
     * @param dictionary Dictionary which contains this entry.
     */
    public GDictEntry( List wordlist, String reading, List translations, int[] rangesOfMeaning,
                       String partOfSpeech, String comment, String reference, 
                       Dictionary dictionary) {
        super( null, reading, translations, dictionary);
        words = new String[wordlist.size()][];
        int c = 0;
        for ( Iterator i=wordlist.iterator(); i.hasNext(); ) {
            List alternatives = (List) i.next();
            words[c++] = (String[]) alternatives.toArray( new String[alternatives.size()]);
        }
        if (rangesOfMeaning == null)
            this.rangesOfMeaning = new int[0];
        else
            this.rangesOfMeaning = rangesOfMeaning;
        this.partOfSpeech = partOfSpeech;
        this.comment = comment;
        this.reference = reference;
    }

    /**
     * Returns the standard spelling of the first word.
     */
    public String getWord() { return getWord( 0, 0); }

    /**
     * Returns the standard spelling of word <code>i</code>. The first word is at i=0.
     */
    public String getWord( int i) {
        return getWord( i, 0);
    }

    /**
     * Returns an alternative spelling of word <code>i</code>.
     *
     * @param i Index of the word. i=0 is the first word.
     * @param alternative Index of the alternative spelling. 0 is the word in its standard spelling,
     *                    1 is the first alternative.
     */
    public String getWord( int i, int alternative) {
        return words[i][alternative];
    }

    /**
     * Returns the number of words without counting alternative spellings.
     */
    public int getWordCount() { return words.length; }

    /**
     * Returns the number of alternative spellings of a word. For a word dictionary entry of the form
     * "word1 (alternative1)" this returns 1.
     */
    public int getAlternativesCount( int i) { return words[i].length-1; }

    /**
     * Returns the list of translations with a distinguished range of meaning.
     *
     * @param rangeOfMeaning Index of the meaning wanted.
     */
    public List getTranslations( int rangeOfMeaning) {
        int from = 0;
        if (rangeOfMeaning >= 1)
            from = rangesOfMeaning[rangeOfMeaning-1];

        int to = translations.size();
        if (rangesOfMeaning.length > rangeOfMeaning)
            to = rangesOfMeaning[rangeOfMeaning];

        return translations.subList( from, to);
    }            

    /**
     * Returns the number of distinct range of meanings of translations in this entry.
     */
    public int getRangesOfMeaningCount() { return rangesOfMeaning.length + 1; }

    public String getPartOfSpeech() { return partOfSpeech; }
    public String getComment() { return comment; }
    public String getReference() { return reference; }

    /**
     * Returns a dictionary entry which returns the specified word/alternative and the reading
     * and translations of the <code>GDictEntry</code>. If <code>word==0</code> and 
     * <code>alternative==0</code>, the <code>GDictEntry</code> itself is returned.
     */
    public DictionaryEntry getDictionaryEntry( int word, int alternative) {
        if (word==0 && alternative==0)
            return this;
        else
            return new DictionaryEntryWrapper( word, alternative);
    }

    public String toString() {
        StringBuffer out = new StringBuffer( 128);
        // add words
        for ( int i=0; i<words.length; i++) {
            out.append( words[i][0]);
            if (words[i].length > 1) {
                // add alternative spellings
                out.append( " \uff08");
                for ( int j=1; j<words[i].length; j++) {
                    out.append( words[i][j]);
                    if (j+1 < words[i].length)
                        out.append( "; ");
                }
                out.append( '\uff09');
            }
            if (i+1 < words.length)
                out.append( "; ");
        }

        if (reading!=null && reading.length()>0) {
            out.append( " \uff3b");
            out.append( reading);
            out.append( '\uff3d');
        }

        out.append( ' ');
        for ( int i=0; i<getRangesOfMeaningCount(); i++) {
            if (getRangesOfMeaningCount() > 1) {
                out.append( "[");
                out.append( i+1);
                out.append( "] ");
            }

            for ( Iterator j=getTranslations( i).iterator(); j.hasNext(); ) {
                out.append( j.next().toString());
                if (j.hasNext())
                    out.append( "; ");
            }
            out.append( '.');
            if (i+1 < getRangesOfMeaningCount())
                out.append( ' ');
        }

        return out.toString();
    }
} // class GDictEntry
