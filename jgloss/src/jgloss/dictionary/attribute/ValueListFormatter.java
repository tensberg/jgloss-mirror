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
 * Format an attribute and its value as a string.
 *
 * @author Michael Koch
 */
public interface ValueListFormatter {
    /**
     * Format an attribute and its list of values as a string.
     *
     * @param valueFormatter The formatter must call 
     *        {@link AttributeValueFormatter#format(Attribute,AttributeValue,StringBuffer) format} 
     *        on this object to format values from the value list.
     * @param val Value of the attribute. May be <code>null</code> if the attribute is set but
     *            has no value.
     * @param buf String buffer to which the formatted attribute is appended.
     * @return The string buffer passed in.
     */
    StringBuffer format( AttributeValueFormatter valueFormatter, Attribute att, ValueList val,
                         StringBuffer buf);
} // interface ValueListFormatter
