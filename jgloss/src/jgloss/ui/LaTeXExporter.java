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
     * Map with LaTeX special characters which have to be escaped.
     */
    private final static Map funnyChars;

    static {
        funnyChars = new HashMap( 15);

        // The characters \ { } have to be treated specially because they are used in the
        // commands inserted by the exporter.
        //funnyChars.put( new Character( '\\'), "$\\backslash$");
        //funnyChars.put( new Character( '{'), "\\{");
        //funnyChars.put( new Character( '}'), "\\}");

        funnyChars.put( new Character( '$'), "\\$");
        funnyChars.put( new Character( '#'), "\\#");
        funnyChars.put( new Character( '%'), "\\%");
        funnyChars.put( new Character( '&'), "\\&");
        funnyChars.put( new Character( '_'), "\\_");
        funnyChars.put( new Character( '~'), "\\verb/~/");
        funnyChars.put( new Character( '^'), "\\verb/^/");
    }

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
        
        StringBuffer text = null;
        try {
            text = new StringBuffer( doc.getText( 0, doc.getLength()));
        } catch (BadLocationException ex) {
            // What ?
            ex.printStackTrace();
        }
        
        // The reading and translation annotations are embedded in text.
        // They will be removed and depending on the settings of writeReading and 
        // writeTranslations re-inserted in a new format.
        // Iterate over all annotations. We have to do that from back to front because the index
        // in the text changes.
        for ( int i=model.getAnnotationCount()-1; i>=0; i--) {
            AnnotationNode node = model.getAnnotationNode( i);
            Element ae = node.getAnnotationElement();
            int tstart = ae.getElement( 2).getStartOffset();
            int tend = ae.getElement( 2).getEndOffset();
            String translation = escape( text.substring( tstart, tend));
            int rstart = ae.getElement( 0).getStartOffset();
            int rend = ae.getElement( 0).getEndOffset();
            String reading = escape( text.substring( rstart, rend));
            String base = escape( text.substring( rend, tstart));

            // handle translation text
            String footnote;
            if (writeTranslations && !translation.equals( " ") && 
                (writeHidden || !node.isHidden())) {
                // if the linked annotation entry is an inflected verb or adjective,
                // output the full verb instead of just the kanji part
                String nb = base;
                String nr = reading;
                Parser.TextAnnotation ta = node.getLinkedAnnotation();
                if (ta != null) {
                    if (ta instanceof Translation) {
                        DictionaryEntry de = ((Translation) ta).getDictionaryEntry();
                        nb = de.getWord();
                        nr = de.getReading();
                    }
                }
                footnote = "\\footnotetext{" + nb;
                if (nr!=null && !nr.equals( " "))
                    footnote += " " + nr;
                footnote += " " + translation + "}";
            }
            else
                footnote = ""; // remove translation
            text.replace( tstart, tend, footnote);

            // handle reading text
            if (writeReading && !reading.equals( " ") &&
                (writeHidden || !node.isHidden())) {
                text.replace( rstart, tstart, "\\ruby{" + base + "}{" + reading + "}");
            }
            else {
                // remove the old reading text
                text.replace( rstart, rend, "");
            }
        }

        // Remove the &nbsp;'s (non-breakable spaces) and let LaTeX determine the insets.
        // Also insert an additional line for every \n, which is LaTeX's mark for a
        // paragraph break and escape special characters.
        // This has to be done after the text replacements because afterwards the character
        // positions stored in the elements are not valid any more.
        for ( int i=text.length()-1; i>=0; i-=1) {
            if (text.charAt( i)=='\u00a0') {
                text.deleteCharAt( i);
            }
            else if (text.charAt( i)=='\n')
                text.insert( i, '\n');
            else {
                String s = (String) funnyChars.get( new Character( text.charAt( i)));
                if (s != null)
                    text.replace( i, i+1, s);
            }
        }

        out.write( "\\documentclass{" + JGloss.prefs.getString( Preferences.EXPORT_LATEX_DOCUMENTCLASS)
                   + "}\n" + JGloss.prefs.getString( Preferences.EXPORT_LATEX_PREAMBLE));
        if (writeReading)
            out.write( "\\usepackage[overlap,CJK]{ruby-annotation}\n");
        out.write( "\\begin{document}\n\n");

        out.write( text.toString());
        out.write( "\n\\end{document}\n");
    }

    /**
     * Escapes the LaTeX special characters \, { and } by inserting a \.
     *
     * @param in The string to escape.
     * @param The escaped string.
     */
    private static String escape( String in) {
        StringBuffer buf = new StringBuffer( in);
        for ( int i=buf.length()-1; i >= 0; i--) {
            char c = buf.charAt( i);
            if (c=='\\' || c=='{' || c=='}')
                buf.insert( i, '\\');
        }

        return buf.toString();
    }
} // class PlainTextExporter
