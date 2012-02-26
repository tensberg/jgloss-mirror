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

import java.util.Iterator;

/**
 * Collection of search parameters of a {@link SearchMode SearchMode}. The collection
 * is sorted in the order in which the parameters were passed in on construction.
 *
 * @author Michael Koch
 */
public class SearchParameters {
    private SearchParameter[] parameters;

    /**
     * Create a new collection which contains the given parameters. The collection
     * is sorted in the order of the parameters.
     */
    public SearchParameters( SearchParameter[] _parameters) {
        this.parameters = _parameters;
    }

    /**
     * Get the parameter at the given index. The index is 0-based.
     */
    public SearchParameter get( int index) throws ArrayIndexOutOfBoundsException {
        return parameters[index];
    }

    /**
     * Return the number of parameters.
     */
    public int size() {
        return parameters.length;
    }

    /**
     * Test if the collection contains the given parameter.
     */
    public boolean contains( SearchParameter parameter) {
        for ( int i=0; i<parameters.length; i++) {
            if (parameter.equals( parameters[i]))
                return true;
        }

        return false;
    }

    /**
     * Return an iterator over all parameters.
     */
    public Iterator iterator() {
        return new Iterator() {
                private int index = 0;
                @Override
				public boolean hasNext() { return index<parameters.length; }
                @Override
				public Object next() { return parameters[index++]; }
                @Override
				public void remove() throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }
            };
    }

    /**
     * Test if the objects in the array match the parameters required by this collection.
     * The array of objects is valid if its size equals the number of search parameters and
     * if the class of each object matches the class required by the search parameter at
     * the given offset.
     *
     * @exception SearchException if the object array is not valid.
     */
    public void isValid( Object[] objects) throws SearchException {
        if (parameters.length != objects.length)
            throw new SearchException( "invalid number of parameters");

        for ( int i=0; i<parameters.length; i++) {
            if (!parameters[i].getClass().isAssignableFrom( objects[i].getClass()))
                throw new SearchException( "invalid class for parameter " + i + "; expected " +
                                           parameters[i].getClass().getName() + ", found " +
                                           objects[i].getClass().getName());
        }
    }
} // class SearchParameters
