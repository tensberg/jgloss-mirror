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
import java.nio.IntBuffer;

public class BinarySearchIndex implements Index {
    private class BinarySearchIterator implements Index.Iterator {
        private final int firstEntry;
        private final int lastEntry;
        private int currentEntry;

        public BinarySearchIterator( int _firstEntry, int _lastEntry) {
            this.firstEntry = _firstEntry;
            this.lastEntry = _lastEntry;
            currentEntry = firstEntry;
        }

        @Override
		public boolean hasNext() { return currentEntry <= lastEntry; }
        @Override
		public int next() {
            if (!hasNext()) {
	            throw new java.util.NoSuchElementException();
            }
            return index.get( currentEntry++);
        }
    } // class BinarySearchIterator

    protected final BinarySearchIterator EMPTY_MATCH = new BinarySearchIterator( 0, -1);

    /**
     * Default type of this index.
     */
    public static final int TYPE = 0x42695365; // BiSe in ASCII

    private IntBuffer index = null;
    private final int type;

    public BinarySearchIndex() {
        this( TYPE);
    }

    public BinarySearchIndex( int _type) {
        type = _type;
    }
    
    @Override
	public int getType() {
        return type;
    }

    @Override
	public void setContainer( IndexContainer container) throws IndexException {
        index = container.getIndexData( type).asIntBuffer();
    }

    @Override
	public Index.Iterator getEntryPositions( Indexable dictionary, ByteBuffer expression,
                                             Object[] parameters) throws IndexException {
        int match = findMatch( dictionary, expression);
        if (match == -1) {
	        return EMPTY_MATCH;
        }
        
        int firstMatch = findMatch( dictionary, expression, match, true);
        int lastMatch = findMatch( dictionary, expression, match, false);
        return new BinarySearchIterator( firstMatch, lastMatch);
    }

    /**
     * Returns the index of an index entry which matches the expression. If there is more than one match,
     * it is not defined which match is returned. If no match is found, <code>-1</code>
     * is returned.
     */
    protected int findMatch( Indexable dictionary, ByteBuffer expression) throws IndexException {
        // do a binary search
        int from = 0;
        int to = index.limit()-1;
        int match = -1;
        int curr;

        // search matching entry
        do {
            curr = (to-from)/2 + from;

            int c = dictionary.compare( expression, index.get( curr));
            if (c > 0) {
	            from = curr+1;
            } else if (c < 0) {
	            to = curr-1;
            } else {
	            match = curr;
            }
        } while (from<=to && match==-1);

        return match;
    }

    /**
     * Searches the index backwards/forwards from a matching entry to the 
     * first/last match of an expression.
     *
     * @param expression Expression to match.
     * @param match Offset in the index to a matching entry.
     * @param first <CODE>true</CODE> if the first matching entry should be returned,
     *              <CODE>false</CODE> if the last matching entry is returned.
     * @return Offset in the index to the first/last matching entry.
     */
    protected int findMatch( Indexable dictionary, ByteBuffer expression, int match, boolean first) 
        throws IndexException {
        int direction = first ? -1 : 1;
        
        try {
            while (dictionary.compare( expression, index.get( match+direction)) == 0) {
	            match += direction;
            }
        } catch (IndexOutOfBoundsException ex) {
            // match is now either 0 or index.size - 1
        }

        return match;
    }

} // class BinarySearchIndex
