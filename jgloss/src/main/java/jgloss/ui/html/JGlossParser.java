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
import java.util.logging.Logger;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.DocumentParser;
import javax.swing.text.html.parser.TagElement;

/**
 * Parser for JGloss documents.
 */
class JGlossParser extends DocumentParser {
    private static final Logger LOGGER = Logger.getLogger(JGlossParser.class.getPackage().getName());

    /**
     * Constructs a new parser. This will use the DTD modified for JGloss
     * tags from {@link JGlossEditorKit#getDTD() getDTD}.
     *
     */
    public JGlossParser() {
        super(JGlossEditorKit.getDTD());

        // Swing 1.4 changes how whitespace is coalesced in class
        // javax.swing.text.html.parser.Parser. The new behavior breaks
        // reading of JGloss documents
        // with tags containing only a single space character (e. g. <trans>
        // </trans>). To switch
        // back to the old behavior, the protected member strict must be
        // changed to true.
        strict = true;
    }

    /**
     * Make sure the document charset is always ignored. The Parser has a
     * bug where it throws an exception when the charset of the reader is
     * identical to that given in a http-equiv charset header in the
     * document instead of when they are different. This parser will always
     * ignore the charset. It will also clear the cache of the
     * jgloss.dictionary.Parser when parsing is finished.
     *
     * @param r
     *            Reader from which the document is read.
     * @param cb
     *            Callback to forward document construction requests to.
     * @param ignoreCharset
     *            <CODE>true</CODE> if charset declarations in the HTML
     *            document should be ignored. This parameter will be
     *            ignored.
     * @exception IOException
     */
    @Override
    public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharset) throws IOException {
        super.parse(r, cb, true);
    }

    /**
     * Sets strict parsing mode. If strict parsing mode is set to
     * <CODE>true</CODE> (default), SGML specification conformance is
     * enforced, otherwise incorrect content is handled mimicking the
     * popular browsers' behavior. Strict mode is needed when a JGloss
     * document is loaded, while non-strict mode is needed when it is
     * edited.
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /**
     * Creates a {@link JGlossTagElement JGlossTagElement}
     * which wraps the given element. This is necessary because the parser
     * superclass does not know how to handle the custom annotation tags.
     *
     * @param e
     *            The element this should wrap.
     * @param fictional
     *            <CODE>true</CODE> if the element does not exist in the
     *            original document.
     * @return The new tag element.
     */
    @Override
    protected TagElement makeTag(javax.swing.text.html.parser.Element e, boolean fictional) {
        return new JGlossTagElement(e, fictional);
    }

    @Override
    protected void handleError(int ln, String errorMsg) {
        LOGGER.warning("error parsing JGloss document at line " + ln + ": " + errorMsg);
    }
}