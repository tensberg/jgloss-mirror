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
 * Single Entry in a dictionary which supports the <CODE>Dictionary</CODE> interface.
 * Only mappings from a single Japanese word to an array of translations is supported.
 *
 * @author Michael Koch
 * @see Dictionary
 */
public interface DictionaryEntry extends WordReadingPair {
    /**
     * Returns the Japanese word of this entry.
     *
     * @return The Japanese word of this entry.
     */
    String getWord();
    /**
     * Returns the reading of this word.
     *
     * @return The reading of this word. May be <CODE>null</CODE> if this word contains no
     *         kanji.
     */
    String getReading();
    /**
     * Returns the translations of this entry.
     *
     * @return List of translations.
     */
    List getTranslations();
    /**
     * Returns the dictionary which contains this entry.
     *
     * @return The dictionary which contains this entry.
     */
    Dictionary getDictionary();

} // interface DictionaryEntry
