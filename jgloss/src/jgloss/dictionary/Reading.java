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
public class Reading implements Parser.TextAnnotation {
    /**
     * Start offset of this annotation in the parsed text.
     */
    private int start;
    /**
     * Length of the annotated text.
     */
    private int length;
    /**
     * Reading for this text.
     */
    private String reading;

    /**
     * Creates a new reading annotation.
     *
     * @param start Start offset of this annotation in the parsed text. 
     * @param length Length of the annotated text.
     * @param reading Reading for this text.
     */
    public Reading( int start, int length, String reading) {
        this.start = start;
        this.length = length;
        this.reading = reading;
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
     * Returns the reading to use for the text delimited by <CODE>getStart()</CODE> and 
     * <CODE>getLength()</CODE>.
     *
     * @return The reading.
     */
    public String getReading() { return reading; }
} // class Reading
