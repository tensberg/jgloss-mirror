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

import java.util.*;
import java.io.*;

/**
 * Reads a plain text file from a reader and formats it as simple HTML. Every line in the
 * input file will be enclosed in <CODE>&lt;P&gt;</CODE> tags. HTML-specific special characters will
 * be replaced by named entities.
 *
 * @author Michael Koch
 */
public class HTMLifyReader extends BufferedReader {
    /**
     * Buffer for the line currently being read.
     */
    private String line;
    /**
     * Position offset in the currently read line.
     */
    private long position;
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
     * Constructs a new reader without inserting a HTML title for an underlying reader.
     *
     * @param in The reader from which to read the plain text file.
     * @exception IOException if an error occurrs when accessing the underlying reader.
     */
    public HTMLifyReader( Reader in) throws IOException {
        this( in, null);
    }

    /**
     * Constructs a new reader which will insert the given HTML title.
     *
     * @param in The reader from which to read the plain text file.
     * @param title The title to insert in the generated HTML document.
     * @exception IOException if an error occurrs when accessing the underlying reader.
     */
    public HTMLifyReader( Reader in, String title) throws IOException {
        super( in);

        if (funnyChars == null) {
            funnyChars = new HashMap( 10);
            funnyChars.put( new Character( '&'), "&amp;");
            funnyChars.put( new Character( '<'), "&lt;");
            funnyChars.put( new Character( '>'), "&gt;");
            funnyChars.put( new Character( '"'), "&quot;");
            
            // The following character is a Japanese space. When it apears in a document
            // it is probably used to format the indention at the beginning of a paragraph.
            // Unfortunately, the layout algorithm uses the space as a opportunity for breaking
            // an overlong line. To prevent this I approximate it with two non-breakable spaces.
            funnyChars.put( new Character( '\u3000'), "&nbsp;&nbsp;");
        }

        line = "<HTML>\n<HEAD>\n";
        if (title != null)
            line += "<TITLE>" + replaceFunnyChars( title) + "</TITLE>\n";

        // Make sure the document has a header for the character encoding.
        // It will be replaced with the correct encoding when the document is written.
        line += "<META http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" >\n";

        line +="</HEAD>\n<BODY>\n<P>";
    }

    public int read() throws IOException {
        if (line == null)
            return -1; // end of file

        char c;
        if (position == line.length()) {
            // read the next line from the underlying reader and HTMLify it.
            position = 0;
            if (eof)
                line = null;
            else {
                String next = replaceFunnyChars( super.readLine());
                if (next == null) {
                    // End of underlying stream encountered. Generate the line closing the document.
                    line = "</P>\n</BODY>\n</HTML>";
                    eof = true;
                }
                else {
                    if (next.trim().length() == 0)
                        line = "</P><P>\n";
                    else
                        line = "" + next + "</P>\n<P>";
                }
            }
        }

        if (line == null) // end of stream
            return -1;

        c = line.charAt( (int) position);
        position++;
        
        return c;
    }

    public int read( char[] buf, int off, int len) throws IOException {
        for ( int i=0; i<len; i++) {
            int c = read();
            if (c == -1) // end of stream
                return i;

            buf[i] = (char) c;
        }

        return len;
    }

    public boolean ready() throws IOException {
        return (line != null);
    }

    public long skip( long n) throws IOException {
        if (n < 0)
            throw new IllegalArgumentException( "negative parameter n");

        long skipped = Math.min( n, line.length() - position);
        position += skipped;
        if (position == line.length()) {
            skipped += super.skip( n - skipped);
            line = "";
            position = 0;
        }

        return skipped;
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
} // class HTMLifyReader
