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

package jgloss.dictionary.attribute;

import java.util.Iterator;
import java.util.NoSuchElementException;

class AttributeSetChainIterator implements Iterator {
    protected Object nextKey;
    protected AttributeSet currentSet;
    protected Iterator currentIterator;

    public AttributeSetChainIterator( AttributeSet first) {
        currentSet = first;
        currentIterator = first.getAttributeKeys( false);
        getNextKey();
    }

    public boolean hasNext() { return nextKey != null; }

    public Object next() throws NoSuchElementException {
        if (!hasNext())
            throw new NoSuchElementException();
        Object currentKey = nextKey;
        getNextKey();
        return currentKey;
    }

    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    protected void getNextKey() {
        while (currentSet!=null && !currentIterator.hasNext()) {
            currentSet = currentSet.getParent();
            if (currentSet != null)
                currentIterator = currentSet.getAttributeKeys( false);
        }

        if (currentIterator.hasNext())
            nextKey = currentIterator.next();
        else
            nextKey = null;
    }
} // class AttributeSetChainIterator
