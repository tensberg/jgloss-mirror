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

public class EDictEntry implements DictionaryEntry {
    protected String word;
    protected String reading;
    protected String[][] translations;
    protected Dictionary dictionary;
    protected AttributeSet generalA;
    protected AttributeSet wordA;
    protected AttributeSet translationA;
    protected AttributeSet[] translationRomA;
    protected DefaultAttributeSet emptySet = new DefaultAttributeSet( null);

    public EDictEntry( String _word, String _reading, List _translations,
                       AttributeSet _generalA, AttributeSet _wordA, AttributeSet _translationA,
                       List _translationRomA, Dictionary _dictionary) {
        this.word = _word;
        this.reading = _reading;
        this.translations = new String[_translations.size()][];
        int rom = 0;
        for ( Iterator i=_translations.iterator(); i.hasNext(); ) {
            List crm = (List) i.next();
            translations[rom++] = (String[]) crm.toArray( new String[crm.size()]);
        }

        this.generalA = _generalA;
        this.wordA = _wordA;
        this.translationA = _translationA;
        translationRomA = new AttributeSet[_translationRomA.size()];
        translationRomA = (AttributeSet[]) _translationRomA.toArray( translationRomA);

        this.dictionary = _dictionary;
    }

    public AttributeSet getGeneralAttributes() {
        return generalA;
    }
 
    public String getWord( int alternative) {
        if (alternative != 0)
            throw new IllegalArgumentException();
        return word;
    }

    public int getWordAlternativeCount() { return 1; }

    public AttributeSet getWordAttributes( int alternative) {
        if (alternative != 0)
            throw new IllegalArgumentException();

        return emptySet.setParent( wordA);
    }

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

        return emptySet.setParent( getReadingAttributes());
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
        try {
            if (crm<0 || crm >= translations[rom].length)
                throw new IllegalArgumentException();
            return emptySet.setParent( translationRomA[rom]);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    public AttributeSet getTranslationAttributes( int rom, int crm) {
        try {
            if (crm<0 || crm >= translations[rom].length)
                throw new IllegalArgumentException();
            return emptySet.setParent( translationRomA[rom]);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    public AttributeSet getTranslationAttributes( int rom) {
        try {
            return translationRomA[rom];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    public AttributeSet getTranslationAttributes() {
        return translationA;
    }

    public Dictionary getDictionary() { return dictionary; }

    public String toString() {
        StringBuffer out = new StringBuffer( 30);
        out.append( generalA.toString());
        out.append( ' ');
        out.append( wordA.toString());
        out.append( ' ');
        out.append( word);
        out.append( " [");
        out.append( reading);
        out.append( "] ");
        out.append( translationA);
        for ( int i=0; i<translations.length; i++) {
            out.append( ' ');
            if (translations.length > 1) {
                out.append( '(');
                out.append( i+1);
                out.append( ") ");
            }
            out.append( translationRomA[i]);
            out.append( ' ');
            for ( int j=0; j<translations[i].length; j++) {
                if (j > 0)
                    out.append( "; ");
                out.append( translations[i][j]);
            }
        }
        
        return out.toString();
    }
} // class EDictEntry
