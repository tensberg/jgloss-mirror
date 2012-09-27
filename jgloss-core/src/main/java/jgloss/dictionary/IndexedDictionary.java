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
 *
 */

package jgloss.dictionary;

/**
 * Interface for dictionaries which need an index to operate. The index needs to be loaded after
 * the index object is instantiated.
 */
public interface IndexedDictionary extends Dictionary {
    /**
     * Load the index for the dictionary. This method must be called before searches can
     * be performed.
     *
     * @return <code>true</code> if the index was loaded successfully, <code>false</code> if
     *         the index does not exist, does not contain all needed index data or is
     *         damaged. In this case, {@link #buildIndex() buildIndex} must be called.
     * @exception IndexException if reading the index failed for an unforeseeable reason.
     *           In this case, calling {@link #buildIndex() buildIndex} will likely also fail
     *           and the dictionary object can't be used.
     */
    boolean loadIndex() throws IndexException;

    /**
     * Rebuild the index or add missing index data to an already existing index file.
     * Building indexes may take a long time, so the user should be informed what is happening
     * before this method is invoked. If the index is damaged, it must be deleted before this
     * method is called in order to be successfully rebuilt. This method will also load the
     * newly created index, calling {@link #loadIndex() loadIndex} after 
     * <code>buildIndex</code> is not neccessary.
     *
     * @exception IndexException if the index creation failed.
     */
    void buildIndex() throws IndexException;
} // interface IndexedDictionary
