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

import java.util.List;

/**
 * Representation of an annotation which contains a DictionaryEntry for a word in
 * the parsed text.
 *
 * @author Michael Koch
 * @see Parser
 */
public class Translation implements Parser.TextAnnotation {
    /**
     * Start offset of this annotation in the parsed text.
     */
    private int start;
    /**
     * Length of the annotated text.
     */
    private int length;
    /**
     * Dictionary entry for the annotated text.
     */
    private DictionaryEntry dictionaryEntry;
    /**
     * Conjugation which was used for the word in the text.
     */
    private Conjugation conjugation;

    /**
     * Constructs a new Translation object for a word in the parsed text which has no
     * associated conjugation.
     *
     * @param start Start offset of this annotation in the parsed text. 
     * @param length Length of the annotated text.
     * @param dictionaryEntry Dictionary entry for the annotated text.
     */
    public Translation( int start, int length, DictionaryEntry dictionaryEntry) {
        this( start, length, dictionaryEntry, null);
    }

    /**
     * Constructs a new Translation object for a word in the parsed text. If <CODE>conjugation</CODE>
     * is not <CODE>null</CODE>, it describes the conjugation which was used to find the
     * dictionary entry.
     *
     * @param start Start offset of this annotation in the parsed text. 
     * @param length Length of the annotated text.
     * @param dictionaryEntry Dictionary entry for the annotated text.
     * @param conjugation The conjugation which was used to derive the plain form for the dictionary
     *                    lookup. May be <CODE>null</CODE>
     */
    public Translation( int start, int length, DictionaryEntry dictionaryEntry, Conjugation conjugation) {
        this.start = start;
        this.length = length;
        this.dictionaryEntry = dictionaryEntry;
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
     * Returns the dictionary entry describing the annotated text.
     *
     * @return The dictionary entry describing the annotated text.
     */
    public DictionaryEntry getDictionaryEntry() { return dictionaryEntry; }
    /**
     * Returns the conjugation used to derive the dictionary form used for the
     * dictionary lookup. May be <CODE>null</CODE> no conjugation was used.
     *
     * @return The conjugation used.
     */
    public Conjugation getConjugation() { return conjugation; }
} // class Translation
