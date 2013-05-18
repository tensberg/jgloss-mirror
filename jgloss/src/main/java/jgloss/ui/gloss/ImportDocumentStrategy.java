/*
 * Copyright (C) 2001-2013 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.ui.gloss;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import jgloss.JGloss;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;
import jgloss.util.CharacterEncodingDetector;

/**
 * Import a document from a URL or file, annotating the Japanese words in the document.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
class ImportDocumentStrategy extends ImportStrategy {

    private static final String DEFAULT_ENCODING_NAME = JGloss.MESSAGES.getString("encodings.default");

    private final String encoding;

    private int contentlength;

    private String title;

    /**
     * Sets up everything neccessary to import a file and loads it. If <CODE>filename</CODE> is
     * a URL, it will create a reader which reads from the location the document points to. If it
     * is a path to a local file, it will create a reader which reads from it.
     * The method will then call <CODE>loadDocument</CODE> with the newly
     * created reader.
     *
     * @param frame Frame into which the document is imported.
     * @param path URL or path of the file to import.
     * @param detectParagraphs Flag if paragraph detection should be done.
     * @param parser Parser used to annotate the text.
     * @param filter Filter for fetching the reading annotations from a parsed document.
     * @param encoding Character encoding of the file. May be either <CODE>null</CODE> or the
     *                 value of the "encodings.default" resource to use autodetection.
     */
    ImportDocumentStrategy(JGlossFrame frame, String path, boolean detectParagraphs, ReadingAnnotationFilter filter,
            Parser parser, String encoding) {
        super(frame, path, detectParagraphs, filter, parser);
        this.encoding = encoding;
    }

    @Override
    Reader createReader() throws IOException {
        Reader in;

        try {
            URL url = new URL( path);
            URLConnection connection = url.openConnection();
            contentlength = connection.getContentLength();
            String connectionEncoding = connection.getContentEncoding();
            in = createInputStreamReader(connection.getInputStream(), connectionEncoding);
            title = url.getFile();
            if (title == null || title.isEmpty()) {
                title = path;
            }
        } catch (MalformedURLException ex) {
            // probably a local file
            File f = new File( path);
            contentlength = (int) f.length();
            in = createInputStreamReader(new FileInputStream(f), null);
            title = f.getName();
        }

        return in;
    }

    private Reader createInputStreamReader(InputStream inputStream, String streamEncoding) throws IOException,
            UnsupportedEncodingException {
        Reader in;
        InputStream is = new BufferedInputStream(inputStream);
        // a user-selected value for encoding overrides streamEncoding
        if (autodetectEncoding()) {
            in = CharacterEncodingDetector.getReader( is, streamEncoding);
        } else {
            in = new InputStreamReader( is, encoding);
        }
        return in;
    }

    @Override
    void customizeModel(JGlossFrameModel model) {
        model.setDocumentName(title);
    }

    private boolean autodetectEncoding() {
        return encoding == null || DEFAULT_ENCODING_NAME.equals(encoding);
    }

    @Override
    int getLength() {
        return CharacterEncodingDetector.guessLength( contentlength, encoding);
    }

}
