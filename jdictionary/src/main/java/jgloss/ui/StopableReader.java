/*
 * Copyright (C) 2001-2004 Michael Koch (tensberg@gmx.net)
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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A reader which can be stopped in mid-stream. If the <CODE>stop</CODE> method of the
 * reader is called, all following read accesses will signal end of stream regardless of
 * the state of the underlying reader.
 *
 * @author Michael Koch
 */
public class StopableReader extends FilterReader {
    /**
     * <CODE>true</CODE> if <CODE>stop</CODE> was called.
     */
    private boolean stopped = false;
    private int charsRead = 0;

    /**
     * Constructs a new StopableReader which will read from the passed in stream.
     *
     * @param in The reader to which read requests will be forwarded.
     */
    public StopableReader( Reader in) {
        super( in);
    }

    /**
     * Reads a single character from the stream. If the <CODE>stop</CODE> method of this reader
     * was called, this method will return -1 to signal end of stream.
     *
     * @return The character read, or -1 if the end of stream has been reached.
     * @exception IOException if the read has failed.
     */
    @Override
	public int read() throws IOException {
        if (stopped)
            return -1;
        else {
            int c = super.read();
            if (c != -1)
                charsRead++;
            return c;
        }
    }

    /**
     * Reads an array of bytes from the stream. If the <CODE>stop</CODE> method of this reader
     * was called, the method will return -1 to signal end of stream.
     *
     * @param cbuf Buffer where the read characters will be stored.
     * @param off Offset into the buffer.
     * @param len Number of characters to read.
     * @return Number of characters actually read, or -1 if the end of stream has been reached.
     * @exception IOException if the read has failed.
     */
    @Override
	public int read( char[] cbuf, int off, int len) throws IOException {
        if (stopped)
            return -1;
        else {
            int count = super.read( cbuf, off, len);
            if (count != -1)
                charsRead += count;
            return count;
        }
    }

    /**
     * Makes this reader signal end of file on all following reads.
     */
    public void stop() {
        stopped = true;
    }

    public int getCharCount() { return charsRead; }
} // class StopableReader
