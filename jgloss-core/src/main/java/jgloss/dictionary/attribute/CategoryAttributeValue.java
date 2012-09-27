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
 * Interface for values of category attributes. Each category has a abbreviated name, and
 * a longer text explaining the meaning of the value. Typical categories are part of speech
 * and usage.
 *
 * @author Michael Koch
 */
public interface CategoryAttributeValue extends AttributeValue {
    /**
     * Short or abbreviated name of this value. The name should be localized to the user's
     * language. Example: "m-sl" for a manga slang expression.
     */
    String getShortName();
    /**
     * Long name or explanation of this value. The name should be localized to the user's
     * language. Example: "manga slang" for a manga slang expression.
     */
    String getLongName();
} // interface CategoryAttributeValue
