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

package jgloss.ui.xml;

import java.util.*;
import java.io.*;

/**
 * Reads a plain text file from a reader and formats it as a JGloss XML file without annotations.
 * Every line in the
 * input file will be enclosed in <CODE>&lt;P&gt;</CODE> tags. XML-specific special characters will
 * be replaced by named entities.
 *
 * @author Michael Koch
 */
class JGlossifyReader extends FilterReader {
    /**
     * Buffer for the line currently being read.
     */
    private char[] line;
    /**
     * Position offset in the currently read line.
     */
    private int position;
    /**
     * Buffer for a single char, used to cache chars which have no special meaning in HTML.
     */
    private final char[] singleChar = new char[1];
    /**
     * The last character read from the underlying stream.
     */
    private char lastChar;
    /**
     * <CODE>true</CODE>, when the end of file has been encountered on the underlying stream.
     */
    private boolean eof = false;
    /**
     * Map of HTML special characters which will be replaced by character entities.
     * Keys are <CODE>Character</CODE> instances,
     * values strings with the entities used to replace the chars.
     */
    private static Map funnyChars;
    /** 
     * Flag if a &lt;p&gt; has been opened.
     */
    private boolean inParagraph;
    /**
     * If <code>true</code>, try to detect paragraph breaks. Otherwise, each line from the
     * underlying stream is made to a single paragraph.
     */
    private boolean detectParagraphs;

    private final static String HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
        "<!DOCTYPE jgloss PUBLIC \"" + JGlossDocument.DTD_PUBLIC + "\" \"" +
        JGlossDocument.DTD_SYSTEM + "\">\n" +
        "<jgloss>\n<head>\n";

    private final static char[] NO_PARAGRAPH_START;
    private final static char[] NO_PARAGRAPH_END;

    private final static char[] END_HTML = "</body>\n</jgloss>\n".toCharArray();
    private final static char[] START_PARAGRAPH = "<p>".toCharArray();
    private final static char[] END_PARAGRAPH = "</p>\n".toCharArray();
    private final static char[] EMPTY_PARAGRAPH = "<p></p>\n".toCharArray();

    static {
        funnyChars = new HashMap( 11);
        funnyChars.put( new Character( '&'), "&amp;".toCharArray());
        funnyChars.put( new Character( '<'), "&lt;".toCharArray());
        funnyChars.put( new Character( '>'), "&gt;".toCharArray());
        funnyChars.put( new Character( '"'), "&quot;".toCharArray());
        
        // The following character is a Japanese space. When it apears in a document
        // it is probably used to format the indention at the beginning of a paragraph.
        // Unfortunately, the layout algorithm uses the space as a opportunity for breaking
        // an overlong line. To prevent this I approximate it with two non-breakable spaces.
        funnyChars.put( new Character( '\u3000'), "&#160;&#160;".toCharArray());

        ResourceBundle strings = ResourceBundle.getBundle( "resources/jgloss-ui-HTMLifyReader");
        NO_PARAGRAPH_START = strings.getString( "no_paragraph_start").toCharArray();
        Arrays.sort( NO_PARAGRAPH_START);
        NO_PARAGRAPH_END = strings.getString( "no_paragraph_end").toCharArray();
        Arrays.sort( NO_PARAGRAPH_END);        
    }
    
    /**
     * Constructs a new reader without inserting a HTML title for an underlying reader and which
     * detects paragraph breaks.
     *
     * @exception IOException if an error occurrs when accessing the underlying reader.
     */
    public JGlossifyReader( Reader in) throws IOException {
        this( in, null, true);
    }
    
    /**
     * Constructs a new reader without inserting a HTML title for an underlying reader.
     *
     * @param in The reader from which to read the plain text file.
     * @exception IOException if an error occurrs when accessing the underlying reader.
     */
    public JGlossifyReader( Reader in, boolean detectParagraphs) throws IOException {
        this( in, null, detectParagraphs);
    }

    /**
     * Constructs a new reader which will insert the given HTML title.
     *
     * @param in The reader from which to read the plain text file.
     * @param title The title to insert in the generated HTML document.
     * @param detectParagraphs If <code>false</code>, every line break in the underlying reader 
     *        will be interpreted as a paragraph break. If <code>true</code>, a heuristic is used to
     *        determine between line breaks and paragraph breaks in the document read from the reader.
     * @exception IOException if an error occurrs when accessing the underlying reader.
     */
    public JGlossifyReader( Reader in, String title, boolean detectParagraphs) throws IOException {
        super( new PushbackReader( in));

        this.detectParagraphs = detectParagraphs;

        StringBuffer lineBuf = new StringBuffer( 128);

        lineBuf.append( HEADER);
        lineBuf.append( "<title>");
        if (title != null) {
            lineBuf.append( replaceFunnyChars( title));
        }
        lineBuf.append( "</title>\n");

        lineBuf.append( "</head>\n<body>\n");

        line = lineBuf.toString().toCharArray();
    }

