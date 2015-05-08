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

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import jgloss.JGloss;
import jgloss.ui.util.JGlossWorker;
import jgloss.ui.util.SwingWorkerProgressFeedback;
import jgloss.ui.xml.JGloss1Converter;
import jgloss.ui.xml.JGlossDocument;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Opens an existing JGloss document from a file.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
public class OpenDocumentWorker extends JGlossWorker<JGlossDocument, Void> {

    private static final Logger LOGGER = Logger.getLogger(OpenDocumentWorker.class.getPackage().getName());

    private final JGlossFrameModel model = new JGlossFrameModel();

    private final File file;

    private final JGlossFrame frame;

    /**
     * Create and execute a worker which opens the given file. Shows progress feedback for the
     * worker.
     */
    public static void openDocument(JGlossFrame frame, File file) {
        OpenDocumentWorker worker = new OpenDocumentWorker(frame, file);
        SwingWorkerProgressFeedback.showProgress(worker, frame);
        worker.execute();
    }

    OpenDocumentWorker(JGlossFrame frame, File file) {
        super("open.progress.title");
        this.frame = frame;
        this.file = file;
    }

    @Override
    protected JGlossDocument doInBackground() throws IOException, SAXException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

        JGlossDocument document;
        try {
        	inputStream = checkConvertJGloss1Doc(inputStream);

            document = new JGlossDocument(new InputSource(inputStream));
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "failed to close input stream for " + file, ex);
            }
        }

        return document;
    }

	private InputStream checkConvertJGloss1Doc(InputStream originalInputStream) throws IOException {
		InputStream convertedInputStream = originalInputStream;

	    if (JGloss1Converter.needsConversion(originalInputStream)) {
	    	LOGGER.log(INFO, "converting JGloss 1 document {0} to JGloss 2 format", file.getName());
	    	try {
	    		convertedInputStream = new JGloss1Converter().convert(originalInputStream);
	    		try {
	    			originalInputStream.close();
	    		} catch (IOException ex) {
	    			LOGGER.log(WARNING, "failed to close JGloss document input stream", ex);
	    		}
	    	} catch (Exception ex) {
	    		LOGGER.log(SEVERE, "failed to convert JGloss 1 file to JGloss 2 format", ex);
	    	}
	    }

	    return convertedInputStream;
    }

    @Override
    protected void done() {
        try {
            model.setDocument(get());
            model.setDocumentName(file.getName());
            model.setDocumentPath(file.getAbsolutePath());
            frame.setModel(model);

            JGlossFrame.OPEN_RECENT.addDocument(file);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            JOptionPane.showConfirmDialog(frame, JGloss.MESSAGES.getString("error.load.exception",
                    file.getAbsolutePath(), ex.getClass().getName(), ex.getLocalizedMessage()),
                    JGloss.MESSAGES.getString("error.load.title"), JOptionPane.DEFAULT_OPTION,
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
