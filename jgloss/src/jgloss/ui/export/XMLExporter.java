/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

import java.io.*;
import java.util.*;

import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * Write a XML representation of the JGloss document. This is basically a hack to the
 * <code>HTMLWriter</code> tweaked to generate well-formed XML. The generated document does
 * not look particularly nice, but it works. The <code>&lt;html&gt;</code> root
 * element is replaced by <code>&lt;jgloss&gt;</code>, <code>meta</code>-Tags in the header
 * are not written and the <code>text_annotation</code> of the <code>anno</code> tag is ignored.
 * Otherwise, the HTML document structure is written unchanged. WARNING: Tags with no corresponding
 * closing tag are not handled correctly. If the HTML document contains any such tags, the generated
 * document will not be well-formed XML.
 *
 * @author Michael Koch
 */
public class XMLExporter extends JGlossWriter {
    /**
     * Name of the root (or document) element of the generated XML file.
     */
    public static final String ROOT_ELEMENT_NAME = "jgloss";
    
    /**
     * Encoding used for the XML document.
     */
    public final static String ENCODING_UTF8 = "UTF-8";
    
    /**
     * Set to <code>true</code> while the <code>head</code> element and its descendants are
     * written.
     */
    protected boolean inHead = false;

    /**
     * If set to <code>true</code> all text until the next closing tag and this tag 
     * will be ignored. The variable will be reset to <code>false</code> when the closing tag is
     * encountered.
     */
    protected boolean skipElement = false;

    /**
     * Creates an XML exporter which will write the given JGloss document to the writer in XML
     * format.
     *
     * @param out The writer to which the document is written. The encoding declaration of
     *            the generated document always specifies "UTF-8", if the writer writes to an
     *            output stream, this encoding must be used.
     * @param doc The JGloss document which is to be exported as XML.
     */
    public XMLExporter( Writer out, JGlossDocument doc) {
        super( out, ENCODING_UTF8, doc);
    }

    protected void startTag( Element elem) throws IOException, BadLocationException {
        AttributeSet attr = elem.getAttributes();
        if (matchNameAttribute( attr, HTML.Tag.HTML))
            // write XML header and ROOT_ELEMENT_NAME instead of HTML element
            writeProlog();
        else if (matchNameAttribute( attr, HTML.Tag.HEAD)) {
            // filter unwanted elements from header and add custom elements.
            super.startTag( elem);
            writeHeadInfo();
            inHead = true;
        }
        else if (inHead)
            return;
        else if (matchNameAttribute( attr, AnnotationTags.ANNOTATION)) {
            // don't output the text_annotation attribute
            Object anno = attr.getAttribute( JGlossDocument.TEXT_ANNOTATION);
            doc.setAttribute( (MutableAttributeSet) attr, JGlossDocument.TEXT_ANNOTATION, null, false);
            super.startTag( elem);
            doc.setAttribute( (MutableAttributeSet) attr, JGlossDocument.TEXT_ANNOTATION, anno, false);
        }
        else
            super.startTag( elem);
    }

    protected void endTag( Element elem) throws IOException {
        if (matchNameAttribute( elem.getAttributes(), HTML.Tag.HEAD)) {
            inHead = false;
            super.endTag( elem);
        }
        else if (matchNameAttribute( elem.getAttributes(), HTML.Tag.HTML))
            writeEpilog();
        else if (!inHead)
            super.endTag( elem);
    }

    protected void emptyTag( Element elem) throws IOException, BadLocationException {
        if (inHead)
            return;

        AttributeSet attr = elem.getAttributes();
        // apparently emptyTag is called for elements which can't have other elements as children
        if ((attr.isDefined( AnnotationTags.READING) ||
             attr.isDefined( AnnotationTags.TRANSLATION))
            && elem.getEndOffset()-elem.getStartOffset() == 1 &&
            doc.getText( elem.getStartOffset(), 1).equals( " ")) {
            // Empty reading/translation tag. The single space is only there so that the
            // HTMLDocument class does not remove the element completely.
            // Output an empty element.
            write( '<');
            if (attr.isDefined( AnnotationTags.READING))
                write( AnnotationTags.READING.getId());
            else
                write( AnnotationTags.TRANSLATION.getId());
            write( " />");
        }
        else
            super.emptyTag( elem);
    }

    /**
     * Writes the XML declaration and the root element open tag.
     */
    protected void writeProlog() throws IOException {
        write( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        writeLineSeparator();
        write( "<" + ROOT_ELEMENT_NAME + ">");
        writeLineSeparator();
    }

    /**
     * Writes the root element close tag.
     */
    protected void writeEpilog() throws IOException {
        write( "</" + ROOT_ELEMENT_NAME + ">");
        writeLineSeparator();
    }

    /**
     * Writes additional information to the <code>head</code> section of the document.
     */
    protected void writeHeadInfo() throws IOException {
        if (doc.getTitle() != null) {
            write( "<title>");
            output( doc.getTitle());
            write( "</title>");
        }
        else
            write( "<title />");
        writeLineSeparator();
    }
} // class XMLExporter
