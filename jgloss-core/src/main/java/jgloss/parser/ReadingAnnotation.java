/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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

package jgloss.parser;

/**
 * Reading for a part of a text. Used by {@link ReadingAnnotationFilter ReadingAnnotationFilter}.
 *
 * @author Michael Koch
 */
public class ReadingAnnotation {
    protected int start;
    protected int length;

    protected String reading;

    public ReadingAnnotation( int _start, int _length, String _reading) {
        start = _start;
        length = _length;
        reading = _reading;
    }

    /**
     * Returns the start offset of this annotation in the parsed text. 
     */
    public int getStart() { return start; }
    /**
     * Returns the length of the annotated text.
     */
    public int getLength() { return length; }

    /**
     * Returns the reading (in hiragana) of the annotated text.
     */
    public String getReading() { return reading; }

    public void setReading( String _reading) { reading = _reading; }
} // class ReadingAnnotation
