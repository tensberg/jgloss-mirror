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

import java.util.Iterator;

/**
 * Set of attribute key/object mappings used by {@link DictionaryEntry DictionaryEntry}.
 * Each attribute set may have parent sets from which key/value pairs are inherited.
 *
 * @author Michael Koch
 */
public interface AttributeSet {
    /**
     * Test if the attribute for a given key is defined.
     *
     * @param key Attribute key which is tested.
     * @param resolveInherited <code>true</code> if the key is searched in the parent sets too, 
     *                  <code>false</code> if inherited attributes should be ignored.
     */
    boolean containsKey( Attribute key, boolean resolveInherited);
    /**
     * Returns the attribute for a given key. The type of the attribute depends on the key.
     *
     * @param key Attribute key for which the attribute is requested.
     * @param resolveInherited <code>true</code> if the attribute is searched in the parent sets too, 
     *                  <code>false</code> if inherited attributes should be ignored.
     * @exception AttributeNotSetException if the attribute is not defined.
     */
    AttributeValue getAttribute( Attribute key, boolean resolveInherited) throws AttributeNotSetException;
    /**
     * Test if a given attribute is inherited from a parent attribute set.
     *
     * @return <code>true</code> if the attribute is inherited, <code>false</code> if the attribute
     *         is contained in this set.
     * @exception AttributeNotSetException if the attribute is not defined in this set or a parent set.
     */
    boolean isInherited( Attribute key) throws AttributeNotSetException;
    /**
     * Returns an iteration of attribute keys defined in the attribute set.
     * 
     * @param resolveInherited <code>true</code> if inherited attribute keys are iterated too, 
     *                  <code>false</code> if inherited attributes should be ignored.
     * @return Iteration of {@link Attribute Attributes}.
     */
    Iterator getAttributeKeys( boolean resolveInherited);
    /**
     * Get the parent attribute set of this set. Returns <code>null</code> if this is a root set.
     */
    AttributeSet getParent();
} // interface AttributeSet
