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

/**
 * Single Entry in a dictionary which supports the <CODE>Dictionary</CODE> interface.
 * Only mappings from a single Japanese word to an array of translations is supported.
 *
 * @author Michael Koch
 * @see Dictionary
 */
public class DictionaryEntry implements WordReadingPair {
    /**
     * The Japanese word of this entry.
     */
    private String word;
    /**
     * The reading of this word. May be <CODE>null</CODE> if this word contains no
     * kanji.
     */
    private String reading;
    /**
     * Array of translations for this entry.
     */
    private String[] translation;
    /**
     * Dictionary which contains this entry.
     */
    private Dictionary dictionary;

    /**
     * Creates a new dictionary entry.
     *
     * @param word The Japanese word of this entry.
     * @param reading The reading of this word. May be <CODE>null</CODE> if this word contains no
     *                kanji.
     * @param translation Array of translations for this entry.
     * @param dictionary Dictionary which contains this entry.
     */
    public DictionaryEntry( String word, String reading, String[] translation, Dictionary dictionary) {
        this.word = word;
        this.reading = reading;
        this.translation = translation;
        this.dictionary = dictionary;
    }

    /**
     * Returns the Japanese word of this entry.
     *
     * @return The Japanese word of this entry.
     */
    public String getWord() { return word; }

    /**
     * Returns the reading of this word.
     *
     * @return The reading of this word. May be <CODE>null</CODE> if this word contains no
     *         kanji.
     */
    public String getReading() { return reading; }
    /**
     * Returns the translations of this entry.
     *
     * @return Array of translations.
     */
    public String[] getTranslations() { return translation; }
    /**
     * Returns the dictionary which contains this entry.
     *
     * @return The dictionary which contains this entry.
     */
    public Dictionary getDictionary() { return dictionary; }

    /**
     * Returns a string representation of this entry.
     *
     * @return A string representation of this entry.
     */
    public String toString() {
        String out = word + " [";
        if (reading != null) 
            out += reading;
        out += "] ";
        for ( int i=0; i<translation.length; i++)
            out += "/" + translation[i];
        out += "/";

        return out;
    }

    /**
     * Tests for equality with another object. The objects equal if the other object
     * is an instance of <CODE>DictionaryEntry</CODE>, has identical word, reading
     * and translations and comes from the same dictionary.
     */
    public boolean equals( Object o) {
        try {
            DictionaryEntry e = (DictionaryEntry) o;
            if (!dictionary.equals( e.dictionary))
                return false;
            if (!word.equals( e.word))
                return false;
            if (reading==null && e.reading!=null)
                return false;
            if (reading!=null && e.reading==null)
                return false;
            if (!reading.equals( e.reading))
                return false;
            if (translation.length != e.translation.length)
                return false;
            for ( int i=0; i<translation.length; i++)
                if (!translation[i].equals( e.translation[i]))
                    return false;

            return true;
        } catch (ClassCastException ex) {
            System.err.println( "classcastexception");
            return false;
        }
    }
} // class Entry
