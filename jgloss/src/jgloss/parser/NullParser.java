/*
 * Copyright (C) 2004 Michael Koch (tensberg@gmx.net)
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

package jgloss.parser;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import jgloss.dictionary.SearchException;

/**
 * Parser which does not add annotations to the parsed text.
 *
 * @author 
 */
public class NullParser implements Parser {
    private final static String PARSER_NAME = 
        ResourceBundle.getBundle( "resources/messages-parser")
        .getString( "parser.null.name");

    public NullParser() {
        super();
    }

    public List parse(char[] text, int start, int length)
        throws SearchException {
        return Collections.EMPTY_LIST;
    }

    public int getParsePosition() {
        return 0;
    }

    public void reset() {}

    public void setIgnoreNewlines(boolean ignoreNewlines) {}

    public boolean getIgnoreNewlines() {
        return false;
    }

    public void setAnnotateFirstOccurrenceOnly(boolean firstOccurrence) {}

    public boolean getAnnotateFirstOccurrenceOnly() {
        return false;
    }

    public String getName() {
        return PARSER_NAME;
    }

    public Locale getLanguage() {
        // TODO: this parser works for any language. Need to rework API for languages
        // if more than Japanese should ever be supported.
        return Locale.JAPANESE;
    }

}
