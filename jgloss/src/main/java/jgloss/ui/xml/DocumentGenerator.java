/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Generate a JGloss {@link Document} from a given XML Document.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
class DocumentGenerator extends DefaultHandler {
	private static final Logger LOGGER = Logger.getLogger(DocumentGenerator.class.getPackage().getName());
	
    private final DocumentBuilder builder;
    private Document document;
    private Node currentParent;

    public DocumentGenerator() {
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * @return Document which was created by parsing the source document. <code>null</code> if no source
     *         document was parsed yet.
     */
    public Document getGeneratedDocument() {
        return document;
    }

    @Override
	public void startDocument() throws SAXException {
        document = builder.newDocument();
        currentParent = document;
    }

    @Override
	public void endDocument() throws SAXException {
    }

    @Override
	public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts)
        throws SAXException {
        Element elem = document.createElementNS( namespaceURI, qName);
        for ( int i=0; i<atts.getLength(); i++) {
            elem.setAttributeNS( atts.getURI( i), atts.getQName( i), atts.getValue( i));
        }
        currentParent.appendChild( elem);
        currentParent = elem;
    }

    @Override
	public void endElement( String namespaceURI, String localName,
                            String qName) throws SAXException {
        currentParent = currentParent.getParentNode();
    }

    @Override
	public void characters( char[] c, int start, int length) throws SAXException {
        currentParent.appendChild( document.createTextNode( new String( c, start, length)));
    }

    @Override
	public void processingInstruction( String target, String data)
        throws SAXException {
        currentParent.appendChild( document.createProcessingInstruction( target, data));
    }

    @Override
	public InputSource resolveEntity( String publicId, String systemId) throws SAXException, IOException {
        if (JGlossDocument.DTD_PUBLIC.equals( publicId)) {
            InputSource dtd = new InputSource( JGlossDocument.class.getResource
                                               ( JGlossDocument.DTD_RESOURCE).toExternalForm());
            dtd.setPublicId( publicId);
            return dtd;
        } else {
	        return super.resolveEntity( publicId, systemId);
        }
    }
} // class DocumentGenerator
