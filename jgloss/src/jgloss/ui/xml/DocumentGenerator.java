package jgloss.ui.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

class DocumentGenerator extends DefaultHandler {
    private DocumentBuilder builder;
    private Document document;
    private Node currentParent;

    public DocumentGenerator() {
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }
    }

    public Document getGeneratedDocument() {
        return document;
    }

    public void startDocument() throws SAXException {
        document = builder.newDocument();
        currentParent = document;
    }

    public void endDocument() throws SAXException {
    }

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

    public void endElement( String namespaceURI, String localName,
                            String qName) throws SAXException {
        currentParent = currentParent.getParentNode();
    }

    public void characters( char[] c, int start, int length) throws SAXException {
        currentParent.appendChild( document.createTextNode( new String( c, start, length)));
    }

    public void processingInstruction( String target, String data)
        throws SAXException {
        currentParent.appendChild( document.createProcessingInstruction( target, data));
    }

    public InputSource resolveEntity( String publicId, String systemId) throws SAXException {
        if (JGlossDocument.DTD_PUBLIC.equals( publicId)) {
            InputSource dtd = new InputSource( JGlossDocument.class.getResource
                                               ( JGlossDocument.DTD_RESOURCE).toExternalForm());
            dtd.setPublicId( publicId);
            return dtd;
        }
        else
            return super.resolveEntity( publicId, systemId);
    }
} // class DocumentGenerator
