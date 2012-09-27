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
 * $Id$
 *
 */

package jgloss.dictionary;

import java.util.List;

import jgloss.dictionary.attribute.AttributeSet;

public class SingleWordEntry extends BaseEntry {
    private final String word;

    private final String reading;

    public SingleWordEntry( int _entryMarker, String _word, String _reading, List<List<String>> _translations,
                       AttributeSet _generalA, AttributeSet _wordA, AttributeSet _translationA,
                       List<AttributeSet> _translationRomA, Dictionary _dictionary) {
        super( _entryMarker, _translations,
               _generalA, _wordA, _translationA,
               _translationRomA, _dictionary);
        this.word = _word;
        this.reading = _reading;
    }

    @Override
	public String getWord( int alternative) {
        if (alternative != 0) {
	        throw new IllegalArgumentException();
        }
        return word;
    }

    @Override
	public int getWordAlternativeCount() { return 1; }

    @Override
	public AttributeSet getWordAttributes( int alternative) {
        if (alternative != 0) {
	        throw new IllegalArgumentException();
        }

        return emptySet.setParent( wordA);
    }

    @Override
	public String getReading( int alternative) {
        if (alternative != 0) {
	        throw new IllegalArgumentException();
        }
        return reading;
    }

    @Override
	public int getReadingAlternativeCount() { return 1; }

    @Override
	public String toString() {
        StringBuilder out = new StringBuilder( 30);
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
                if (j > 0) {
	                out.append( "; ");
                }
                out.append( translations[i][j]);
            }
        }
        
        return out.toString();
    }
} // class SingleWordEntry
