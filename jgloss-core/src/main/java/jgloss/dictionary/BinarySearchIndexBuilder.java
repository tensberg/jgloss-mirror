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

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Builder class for a {@link BinarySearchIndex BinarySearchIndex}.
 *
 * @author Michael Koch
 */
public class BinarySearchIndexBuilder implements IndexBuilder {
	private static final Logger LOGGER = Logger.getLogger(BinarySearchIndexBuilder.class.getPackage().getName());
	
    private static final int INITIAL_INDEX_SIZE = 500000;

    protected IndexContainer indexContainer;
    protected Indexable dictionary;
    protected ByteBuffer tempIndex;
    protected IntBuffer tempIndexInt;
    protected int type;

    /**
     * Random number generator. Used for randomized quicksort in index creation.
     */
    protected static Random random = new Random();

    public BinarySearchIndexBuilder( int _type) {
        type = _type;
    }
    
    @Override
	public void startBuildIndex( IndexContainer _container, Indexable _dictionary) throws IndexException {
        this.indexContainer = _container;
        this.dictionary = _dictionary;

        try {
            tempIndex = ByteBuffer.allocate( INITIAL_INDEX_SIZE*4);
        } catch (OutOfMemoryError er) {
            throw new IndexException( er);
        }
        tempIndex.order( indexContainer.getIndexByteOrder());
        tempIndexInt = tempIndex.asIntBuffer();
    }
                                                                                  
    @Override
	public boolean addEntry( int location, int length, DictionaryEntryField field) throws IndexException {
        if (field == DictionaryEntryField.WORD ||
            field == DictionaryEntryField.READING ||
            field == DictionaryEntryField.TRANSLATION) {
            try {
                tempIndexInt.put( location);
            } catch (BufferOverflowException ex) {
                // index buffer is not large enough, increase the size
                ByteBuffer newIndex;
                try {
                    newIndex = ByteBuffer.allocate( (int) (tempIndex.capacity()*1.5));
                } catch (OutOfMemoryError er) {
                    tempIndex = null;
                    throw new IndexException("out of memory", er); // NOPMD: BufferOverflowException is irrelevant
                }
                newIndex.order( tempIndex.order());
                newIndex.put( tempIndex);
                tempIndex = newIndex;
                tempIndex.rewind();
                int intpos = tempIndexInt.position();
                tempIndexInt = tempIndex.asIntBuffer();
                tempIndexInt.position( intpos);
                tempIndexInt.put( location);
            }
            return true;
        } else {
	        return false;
        }
    }

    @Override
	public void endBuildIndex( boolean commit) throws IndexException {
        try {
            if (commit) {
                // position of tempIndexInt is now one after the last int written
                tempIndex.limit( tempIndexInt.position()*4);
                tempIndex.rewind();

                LOGGER.info( tempIndexInt.position() + " entries");
                LOGGER.info( "sorting index");
                quicksortIndex( 0, tempIndexInt.position()-1, tempIndexInt);

                // copy index data from temp file to index container
                try {
                    indexContainer.createIndex( type, tempIndex);
                } catch (IndexOutOfBoundsException ex) {
                    throw new IndexException( ex);
                }
            }

            tempIndex = null;
        } catch (IOException ex) {
            throw new IndexException( ex);
        }
    }

    /**
     * Sorts a part of the index array using randomized quicksort. Call this with
     * (0, index lenght-1) to sort the whole index.
     */
    protected void quicksortIndex( int left, int right, IntBuffer index) throws IOException,
                                                                                IndexException {
        if (left >= right) {
	        return;
        }

        int middle = left + random.nextInt( right-left+1);
        int mv = index.get( middle);
        index.put( middle, index.get( left));

        int l = left + 1; // l is the first index which compares greater mv
        for ( int i=l; i<=right; i++) {
            if (dictionary.compare( mv, index.get( i)) > 0) {
                if (i > l) {
                    int t = index.get( i);
                    index.put( i, index.get( l));
                    index.put( l, t);
                }
                l++;
            }
        }
        l--;
        index.put( left, index.get( l));
        index.put( l, mv);
        
        // sorting the smaller subset first will keep the stack depth small
        if (l < (left+right)/2) {
            quicksortIndex( left, l-1, index);
            quicksortIndex( l+1, right, index);
        }
        else {
            quicksortIndex( l+1, right, index);
            quicksortIndex( left, l-1, index);
        }
    }
} // class BinarySearchIndexBuilder
