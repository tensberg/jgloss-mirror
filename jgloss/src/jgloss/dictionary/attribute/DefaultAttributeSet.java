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

public class DefaultAttributeSet implements AttributeSet {
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

    public ValueList getAttribute( Attribute key, boolean resolveInherited) 
        throws AttributeNotSetException {
        if (attributes!=null && attributes.containsKey( key))
            return (ValueList) attributes.get( key);
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
        attributes.put( key, value);
    }

    public void putAttributes( Attribute key, ValueList values) {
        if (attributes == null)
            attributes = new HashMap( 11);
        if (!attributes.containsKey( 
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
