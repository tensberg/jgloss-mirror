/*
 * Copyright (C) 2001,2002 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.export;

import jgloss.*;
import jgloss.ui.*;
import jgloss.ui.doc.*;
import jgloss.ui.annotation.*;

import java.io.*;
import java.util.*;

/**
 * Export an annotated JGloss document as LaTeX-CJK file.
 *
 * @author Michael Koch
 */
public class LaTeXExporter extends TemplateExporter {
    public static final String FONT_SIZE = "%font-size%";

    public LaTeXExporter() {}

    /**
     * Exports an annotated JGloss document as LaTeX-CJK file.
     *
     * @param doc The document to write.
     * @param documentName Name of the document as chosen by the user.
     * @param model Annotation model which contains the annotations of the document.
     * @param out Writer to which the document will be written.
     * @param writeHidden <CODE>true</CODE> if annotations marked as hidden should be written.
     * @exception IOException
     */
    public void export( Reader template, JGlossDocument doc, String documentName, String fontSize,
                        AnnotationModel model, Writer out,
                        boolean writeHidden) throws IOException {
        Map variables = prepareExport( doc, documentName, model, writeHidden);
        variables.put( FONT_SIZE, fontSize);

        export( template, out, variables);
    }

    /**
     * Escapes the LaTeX special characters.
     *
     * @param in The string to escape.
     * @return The escaped string.
     */
    protected String escape( String in) {
        StringBuffer buf = new StringBuffer( in);
        for ( int i=buf.length()-1; i >= 0; i--) {
            switch (buf.charAt( i)) {
            case '{':
            case '}':
            case '$':
            case '#':
            case '%':
            case '&':
            case '_':
                buf.insert( i, '\\');
                break;
            case '\\':
                buf.replace( i, i+1, "$\\backslash$");
                break;
            case '~':
                buf.replace( i, i+1, "\\~{}");
                break;
            case '^':
                buf.replace( i, i+1, "\\^{}");
                break;
            case 0xa0: // non-breakable space
                buf.setCharAt( i, ' ');
                break;
            }
        }

        return buf.toString();
    }
} // class LaTeXExporter
