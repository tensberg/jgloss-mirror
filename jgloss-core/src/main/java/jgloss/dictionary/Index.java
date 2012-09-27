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
 * $Id$
 *
 */

package jgloss.dictionary;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

/**
 * An index is a store for locations in a dictionary which enables fast lookups of
 * entries matching certain parameters. Entry locations are encoded as positive integers,
 * which allows compact storage of indexes. The <code>Index</code> stores locations
 * and accesses the entries for comparison through the {@link Indexable Indexable}
 * interface.
 *
 * @author Michael Koch
 * @see Indexable
 * @see IndexBuilder
 */
public interface Index {
    /**
     * Return type for {@link #getType() getType} to signal that no index container data is used
     * by this index.
     */
    int NO_TYPE = -1;

    /**
     * Iterator over index entries. Since index entry locations are stored as integers,
     * the {@link #next next} method returns <code>ints</code>.
     */
    interface Iterator {
        boolean hasNext();
        int next() throws NoSuchElementException;
    } // interface Iterator

    /**
     * Returns the index type, which is used to fetch the index data from an 
     * {@link IndexContainer IndexContainer}.
     *
     * @return The index type code, or {@link #NO_TYPE NO_TYPE} if the index does not
     *         use data from an index container.
     */
    int getType();

    /**
     * Sets the index container from which the index data is read. The container object must contain
     * index data of the type returned by {@link #getType() getType}.
     */
    void setContainer( IndexContainer container) throws IndexException ;

    /**
     * Returns an iterator over index entries matching certain criteria.
     *
     * @param dictionary Dictionary which this <code>Index</code> indexes.
     * @param expression The search expression, encoded in a form compatible to the 
     *                   <code>dictionary</code>.
     * @param parameters Additional index-dependent parameters. May be <code>null</code> if the
     *                   index does not use additional search parameters.
     */
    Iterator getEntryPositions( Indexable dictionary, ByteBuffer expression,
                                Object[] parameters) throws IndexException;
} // interface index
