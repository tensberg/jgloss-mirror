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
     * Current version of JGloss. Will be written to the generator meta-tag
     * of the generated file.
     */
    public static int JGLOSS_VERSION = 93;
    
    /**
     * Major version of the JGloss file format. The major version is changed if a
     * new file format revision has changes incompatible to previous versions.
     */
    public static int FORMAT_MAJOR_VERSION = 2;
    /**
     * Minor version of the JGloss file format. The minor version is changed if a new
     * file format revision is changed in a way that it still is compatible to earlier
     * formats.
     */
    public static int FORMAT_MINOR_VERSION = 1;

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
     * Flag if the generator meta tag already exists in the document. If not, it will be
     * generated when the document is written. The generator will contain the version of
     * JGloss and of the JGloss file format.
     */
    protected boolean generatorTagExists;
    /**
     * Flag if the content meta tag already exists in the document.
     */
    protected boolean contentTypeTagExists;

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

        generatorTagExists = false;
        // search for existing meta tag
        Element e = doc.getDefaultRootElement();
        // search the "body" element
        int i = 0;
        while (i < e.getElementCount()) {
            Element ec = e.getElement( i);
            if (ec.getName().equalsIgnoreCase( HTML.Tag.HEAD.toString()) || 
                ec.getName().equalsIgnoreCase( HTML.Tag.IMPLIED.toString())) {
                // poor man's recursion: iterate over children of ec
                e = ec;
                i = 0;
                continue;
            }
            else if (ec.getAttributes().containsAttribute
                     ( StyleConstants.NameAttribute, HTML.Tag.META)) {
                AttributeSet attr = ec.getAttributes();
                if (attr.containsAttribute( HTML.Attribute.NAME, "generator")) {
                    doc.setAttribute( (MutableAttributeSet) attr, HTML.Attribute.CONTENT,
                                      getFileVersionString());
                    generatorTagExists = true;
                } else if (attr.containsAttribute( HTML.Attribute.HTTPEQUIV, "content-type")) {
                    doc.setAttribute( (MutableAttributeSet) attr, HTML.Attribute.CONTENT,
                                      "text/html; charset=" + getCharacterEncoding());
                    contentTypeTagExists = true;
                }
            }

            i++;
        }
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
     * for annotation elements, and to normalize the dict_word and dict_reading attributes.
     */
    protected void startTag( Element elem) throws IOException, BadLocationException {
        AttributeSet attr = elem.getAttributes();
        if (attr.getAttribute( StyleConstants.NameAttribute).equals( AnnotationTags.ANNOTATION)) {
            skipLineSeparator = true;

            // Normalize the dict_word and dict_reading attributes.
            // If the dict_word attribute equals the annotated word, remove it
            String dictWord = (String) attr.getAttribute( JGlossDocument.DICTIONARY_WORD);
            if (dictWord != null) {
                Element wordElement = elem.getElement( 1);
                try {
                    String wordText = doc.getText( wordElement.getStartOffset(),
                                                   wordElement.getEndOffset()-
                                                   wordElement.getStartOffset());
                    if (wordText.equals( dictWord))
                        doc.setAttribute( (MutableAttributeSet) attr, 
                                          JGlossDocument.DICTIONARY_WORD, null);
                } catch (BadLocationException ex) {}
            }
            // If the dict_reading attribute equals the reading of the word, remove it
            String dictReading = (String) attr.getAttribute( JGlossDocument.DICTIONARY_READING);
            if (dictReading != null) {
                Element readingElement = elem.getElement( 0);
                try {
                    String readingText = doc.getText( readingElement.getStartOffset(),
                                                      readingElement.getEndOffset()-
                                                      readingElement.getStartOffset());
                    if (readingText.equals( dictReading))
                        doc.setAttribute( (MutableAttributeSet) attr, 
                                          JGlossDocument.DICTIONARY_READING, null);
                } catch (BadLocationException ex) {}
            }
        }
        super.startTag( elem);
        skipLineSeparator = false;
    }


    /**
     * Writes an end tag. Overridden to suppress the writing of the line separator char
     * for annotation elements.
     */
    protected void endTag( Element elem) throws IOException {
        if (elem.getName().equals( HTML.Tag.HEAD.toString()) && !generatorTagExists) {
            String generator = "<meta name=\"generator\" content=\"" + getFileVersionString() +
                "\">\n";
            nonEscapedOutput( generator.toCharArray(), 0, generator.length());
        }

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

    /**
     * Returns the file format version string. This string will be embedded in a generator
     * meta tag in the written JGloss document.
     *
     * @return The file format string, containing the current JGloss version and the version of
     *         the JGloss file format.
     */
    public static String getFileVersionString() {
        return "JGloss " + (JGLOSS_VERSION/100) + "." + (JGLOSS_VERSION%100) + 
            "; file format version " +
            FORMAT_MAJOR_VERSION + "." + FORMAT_MINOR_VERSION;
    }

    /**
     * Parse the file version generated by {@link #getFileVersionString getFileVersionString}.
     *
     * @param version The version string.
     * @return Array of three <CODE>Integers</CODE>: JGloss version (*100), file format major version
     *         and file format minor version; or <CODE>null</CODE> if the version string could not be
     *         parsed.
     */
    public static Integer[] parseFileVersionString( String version) {
        Integer[] out = new Integer[3];

        int i = version.indexOf( ';');
        // get the JGloss version
        if (i==-1 || i==version.length()-1)
            return null;
        String v = version.substring( 0, i).trim();
        int j = v.lastIndexOf( ' ');
        if (j == -1)
            return null;
        try {
            out[0] = new Integer( (int) (Double.parseDouble( v.substring( j+1))*100));
        } catch (NumberFormatException ex) {
            return null;
        }

        // get the file format version
        j = version.indexOf( ';', i+1); // currently not used, but might be introduced in later versions
        if (j == -1)
            j = version.length();
        v = version.substring( i+1, j).trim();
        j = v.lastIndexOf( ' ');
        if (j == -1)
            return null;
        v = v.substring( j+1);
        j = v.indexOf( '.');
        if (j == -1)
            return null;
        try {
            out[1] = new Integer( v.substring( 0, j)); // major version
            out[2] = new Integer( v.substring( j+1)); // minor version
        } catch (NumberFormatException ex) {
            return null;
        }

        return out;
    }
} // class JGlossWriter
