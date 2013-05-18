/*
 * Copyright (C) 2001-2013 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.ui.gloss;

import java.io.Reader;
import java.io.StringReader;

import jgloss.JGloss;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;

/**
 * Creates a new annotated document by reading the original text from a string.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
public class ImportStringStrategy extends ImportStrategy {

    private static final String IMPORT_STRING = JGloss.MESSAGES.getString("import.textarea");

    private final String string;

    /**
     * Creates a new annotated document by reading the original text from a string.
     * The method can only be applied on a <CODE>JGlossFrame</CODE> with no open document.
     *
     * @param string The text which will be imported.
     * @param detectParagraphs Flag if paragraph detection should be done.
     */
    public ImportStringStrategy(JGlossFrame frame, String string, boolean detectParagraphs,
                    ReadingAnnotationFilter filter, Parser parser) {
        super(frame, IMPORT_STRING, detectParagraphs, filter, parser);
        this.string = string;
    }

    @Override
    Reader createReader() {
        return new StringReader(string);
    }

    @Override
    void customizeModel(JGlossFrameModel model) {
        model.setDocumentName(IMPORT_STRING);
        model.setDocumentChanged(true);
    }

    @Override
    int getLength() {
        return string.length();
    }

}
