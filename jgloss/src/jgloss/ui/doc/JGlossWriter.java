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
 * Write a JGloss document. The standard HTMLWriter will encode all characters
 * not in the ASCII range as character entities, even if the writers charset supports
 * the characters. Since this Writer is always used with UTF-8 encoding, it modifies the
 * behavior of the superclass to directly write these characters. 
 *
 * @author Michael Koch
 */
public class JGlossWriter extends HTMLWriter {
    /**
     * Document which will be written.
     */
    protected JGlossDocument doc;

    /**
     * Character encoding used by the writer.
     */
    protected String encoding;

    /**
     * Flag if the writing of the line separator should be suppressed.
     */
    protected boolean skipLineSeparator;
    
    /**
     * Creates a new writer for a JGloss document which outputs to the given writer.
     * The writer must support the full charset of the characters used in the document.
     *
     * @param out The writer to which the document will be written.
     * @param encoding The character encoding used by the writer.
     * @param doc The document to write.
     */
    public JGlossWriter( Writer out, String encoding, JGlossDocument doc) {
        super( out, doc);
        this.encoding = encoding;
        this.doc = doc;
    }

    /**
     * Outputs a range of characters. Overridden to prevent the substitution of non-ASCII characters
     * with character entities. HTML special characters will still be substituted.
     *
     * @param chars The characters to write.
     * @param start Start offset in the character array.
     * @param length Number of characters to write.
     * @exception java.io.IOException if an error occurs during writing.
     */
    protected void output( char[] chars, int start, int length)
        throws IOException {
        if (length == 0)
            return;

        // search for subsections which contain non-ASCII characters.
        int from = start;
        int to = from + 1;
        boolean ascii = (chars[from] <= 127);
        while (to < start+length) {
            if (ascii != (chars[to] <= 127)) {
                if (ascii)
                    super.output( chars, from, to-from);
                else
                    nonEscapedOutput( chars, from, to-from);
                from = to;
                ascii = !ascii;
            }
            to++;
        }

        // write the remainder
        if (ascii)
            super.output( chars, from, to-from);
        else
            nonEscapedOutput( chars, from, to-from);
    }

    /**
     * Writes an attribute set. Overridden to encode the JGlossDocument.TEXT_ANNOTATION
     * attribute value, which is a list of text annotations, with a string encoding 
     * suitable as an HTML attribute value.
     *
     * @param attr The attribute set to write.
     * @exception java.io.IOException if an error occurs during writing.
     */
    protected void writeAttributes( AttributeSet attr) throws IOException {
        if (attr.isDefined( StyleConstants.NameAttribute) &&
            attr.getAttribute( StyleConstants.NameAttribute).equals( HTML.Tag.META)) {
            if (attr.isDefined( HTML.Attribute.HTTPEQUIV) &&
                attr.getAttribute( HTML.Attribute.HTTPEQUIV).equals( "content-type")) {
                // META-Tag, replace the charset with the one of the writer
                doc.setAttribute( (MutableAttributeSet) attr, HTML.Attribute.CONTENT, 
                                  "text/html; charset=" + getCharacterEncoding());
            }
        }

        if (attr.isDefined( JGlossDocument.TEXT_ANNOTATION)) {
            attr = new SimpleAttributeSet( attr);
            doc.encodeAnnotation( (MutableAttributeSet) attr);
        }

        super.writeAttributes( attr);
    }

    /**
     * Writes an array of non-ASCII characters directly to the writer, without using
     * escape sequences.
     *
     * @param chars The characters to write.
     * @param start Start offset in the character array.
     * @param length Number of characters to write.
     * @exception java.io.IOException if an error occurs during writing.
     */
    protected void nonEscapedOutput( char[] chars, int start, int length) throws IOException {
        getWriter().write( chars, start, length);
        setCurrentLineLength( getCurrentLineLength() + length);
    }

    /**
     * Writes a start tag. Overridden to suppress the writing of the line separator char
     * for annotation elements.
     */
    protected void startTag( Element elem) throws IOException, BadLocationException {
        if (elem.getAttributes()
            .getAttribute( StyleConstants.NameAttribute).equals( AnnotationTags.ANNOTATION))
            skipLineSeparator = true;
        super.startTag( elem);
        skipLineSeparator = false;
    }


    /**
     * Writes an end tag. Overridden to suppress the writing of the line separator char
     * for annotation elements.
     */
    protected void endTag( Element elem) throws IOException {
        if (elem.getAttributes()
            .getAttribute( StyleConstants.NameAttribute).equals( AnnotationTags.ANNOTATION))
            skipLineSeparator = true;
        super.endTag( elem);
        skipLineSeparator = false;
    }

    /**
     * Prevent linebreaks. This method always returns false to prevent line breaks, which would
     * mess up the document layout when it is re-opened.
     *
     * @return Always <CODE>false</CODE>.
     */
    protected boolean getCanWrapLines() {
        return false;
    }

    /**
     * Returns the indent space for a new line. Overridden to always return 0 to prevent 
     * indents from messing up the layout when the document is re-opened.
     *
     * @return Always <CODE>0</CODE>.
     */
    protected int getIndentSpace() {
        return 0;
    }

    /**
     * Returns the character encoding used by the underlying writer.
     *
     * @return The character encoding.
     */
    protected String getCharacterEncoding() {
        return encoding;
    }

    /**
     * Write the line separator. Overridden to allow the suppression of the line separator
     * for annotation elements because this can break the layout when the document is loaded.
     */
    protected void writeLineSeparator() throws IOException {
        if (!skipLineSeparator)
            super.writeLineSeparator();
    }
} // class JGlossWriter
