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

import jgloss.dictionary.attribute.AttributeSet;
import jgloss.dictionary.attribute.DefaultAttributeSet;

import java.util.Iterator;
import java.util.List;

abstract class BaseEntry implements DictionaryEntry {
    protected String reading;
    protected String[][] translations;
    protected AttributeSet generalA;
    protected AttributeSet wordA;
    protected AttributeSet translationA;
    protected AttributeSet[] translationRomA;

    protected Dictionary dictionary;
    /**
     * Unique marker of this dictionary entry relative to other dictionary entries in
     * this dictionary. Used for fast equality test. Every dictionary entry object which
     * is created from the same dictionary entry in a dictionary instance must have the
     * same <code>entryMarker</code>, and for dictionary entry objects created from different
     * dictionary entries the marker must always be different. The marker usually is the
     * position where the entry is found in the dictionary, since this is unique.
     */
    protected int entryMarker;

    protected DefaultAttributeSet emptySet = new DefaultAttributeSet( null);

    public BaseEntry( int _entryMarker, String _reading, List _translations,
                      AttributeSet _generalA, AttributeSet _wordA,
                      AttributeSet _translationA,
                      List _translationRomA, Dictionary _dictionary) {
        entryMarker = _entryMarker;
        reading = _reading;
        translations = new String[_translations.size()][];
        int rom = 0;
        for ( Iterator i=_translations.iterator(); i.hasNext(); ) {
            List crm = (List) i.next();
            translations[rom++] = (String[]) crm.toArray( new String[crm.size()]);
        }

        generalA = _generalA;
        wordA = _wordA;
        translationA = _translationA;
        translationRomA = new AttributeSet[_translationRomA.size()];
        translationRomA = (AttributeSet[]) _translationRomA.toArray( translationRomA);

        dictionary = _dictionary;
    }

    public AttributeSet getGeneralAttributes() {
        return generalA;
    }
 
    public abstract String getWord( int alternative);

    public abstract int getWordAlternativeCount();

    public abstract AttributeSet getWordAttributes( int alternative);

    public AttributeSet getWordAttributes() {
        return wordA;
    }

    public String getReading( int alternative) {
        if (alternative != 0)
            throw new IllegalArgumentException();
        return reading;
    }

    public int getReadingAlternativeCount() { return 1; }

    public AttributeSet getReadingAttributes( int alternative) {
        if (alternative != 0)
            throw new IllegalArgumentException();

        return emptySet.setParent( generalA);
    }

    public AttributeSet getReadingAttributes() {
        return emptySet.setParent( generalA);
    }

    public String getTranslation( int rom, int crm, int synonym) {
        if (synonym != 0)
            throw new IllegalArgumentException();
        try {
            return translations[rom][crm]; // synonyms are not supported
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    public int getTranslationRomCount() { return translations.length; }

    public int getTranslationCrmCount( int rom) {
        try {
            return translations[rom].length;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    public int getTranslationSynonymCount( int rom, int crm) { return 1; }

    public AttributeSet getTranslationAttributes( int rom, int crm, int synonym) {
        if (synonym != 0)
            throw new IllegalArgumentException();
        return getTranslationAttributes( rom, crm);
    }

    public AttributeSet getTranslationAttributes( int rom, int crm) {
        try {
            if (crm<0 || crm >= translations[rom].length)
                throw new IllegalArgumentException();
            if (translationRomA[rom] != null)
                return emptySet.setParent( translationRomA[rom]);
            else
                return emptySet.setParent( translationA);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    public AttributeSet getTranslationAttributes( int rom) {
        try {
            if (translationRomA[rom] != null)
                return translationRomA[rom];
            else
                return emptySet.setParent( translationA);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    public AttributeSet getTranslationAttributes() {
        return translationA;
    }

    public Dictionary getDictionary() { return dictionary; }

    public boolean equals( Object o) {
        return (o instanceof BaseEntry &&
                ((BaseEntry) o).dictionary == dictionary &&
                ((BaseEntry) o).entryMarker == entryMarker);
    }
} // class BaseEntry
