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
 *
 */

package jgloss.ui.export;

import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import jgloss.JGloss;
import jgloss.ui.gloss.JGlossFrameModel;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Standard exporter which applies an XSLT style sheet to the JGloss document and writes the
 * result to a file.
 *
 * @author Michael Koch
 */
class XSLTExporter implements Exporter {
	private static final Logger LOGGER = Logger.getLogger(XSLTExporter.class.getPackage().getName());
	
	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

    XSLTExporter() {}

    /**
     * Shows the export file chooser and runs the export.
     */
    @Override
	public void export(ExportConfiguration configuration,
                       JGlossFrameModel source,
                       Document doc, Component parent) {
        File outfile = chooseOutputFile(configuration, parent);
        if (outfile != null) {
            doc = applyFilter(configuration, doc);
            transformToFile(outfile, configuration, source, doc, parent);
        }
    }

    /**
     * Filter the document in some way not possible with XSLT style sheets.
     * This implementation returns the unchanged document, derived classes may preprocess the document.
     *
     * @return a filtered copy of the document.
     */
    protected Document applyFilter(ExportConfiguration configuration, Document doc) {
        return doc;
    }

    /**
     * Shows the file selection dialog for the export target file.
     *
     * @return The chosen file, or <code>null</code> if the dialog was cancelled.
     */
    protected File chooseOutputFile(ExportConfiguration configuration, Component parent) {
        List<Parameter> parameters = configuration.getParameters();
        List<UIParameter> uiparameters = new ArrayList<UIParameter>(parameters.size());
        for (Parameter parameter : parameters) {
            if (parameter instanceof UIParameter) {
	            uiparameters.add( (UIParameter) parameter);
            }
        }
        ExportFileChooser filechooser = 
            new ExportFileChooser( JGloss.getApplication().getCurrentDir(), configuration.getTitle(),
                                   uiparameters);
        if (configuration.getFileFilter() != null) {
	        filechooser.setFileFilter( configuration.getFileFilter());
        }

        filechooser.setCurrentDirectory( new File( JGloss.getApplication().getCurrentDir()));
        int result = filechooser.showSaveDialog( parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            return filechooser.getSelectedFile();
        } else {
	        return null;
        }
    }

    /**
     * Opens the output file and applies the transformation. Shows an error dialog if an exception
     * is thrown.
     */
    protected void transformToFile(File outfile, ExportConfiguration configuration,
                                   JGlossFrameModel source, Document doc, Component parent) {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(outfile));
            transform(new StreamResult(out), configuration, source, doc);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            JOptionPane.showConfirmDialog
                ( parent, JGloss.MESSAGES.getString
                  ( "error.export.exception", new Object[] 
                      { outfile.getPath(), ex.getClass().getName(),
                        ex.getLocalizedMessage() }),
                  JGloss.MESSAGES.getString( "error.export.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);    
        } finally {
            if (out != null) {
	            try {
	                out.close();
	            } catch (IOException ex) {
	                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
	            }
            }
        }
    }

    /**
     * Creates a transformer from the XSLT style sheet and parameters set in the configuration
     * and applies it to the document.
     */
    protected void transform(Result out, ExportConfiguration configuration,
                             JGlossFrameModel source, Document doc) 
        throws IOException, TransformerConfigurationException, TransformerException {
        Transformer transformer = transformerFactory.newTransformer
            ( new SAXSource( new InputSource( (String) configuration.getTemplate()
                                              .getValue(source, configuration.getSystemId()))));

        String encoding = configuration.getEncoding();
        if (encoding != null) {
	        transformer.setOutputProperty( OutputKeys.ENCODING, encoding);
        }
        
        setParameters(configuration, source, transformer);

        transformer.transform( new DOMSource( doc), out);
    }
    
    /**
     * Sets the export parameters from the export configuration in the transformer.
     */
    protected void setParameters(ExportConfiguration configuration,
                                 JGlossFrameModel source, Transformer transformer) {
        for (Parameter parameter : configuration.getParameters()) {
            transformer.setParameter(parameter.getName(), parameter.getValue( source, configuration.getSystemId()));
        }
    }
} // class Exporter
