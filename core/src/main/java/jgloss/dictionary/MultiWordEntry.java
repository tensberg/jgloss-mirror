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

import java.util.List;

import jgloss.dictionary.attribute.AttributeSet;

public class MultiWordEntry extends BaseEntry {
    protected String[] words;
    protected AttributeSet[] wordsA;

    public MultiWordEntry( int _entryMarker, List _words, String _reading, List _translations,
                           AttributeSet _generalA, AttributeSet _wordA, List _wordsA,
                           AttributeSet _translationA,
                           List _translationRomA, Dictionary _dictionary) {
        super( _entryMarker, _reading, _translations,
               _generalA, _wordA,
               _translationA, _translationRomA, _dictionary);
        words = new String[_words.size()];
        words = (String[]) _words.toArray( words);
        wordsA = new AttributeSet[_wordsA.size()];
        wordsA = (AttributeSet[]) _wordsA.toArray( wordsA);
    }

    @Override
	public String getWord( int alternative) {
        try {
            return words[alternative];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    @Override
	public int getWordAlternativeCount() { return words.length; }

    @Override
	public AttributeSet getWordAttributes( int alternative) {
        try {
            if (wordsA[alternative] == null)
                return emptySet.setParent( wordA);
            else
                return wordsA[alternative];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
    }

    @Override
	public String toString() {
        StringBuffer out = new StringBuffer( 30);
        out.append( generalA.toString());
        out.append( ' ');
        out.append( wordA.toString());
        out.append( ' ');
        for ( int i=0; i<words.length; i++) {
            if (i > 0)
                out.append( "; ");
            if (wordsA[i] != null) {
                out.append( '(');
                out.append( wordsA.toString());
                out.append( ") ");
            }
            out.append( words[i]);
        }
        out.append( " [");
        out.append( reading);
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
                if (j > 0)
                    out.append( "; ");
                out.append( translations[i][j]);
            }
        }
        
        return out.toString();
    }
} // class MultiWordEntry
