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

import jgloss.util.NullIterator;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class DefaultAttributeSet implements AttributeSet {
    protected class MutableValueList implements ValueList {
        private List values = new ArrayList( 2);

        public MutableValueList() {}

        public int size() { return values.size(); }
        public AttributeValue get( int index) { return (AttributeValue) values.get( index); }
        public void add( AttributeValue value) { values.add( value); }
        public void addAll( ValueList _values) {
            if (_values instanceof MutableValueList) {
                values.addAll( ((MutableValueList) _values).values);
            }
            else {
                for ( int i=0; i<_values.size(); i++)
                    values.add( _values.get( i));
            }
        }
        public boolean contains( AttributeValue value) {
            return values.contains( value);
        }
        public String toString() {
            StringBuffer out = new StringBuffer();
            out.append( '[');
            for ( Iterator i=values.iterator(); i.hasNext(); ) {
                out.append( i.next().toString());
                if (i.hasNext())
                    out.append( ',');
            }
            out.append( ']');
            return out.toString();
        }
    } // class MutableValueList

    protected class NestedValueList implements ValueList {
        private ValueList base;
        private ValueList parent;

        public NestedValueList( ValueList _base, ValueList _parent) {
            base = _base;
            parent = _parent;
        }
    
        public int size() { return base.size() + parent.size(); }
        
        public AttributeValue get( int index) {
            if (index < base.size())
                return base.get( index);
            else
                return parent.get( index-base.size());
        }

        public String toString() { return "[/" + base.toString() + parent.toString() + "]"; }
    } // class NestedValueList

    protected AttributeSet parent = null;
    protected Map attributes = null;

    public DefaultAttributeSet() {
    }

    public DefaultAttributeSet( AttributeSet _parent) {
        this.parent = _parent;
    }

    public boolean containsKey( Attribute key, boolean resolveInherited) {
        if (attributes!=null && attributes.containsKey( key))
            return true;
        else if (resolveInherited && parent!=null)
            return parent.containsKey( key, true);
        else
            return false;
    }

    public boolean contains( Attribute key, AttributeValue value, boolean resolveInherited) {
        if (attributes!=null && attributes.containsKey( key)) {
            Object v = attributes.get( key);
            if (v instanceof MutableValueList)
                return ((MutableValueList) v).contains( value);
            else // v is SingletonValueList
                return ((SingletonValueList) v).get( 0).equals( value);
        }
        else if (resolveInherited && parent!=null)
            return parent.contains( key, value, true);
        else
            return false;
    }

    public ValueList getAttribute( Attribute key, boolean resolveInherited) {
        ValueList base = null;
        ValueList parentv = null;

        if (resolveInherited && parent!=null)
            parentv = parent.getAttribute( key, true);

        if (attributes!=null && attributes.containsKey( key)) {
            Object v = attributes.get( key);
            if (v != null) {
                if (v instanceof ValueList)
                    base = (ValueList) v;
                else
                    base = new SingletonValueList( (AttributeValue) v);
            }
        }

        if (parentv == null)
            return base; // this also covers the case where base==parentv==null (att not set)
        else if (base == null)
            return parentv; // just inherited attributes
        else
            return new NestedValueList( base, parentv);
    }

    public boolean isInherited( Attribute key) throws AttributeNotSetException {
        if (attributes!=null && attributes.containsKey( key))
            return false;
        else if (parent.containsKey( key, true))
            return true;
        else
            throw new AttributeNotSetException( key);
    }

    public Iterator getAttributeKeys( boolean resolveInherited) {
        if (resolveInherited)
            return new AttributeSetChainIterator( this);
        else if (attributes != null)
            return attributes.keySet().iterator();
        else
            return NullIterator.INSTANCE;
    }

    /**
     * Set the parent attribute set used to resolve inherited attributes.
     *
     * @return This attribute set.
     */
    public DefaultAttributeSet setParent( AttributeSet _parent) {
        if (_parent == this)
            throw new IllegalArgumentException();

        this.parent = _parent;
        return this;
    }

    public AttributeSet getParent() { return parent; }

    public void addAttribute( Attribute key, AttributeValue value) {
        if (attributes == null)
            attributes = new HashMap();
        Object v = attributes.get( key);

        if (v == null)
            attributes.put( key, value!=null ? new SingletonValueList( value) : null);
        else if (value == null)
            // nothing to be done, since attibute already set
            return;
        else if (v instanceof MutableValueList)
            ((MutableValueList) v).add( value);
        else { // SingletonValueList
            MutableValueList list = new MutableValueList();
            list.add( ((ValueList) v).get( 0));
            list.add( value);
            attributes.put( key, list);
        }
    }

    public boolean isEmpty() {
        return attributes == null;
    }

    public String toString() {
        if (attributes == null)
            return "()";

        StringBuffer out = new StringBuffer( 128);
        out.append( '(');
        for ( Iterator i=attributes.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            out.append( entry.getKey().toString());
            if (entry.getValue() != null) {
                out.append( ':');
                out.append( entry.getValue().toString());
            }
            if (i.hasNext())
                out.append( ',');
        }
        out.append( ')');
        return out.toString();
    }
} // class DefaultAttributeSet
