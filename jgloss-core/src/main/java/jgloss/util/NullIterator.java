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

package jgloss.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator implementation without any elements. The singleton instance of this
 * iterator can be used wherever an iteration over an empty collection is required.
 */
public class NullIterator<T> implements Iterator<T> {
    public static final Iterator<Object> INSTANCE = new NullIterator<Object>();
    
    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> instance() {
    	return (Iterator<T>) INSTANCE;
    }

    private NullIterator() {}

    @Override
	public boolean hasNext() { return false; }

    @Override
	public T next() throws NoSuchElementException { 
        throw new NoSuchElementException();
    }

    @Override
	public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
} // class NullIterator
