/*
 * Copyright (C) 2001,2002 Michael Koch (tensberg@gmx.net)
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
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Default implementation of {@link DictionaryEntry DictionaryEntry}.
 * Only mappings from a single Japanese word to an array of translations are supported.
 *
 * @author Michael Koch
 * @see Dictionary
 */
public class DefaultDictionaryEntry implements DictionaryEntry {
    /**
     * The Japanese word of this entry.
     */
    protected String word;
    /**
     * The reading of this word. May be <CODE>null</CODE> if this word contains no
     * kanji.
     */
    protected String reading;
    /**
     * Array of translations for this entry.
     */
    protected List translations;
    /**
     * Dictionary which contains this entry.
     */
    protected Dictionary dictionary;

    /**
     * Creates a new dictionary entry.
     *
     * @param word The Japanese word of this entry.
     * @param reading The reading of this word. May be <CODE>null</CODE> if this word contains no
     *                kanji.
     * @param translation List of translations for this entry. The list is not copied but used as is.
     * @param dictionary Dictionary which contains this entry.
     */
    public DefaultDictionaryEntry( String word, String reading, List translations, 
                                   Dictionary dictionary) {
        this.word = word;
        this.reading = reading;
        this.translations = translations;
        this.dictionary = dictionary;
    }

    /**
     * Creates a new dictionary entry.
     *
     * @param word The Japanese word of this entry.
     * @param reading The reading of this word. May be <CODE>null</CODE> if this word contains no
     *                kanji.
     * @param translation Array of translations for this entry.
     * @param dictionary Dictionary which contains this entry.
     */
    public DefaultDictionaryEntry( String word, String reading, String[] translations, 
                                   Dictionary dictionary) {
        this( word, reading, new ArrayList( translations.length), dictionary);
        for ( int i=0; i<translations.length; i++)
            this.translations.add( translations[i]);
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
    public List getTranslations() { return translations; }
    /**
     * Returns the dictionary which contains this entry.
     *
     * @return The dictionary which contains this entry.
     */
    public Dictionary getDictionary() { return dictionary; }

    /**
     * Returns a string representation of a dictionary entry.
     */
    public static String toString( DictionaryEntry entry) {
        StringBuffer out = new StringBuffer( 128);
        out.append( entry.getWord());
        if (entry.getReading() != null) {
            out.append( " \uff08");
            out.append( entry.getReading());
            out.append( '\uff09');
        }
        out.append( ' ');
        for ( Iterator i=entry.getTranslations().iterator(); i.hasNext(); ) {
            out.append( i.next().toString());
            if (i.hasNext())
                out.append( "; ");
        }

        return out.toString();
    }

    public String toString() {
        return toString( this);
    }

    /**
     * Tests for equality with another object. The objects equal if the other object
     * is an instance of <CODE>DictionaryEntry</CODE>, has identical word, reading
     * and translations and comes from the same dictionary.
     */
    public boolean equals( Object o) {
        try {
            DefaultDictionaryEntry e = (DefaultDictionaryEntry) o;
            if (!dictionary.equals( e.getDictionary()))
                return false;
            if (!word.equals( e.word))
                return false;
            if (reading==null && e.reading!=null)
                return false;
            if (reading!=null && e.reading==null)
                return false;
            if (!reading.equals( e.reading))
                return false;
            return (translations.equals( e.translations));
        } catch (ClassCastException ex) {
            return false;
        }
    }
} // class DefaultDictionaryEntry
