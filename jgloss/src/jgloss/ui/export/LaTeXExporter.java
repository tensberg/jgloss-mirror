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
 * Export an annotated JGloss document as LaTeX-CJK file. This class extends the template
 * mechanism by adding the variable {@link #FONT_SIZE FONT_SIZE} and by adding escapes for
 * LaTeX special characters. Templates for this exporter are managed by the class
 * {@link LaTeXExportFileChooser LaTeXExportFileChooser}.
 *
 * @author Michael Koch
 */
public class LaTeXExporter extends TemplateExporter {
    /**
     * Variable which is substituted for the font size string passed to 
     * {@link #export(Reader,JGlossDocument,String,String,AnnotationModel,Writer,boolean) export}.
     */
    public static final String FONT_SIZE = "font-size";

    /**
     * Flag if {@link #escape(String) escape} should replace umlauts with the appropriate LaTeX
     * escapes. This is set to <code>true</code> if the output writer uses a Japanese encoding
     * which can't deal with accented characters.
     */
    private boolean escapeAccentedCharacters;

    /**
     * Create a new LaTeX exporter.
     */
    public LaTeXExporter() {}

    /**
     * Exports an annotated JGloss document as LaTeX file.
     *
     * @param template Reader for the template file.
     * @param doc The document to write.
     * @param documentName Filename of the document as chosen by the user.
     * @param fontSize Value for the {@link #FONT_SIZE FONT_SIZE} variable.
     * @param model Annotation model which contains the annotations of the document.
     * @param out Writer to which the document will be written.
     * @param outEncoding Character encoding used for the output writer (if applicable).
     * @param writeHidden <CODE>true</CODE> if annotations marked as hidden should be written.
     * @exception IOException
     */
    public void export( Reader template, JGlossDocument doc, String documentName, String fontSize,
                        AnnotationModel model, Writer out, String outEncoding,
                        boolean writeHidden) throws IOException {
        escapeAccentedCharacters = (outEncoding != null &&
                                    !outEncoding.toLowerCase().startsWith( "utf"));
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
            String replacement = null;
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
                replacement = "$\\backslash$";
                break;
            case '~':
                replacement = "\\~{}";
                break;
            case '^':
                replacement = "\\^{}";
                break;
            case 0xa0: // non-breakable space
                buf.setCharAt( i, ' ');
                break;
            case '\u00c2':
                replacement = "\\={A}";
                break;
            case '\u00ca':
                replacement = "\\={E}";
                break;
            case '\u00d4':
                replacement = "\\={O}";
                break;
            case '\u00db':
                replacement = "\\={U}";
                break;
            case '\u00e2':
                replacement = "\\={a}";
                break;
            case '\u00ea':
                replacement = "\\={e}";
                break;
            case '\u00f4':
                replacement = "\\={o}";
                break;
            case '\u00fb':
                replacement = "\\={u}";
                break;
            }

            if (escapeAccentedCharacters) {
                switch (buf.charAt( i)) {
                case '\u00e4':
                    replacement = "\\\"{a}";
                    break;
                case '\u00f6':
                    replacement = "\\\"{o}";
                    break;
                case '\u00fc':
                    replacement = "\\\"{u}";
                    break;
                case '\u00c4':
                    replacement = "\\\"{A}";
                    break;
                case '\u00d6':
                    replacement = "\\\"{O}";
                    break;
                case '\u00dc':
                    replacement = "\\\"{U}";
                    break;
                case '\u00df':
                    replacement = "\\ss{}";
                    break;
                }
            }

            if (replacement != null)
                buf.replace( i, i+1, replacement);
        }

        return buf.toString();
    }

    /**
     * Returns \\, which is the LaTeX line break marker, followed by a line break.
     */
    protected String defaultLineBreakPattern() { return "\\\\\n"; }
} // class LaTeXExporter
