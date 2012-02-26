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

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Index container which stores index data in a file.
 *
 * @author Michael Koch
 */
public class FileIndexContainer implements IndexContainer {
    public static void main( String[] args) throws Exception {
        FileIndexContainer f = new FileIndexContainer( new File( args[0]), false);
        System.err.println( f.indexes);
        f.close();
    }

    /**
     * Meta data for indexes stored in the container.
     */
    protected class IndexMetaData {
        protected static final int INDEX_OFFSET = 3*4;

        protected long start;
        protected int type;
        protected int length;
        protected int offset;
        protected MappedByteBuffer data = null;

        public IndexMetaData( RandomAccessFile indexFile) throws IOException {
            start = indexFile.getFilePointer();
            type = indexFile.readInt(); // index type
            length = indexFile.readInt(); // length of the index data in bytes (without this header)
            offset = indexFile.readInt(); // offset in bytes from the start of the header to the index data
        }

        public IndexMetaData( int _type, RandomAccessFile indexFile, int dataLength) 
            throws IOException {
            start = indexFile.getFilePointer();
            type = _type;
            length = dataLength;
            offset = INDEX_OFFSET;

            indexFile.writeInt( type);
            indexFile.writeInt( length);
            indexFile.writeInt( offset);
        }

        public long nextIndexMetaDataOffset() {
            return start + offset + length;
        }

        public long getHeaderOffset() { return start; }
        public int getType() { return type; }
        public int getDataLength() { return length; }
        public long getDataOffset() { return start + offset; }
        public ByteBuffer getIndexData( FileChannel indexFile) throws IOException {
            if (data == null) {
                data = indexFile.map( FileChannel.MapMode.READ_ONLY, getDataOffset(),
                                      getDataLength());
                data.order( indexByteOrder);
                return data;
            }
            else
                return data.duplicate();
        }

        @Override
		public String toString() {
            return "Index data: " + Integer.toHexString( type) + "/" + start + "/" + length;
        }
    } // class IndexMetaData
    
    /**
     * Standard filename extension for indexes in this format.
     */
    public static final String EXTENSION = ".index";
    private static final byte INDEXCONTAINER_HEADER_LENGTH = 4*4;
    private static final byte FIRST_INDEX_POINTER_OFFSET = 2*4; // 2 headers with 4 bytes each
    private static final int BIG_ENDIAN = 1;
    private static final int LITTLE_ENDIAN = 2;

    /**
     * Magic number used in the index file header.
     */
    public static final int MAGIC = 0x4a474958; // JGIX (JGloss IndeX) in ASCII
    /**
     * Version number of the index format.
     */
    public static final int VERSION = 1000;
    
    protected RandomAccessFile indexFile;
    protected boolean editMode;
    protected List indexes = new ArrayList( 5);
    protected ByteOrder indexByteOrder;

    /**
     * Create a new file index container or open an existing file in edit or access mode.
     *
     * @exception FileNotFoundException if the index container is opened in access mode and the
     *            index file does not already exist.
     * @exception IndexException if the selected file exists but does not contain a valid index
     *            structure.
     */
    public FileIndexContainer( File _indexfile, boolean _editMode) throws FileNotFoundException,
                                                                          IOException,
                                                                          IndexException {
        this.editMode = _editMode;
        
        boolean indexExists = _indexfile.exists();

        indexFile = new RandomAccessFile( _indexfile, editMode ? "rw" : "r");

        if (editMode && !indexExists)
            createIndexFile();
        else {
            readHeader();
            readIndexMetaData();
        }
    }

    @Override
	public boolean hasIndex( int indexType) {
        return getIndexMetaData( indexType) != null;
    }

    @Override
	public ByteBuffer getIndexData( int indexType) throws IndexException,
                                                          IllegalStateException {
        if (editMode)
            throw new IllegalStateException();

        IndexMetaData index = getIndexMetaData( indexType);
        if (index == null)
            throw new IndexException( "No index data of type " + indexType + " available");

        try {
            return index.getIndexData( indexFile.getChannel());
        } catch (IOException ex) {
            throw new IndexException( ex);
        }
    }

    @Override
	public ByteOrder getIndexByteOrder() {
        return indexByteOrder;
    }

