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
import jgloss.util.UTF8ResourceBundleControl;

/**
 * Parser which does not add annotations to the parsed text.
 *
 * @author Michael Koch
 */
public class NullParser implements Parser {
    private static final String PARSER_NAME = 
        ResourceBundle.getBundle( "messages-parser", new UTF8ResourceBundleControl())
        .getString( "parser.null.name");

    @Override
	public List<TextAnnotation> parse(char[] text, int start, int length)
        throws SearchException {
        return Collections.emptyList();
    }

    @Override
	public int getParsePosition() {
        return 0;
    }

    @Override
	public void reset() {}

    @Override
	public void setIgnoreNewlines(boolean ignoreNewlines) {}

    @Override
	public boolean isIgnoreNewlines() {
        return false;
    }

    @Override
	public void setAnnotateFirstOccurrenceOnly(boolean firstOccurrence) {}

    @Override
	public boolean isAnnotateFirstOccurrenceOnly() {
        return false;
    }

    @Override
	public String getName() {
        return PARSER_NAME;
    }

    @Override
	public Locale getLanguage() {
        // TODO: this parser works for any language. Need to rework API for languages
        // if more than Japanese should ever be supported.
        return Locale.JAPANESE;
    }

}
