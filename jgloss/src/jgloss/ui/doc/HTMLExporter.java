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

package jgloss.ui.doc;

import jgloss.dictionary.*;

import java.io.*;
import java.util.*;

import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * Exports the JGloss document as HTML file. The writer uses the Ruby Annotation
 * W3C recommendation (see <A HREF="http://www.w3.org/TR/ruby/">http://www.w3.org/TR/ruby/</A>
 * for the markup of the annotations. Optionally, the exported HTML will be made interactive
 * by only displaying the annotation when the mouse hovers over the text.
 *
 * @author Michael Koch
 */
public class HTMLExporter extends JGlossWriter {
    /**
     * Path to the script fragment resource which will be embedded in the
     * HEAD part of the HTML file.
     */
    private final static String SCRIPT_RESOURCE = "/data/HTMLExporter";

    /**
     * The writer will skip all output when this is <CODE>true</CODE>. It is used to skip everything
     * inside an annotation element.
     */
    private boolean skipAll;

    /**
     * Flag if reading annotations should be written in the HTML document.
     */
    private boolean writeReading;
    /**
     * Flag if translation annotations should be written in the HTML document.
     */
    private boolean writeTranslations;
    /**
     * Flag if the the generated HTML should be more backwards
     * compatible.
     */
    private boolean backwardsCompatible;
    /**
     * Flag if the generated HTML document should display popups for annotations.
     */
    private boolean interactive;
    /**
     * Flag if annotations marked as hidden should be written.
     */
    private boolean writeHidden;
    /**
     * The non-breakable space named entity as char array.
     */
    private static final char[] NBSP = "&nbsp;".toCharArray();

    /**
     * Creates a new HTML writer for a JGloss document which outputs to the given writer.
     *
     * @param out The writer to which the document will be written.
     * @param encoding Character encoding the writer uses.
     * @param writeReading Flag if reading annotations should be written in the HTML document.
     * @param writeTranslations Flag if translation annotations should be written in the HTML document.
     * @param backwardsCompatible Flag if the the generated HTML should be more backwards
     *        compatible. This mainly adds parentheses around the annotations, which will have
     *        to be removed with javascript on browsers which support HTML Ruby Annotations.
     * @param interactive Flag if the generated HTML document should display popups for annotations.
     * @param writeHidden Flag if annotations marked as hidden should be written.
     * @param doc The document to write.
     */
    public HTMLExporter( Writer out, String encoding, JGlossDocument doc,
                         boolean writeReading, boolean writeTranslations,
                         boolean backwardsCompatible,
                         boolean interactive, boolean writeHidden) {
        super( out, encoding, doc);
        skipAll = false;
        this.writeReading = writeReading;
        this.writeTranslations = writeTranslations;
        this.backwardsCompatible = backwardsCompatible;
        this.interactive = interactive;
        this.writeHidden = writeHidden;
    }


    /**
     * Writes out a start tag for the element. This is overridden to treat annotation elements
     * specially.
     */
    protected void startTag( Element elem)
        throws IOException, BadLocationException {
        if (skipAll)
            return;

        if (isAnnotationElement( elem)) {
            // construct an HTML element which will contain the reading and translation
            // annotations as attributes and enclose the kanji text.

            String word = doc.getText( elem.getElement( 1).getStartOffset(),
                                       elem.getElement( 1).getEndOffset()-
                                       elem.getElement( 1).getStartOffset());
            if ((writeReading || writeTranslations) && (writeHidden ||
                !JGlossDocument.HIDDEN_ATTRIBUTE_TRUE.equals( elem.getAttributes().getAttribute
                                                              ( JGlossDocument.HIDDEN_ATTRIBUTE)))) {
                word = "<rbc><rb>" + word + "</rb></rbc>";
                String reading = doc.getText( elem.getElement( 0).getStartOffset(),
                                           elem.getElement( 0).getEndOffset()-
                                           elem.getElement( 0).getStartOffset());
                if (!reading.equals( " ")) {
                    if (backwardsCompatible)
                        reading = "\u300a" + reading + "\u300b";
                    reading = "<rtc><rt class=\"re\">" + reading + "</rt></rtc>";
                }
                String translation = doc.getText( elem.getElement( 2).getStartOffset(),
                                                  elem.getElement( 2).getEndOffset()-
                                                  elem.getElement( 2).getStartOffset());
                if (!translation.equals( " ")) {
                    if (backwardsCompatible)
                        translation = "{" + translation + "}";
                    translation = "<rtc><rt class=\"tr\">" + translation + "</rt></rtc>";
                }
                
                // construct a new attribute set with special attributes removed
                MutableAttributeSet attr = new SimpleAttributeSet( elem.getAttributes());
                attr.removeAttribute( JGlossDocument.TEXT_ANNOTATION);
                attr.removeAttribute( JGlossDocument.LINKED_ANNOTATION);
                attr.removeAttribute( JGlossDocument.HIDDEN_ATTRIBUTE);
                
                attr.addAttribute( "class", "an");
                if (interactive) {
                    attr.addAttribute( "onClick", "tp(this)");
                    attr.addAttribute( "onMouseover", "sp(this)");
                    attr.addAttribute( "onMouseout", "hp(this)");
                }
                write( "<span ");
                writeAttributes( attr);
                write( ">");
                
                write( "<ruby>" + word);
                
                if (writeReading && !reading.equals( " "))
                    write( reading);
                if (writeTranslations && !translation.equals( " "))
                    write( translation);
            }
            else
                write( word);

            // skip all output until this element is closed
            skipAll = true;
        } else if ((writeReading || writeTranslations) &&
                   elem.getAttributes().getAttribute( StyleConstants.NameAttribute)
                   .equals( HTML.Tag.BODY)) {
            // add the document setup handler
            ((JGlossDocument) elem.getDocument()).setAttribute
                ( (MutableAttributeSet) elem.getAttributes(),
                  "onLoad", "setupDoc(" + String.valueOf( backwardsCompatible) + ","
                  + String.valueOf( interactive) + ")");
            super.startTag( elem);
        }
        else
            super.startTag( elem);
    }

    protected void endTag( Element elem) throws IOException {
        if (isAnnotationElement( elem)) {
            skipAll = false;
            if ((writeReading || writeTranslations) && (writeHidden ||
                !JGlossDocument.HIDDEN_ATTRIBUTE_TRUE.equals( elem.getAttributes().getAttribute
                                                              ( JGlossDocument.HIDDEN_ATTRIBUTE)))) {
                write( "</ruby></span>");
            }
            return;
        }

        if ((writeReading || writeTranslations) &&
            elem.getAttributes().getAttribute( StyleConstants.NameAttribute)
            .equals( HTML.Tag.HEAD)) {
            try {
                Reader resource = new InputStreamReader( HTMLExporter.class
                                                         .getResourceAsStream( SCRIPT_RESOURCE), "UTF-8");
                char[] buf = new char[512];
                StringBuffer script = new StringBuffer();
                int r;
                while ((r=resource.read( buf)) != -1) {
                    script.append( buf, 0, r);
                }
                resource.close();
                
                write( script.toString());
            } catch (Exception ex) {
                // Could not load resource? Bad luck.
            }
        }            

        super.endTag( elem);
    }

    protected void text( Element elem) throws BadLocationException, IOException {
        if (skipAll)
            return;

        super.text( elem);
    }

    protected void writeEmbeddedTags( AttributeSet attr) throws IOException {
        if (!skipAll)
            super.writeEmbeddedTags( attr);
    }

    protected void output( char[] content, int start, int length) throws IOException {
        int last = start;
        // replace non-breakable spaces with the "&nbsp;" entity
        for ( int i=start; i<start+length; i++) {
            if (content[i] == '\u00a0') {
                if (i > last)
                    super.output( content, last, i-last);

                // the superclass would replace the & of the &nbsp; with a &amp;, so we have
                // to go directly to the writer
                getWriter().write( NBSP);
                setCurrentLineLength( getCurrentLineLength() + NBSP.length);

                last = i + 1;
            }
        }
        // write the remaining chars
        if (last < start+length)
            super.output( content, last, start+length-last);
    }

    /**
     * Tests if the given element is an annotation element.
     * 
     * @param elem The element to test.
     * @return <CODE>true</CODE> if the element is an annotation element.
     */
    protected boolean isAnnotationElement( Element elem) {
        return elem.getAttributes().getAttribute( StyleConstants.NameAttribute)
            .equals( AnnotationTags.ANNOTATION);
    }
} // class HTMLExporter
