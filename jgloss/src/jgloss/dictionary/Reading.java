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
 * Representation of a reading annotation in a parsed text.
 *
 * @author Michael Koch
 * @see Parser
 */
public class Reading extends AbstractAnnotation {
    /**
     * Word/Reading pair for the text.
     */
    protected WordReadingPair entry;

    /**
     * Create a reading without location information and conjugation.
     */
    public Reading( WordReadingPair wrp) {
        this( 0, 0, wrp, null);
    }

    /**
     * Create a reading without location information, but with conjugation.
     */
    public Reading( WordReadingPair wrp, Conjugation conjugation) {
        this( 0, 0, wrp, conjugation);
    }

    /**
     * Creates a new reading annotation.
     *
     * @param start Start offset of this annotation in the parsed text. 
     * @param length Length of the annotated text.
     * @param wrp Word/Reading pair of this entry.
     */
    public Reading( int start, int length, WordReadingPair wrp) {
        this( start, length, wrp, null);
    }

    /**
     * Creates a new reading annotation.
     *
     * @param start Start offset of this annotation in the parsed text. 
     * @param length Length of the annotated text.
     * @param wrp Word/Reading pair of this entry.
     * @param conjugation The conjugation which was used to derive the plain form for the dictionary
     *                    lookup. May be <CODE>null</CODE>
     */
    public Reading( int start, int length, WordReadingPair wrp, Conjugation conjugation) {
        super( start, length, conjugation);
        this.entry = wrp;
    }

    public String getWord() { return entry.getWord(); }
    public String getReading() { return entry.getReading(); }

    /**
     * Returns the Word/Reading pair of this entry.
     */
    public WordReadingPair getWordReadingPair() { return entry; }
} // class Reading
