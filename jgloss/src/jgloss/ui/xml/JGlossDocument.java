package jgloss.ui.xml;

import jgloss.ui.html.JGlossHTMLDoc;

import java.io.IOException;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class JGlossDocument {
    public static class Elements {
        public static final String HEAD = "head";
        public static final String BODY = "body";
        public static final String GENERATOR = "generator";
        public static final String TITLE = "title";
        public static final String ANNOTATION = "anno";
        public static final String RBASE = "rbase";
    } // class Elements

    public static class Attributes {
        public static final String TRANSLATION = "tr";
        public static final String BASE = "base";
        public static final String BASE_READING = "basere";
        public static final String TYPE = "type";
        public static final String READING = "re";
        public static final String DOCREADING = "docre";
    } // class Attributes

    public static final String DTD_PUBLIC = "JGloss/0.9.9/JGloss document/EN";
    public static final String DTD_SYSTEM = "http://jgloss.sourceforge.net/jgloss-0.9.9.dtd";
    public static final String DTD_RESOURCE = "/data/jgloss.dtd";

    private static final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    private Document doc;
    private JGlossHTMLDoc htmlDoc;

    public JGlossDocument( InputSource _in) throws IOException, SAXException {
        this( readDocument( _in));
    }

    private static Document readDocument( InputSource _in) throws IOException, SAXException {
        try {
            return docFactory.newDocumentBuilder().parse( _in);
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public JGlossDocument( Document _doc) {
        setDocument( _doc);
    }

    public synchronized Document getDOMDocument() { 
        if (doc == null)
            validate();

        return doc;
    }

    private void setDocument( Document _doc) {
        doc = _doc;
    }

    public void linkWithHTMLDoc( JGlossHTMLDoc _htmlDoc) {
        htmlDoc = _htmlDoc;

        htmlDoc.addDocumentListener( new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    invalidate();
                }
                public void removeUpdate(DocumentEvent e) {
                    invalidate();
                }
                public void changedUpdate(DocumentEvent e) {
                    invalidate();
                }
            });
    }

    private synchronized void invalidate() {
        doc = null;
    }

    private synchronized void validate() {
        // TODO: create XML Document from htmlDoc
    }
} // class JGlossDocument
