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
     * @param documentName Name of the document as chosen by the user.
     * @param model Annotation model which contains the annotations of the document.
     * @param out Writer to which the document will be written.
     * @param writeReading <CODE>true</CODE> if the reading annotations should be written.
     * @param writeTranslations <CODE>true</CODE> if the translation annotations should be written.
     * @param translationsOnPage <CODE>true</CODE> if translations should be placed on the same page
     *                           as the word, or in a separate list at the end of the text. This flag
     *                           is ignored if <CODE>writeTranslations</CODE> is <CODE>false</CODE>.
     * @param writeHidden <CODE>true</CODE> if annotations marked as hidden should be written.
     * @exception IOException
     */
    public static void export( JGlossDocument doc, String documentName,
                               AnnotationModel model, Writer out, 
                               boolean writeReading, boolean writeTranslations,
                               boolean translationsOnPage,
                               boolean writeHidden) throws IOException {
        String text = null;
        try {
            text = doc.getText( 0, doc.getLength());
        } catch (BadLocationException ex) {}
        StringBuffer outtext = new StringBuffer( text.length());
        StringBuffer translations = new StringBuffer();
        String longestword = ""; // longest annotated word in the document
        String longestreading = ""; // longest reading of any annotated word
        
        int prevend = 0; // end offset of the previous annotation
        for ( int i=0; i<model.getAnnotationCount(); i++) {
            AnnotationNode annotation = model.getAnnotationNode( i);
            Element ae = annotation.getAnnotationElement();

            // add text between annotations
            if (prevend < ae.getStartOffset())
                outtext.append( escape( text.substring( prevend, ae.getStartOffset())));
            prevend = ae.getEndOffset();

            // handle translation text
            String translation = escape( text.substring( ae.getElement( 1).getStartOffset(), 
                                                         ae.getElement( 1).getEndOffset()));
            if (writeTranslations && !translation.equals( " ") && 
                (writeHidden || !annotation.isHidden())) {
                // if the linked annotation entry is an inflected verb or adjective,
                // output the full verb instead of just the kanji part
                String nb = annotation.getDictionaryFormNode().getWord();
                String nr = annotation.getDictionaryFormNode().getReading();

                StringBuffer footnote = new StringBuffer( 32);
                footnote.append( "\\fn{");
                footnote.append( nb);
                footnote.append( "}{");
                if (nr != null)
                    footnote.append( nr);
                footnote.append( "}{");
                footnote.append( translation);
                footnote.append( "}");
                if (translationsOnPage) {
                    // place translation as footnote on the current page
                    outtext.append( footnote.toString());
                }
                else {
                    // place translation in separate list
                    translations.append( footnote.toString());
                    translations.append( '\n');
                }

                // The longest word and reading are used for proper alignment
                if (longestword.length() < nb.length())
                    longestword = nb;
                if (longestreading.length() < nr.length())
                    longestreading = nr;
            }

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
        }
        // add the remaining text
        if (prevend < text.length())
            outtext.append( escape( text.substring( prevend, text.length())));

        // Insert an additional line for every \n, which is LaTeX's mark for a
        // paragraph break.
        for ( int i=outtext.length()-1; i>=0; i-=1) {
            if (outtext.charAt( i)=='\n') {
                if (i>0 && outtext.charAt( i-1)=='\n') {
                    // an empty paragraph in the original document,
                    // probably used as a separator. Insert some blank
                    // space in the LaTeX document.
                    outtext.insert( i, "\\bigskip\n");
                }
                else {
                    // End of a paragraph in the original document. Generate LaTeX paragraph end mark.
                    outtext.insert( i, '\n');
                }
            }
        }
        
        out.write( JGloss.messages.getString( "export.latex.header", new Object[]
            { documentName, new Date() }));

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

        StringBuffer lengths = new StringBuffer( 256);
        // generate length calculation for size of the longest word and reading
        lengths.append( "\\newlength{\\ww}\n\\settowidth{\\ww}{");
        lengths.append( longestword);
        lengths.append( "--}\n");
        
        lengths.append( "\\newlength{\\rw}\n\\settowidth{\\rw}{");
        lengths.append( longestreading);
        lengths.append( "--}\n\n");
        
        // length calculation for the translation text
        lengths.append( "\\newlength{\\tw}\n");
        lengths.append( "\\setlength{\\tw}{\\textwidth}\n");
        lengths.append( "\\addtolength{\\tw}{-1.0\\ww}\n");
        lengths.append( "\\addtolength{\\tw}{-1.0\\rw}\n");
        
        if (doc.getTitle() != null) {
            out.write( "\\pagestyle{myheadings}\n\\markright{" + escape( doc.getTitle()) + "}\n");
        }

        if (translationsOnPage) {
            out.write( lengths.toString());
            out.write( "\\addtolength{\\tw}{-20pt}\n"); // I'm not sure where the 20pt come from
            // define footnote command for annotations
            out.write( "\\newcommand{\\fn}[3]{\\footnotetext{\\makebox[\\ww][l]{#1}" + 
                       " \\makebox[\\rw][l]{#2} \\parbox[t]{\\tw}{#3}}}\n\n");
        }

        out.write( outtext.toString());

        if (!translationsOnPage && translations.length() > 0) {
            // append translation list to the end of the document
            out.write( "\n\n\\newpage\n\\small\n");
            if (doc.getTitle() != null) {
                out.write( "\\markright{" + escape( doc.getTitle()) + 
                           JGloss.messages.getString( "export.latex.vocabulary") + "}\n");
            }
            out.write( lengths.toString());
            out.write( "\\addtolength{\\tw}{-4pt}\n"); // I'm not sure where the 4pt come from
            // define footnote command for annotations
            out.write( "\\newcommand{\\fn}[3]{\\noindent \\makebox[\\ww][l]{#1}" + 
                       " \\makebox[\\rw][l]{#2} \\parbox[t]{\\tw}{#3}}\n\n");

            out.write( translations.toString());
            out.write( "\n");
        }

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
} // class PlainTextExporter
