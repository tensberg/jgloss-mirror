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

/**
 * Reference to a dictionary entry. Implementations of this interface may use a more memory-
 * efficient way of storing the dictionary entry than keeping the dictionary entry object in memory.
 * For example, if it is possible to recreate the dictionary entry object from permanent storage,
 * the dictionary object could be cached using a <code>SoftReference</code>, and if it has
 * been garbage collected by the time {@link #getEntry() getEntry()} is called, it will be
 * recreated.
 *
 * @author Michael Koch
 */
public interface DictionaryEntryReference {
    /**
     * Returns the referenced dictionary entry.
     *
     * @exception SearchException if the construction of the dictionary entry fails.
     */
    DictionaryEntry getEntry() throws SearchException;
} // interface DictionaryEntryReference
