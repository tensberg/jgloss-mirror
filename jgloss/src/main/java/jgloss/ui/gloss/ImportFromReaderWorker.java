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

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import jgloss.JGloss;
import jgloss.dictionary.Dictionary;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;
import jgloss.ui.Dictionaries;
import jgloss.ui.StopableReader;
import jgloss.ui.util.Cancelable;
import jgloss.ui.util.JGlossWorker;
import jgloss.ui.xml.JGlossDocument;
import jgloss.ui.xml.JGlossDocumentBuilder;

import org.xml.sax.SAXException;

/**
 * Import a document from a reader and set is as model on a {@link JGlossFrame}.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
class ImportFromReaderWorker extends JGlossWorker<JGlossDocument, Void> implements Cancelable {
    private static final Logger LOGGER = Logger.getLogger(ImportFromReaderWorker.class.getPackage().getName());

    private final JGlossFrame frame;

    private final JGlossFrameModel model;

    private final StopableReader documentReader;

    private final boolean detectParagraphs;

    private final ReadingAnnotationFilter filter;

    private final Parser parser;

    private final int length;

    private final Dictionary[] dictionaries;

    public ImportFromReaderWorker(JGlossFrame frame, JGlossFrameModel model, Reader documentReader, boolean detectParagraphs, ReadingAnnotationFilter filter,
            Parser parser, int length) {
        this.frame = frame;
        this.model = model;
        this.documentReader = new StopableReader(documentReader);
        this.detectParagraphs = detectParagraphs;
        this.filter = filter;
        this.parser = parser;
        this.length = length;

        setMessage(JGloss.MESSAGES.getString("import.progress", model.getDocumentName()));
        dictionaries = Dictionaries.getInstance().getDictionaries();
    }

    @Override
    protected JGlossDocument doInBackground() throws IOException, SAXException {
        // TODO: add progress support

        JGlossDocument document;

        try {
            document = new JGlossDocumentBuilder().build(documentReader,
                    detectParagraphs, filter, parser, dictionaries);
        } finally {
            try {
                documentReader.close();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "failed to close reader for " + model.getDocumentPath(), ex);
            }
        }

        return document;
    }

    @Override
    protected void done() {
        try {
            model.setDocument(get());
            frame.setModel(model);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            JOptionPane.showConfirmDialog
                ( frame, JGloss.MESSAGES.getString
                  ( "error.import.exception", model.getDocumentPath(), ex.getClass().getName(), ex.getLocalizedMessage()),
                  JGloss.MESSAGES.getString( "error.import.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void cancel() {
        documentReader.stop();
    }
}