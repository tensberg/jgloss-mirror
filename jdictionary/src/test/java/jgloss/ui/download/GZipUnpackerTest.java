/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 */

package jgloss.ui.download;

import static org.fest.assertions.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import jgloss.ui.download.schema.Dictionary;
import jgloss.ui.download.schema.Download;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GZipUnpackerTest {
    
    private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    
    private InputStream sourceStream;
    
    @Before
    public void initSourceStream() {
        sourceStream = GZipUnpackerTest.class.getResourceAsStream("GZipUnpackerTest.gz");
        assertThat(sourceStream).isNotNull();
    }
    
    @After
    public void closeSourceStream() throws IOException {
        sourceStream.close();
    }
    
    @Test
    public void testUnpack() throws IOException {
        Dictionary dictionary = new Dictionary();
        Download download = new Download();
        download.setCompression("gz");
        dictionary.setDownload(download);
        
        try {
            new GZipUnpacker().unpack(dictionary, sourceStream, outStream);
        } finally {
            outStream.close();
        }
        
        assertThat(new String(outStream.toByteArray(), "ASCII")).isEqualTo("test");
    }
}