    @Override
	public void createIndex( int indexType, ByteBuffer data) throws IndexException,
                                                                    IllegalStateException {
        if (!editMode)
            throw new IllegalStateException();

        if (hasIndex( indexType))
            throw new IndexException( "Index data of type " + indexType + " already exists");

        // append the index data to the end of the index file
        try {
            indexFile.seek( indexFile.length());
            IndexMetaData index = new IndexMetaData( indexType, indexFile, data.remaining());
            indexFile.getChannel().write( data);
            indexes.add( index);
        } catch (IOException ex) {
            throw new IndexException( ex);
        }
    }

    @Override
	public boolean canAccess() {
        return !editMode;
    }

    @Override
	public boolean canEdit() {
        return editMode;
    }

    @Override
	public void deleteIndex( int indexType) throws IndexException, IllegalStateException {
        if (!editMode)
            throw new IllegalStateException();

        IndexMetaData index = getIndexMetaData( indexType);
        if (index == null)
            throw new IndexException( "No index data of type " + indexType + " available");

        FileChannel indexChannel = indexFile.getChannel();
        // move all index data in the index file after the index data backwards, overwriting the data
        try {
            long remainder = indexChannel.size() - index.nextIndexMetaDataOffset();
            if (remainder > 0) {
                indexChannel.position( index.getHeaderOffset());
                indexChannel.transferFrom( indexChannel, index.nextIndexMetaDataOffset(),
                                           remainder);
                indexChannel.truncate( index.getHeaderOffset() + remainder);
            }
            // reloading the index meta data is the simplest way of correcting the meta data offsets
            readIndexMetaData();
        } catch (IOException ex) {
            throw new IndexException( ex);
        }
    }

    @Override
	public void endEditing() throws IndexException, IllegalStateException {
        if (!editMode)
            throw new IllegalStateException();

        editMode = false;
    }

    @Override
	public void close() {
        try {
            indexFile.close();
        } catch (IOException ex) {}
        indexes.clear(); // free index meta data for garbage collection, which clears the file mappings
    }

    /**
     * Creates a new empty index file with just a header.
     */
    protected void createIndexFile() throws IOException {
        // Write header information to index file
        // Magic Number marking this as 
        indexFile.writeInt( MAGIC);
        // version of the index file
        indexFile.writeInt( VERSION);
        // offset in bytes from the start of the index container file to the first index
        indexFile.writeInt( INDEXCONTAINER_HEADER_LENGTH);
        indexByteOrder = ByteOrder.nativeOrder();
        indexFile.writeInt( indexByteOrder==ByteOrder.BIG_ENDIAN ?
                            BIG_ENDIAN : LITTLE_ENDIAN);
    }

    protected void readHeader() throws IOException, IndexException {
        try {
            int data = indexFile.readInt();
            if (data != MAGIC)
                throw new IndexException( "Index file does not start with magic number");
            data = indexFile.readInt();
            if (data != VERSION)
                // later releases might replace this with a more sophisticated compatibility test
                throw new IndexException( "Index version " + data + " not supported");
            indexFile.readInt(); // offset, must not generate an EOF exception

            // read byte order
            data = indexFile.readInt();
            indexByteOrder = (data==BIG_ENDIAN) ? ByteOrder.BIG_ENDIAN :
                ByteOrder.LITTLE_ENDIAN;
        } catch (EOFException ex) {
            throw new IndexException( "Premature end of index file");
        }
    }

    protected void readIndexMetaData() throws IOException, IndexException {
        indexes.clear();
        try {
            indexFile.seek( FIRST_INDEX_POINTER_OFFSET);
            long indexOffset = indexFile.readInt();
            long length = indexFile.length(); // length() is a slow operation, cache the value
            
            // the last indexOffset points after the end of the index container file
            while (indexOffset < length) {
                indexFile.seek( indexOffset);
                IndexMetaData index = new IndexMetaData( indexFile);
                indexes.add( index);
                indexOffset = index.nextIndexMetaDataOffset();
            }
        } catch (EOFException ex) {
            throw new IndexException( "Premature end of index file");
        }
    }

    protected IndexMetaData getIndexMetaData( int indexType) {
        for ( Iterator i=indexes.iterator(); i.hasNext(); ) {
            IndexMetaData data = (IndexMetaData) i.next();
            if (data.getType() == indexType)
                return data;
        }

        return null;
    }
} // class FileIndexContainer
