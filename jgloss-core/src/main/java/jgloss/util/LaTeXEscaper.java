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

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Escapes LaTeX special characters.
 *
 * @author Michael Koch
 */
public class LaTeXEscaper extends AbstractEscaper {
    private static final Map<Character, String> ESCAPE_MAP = initEscapeMap();

    private static Map<Character, String> initEscapeMap() {
        Map<Character, String> escapeMap = new HashMap<Character, String>();
        escapeMap.put( new Character('#'), "\\#");
        escapeMap.put( new Character('$'), "\\$");
        escapeMap.put( new Character('%'), "\\%");
        escapeMap.put( new Character('&'), "\\&");
        escapeMap.put( new Character('_'), "\\_");
        escapeMap.put( new Character('{'), "\\{");
        escapeMap.put( new Character('}'), "\\}");
        escapeMap.put( new Character('~'), "\\verb|~|");
        escapeMap.put( new Character('^'), "\\verb|^|");
        escapeMap.put( new Character('\\'), "$\\backslash$");
        escapeMap.put( new Character('\u00a0'), "~"); // non-breakable space
        
        return unmodifiableMap(escapeMap);
    }

    @Override
	protected Map<Character, String> getEscapeMap() {
        return ESCAPE_MAP;
    }
} // class LaTeXEscaper
