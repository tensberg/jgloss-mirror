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
    public static final String ROOT_ELEMENT_NAME = "jgloss";
    
    public final static String ENCODING_UTF8 = "UTF-8";
    
    protected boolean inHead = false;
    protected boolean writingEmptyTag = false;

    public XMLExporter( Writer out, JGlossDocument doc) {
        super( out, ENCODING_UTF8, doc);
    }

    protected void startTag( Element elem) throws IOException, BadLocationException {
        AttributeSet attr = elem.getAttributes();
        if (matchNameAttribute( attr, HTML.Tag.HTML))
            writeProlog();
        else if (matchNameAttribute( attr, HTML.Tag.HEAD)) {
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

        super.emptyTag( elem);
    }

    protected void writeProlog() throws IOException {
        write( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        writeLineSeparator();
        write( "<" + ROOT_ELEMENT_NAME + ">");
        writeLineSeparator();
    }

    protected void writeEpilog() throws IOException {
        write( "</" + ROOT_ELEMENT_NAME + ">");
        writeLineSeparator();
    }

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
