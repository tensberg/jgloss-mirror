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

package jgloss.dictionary.attribute;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class IDAttributeValueFactory {
    private Map values;

    public IDAttributeValueFactory() {
        values = new HashMap();
    }

    public IDAttributeValueFactory( int size) {
        values = new HashMap( size);
    }

    public CategoryAttributeValue getValueFor( String id) {
        if (!values.containsKey( id)) {
            id = new String( id); // invoking the copy constructor minimizes the space requirements
            values.put( id, createValue( id));
        }

        return (CategoryAttributeValue) values.get( id);
    }

    protected abstract CategoryAttributeValue createValue( String id);

    public Iterator valueIterator() {
        return values.values().iterator();
    }
} // class IDAttributeValueFactory
