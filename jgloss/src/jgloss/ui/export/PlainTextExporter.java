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

package jgloss.ui.export;

import jgloss.ui.doc.*;
import jgloss.ui.annotation.*;

import java.io.*;

import javax.swing.text.*;

/**
 * Export an annotated JGloss document as plain text file.
 *
 * @author Michael Koch
 */
public class PlainTextExporter {
    /**
     * Exports an annotated JGloss document as plain text file.
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
        } catch (BadLocationException ex) {}

        // The reading and translation annotations are embedded in text.
        // They will be removed and depending on the settings of writeReading and 
        // writeTranslations re-inserted in a new format.
        // Iterate over all annotations. We have to do that from back to front because the index
        // in the text changes.
        for ( int i=model.getAnnotationCount()-1; i>=0; i--) {
            AnnotationNode annotation = model.getAnnotationNode( i);
            Element ae = annotation.getAnnotationElement();
            // handle translation text
            int tstart = ae.getElement( 1).getStartOffset();
            int tend = ae.getElement( 1).getEndOffset();
            String translation = text.substring( tstart, tend);
            if (writeTranslations && !translation.equals( " ") && 
                (writeHidden || !annotation.isHidden()))
                translation = "(" + translation + ")";
            else
                translation = ""; // remove translation
            text.replace( tstart, tend, translation);

            // handle reading text in WORD element
            Element wordelement = ae.getElement( 0);
            for ( int j=wordelement.getElementCount()-1; j>=0; j--) {
                Element child = wordelement.getElement( j);
                if (child.getElementCount() == 2) { // READING_BASETEXT element
                    int rstart = child.getElement( 0).getStartOffset();
                    int rend = child.getElement( 0).getEndOffset();
                    String reading = text.substring( rstart, rend);
                    if (writeReading && !reading.equals( " ") &&
                        (writeHidden || !annotation.isHidden()))
                        // insert after kanji and before translation
                        text.insert( child.getElement( 1).getEndOffset(), "\u300a" + reading + "\u300b");
                    // remove the old reading text
                    text.replace( rstart, rend, "");
                }
                // else: BASETEXT element, nothing to do
            }
        }

        // Replace the two &nbsp;'s (non-breakable spaces) which were inserted 
        // by the HTMLifyReader for every Japanese space.
        for ( int i=text.length()-1; i>=1; i-=1) {
            if (text.charAt( i)=='\u00a0' &&
                text.charAt( i-1)=='\u00a0' ) {
                text.replace( i-1, i+1, "\u3000");
            }
        }

        out.write( text.toString());
    }
} // class PlainTextExporter
