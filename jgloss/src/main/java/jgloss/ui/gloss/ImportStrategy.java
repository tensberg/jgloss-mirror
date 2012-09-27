package jgloss.ui.gloss;

import static jgloss.ui.util.SwingWorkerProgressFeedback.showProgress;

import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import jgloss.JGloss;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;

/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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

/**
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
abstract class ImportStrategy {
    private static final Logger LOGGER = Logger.getLogger(ImportStrategy.class.getPackage().getName());
    
    final JGlossFrame frame;
    
    final String path;
    
    private final JGlossFrameModel model = new JGlossFrameModel();
    
    private final boolean detectParagraphs;
    
    private final ReadingAnnotationFilter filter;
    
    private final Parser parser;
    
    ImportStrategy(JGlossFrame frame, String path, boolean detectParagraphs, ReadingAnnotationFilter filter, Parser parser) {
        this.frame = frame;
        this.path = path;
        this.detectParagraphs = detectParagraphs;
        this.filter = filter;
        this.parser = parser;
    }
    
    abstract Reader createReader() throws Exception;
    
    abstract void customizeModel(JGlossFrameModel model);
    
    abstract int getLength();
    
    public void executeImport() {
        Reader reader;
        try {
            reader = createReader();
            customizeModel(model);

            ImportFromReaderWorker worker = new ImportFromReaderWorker(frame, model, reader, detectParagraphs, filter, parser, getLength());
            showProgress(worker, frame);
            worker.execute();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "failed to create reader for import", ex);
            JOptionPane.showConfirmDialog(frame, JGloss.MESSAGES.getString
                    ( "error.import.exception", model.getDocumentPath(), ex.getClass().getName(),
                            ex.getLocalizedMessage()),
                            JGloss.MESSAGES.getString( "error.import.title"),
                            JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            // error before document was opened, close window
            frame.dispose();
        }
    }
}