    public int read() throws IOException {
        /* line buffers the data read from the underlying reader and additional HTML markup added
         * by this class.
         * line[position] contains the character which is returned from this call to read(). If
         * line[position] does not exist, read the next char from the underlying reader and interpret it.
         */

        if (position >= line.length) {
            if (eof) {
                // eof at underlying reader and all additional HTML already returned
                return -1;
            }

            position = 0;

            int next = super.read();
            if (next == -1) {
                // eof at underlying reader. Generate HTML document end tags.
                eof = true;
                line = END_HTML;
                if (inParagraph) {
                    line = new StringBuffer( 32).append( END_PARAGRAPH).append( END_HTML).toString()
                        .toCharArray();
                    inParagraph = false;
                }
            }
            else {
                if (isLineBreak( (char) next)) {
                    // Usually, for each line a paragraph is created. A line break thus marks the
                    // end of the paragraph.
                    if (inParagraph) {
                        boolean closeParagraph = true;
                        if (detectParagraphs) {
                            // if the last line ended with a normal (not punctuation mark) char, and
                            // the new line starts with a normal char, no paragraph break will be generated
                            if (!endsParagraph( lastChar)) {
                                next = in.read(); // first character in the following input line
                                if (next != -1) {
                                    char c = (char) next;
                                    if (!startsParagraph( c)) {
                                        // no paragraph break
                                        line = escape( c);
                                        closeParagraph = false;
                                    }
                                    else
                                        ((PushbackReader) in).unread( c);
                                }
                            }
                        }
                            
                        if (closeParagraph) {
                            line = END_PARAGRAPH;
                            inParagraph = false;
                        }
                    }
                    else {
                        // two adjacent line breaks: empty line; generate empty paragraph
                        line = EMPTY_PARAGRAPH;
                    }
                }
                else {
                    if (!inParagraph) {
                        line = START_PARAGRAPH;
                        inParagraph = true;
                        ((PushbackReader) in).unread( next);
                    }
                    else {
                        line = escape( (char) next);
                    }
                }
            }
        }

        lastChar = line[position++];
        return lastChar;
    }

    public int read( char[] buf, int off, int len) throws IOException {
        if (eof)
            return -1;

        for ( int i=0; i<len; i++) {
            int c = read();
            if (c == -1) // end of stream
                return i;

            buf[off+i] = (char) c;
        }

        return len;
    }

    public boolean ready() throws IOException {
        return (!eof && (position<line.length || super.ready()));
    }

    /**
     * Replace HTML special characters in a plain text string with named entities.
     *
     * @param in The string which is to be made HTML-safe.
     * @return The converted string.
     */
    private String replaceFunnyChars( String in) {
        if (in == null)
            return null;

        StringBuffer line = new StringBuffer( in);
        for ( int i=line.length()-1; i>=0; i--) {
            Character c = new Character( line.charAt( i));
            if (funnyChars.containsKey( c))
                line.replace( i, i+1, (String) funnyChars.get( c));
        }

        return line.toString();
    }

    /**
     * Test if the character read from the underlying stream signals a line break.
     * If the character is a '\r', the method will test if the following character is a '\n' and
     * will consume that char.
     *
     * @param c The character last read from the underlying reader.
     */
    private boolean isLineBreak( char c) throws IOException {
        if (c == '\n') {
            return true;
        }
        else if (c == '\r') {
            // if the \r is immediately followed by a \n, this counts as one line break char
            int next = in.read();
            if (next!=-1 && next!='\n') {
                // non-line break; handle this char later
                ((PushbackReader) in).unread( (char) next);
            }

            return true;
        }
        else
            return false;
    }

    /**
     * Test if a character signifies the beginning of a new paragraph if encountered at the
     * beginning of a line.
     */
    private boolean startsParagraph( char c) {
        if (contains( NO_PARAGRAPH_START, c))
            return false;

        if (Character.isLetterOrDigit( c))
            return false;

        return true;
    }

    /**
     * Test if a character signifies the end of a paragraph if encountered at the
     * beginning of a line.
     */
    private boolean endsParagraph( char c) {
        if (contains( NO_PARAGRAPH_END, c))
            return false;

        if (Character.isLetterOrDigit( c))
            return false;

        return true;
    }

    /**
     * Test if a character c is contained in a sorted character array a. The
     * character array must be sorted lexicographically.
     */
    private boolean contains( char[] a, char c) {
        int where = Arrays.binarySearch( a, c);
        return (where>=0 && where<a.length && a[where]==c);
    }

    /**
     * Returns a character array containing the character in a form suitable for inclusion
     * in a HTML document. If the character is a HTML special character, the array will contain
     * the escaped form of the char, otherwise the {@link singleChar} array containing the character
     * is returned. The returned array may not be modified.
     */
    private char[] escape( char c) {
        Character co = new Character( c);
        if (funnyChars.containsKey( co))
            return (char[]) funnyChars.get( co);
        else {
            singleChar[0] = c;
            return singleChar;
        }
    }

} // class JGlossifyReader
