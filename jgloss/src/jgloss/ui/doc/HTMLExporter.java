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
 * for the markup of the annotations.
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
     * Flag if the the generated HTML should be backwards compatible.
     */
    private boolean backwardsCompatible;
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
     * @param doc The document to write.
     * @param writeReading Flag if reading annotations should be written in the HTML document.
     * @param writeTranslations Flag if translation annotations should be written in the HTML document.
     * @param backwardsCompatible Flag if the the generated HTML should be backwards compatible.
     * @param writeHidden Flag if annotations marked as hidden should be written.
     */
    public HTMLExporter( Writer out, String encoding, JGlossDocument doc,
                         boolean writeReading, boolean writeTranslations,
                         boolean backwardsCompatible, boolean writeHidden) {
        super( out, encoding, doc);
        skipAll = false;
        this.writeReading = writeReading;
        this.writeTranslations = writeTranslations;
        this.backwardsCompatible = backwardsCompatible;
        this.writeHidden = writeHidden;
    }


    /**
     * Writes out a start tag for the element. This is overridden to treat annotation elements
     * specially.
     */
    protected void startTag( Element elem)
        throws IOException, BadLocationException {
        if (skipAll) // don't write descendants of an annotation element
            return;

        if (isAnnotationElement( elem)) {
            // construct an HTML element which will contain the reading and translation
            // annotations as attributes and enclose the kanji text.
            Element wordelement = elem.getElement( 0);
            StringBuffer word = new StringBuffer( 20); // base text including <rb> markup
            StringBuffer base = new StringBuffer( 10); // base text without <rb> markup

            String translation = "";
            boolean writeThisTranslation = writeTranslations && (writeHidden || !isHidden( elem));
            if (writeThisTranslation) {
                translation = doc.getText( elem.getElement( 1).getStartOffset(),
                                           elem.getElement( 1).getEndOffset()-
                                           elem.getElement( 1).getStartOffset());
                writeThisTranslation &= !" ".equals( translation);
            }

            StringBuffer reading = new StringBuffer( 10);
            boolean writeThisReading = writeReading && (writeHidden || !isHidden( elem));

            // if useSimpleRuby is set, ruby annotations of the form
            // <ruby><rb>base text</rb><rt>reading</rt></ruby> will be generated for every
            // READING_BASETEXT element, and a BASETEXT without reading will be inserted as plain text.
            // If not set, a complex ruby annotation of the form
            // <ruby><rbc><rb>base<rb><rb...</rbc><rtc><rt>reading...</rtc>
            // <rtc span=...><rt>translation</rt></rtc></ruby> will be generated for the
            // whole annotation element.
            boolean useSimpleRuby = backwardsCompatible || !writeThisTranslation;
            StringBuffer rtc = null; // reading including <rt> markup
            if (!useSimpleRuby)
                rtc = new StringBuffer( 30);

            for ( int i=0; i<wordelement.getElementCount(); i++) {
                Element child = wordelement.getElement( i);
                if (child.getElementCount() == 2) { // READING_BASETEXT element
                    // extract BASETEXT text
                    String thisbase = doc.getText( child.getElement( 1).getStartOffset(),
                                                   child.getElement( 1).getEndOffset() -
                                                   child.getElement( 1).getStartOffset());
                    base.append( thisbase);
                    String thisreading = " ";
                    if (writeThisReading) {
                        thisreading = doc.getText( child.getElement( 0).getStartOffset(),
                                                   child.getElement( 0).getEndOffset() -
                                                   child.getElement( 0).getStartOffset());
                    }
                    if (!" ".equals( thisreading)) {
                        reading.append( thisreading);
                        if (useSimpleRuby)
                            word.append( "<ruby><rb>" + thisbase + "</rb><rp>\u300a</rp><rt>"
                                         + thisreading + "</rt><rp>\u300b</rp></ruby>");
                        else {
                            word.append( "<rb>" + thisbase + "</rb>");
                            rtc.append( "<rt>" + thisreading + "</rt>");
                        }
                    }
                    else if (useSimpleRuby)
                        word.append( thisbase);
                    else {
                        word.append( "<rb>" + thisbase + "</rb>");
                        rtc.append( "<rt></rt>");
                    }
                }
                else { // BASETEXT element
                    String thisbase = doc.getText( child.getStartOffset(),
                                                   child.getEndOffset() -
                                                   child.getStartOffset());
                    base.append( thisbase);
                    if (i < wordelement.getElementCount()-1) // don't include inflection
                        reading.append( thisbase);
                    if (useSimpleRuby)
                        word.append( thisbase);
                    else {
                        word.append( "<rb>" + thisbase + "</rb>");
                        rtc.append( "<rt></rt>");
                    }
                }
            }

            if (writeThisReading || writeThisTranslation) {
                if (backwardsCompatible) {
                    // Write a span tag with a mouseover trigger which shows the reading and
                    // translation as popup.
                    MutableAttributeSet attr = new SimpleAttributeSet();
                
                    attr.addAttribute( "class", "an");
                    attr.addAttribute( "onMouseover", "sp(this,&quot;" + reading.toString() + 
                                       "&quot;,&quot;" + translation + "&quot;)");
                    attr.addAttribute( "onMouseout", "hp(this)");
                    write( "<span");
                    writeAttributes( attr);
                    write( ">");
                }
                
                if (useSimpleRuby)
                    write( word.toString());
                else {
                    write( "<ruby><rbc>");
                    write( word.toString());
                    write( "</rbc><rtc>");
                    write( rtc.toString());
                    write( "</rtc>");
                    if (writeThisTranslation) {
                        write( "<rtc");
                        if (wordelement.getElementCount() > 1)
                            write( " span=\"" + wordelement.getElementCount() + "\"");
                        write( "><rt>");
                        write( translation);
                        write( "</rt></rtc></ruby>");
                    }
                }

                if (backwardsCompatible) {
                    write( "</span>");
                }
            }
            else
                write( base.toString()); // write unannotated word

            // skip all output until this element is closed
            skipAll = true;
        } else if (backwardsCompatible && (writeReading || writeTranslations) &&
                   elem.getAttributes().getAttribute( StyleConstants.NameAttribute)
                   .equals( HTML.Tag.BODY)) {
            // add the document setup handler
            doc.setAttribute
                ( (MutableAttributeSet) elem.getAttributes(),
                  "onLoad", "setupDoc()", false);
            super.startTag( elem);
            doc.setAttribute
                ( (MutableAttributeSet) elem.getAttributes(), "onLoad", null, false);
        }
        else
            super.startTag( elem);
    }

    protected void endTag( Element elem) throws IOException {
        if (isAnnotationElement( elem)) {
            skipAll = false;
            return;
        }

        if (skipAll)
            return;

        if (backwardsCompatible && writeTranslations &&
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

        if (backwardsCompatible && writeTranslations &&
            elem.getAttributes().getAttribute( StyleConstants.NameAttribute)
            .equals( HTML.Tag.BODY)) {
            // write DIV for floating popup
            write( "<div id=\"reading-popup\" class=\"popup\">&nbsp;</div>\n");
            write( "<div id=\"translation-popup\" class=\"popup\">&nbsp;</div>\n");
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

    /**
     * Tests if the HIDDEN attribute of an annotation element is set.
     */
    protected boolean isHidden( Element elem) {
        return JGlossDocument.HIDDEN_ATTRIBUTE_TRUE.equals( elem.getAttributes().getAttribute
                                                            ( JGlossDocument.HIDDEN_ATTRIBUTE));
    }
} // class HTMLExporter
