/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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
import java.nio.ByteOrder;

/**
 * Container which can store the data for several different index types. Each index type
 * has a unique integer ID, which is used to access it. An instance of <code>IndexContainer</code>
 * is usually passed to a {@link Index Index} instance at construction time to allow it to get
 * its data.
 * <p>
 * An index container can be in two modes, <code>access</code> or <code>edit</code> mode. 
 * In access mode, {@link Index Index}
 * instances can fetch and read the data stored in the index container, but the index and index
 * container structure can not be changed. In edit mode, the index container structure can be
 * changed: indexes can be added and deleted; but it is not allowed for {@link Index Index}
 * instances to access the data.
 *
 * @author Michael Koch
 */
public interface IndexContainer {
    /**
     * Test if the index data of the particular index type is stored in this container.
     * Can be called in access and edit mode.
     */
    boolean hasIndex( int indexType);

    /**
     * Return the byte order used by this index container for <code>ByteBuffers</code>.
     * {@link Index Indexes} and {@link IndexBuilder IndexBuilders} should use this byte order
     * to guarantee that the index file is portable. <code>ByteBuffers</code> returned by
     * {@link #getIndexData(int) getIndexData} are set to the byte order returned by this method.
     */
    ByteOrder getIndexByteOrder();

    /**
     * Fetch the index data for a particular index type for read access. {@link Index Index}
     * instances will call this method to get at their data.
     *
     * @return Byte buffer with the index data. The buffer is valid until {@link #close() close} 
     *         is called.
     * @exception IllegalStateException if the index container is not in <code>access</code> mode.
     */
    ByteBuffer getIndexData( int indexType) throws IndexException,
                                                   IllegalStateException;

    /**
     * Close this index container. After this method is called, all buffers returned by
     * {@link #getIndexData(int) getIndexData} are invalid.
     */
    void close();

    /**
     * Add index data for a particular index type to the container. An index of this type
     * must not already exist. This method can only be called in <code>edit</code> mode.
     *
     * @param indexType Index type of the data added.
     * @param data Buffer containing the index data. The format of the data stored in the buffer
     *        is dependent on the index used, not on the index container.
     * @exception IndexException if index data of the selected type already exists or an error
     *            occurrs while storing the data.
     * @exception IllegalStateException if the index container is not in <code>edit</code> mode.
     */
    void createIndex( int indexType, ByteBuffer data) throws IndexException,
                                                             IllegalStateException;
    /**
     * Delete the index data of a particular index type from the container. If no index data for
     * the selected type exists, the method does nothing.
     *
     * @param indexType Index type of the data to delete.
     * @exception IndexException if an error occurrs while deleting the data.
     * @exception IllegalStateException if the index container is not in <code>edit</code> mode.
     */
    void deleteIndex( int indexType) throws IndexException, IllegalStateException;
    /**
     * Switch the index container from <code>edit</code> mode to <code>access</code> mode.
     */
    void endEditing() throws IndexException, IllegalStateException;

    /**
     * Return if the index container is in <code>access</code> mode.
     */
    boolean canAccess();
    /**
     * Return if the index container is in <code>edit</code> mode.
     */
    boolean canEdit();
} // interface IndexContainer
