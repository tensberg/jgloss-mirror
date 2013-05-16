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
 *
 */

package jgloss.dictionary.attribute;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class IDAttributeValueFactory {
    private final Map<String, CategoryAttributeValue> values;

    public IDAttributeValueFactory() {
        values = new HashMap<String, CategoryAttributeValue>();
    }

    public IDAttributeValueFactory( int size) {
        values = new HashMap<String, CategoryAttributeValue>( size);
    }

    public CategoryAttributeValue getValueFor( String id) {
        if (!values.containsKey( id)) {
            id = new String( id); 
            values.put( id, createValue( id));
        }

        return values.get( id);
    }

    protected abstract CategoryAttributeValue createValue( String id);

    public Iterator<CategoryAttributeValue> valueIterator() {
        return values.values().iterator();
    }
} // class IDAttributeValueFactory
