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

import java.util.Map;

/**
 * Abstract implementation of an escaper.
 *
 * @author Michael Koch
 */
public abstract class AbstractEscaper implements Escaper {
 
    /**
     * Returns the escape sequence for the character, or <code>null</code>. This implementation
     * calls {@link #getEscapeMap() getEscapeMap} and looks up the character in the map.
     */
    @Override
	public String escapeChar(char c) {
        return getEscapeMap().get(new Character(c));
    }

    /**
     * Returns the map from <code>Character</code>s to their escape sequence strings.
     * Subclasses typically return a <code>Map</code> instance which is a static final
     * member of the class.
     */
    protected abstract Map<Character, String> getEscapeMap();

    /**
     * Escape all special characters in a string.
     */
    @Override
	public String escape(String text) {
    	StringBuilder out = escape((CharSequence) text);

        return out==null ? text : out.toString();
    }

    /**
     * Escape all special characters in a string buffer. The modification may be done in place.
     */
    @Override
	public StringBuilder escape(StringBuilder text) {
        StringBuilder out = escape((CharSequence) text);

        return out==null ? text : out;
    }

    /**
     * Escapes the characters in a <code>CharSequence</code>.
     *
     * @return A <code>StringBuilder</code> containing the escaped text sequence, or
     *         <code>null</code> if no characters needed escaping.
     */
    protected StringBuilder escape(CharSequence text) {
        StringBuilder out = null; // only initialized if escaping must be done

        for ( int i=text.length()-1; i>=0; i--) {
            String replacement = escapeChar(text.charAt( i));
            if (replacement != null) {
                if (out == null) {
                    if (text instanceof StringBuilder)
                        out = (StringBuilder) text; // in-place escaping for string buffers
                    else
                        out = new StringBuilder(text.toString());
                }

                out.replace(i, i+1, replacement);
            }
        }

        return out;
    }
} // class AbstractEscaper
