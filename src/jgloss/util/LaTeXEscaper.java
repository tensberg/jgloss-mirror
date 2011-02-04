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

import java.util.HashMap;
import java.util.Map;

/**
 * Escapes LaTeX special characters.
 *
 * @author Michael Koch
 */
public class LaTeXEscaper extends AbstractEscaper {
    protected static final Map DEFAULT_ESCAPE_MAP;
    protected static final Map UMLAUT_ESCAPE_MAP;

    protected Map escapeMap;

    static {
        DEFAULT_ESCAPE_MAP = new HashMap();
        DEFAULT_ESCAPE_MAP.put( new Character('#'), "\\#");
        DEFAULT_ESCAPE_MAP.put( new Character('$'), "\\$");
        DEFAULT_ESCAPE_MAP.put( new Character('%'), "\\%");
        DEFAULT_ESCAPE_MAP.put( new Character('&'), "\\&");
        DEFAULT_ESCAPE_MAP.put( new Character('_'), "\\_");
        DEFAULT_ESCAPE_MAP.put( new Character('{'), "\\{");
        DEFAULT_ESCAPE_MAP.put( new Character('}'), "\\}");
        DEFAULT_ESCAPE_MAP.put( new Character('~'), "\\verb|~|");
        DEFAULT_ESCAPE_MAP.put( new Character('^'), "\\verb|^|");
        DEFAULT_ESCAPE_MAP.put( new Character('\\'), "$\\backslash$");
        DEFAULT_ESCAPE_MAP.put( new Character('\u00a0'), "~"); // non-breakable space

        UMLAUT_ESCAPE_MAP = new HashMap(DEFAULT_ESCAPE_MAP);
        // German umlauts
        UMLAUT_ESCAPE_MAP.put( new Character('\u00e4'), "\\\"{a}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00f6'), "\\\"{o}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00fc'), "\\\"{u}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00c4'), "\\\"{A}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00d6'), "\\\"{O}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00dc'), "\\\"{U}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00df'), "\\ss ");

        // umlauts with circumflex
        UMLAUT_ESCAPE_MAP.put( new Character('\u00e2'), "\\={a}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00f4'), "\\={o}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00fb'), "\\={u}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00c2'), "\\={A}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00d4'), "\\={O}");
        UMLAUT_ESCAPE_MAP.put( new Character('\u00db'), "\\={U}");

        //TODO: add umlauts for languages other than German
    }

    public LaTeXEscaper(boolean escapeUmlauts) {
        escapeMap = escapeUmlauts ? UMLAUT_ESCAPE_MAP : DEFAULT_ESCAPE_MAP;
    }

    protected Map getEscapeMap() {
        return escapeMap;
    }
} // class LaTeXEscaper
