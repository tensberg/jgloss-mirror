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
public class Translation extends Reading {
    /**
     * Create a translation without location information and conjugation.
     */
    public Translation( DictionaryEntry entry) {
        this( 0, 0, entry, null);
    }

    /**
     * Create a translation without location information, but with conjugation.
     */
    public Translation( DictionaryEntry entry, Conjugation conjugation) {
        this( 0, 0, entry, conjugation);
    }

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
        super( start, length, dictionaryEntry, conjugation);
    }

    /**
     * Returns the dictionary entry describing the annotated text.
     *
     * @return The dictionary entry describing the annotated text.
     */
    public DictionaryEntry getDictionaryEntry() { return (DictionaryEntry) entry; }
} // class Translation
