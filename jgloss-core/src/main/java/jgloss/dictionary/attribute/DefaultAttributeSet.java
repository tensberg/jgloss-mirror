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

import static java.util.Collections.checkedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jgloss.util.NullIterator;

public class DefaultAttributeSet implements AttributeSet {

    protected AttributeSet parent = null;
    protected Map<Attribute<?>, List<? extends AttributeValue>> attributes = null;

    public DefaultAttributeSet() {
    }

    public DefaultAttributeSet( AttributeSet _parent) {
        this.parent = _parent;
    }

    @Override
	public boolean containsKey( Attribute<?> key, boolean resolveInherited) {
        if (attributes!=null && attributes.containsKey( key)) {
	        return true;
        } else if (resolveInherited && parent!=null) {
	        return parent.containsKey( key, true);
        } else {
	        return false;
        }
    }

    @Override
	public boolean contains( Attribute<?> key, AttributeValue value, boolean resolveInherited) {
        if (attributes!=null && attributes.containsKey( key)) {
            return attributes.get( key).contains(value);
            
        } else if (resolveInherited && parent!=null) {
            return parent.contains( key, value, true);
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
	public <T extends AttributeValue> List<T> getAttribute( Attribute<T> key, boolean resolveInherited) {
        List<T> base = null;
        List<T> parentv = null;

        if (resolveInherited && parent!=null) {
	        parentv = parent.getAttribute( key, true);
        }

        if (attributes!=null && attributes.containsKey( key)) {
            List<? extends AttributeValue> v = attributes.get( key);
            if (v != null) {
                base = (List<T>) v;
            }
        }

        if (parentv == null) {
            return base; // this also covers the case where base==parentv==null (att not set)
        } else if (base == null) {
            return parentv; // just inherited attributes
        } else {
        	List<T> combined = new ArrayList<T>(base.size() + parentv.size());
        	combined.addAll(base);
        	combined.addAll(parentv);
            return combined;
        }
    }

    @Override
	public boolean isInherited( Attribute<?> key) throws AttributeNotSetException {
        if (attributes!=null && attributes.containsKey( key)) {
	        return false;
        } else if (parent.containsKey( key, true)) {
	        return true;
        } else {
	        throw new AttributeNotSetException( key);
        }
    }

    @Override
	public Iterator<Attribute<?>> getAttributeKeys( boolean resolveInherited) {
        if (resolveInherited) {
	        return new AttributeSetChainIterator( this);
        } else if (attributes != null) {
	        return attributes.keySet().iterator();
        } else {
	        return NullIterator.instance();
        }
    }

    /**
     * Set the parent attribute set used to resolve inherited attributes.
     *
     * @return This attribute set.
     */
    public DefaultAttributeSet setParent( AttributeSet _parent) {
        if (_parent == this) {
	        throw new IllegalArgumentException();
        }

        this.parent = _parent;
        return this;
    }

    @Override
	public AttributeSet getParent() { return parent; }

    public <T extends AttributeValue> void addAttribute(AttributeMapper.Mapping<T> mapping) {
    	addAttribute(mapping.getAttribute(), mapping.getValue());
    }
    
    @SuppressWarnings("unchecked")
    public <T extends AttributeValue> void addAttribute( Attribute<T> key, T value) {
        if (attributes == null) {
	        attributes = new HashMap<Attribute<?>, List<? extends AttributeValue>>();
        }
        List<? extends AttributeValue> v = attributes.get( key);

        if (v == null) {
	        attributes.put( key, value!=null ? checkedList(new ArrayList<T>(3), key.getAttributeValueClass()) : null);
        } else if (value == null) {
	        // nothing to be done, since attibute already set
            return;
        } else {
            ((List<T>) v).add( value);
        }
    }

    @Override
	public boolean isEmpty() {
        return attributes == null;
    }

    @Override
	public String toString() {
        if (attributes == null) {
	        return "()";
        }

        StringBuilder out = new StringBuilder( 128);
        out.append( '(');
        boolean firstEntry = true;
        for ( Entry<Attribute<?>, List<? extends AttributeValue>> entry : attributes.entrySet()) {
        	if (firstEntry) {
        		firstEntry = false;
        	} else {
        		out.append( ',');
        	}
            out.append( entry.getKey().toString());
            if (entry.getValue() != null) {
                out.append( ':');
                out.append( entry.getValue().toString());
            }
        }
        out.append( ')');
        return out.toString();
    }
} // class DefaultAttributeSet
