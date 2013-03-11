/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.dictionary;

import java.util.List;

import jgloss.dictionary.attribute.AttributeSet;

public class MultiWordEntry extends BaseEntry {
	private static final AttributeSet[] EMPTY_ATTRIBUTE_SET_ARRAY = new AttributeSet[0];

    private final String[] words;
    private final AttributeSet[] wordsA;
    private final String[] readings;
    private final AttributeSet[] readingsA;

    public MultiWordEntry( int _entryMarker, String[] _words, String[] _readings, List<List<String>> _translations,
                           AttributeSet _generalA, AttributeSet _wordA, AttributeSet[] _wordsA,
                           AttributeSet _readingA, AttributeSet[] _readingsA, AttributeSet _translationA,
                           List<AttributeSet> _translationRomA, Dictionary _dictionary) {
        super( _entryMarker, _translations,
               _generalA, _wordA, _readingA,
               _translationA, _translationRomA, _dictionary);
		this.words = _words;
        this.wordsA = _wordsA == null ? EMPTY_ATTRIBUTE_SET_ARRAY : _wordsA;
		this.readings = _readings;
        this.readingsA = _readingsA == null ? EMPTY_ATTRIBUTE_SET_ARRAY : _readingsA;
    }

    @Override
	public String getWord( int alternative) {
        try {
            return words[alternative];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
	public int getWordAlternativeCount() { return words.length; }

    @Override
	public AttributeSet getWordAttributes( int alternative) {
        return getAttributes(alternative, wordsA, wordA);
    }

    @Override
    public int getReadingAlternativeCount() {
    	return readings.length;
    }

	@Override
    public String getReading(int alternative) {
		try {
			return readings[alternative];
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new IllegalArgumentException(ex);
		}
    }

    @Override
    public AttributeSet getReadingAttributes(int alternative) {
        return getAttributes(alternative, readingsA, readingA);
    }

    private AttributeSet getAttributes(int alternative, AttributeSet[] attributeSets, AttributeSet baseA) {
        try {
            if (alternative >= attributeSets.length || attributeSets[alternative] == null) {
                return emptySet.setParent(baseA);
            } else {
                return attributeSets[alternative];
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
	public String toString() {
        StringBuilder out = new StringBuilder( 30);
        out.append( generalA.toString());
        out.append( ' ');
        out.append( wordA.toString());
        out.append( ' ');
        for ( int i=0; i<words.length; i++) {
            if (i > 0) {
	            out.append( "; ");
            }
            if (wordsA[i] != null) {
                out.append( '(');
                out.append( wordsA.toString());
                out.append( ") ");
            }
            out.append( words[i]);
        }
        out.append( " [");
        for ( int i=0; i<readings.length; i++) {
        	if (i > 0) {
        		out.append( "; ");
        	}
        	out.append(readings[i]);
        }
        out.append( "] ");
        out.append( translationA.toString());
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
                if (j > 0) {
	                out.append( "; ");
                }
                out.append( translations[i][j]);
            }
        }

        return out.toString();
    }
} // class MultiWordEntry
