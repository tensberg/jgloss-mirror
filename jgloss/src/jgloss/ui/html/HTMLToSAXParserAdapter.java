package jgloss.ui.html;

import jgloss.JGloss;
import jgloss.ui.xml.JGlossDocument;

import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.BadLocationException;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class HTMLToSAXParserAdapter {
    private Segment segment;
    private ContentHandler saxContentHandler;
    private JGlossHTMLDoc htmlDoc;

    private static final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();

    private static final String CDATA = "CDATA";

    public HTMLToSAXParserAdapter() {
        segment = new Segment();
        segment.setPartialReturn( true);
    }

    public void transform( JGlossHTMLDoc _htmlDoc, ContentHandler _saxContentHandler) 
        throws SAXException {
        saxContentHandler = _saxContentHandler;
        htmlDoc = _htmlDoc;

        saxContentHandler.startDocument();

        Element root = htmlDoc.getDefaultRootElement();

        saxContentHandler.startElement( null, null, JGlossDocument.Elements.JGLOSS, 
                                        EMPTY_ATTRIBUTES);

        handleHead( root.getElement( 0));
        handleBody( root.getElement( 1));

        saxContentHandler.endElement( null, null, JGlossDocument.Elements.JGLOSS);

        saxContentHandler.endDocument();        
    }

    private void handleHead( Element head) throws SAXException {
        saxContentHandler.startElement( null, null, JGlossDocument.Elements.HEAD,
                                        EMPTY_ATTRIBUTES);

        saxContentHandler.startElement( null, null, JGlossDocument.Elements.TITLE,
                                        EMPTY_ATTRIBUTES);
        String title = htmlDoc.getTitle();
        if (title != null)
            saxContentHandler.characters( title.toCharArray(), 0, title.length());
        saxContentHandler.endElement( null, null, JGlossDocument.Elements.TITLE);
        
        saxContentHandler.startElement( null, null, JGlossDocument.Elements.GENERATOR,
                                        EMPTY_ATTRIBUTES);
        String generator = JGloss.messages.getString( "jgloss.generator");
        saxContentHandler.characters( generator.toCharArray(), 0, generator.length());
        saxContentHandler.endElement( null, null, JGlossDocument.Elements.GENERATOR);

        saxContentHandler.endElement( null, null, JGlossDocument.Elements.HEAD);
    }

    private void handleBody( Element body) throws SAXException {
        saxContentHandler.startElement( null, null, JGlossDocument.Elements.BODY,
                                        EMPTY_ATTRIBUTES);
        
        for ( int i=0; i<body.getElementCount(); i++) {
            handleP( body.getElement( i));
        }

        saxContentHandler.endElement( null, null, JGlossDocument.Elements.BODY);
    }

    private void handleP( Element p) throws SAXException {
        saxContentHandler.startElement( null, null, JGlossDocument.Elements.P,
                                        EMPTY_ATTRIBUTES);

        // content model of p is (#PCDATA | anno)
        for ( int i=0; i<p.getElementCount(); i++) {
            Element child = p.getElement( i);
            if (child.getName().equals( AnnotationTags.ANNOTATION.getId()))
                handleAnnotation( child);
            else
                handleText( child);
        }

        saxContentHandler.endElement( null, null, JGlossDocument.Elements.P);
    }

    private void handleAnnotation( Element anno) throws SAXException {
        AttributesImpl a = new AttributesImpl();
        String translation = getText( anno.getElement( 1), true);
        if (translation.length() > 0)
            a.addAttribute( null, null, JGlossDocument.Attributes.TRANSLATION,
                            CDATA, translation);
        String base = (String) anno.getAttributes()
            .getAttribute( JGlossHTMLDoc.Attributes.BASE);
        if (base != null && base.length() > 0)
            a.addAttribute( null, null, JGlossDocument.Attributes.BASE, CDATA, base);
        String basere = (String) anno.getAttributes()
            .getAttribute( JGlossHTMLDoc.Attributes.BASE_READING);
        if (basere != null && basere.length() > 0)
            a.addAttribute( null, null, JGlossDocument.Attributes.BASE_READING, CDATA, basere);
        String type = (String) anno.getAttributes()
            .getAttribute( JGlossHTMLDoc.Attributes.TYPE);
        if (type != null && type.length() > 0)
            a.addAttribute( null, null, JGlossDocument.Attributes.TYPE, CDATA, type);

        saxContentHandler.startElement( null, null, JGlossDocument.Elements.ANNOTATION,
                                        a);

        handleWord( anno.getElement( 0));

        saxContentHandler.endElement( null, null, JGlossDocument.Elements.ANNOTATION);
    }

    private void handleWord( Element word) throws SAXException {
        for ( int i=0; i<word.getElementCount(); i++) {
            Element child = word.getElement( i);
            if (child.getElementCount() == 0)
                // simple basetext
                handleText( child);
            else
                handleRB( child);
        }
    }

    private void handleRB( Element rb) throws SAXException {
        AttributesImpl a = new AttributesImpl();

        String reading = getText( rb.getElement( 0), true);
        if (reading.length() > 0) {
            a.addAttribute( null, null, JGlossDocument.Attributes.READING, CDATA, reading);
        }
     
        String docre = (String) rb.getAttributes().getAttribute( JGlossHTMLDoc.Attributes.DOCREADING);
        if (docre!=null && docre.length()>0) {
            a.addAttribute( null, null, JGlossDocument.Attributes.DOCREADING, CDATA, docre);
        }

        saxContentHandler.startElement( null, null, JGlossDocument.Elements.RBASE, a);
        handleText( rb.getElement( 1));
        saxContentHandler.endElement( null, null, JGlossDocument.Elements.RBASE);
    }

    private void handleText( Element leaf) throws SAXException {
        int offset = leaf.getStartOffset();
        int length = leaf.getEndOffset()-leaf.getStartOffset();
        
        try {
            while (length > 0) {
                htmlDoc.getText( offset, length, segment);
                saxContentHandler.characters( segment.array, segment.offset, segment.count);
                offset += segment.count;
                length -= segment.count;
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private String getText( Element elem, boolean handleEmptyPlaceholder) {
        try {
            String text = htmlDoc.getText( elem.getStartOffset(), 
                                           elem.getEndOffset() - elem.getStartOffset());
            if (handleEmptyPlaceholder && JGlossHTMLDoc.EMPTY_ELEMENT_PLACEHOLDER.equals( text))
                text = "";
            
            return text;
        } catch (BadLocationException ex) {
            ex.printStackTrace();
            return "";
        }
    }
} // class HTMLToSAXParserAdapter
