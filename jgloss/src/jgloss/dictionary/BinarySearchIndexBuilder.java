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
import java.nio.*;
import java.nio.channels.*;
import java.util.Random;

/**
 * Builder class for a {@link BinarySearchIndex BinarySearchIndex}.
 *
 * @author Michael Koch
 */
public class BinarySearchIndexBuilder implements IndexBuilder {
    protected IndexContainer indexContainer;
    protected Indexable dictionary;
    protected File tempIndexFile;
    protected RandomAccessFile tempIndex;

    /**
     * Random number generator. Used for randomized quicksort in index creation.
     */
    protected static Random random = new Random();

    public BinarySearchIndexBuilder() {
    }
    
    public void startBuildIndex( IndexContainer _container, Indexable _dictionary) throws IndexException {
        this.indexContainer = _container;
        this.dictionary = _dictionary;

        try {
            tempIndexFile = File.createTempFile( "jgloss", null);
            tempIndex = new RandomAccessFile( tempIndexFile, "rw");
        } catch (IOException ex) {
            throw new IndexException( ex);
        }
    }
                                                                                  
    public boolean addEntry( int location, int length, DictionaryField field) throws IndexException {
        if (field == DictionaryField.WORD ||
            field == DictionaryField.READING ||
            field == DictionaryField.TRANSLATION) {
            try {
                if (indexContainer.getIndexByteOrder() == ByteOrder.BIG_ENDIAN)
                    tempIndex.writeInt( location);
                else { // shuffle bytes
                    tempIndex.write( location); // writes the low eight bits of location
                    location >>>= 8;
                    tempIndex.write( location);
                    location >>>= 8;
                    tempIndex.write( location);
                    location >>>= 8;
                    tempIndex.write( location);
                }

                return true;
            } catch (IOException ex) {
                throw new IndexException( ex);
            }
        }
        else
            return false;
    }

    public void endBuildIndex( boolean commit) throws IndexException {
        try {
            if (commit) {
                FileChannel indexChannel = tempIndex.getChannel();
                ByteBuffer buffer = indexChannel.map( FileChannel.MapMode.READ_WRITE, 0,
                                                      tempIndex.length());
                buffer.order( indexContainer.getIndexByteOrder());
                IntBuffer index = buffer.asIntBuffer();
                
                quicksortIndex( 0, index.capacity()-1, index);
                
                // copy index data from temp file to index container
                buffer.position( 0);
                indexContainer.createIndex( BinarySearchIndex.TYPE, buffer);
            }

            tempIndex.close();
            tempIndexFile.delete();
            tempIndex = null;
            tempIndexFile = null;
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
        if (left >= right)
            return;

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

    protected void finalize() {
        if (tempIndex != null) {
            try {
                tempIndex.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            tempIndexFile.delete();
        }
    }
} // class BinarySearchIndexBuilder
