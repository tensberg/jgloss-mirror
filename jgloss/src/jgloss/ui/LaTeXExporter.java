/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui;

import jgloss.*;
import jgloss.ui.doc.*;
import jgloss.ui.annotation.*;
import jgloss.dictionary.*;

import java.io.*;
import java.util.*;

import javax.swing.text.*;

/**
 * Export an annotated JGloss document as LaTeX-CJK file.
 *
 * @author Michael Koch
 */
public class LaTeXExporter {
    /**
     * Exports an annotated JGloss document as LaTeX-CJK file.
     *
     * @param doc The document to write.
     * @param model Annotation model which contains the annotations of the document.
     * @param out Writer to which the document will be written.
     * @param writeReading <CODE>true</CODE> if the reading annotations should be written.
     * @param writeTranslations <CODE>true</CODE> if the translation annotations should be written.
     * @param writeHidden <CODE>true</CODE> if annotations marked as hidden should be written.
     * @exception IOException
     */
    public static void export( JGlossDocument doc, AnnotationModel model, Writer out, 
                               boolean writeReading, boolean writeTranslations,
                               boolean writeHidden) throws IOException {
        
        String text = null;
        try {
            text = doc.getText( 0, doc.getLength());
        } catch (BadLocationException ex) {}
        StringBuffer outtext = new StringBuffer( text.length());
        
        int prevend = 0; // end offset of the previous annotation
        for ( int i=0; i<model.getAnnotationCount(); i++) {
            AnnotationNode annotation = model.getAnnotationNode( i);
            Element ae = annotation.getAnnotationElement();

            // add text between annotations
            if (prevend < ae.getStartOffset())
                outtext.append( escape( text.substring( prevend, ae.getStartOffset())));
            prevend = ae.getEndOffset();

            // handle reading and word text
            boolean discardReadings = !writeReading || (!writeHidden && annotation.isHidden());
            Element wordelement = ae.getElement( 0);
            for ( int j=0; j<wordelement.getElementCount(); j++) {
                Element child = wordelement.getElement( j);
                if (child.getElementCount() == 2) { // READING_BASETEXT element
                    String word = text.substring( child.getElement( 1).getStartOffset(),
                                                  child.getElement( 1).getEndOffset());
                    String reading = text.substring( child.getElement( 0).getStartOffset(),
                                                     child.getElement( 0).getEndOffset());
                    if (discardReadings || " ".equals( reading))
                        outtext.append( escape( word));
                    else
                        outtext.append( "\\ruby{" + escape( word) + "}{" + escape( reading) + "}");
                }
                else { // BASETEXT element
                    outtext.append( escape( text.substring( child.getStartOffset(),
                                                            child.getEndOffset())));
                }
            }

            // handle translation text
            String translation = escape( text.substring( ae.getElement( 1).getStartOffset(), 
                                                         ae.getElement( 1).getEndOffset()));
            if (writeTranslations && !translation.equals( " ") && 
                (writeHidden || !annotation.isHidden())) {
                String footnote;
                // if the linked annotation entry is an inflected verb or adjective,
                // output the full verb instead of just the kanji part
                String nb = annotation.getDictionaryFormNode().getWord();
                String nr = annotation.getDictionaryFormNode().getReading();
                footnote = "\\footnotetext{" + nb;
                if (nr!=null && !nr.equals( " "))
                    footnote += " " + nr;
                footnote += " " + translation + "}";

                outtext.append( footnote);
            }
        }
        // add the remaining text
        if (prevend < text.length())
            outtext.append( escape( text.substring( prevend, text.length())));

        // Remove the &nbsp;'s (non-breakable spaces) and let LaTeX determine the insets.
        // Also insert an additional line for every \n, which is LaTeX's mark for a
        // paragraph break.
        for ( int i=outtext.length()-1; i>=0; i-=1) {
            if (outtext.charAt( i)=='\u00a0') {
                outtext.deleteCharAt( i);
            }
            else if (outtext.charAt( i)=='\n')
                outtext.insert( i, '\n');
        }

        String preamble = "\\documentclass";
        String opts = JGloss.prefs.getString( Preferences.EXPORT_LATEX_DOCUMENTCLASS_OPTIONS);
        if (opts != null)
            preamble += "[" + opts + "]";
        preamble += "{" + JGloss.prefs.getString( Preferences.EXPORT_LATEX_DOCUMENTCLASS) + "}\n";
        if (writeReading) {
            preamble += "\\usepackage";
            opts = JGloss.prefs.getString( Preferences.EXPORT_LATEX_RUBY_OPTIONS);
            if (opts != null)
                preamble += "[" + opts + "]";
            preamble += "{ruby-annotation}\n";
        }
        preamble += JGloss.prefs.getString( Preferences.EXPORT_LATEX_PREAMBLE) + "\n";
        out.write( preamble);
        out.write( "\\begin{document}\n\n");

        out.write( outtext.toString());
        out.write( "\n\\end{document}\n");
    }

    /**
     * Escapes the LaTeX special characters.
     *
     * @param in The string to escape.
     * @param The escaped string.
     */
    private static String escape( String in) {
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
                buf.replace( i, i+1, "\\verb/~/");
                break;
            case '^':
                buf.replace( i, i+1, "\\verb/^/");
                break;
            }
        }

        return buf.toString();
    }
} // class PlainTextExporter
