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

package jgloss.ui;

import jgloss.dictionary.*;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Dictionary entry wrapper which shortens the first translation of the wrapped entry.
 *
 * @author Michael Koch
 */
public class ShortenedTranslation implements DictionaryEntry {
    private String word;
    private String reading;
    private List translations;
    private Dictionary dictionary;

    /**
     * Try to create a dictionary entry with shorter first translation. Some heuristics are applied to
     * the first translation of the original dictionary entry. The shortened translation then becomes
     * the first translation of the newly created entry, the original translation becomes the second
     * translation etc. If the heuristics fail and the entry can't be shortened, the original
     * dictionary entry is returned.
     */
    public static DictionaryEntry create( DictionaryEntry orig, int targetLength) {
        List translations = null; // will only be initialized if modifications take place
        for ( ListIterator i=orig.getTranslations().listIterator(); i.hasNext(); ) {
            String translation = (String) i.next();
            if (translation.length() > targetLength) {
                String st = shorten( translation);
                if (st != null) {
                    if (translations == null) {
                        // copy all previously seen translations
                        translations = new ArrayList( orig.getTranslations().size()+1);
                        translations.addAll( orig.getTranslations().subList( 0,
                                                                             i.previousIndex()));
                    }
                    translations.add( st);
                    // original translation will be added next
                }
            }
            if (translations != null)
                // translation list was modified, copy all original translations to new list
                translations.add( translation);
        }

        if (translations != null)
            return new ShortenedTranslation( orig, translations);
        else // no modified translations
            return orig;
    }
    
    private static String shorten( String translation) {
        int i = translation.indexOf( '(');
        if (i < 1) { // not found (-1) or first char (0)
            i = translation.indexOf( '{');
            if (i < 1) {
                i = translation.indexOf( '.');
                if (i < 1 
                    || i==translation.length()-1 // last char in string
                    || translation.charAt( i+1)!=' ') { // . marks abbrev., not end of sentence
                    i = translation.indexOf( ',');
                    if (i < 1
                        || i==translation.length()-1 // last char in string
                        || translation.charAt( i+1)!=' ') { // , not end of sentence
                        return null;
                    }
                }
            }
        }

        return translation.substring( 0, i).trim();
    }

    private ShortenedTranslation( DictionaryEntry entry, List translations) {
        this.word = entry.getWord();
        this.reading = entry.getReading();
        this.translations = translations;
        this.dictionary = entry.getDictionary();
    }

    public String getWord() { return word; }
    public String getReading() { return reading; }
    public List getTranslations() { return translations; }
    public Dictionary getDictionary() { return dictionary; }
} // class ShortenedTranslation
