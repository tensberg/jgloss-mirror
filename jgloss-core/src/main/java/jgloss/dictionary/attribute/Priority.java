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
 * $Id$
 *
 */

package jgloss.dictionary.attribute;

/**
 * Priority of a dictionary entry. Dictionary entries may have different priorities depending
 * on e.g. commonness, the entry being an example etc. The ranges of priorities and their
 * representation are dictionary-dependent. Priorities of entries originating from the
 * same dictionary can be compared.
 *
 * @author Michael Koch
 * @see PriorityComparator
 */
public interface Priority extends AttributeValue {
    /**
     * Get the priority representation. The representation is dictionary-dependent and may
     * be any string.
     */
    String getPriority();
    /**
     * Compare this priority to another priority, imposing a total ordering on priorities.
     * Returns a negative integer, zero, or a positive integer as this object is less than,
     * equal to, or greater than the specified object.
     * <p>
     * Only priorities originating from the same dictionary may be compared. If the
     * priority parameter is not of the expected <code>Priority</code> subclass, the method
     * may throw an <code>IllegalArgumentException</code>.
     * </p><p>
     * If, in a given dictionary, some dictionary entries have a priority attribute set, while
     * others don't, the contract is that the entries without priority attribute have a lower
     * priority than those with the attribute.
     * </p>
     */
    int compareTo( Priority priority) throws IllegalArgumentException;
} // interface Priority
