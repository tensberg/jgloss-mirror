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

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Index container which stores index data in a file.
 *
 * @author Michael Koch
 */
public class FileIndexContainer implements IndexContainer {
    public static final String EXTENSION = "index";

    /**
     * Magic number used in the index file header.
     */
    public static final int MAGIC = 0x4a474958; // JGIX (JGloss IndeX) in ASCII
    public static final int VERSION = 1000;
    
    protected File indexfile;
    protected boolean editMode;

    public FileIndexContainer( File _indexfile, boolean _editMode) throws IOException {
        this.indexfile = _indexfile;
        this.editMode = editMode;
    }

    public boolean hasIndex( int indexType) {
        return false;
    }

    public ByteBuffer getIndexData( int indexType) throws IndexException,
                                                          IllegalStateException {
        if (editMode)
            throw new IllegalStateException();

        return null;
    }

    public void createIndex( int indexType, ByteBuffer data) throws IndexException,
                                                                    IllegalStateException {
        if (!editMode)
            throw new IllegalStateException();
    }

    public boolean canAccess() {
        return !editMode;
    }

    public boolean canEdit() {
        return editMode;
    }

    public void deleteIndex( int indexType) throws IndexException, IllegalStateException {
    }

    public void endEditing() throws IndexException, IllegalStateException {
    }

    public void close() {
    }
} // class FileIndexContainer
