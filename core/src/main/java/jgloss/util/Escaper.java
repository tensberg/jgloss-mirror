/*
 * Copyright (C) 2003-2004 Michael Koch (tensberg@gmx.net)
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

package jgloss.util;

/**
 * Escape a string for a specific text format.
 *
 * @author Michael Koch
 */
public interface Escaper {
    /**
     * Escape a single character, if neccessary.
     *
     * @return The escape sequence for the character, or <code>null</code> if no escaping is
     *         neccessary.
     */
    String escapeChar(char c);

    /**
     * Escape all special characters in a string.
     */
    String escape(String text);

    /**
     * Escape all special characters in a string builder. The modification may be done in place.
     */
    StringBuilder escape(StringBuilder text);
} // interface Escaper
