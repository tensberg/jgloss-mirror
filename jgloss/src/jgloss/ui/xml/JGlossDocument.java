package jgloss.ui.xml;

import jgloss.ui.html.JGlossHTMLDoc;
import jgloss.ui.html.HTMLToSAXParserAdapter;

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
    public interface Elements {
        String JGLOSS = "jgloss";
        String HEAD = "head";
        String TITLE = "title";
        String GENERATOR = "generator";
        String BODY = "body";
        String P = "p";
        String ANNOTATION = "anno";
        String RBASE = "rbase";
    } // class Elements

    public interface Attributes {
        String TRANSLATION = "tr";
        String BASE = "base";
        String BASE_READING = "basere";
        String TYPE = "type";
        String READING = "re";
        String DOCREADING = "docre";
    } // class Attributes

    public static final String DTD_PUBLIC = "JGloss/0.9.9/JGloss document/EN";
    public static final String DTD_SYSTEM = "http://jgloss.sourceforge.net/jgloss-0.9.9.dtd";
    public static final String DTD_RESOURCE = "/data/jgloss.dtd";

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
        try {
            DocumentGenerator generator = new DocumentGenerator();
            new HTMLToSAXParserAdapter().transform( htmlDoc, generator);
            setDocument( generator.getGeneratedDocument());
        } catch (SAXException ex) {
            ex.printStackTrace();
        }
    }
} // class JGlossDocument
