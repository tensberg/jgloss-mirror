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

    protected class SingleValueList implements ValueList {
        private AttributeValue value;

        public SingleValueList( AttributeValue _value) {
            this.value = _value;
        }

        public SingleValueList set( AttributeValue _value) {
            this.value = _value;
            return this;
        }

        public AttributeValue get( int index) {
            if (index != 0)
                throw new IllegalArgumentException();
            return value;
        }

        public int size() { return 1; }
        public String toString() { return "[_" + value.toString() + ']'; }
    }

    protected AttributeSet parent = null;
    protected Map attributes = null;
    protected SingleValueList singleList = new SingleValueList( null);

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

    public ValueList getAttribute( Attribute key, boolean resolveInherited) 
        throws AttributeNotSetException {
        if (attributes!=null && attributes.containsKey( key)) {
            Object v =  attributes.get( key);
            if (v instanceof ValueList)
                return (ValueList) v;
            else
                return singleList.set( (AttributeValue) v);
        }
        else if (resolveInherited && parent!=null)
            return parent.getAttribute( key, true);
        else
            throw new AttributeNotSetException( key);
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
        this.parent = _parent;
        return this;
    }

    public AttributeSet getParent() { return parent; }

    public void putAttribute( Attribute key, AttributeValue value) {
        if (attributes == null)
            attributes = new HashMap( 11);
        Object v = attributes.get( key);
        if (v == null)
            attributes.put( key, value);
        else if (v instanceof MutableValueList)
            ((MutableValueList) v).add( value);
        else { // AttributeValue
            MutableValueList list = new MutableValueList();
            list.add( (AttributeValue) v);
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
            out.append( ':');
            out.append( entry.getValue().toString());
            if (i.hasNext())
                out.append( ',');
        }
        out.append( ')');
        return out.toString();
    }
} // class DefaultAttributeSet
