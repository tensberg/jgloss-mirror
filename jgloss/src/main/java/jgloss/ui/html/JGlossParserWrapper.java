/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 */

package jgloss.ui.html;

import java.io.IOException;
import java.io.Reader;

import javax.swing.text.html.HTMLEditorKit;


/**
 * Instance of <CODE>HTMLEditorKit.Parser</CODE>, which will forward parse
 * requests to a {@link JGlossParser JGlossParser}.
 * <CODE>JGlossParser</CODE> is derived from <CODE>DocumentParser</CODE>,
 * which prevents it from also being a <CODE>HTMLEditorKit.Parser</CODE>.
 */
class JGlossParserWrapper extends HTMLEditorKit.Parser {
    /**
     * The parser to which parse requests will be forwarded.
     */
    private final JGlossParser parser;

    /**
     * Creates a new wrapper with an associated <CODE>JGlossParser</CODE>.
     */
    public JGlossParserWrapper() {
        parser = new JGlossParser();
    }

    /**
     * Parse a document. This forwards the request to the underlying parser.
     */
    @Override
    public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharset) throws IOException {
        parser.parse(r, cb, ignoreCharset);
    }

    /**
     * Sets strict parsing mode on the wrapped <CODE>JGlossParser</CODE>.
     *
     * @see JGlossParser#setStrict(boolean)
     */
    public void setStrict(boolean strict) {
        parser.setStrict(strict);
    }
}