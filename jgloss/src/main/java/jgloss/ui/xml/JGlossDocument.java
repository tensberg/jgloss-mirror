/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jgloss.ui.html.HTMLToSAXParserAdapter;
import jgloss.ui.html.JGlossHTMLDoc;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class JGlossDocument {
    public interface Elements { // TODO: replace by enum
        String JGLOSS = "jgloss";
        String HEAD = "head";
        String TITLE = "title";
        String GENERATOR = "generator";
        String BODY = "body";
        String DIV = "div";
        String P = "p";
        String ANNOTATION = "anno";
        String RBASE = "rbase";
        String BR = "br";
    } // class Elements

    public interface Attributes { // TODO: replace by enum
        String TRANSLATION = "tr";
        String BASE = "base";
        String BASE_READING = "basere";
        String TYPE = "type";
        String READING = "re";
        String DOCREADING = "docre";
    } // class Attributes

    private static final Logger LOGGER = Logger.getLogger(JGlossDocument.class.getPackage().getName());
    
    public static final String DTD_PUBLIC = "JGloss/0.9.9/JGloss document/EN";
    public static final String DTD_SYSTEM = "http://jgloss.sourceforge.net/jgloss-0.9.9.dtd";
    public static final String DTD_RESOURCE = "/xml/jgloss.dtd";

    private static final DocumentBuilderFactory docFactory = initDocFactory();

    private static DocumentBuilderFactory initDocFactory() {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setIgnoringComments( true);
        docFactory.setCoalescing( true);
        docFactory.setIgnoringElementContentWhitespace( true);
        return docFactory;
    }

    private Document doc;
    private JGlossHTMLDoc htmlDoc;

    public JGlossDocument( InputSource _in) throws IOException, SAXException {
        this( readDocument( _in));
    }

    private static Document readDocument( InputSource _in) throws IOException, SAXException {
        try {
            return docFactory.newDocumentBuilder().parse( _in);
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

    public JGlossDocument( Document _doc) {
        setDocument( _doc);
    }

    public synchronized Document getDOMDocument() { 
        if (doc == null) {
	        validate();
        }

        return doc;
    }

    private void setDocument( Document _doc) {
        doc = _doc;
    }

    public void linkWithHTMLDoc( JGlossHTMLDoc _htmlDoc) {
        htmlDoc = _htmlDoc;

        htmlDoc.addDocumentListener( new DocumentListener() {
                @Override
				public void insertUpdate(DocumentEvent e) {
                    invalidate();
                }
                @Override
				public void removeUpdate(DocumentEvent e) {
                    invalidate();
                }
                @Override
				public void changedUpdate(DocumentEvent e) {
                    invalidate();
                }
            });
    }

    private synchronized void invalidate() {
        doc = null;
    }

    private synchronized void validate() {
        try {
            DocumentGenerator generator = new DocumentGenerator();
            new HTMLToSAXParserAdapter().transform( htmlDoc, generator);
            setDocument( generator.getGeneratedDocument());
        } catch (SAXException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
} // class JGlossDocument
