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

package jgloss.parser;

/**
 * Representation of a reading annotation in a parsed text.
 *
 * @author Michael Koch
 * @see Parser
 */
public abstract class AbstractAnnotation implements Parser.TextAnnotation {
    /**
     * Start offset of this annotation in the parsed text.
     */
    protected int start;
    /**
     * Length of the annotated text.
     */
    protected int length;
    /**
     * Conjugation which was used for the word in the text.
     */
    protected Conjugation conjugation;

    /**
     * Creates a new abstract annotation.
     *
     * @param start Start offset of this annotation in the parsed text. 
     * @param length Length of the annotated text.
     * @param conjugation The conjugation which was used to derive the plain form for the dictionary
     *                    lookup. May be <CODE>null</CODE>
     */
    public AbstractAnnotation( int start, int length, Conjugation conjugation) {
        this.start = start;
        this.length = length;
        this.conjugation = conjugation;
    }

    /**
     * Returns the start offset of this annotation in the parsed text. 
     *
     * @return The start offset.
     */
    public int getStart() { return start; }
    /**
     * Returns the length of the annotated text.
     *
     * @return The length of the annotated text.
     */
    public int getLength() { return length; }
    /**
     * Returns the dictionary form of the word of this entry.
     *
     * @return The word.
     */
    public abstract String getWord();
    /**
     * Returns the dictionary form of the reading of this entry.
     *
     * @return The reading.
     */
    public abstract String getReading();
    /**
     * Returns the conjugation used to derive the dictionary form used for the
     * dictionary lookup. May be <CODE>null</CODE> no conjugation was used.
     *
     * @return The conjugation used.
     */
    public Conjugation getConjugation() { return conjugation; }
} // class Reading
