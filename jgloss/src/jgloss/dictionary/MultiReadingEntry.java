/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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

import java.util.Collections;
import java.util.List;

import jgloss.dictionary.attribute.AttributeSet;
import jgloss.dictionary.attribute.DefaultAttributeSet;

/**
 * Dictionary entry with one word, multiple readings and no attributes.
 *
 * @author Michael Koch
 */
class MultiReadingEntry implements DictionaryEntry {
    protected static final AttributeSet EMPTY_ATTRIBUTE_SET = new DefaultAttributeSet();
    
    protected String word;
    protected String[] readings;
    protected String[] translations;
    protected Dictionary dictionary;

    public MultiReadingEntry( String _word, String _reading, String[] _translations,
                              Dictionary _dictionary) {
        this( _word, Collections.singletonList( _reading), _translations, _dictionary);
    }

    public MultiReadingEntry( String _word, List _readings, String[] _translations,
                              Dictionary _dictionary) {
        word = _word;
        readings = new String[_readings.size()];
        readings = (String[]) _readings.toArray( readings);
        translations = _translations;
        if (translations == null)
            translations = new String[0];
        dictionary = _dictionary;
    }

    public AttributeSet getGeneralAttributes() { return EMPTY_ATTRIBUTE_SET; }
 
    public String getWord( int alternative) {
        if (alternative != 0)
            throw new IllegalArgumentException();
        return word;
    }

    public int getWordAlternativeCount() { return 1; }

    public AttributeSet getWordAttributes( int alternative) {
        if (alternative != 0)
            throw new IllegalArgumentException();
        return EMPTY_ATTRIBUTE_SET;
    }

    public AttributeSet getWordAttributes() {
        return EMPTY_ATTRIBUTE_SET;
    }

    public String getReading( int alternative) {
        try {
            return readings[alternative];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    public int getReadingAlternativeCount() { return readings.length; }

    public AttributeSet getReadingAttributes( int alternative) {
        if (alternative<0 || alternative>=readings.length)
            throw new IllegalArgumentException();
        return EMPTY_ATTRIBUTE_SET;
    }

    public AttributeSet getReadingAttributes() { return EMPTY_ATTRIBUTE_SET; }

    public String getTranslation( int rom, int crm, int synonym) {
        if (rom!=0 || synonym!=0 || crm<0 || crm>=translations.length)
            throw new IllegalArgumentException();
        return translations[crm];
    }

    public int getTranslationRomCount() {
        return translations.length>0 ? 1 : 0;
    }

    public int getTranslationCrmCount( int rom) {
        if (translations.length==0 || rom!=0)
            throw new IllegalArgumentException();
        return translations.length;
    }

    public int getTranslationSynonymCount( int rom, int crm) {
        if (rom!=0 || crm<0 || crm>=translations.length)
            throw new IllegalArgumentException();
        return 1;
    }

    public AttributeSet getTranslationAttributes( int rom, int crm, int synonym) {
        if (rom!=0 || synonym!=0 || crm<0 || crm>=translations.length)
            throw new IllegalArgumentException();
        return EMPTY_ATTRIBUTE_SET;
    }

    public AttributeSet getTranslationAttributes( int rom, int crm) {
        if (rom!=0 || crm<0 || crm>=translations.length)
            throw new IllegalArgumentException();
        return EMPTY_ATTRIBUTE_SET;
    }

    public AttributeSet getTranslationAttributes( int rom) {
        if (translations.length==0 || rom!=0)
            throw new IllegalArgumentException();
        return EMPTY_ATTRIBUTE_SET;
    }

    public AttributeSet getTranslationAttributes() {
        return EMPTY_ATTRIBUTE_SET;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    public DictionaryEntryReference getReference() {
        return new DictionaryEntryReference() {
                public DictionaryEntry getEntry() { return MultiReadingEntry.this; }
            };
    }
} // class MultiReadingEntry
