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

/**
 * Attributes which encapsulate extended informations for dictionary entries.
 * Singleton instances of classes implementing this interface are used as keys in
 * {@link AttributeSet AttributeSets}. The attribute instance provides a description of itself
 * and can render attribute values as string.
 *
 * @author Michael Koch
 */
public interface Attribute {
    /**
     * Return a short name describing the attribute to the user.
     */
    String getName();
    /**
     * Return a short description explaining the attribute to the user.
     */
    String getDescription();

    boolean isInheritable();

    Class getAttributeValueClass();
} // interface Attribute
