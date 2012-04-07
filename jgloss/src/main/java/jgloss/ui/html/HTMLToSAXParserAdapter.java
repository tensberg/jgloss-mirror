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

package jgloss.ui.html;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.html.HTML;

import jgloss.JGloss;
import jgloss.ui.xml.JGlossDocument;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Create the JGloss XML document structure from a {@link JGlossHTMLDoc JGlossHTMLDoc}.
 */
public class HTMLToSAXParserAdapter {
	private static final Logger LOGGER = Logger.getLogger(HTMLToSAXParserAdapter.class.getPackage().getName());
	
	private ContentHandler saxContentHandler;
    private JGlossHTMLDoc htmlDoc;
    private ElementHandler copyElementHandler;
    private final Segment segment;

    private Map<String, ElementHandler> elementHandlers;

    private static final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();

    private static final String CDATA = "CDATA";

    public HTMLToSAXParserAdapter() {
        segment = new Segment();
        segment.setPartialReturn( true);
        initElementHandlers();
    }

    private void initElementHandlers() {
        elementHandlers = new HashMap<String, ElementHandler>(23);
        elementHandlers.put(HTML.Tag.HTML.toString(), 
                            new NameMapperHandler(JGlossDocument.Elements.JGLOSS));
        elementHandlers.put(HTML.Tag.HEAD.toString(),
                            new HeadHandler());
        elementHandlers.put(HTML.Tag.P.toString(),
                            new PHandler());
        elementHandlers.put(AnnotationTags.ANNOTATION.getId(),
                            new AnnotationHandler());
        elementHandlers.put(AnnotationTags.WORD.getId(),
                            new StripElementHandler());
        elementHandlers.put(AnnotationTags.READING_BASETEXT.getId(),
                            new RBHandler());
        // everything else will be handled by the standard CopyElementHandler
        copyElementHandler = new CopyElementHandler();
    }

    public void transform( JGlossHTMLDoc _htmlDoc, ContentHandler _saxContentHandler) 
        throws SAXException {
        saxContentHandler = _saxContentHandler;
        htmlDoc = _htmlDoc;

        saxContentHandler.startDocument();

        handleElement(htmlDoc.getDefaultRootElement());

        saxContentHandler.endDocument();        
    }

    /**
     * Calls {@link #handleElement(Element) handleElement]} for every child of the element.
     */
    private void handleChildren(Element elem) throws SAXException {
        for ( int i=0; i<elem.getElementCount(); i++) {
            handleElement(elem.getElement( i));
        }
    }

    /**
     * Calls the specific handler for the element, determined by its name. If no handler is
     * registered and the element is a leaf, {@link #handleText(Element) handleText} is called; if it
     * is no leaf, a {@link CopyElementHandler CopyElementHandler} is called..
     */
    private void handleElement(Element elem) throws SAXException {
        ElementHandler handler = elementHandlers.get(elem.getName());
        if (handler != null) {
            handler.handle(elem);
        }
        else {
            if (elem.isLeaf()) {
	            handleText(elem);
            } else {
	            copyElementHandler.handle(elem);
            }
        }
    }

    private void handleText( Element leaf) throws SAXException {
        handleText(leaf, leaf.getStartOffset(), leaf.getEndOffset());
    }

    private void handleText( Element leaf, int start, int end) throws SAXException {
        int offset = start;
        int length = end-start;
        
        try {
            while (length > 0) {
                htmlDoc.getText( offset, length, segment);
                saxContentHandler.characters( segment.array, segment.offset, segment.count);
                offset += segment.count;
                length -= segment.count;
            }
        } catch (BadLocationException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Return the text contained by an element.
     *
     * @param handleEmptyPlaceholder If <code>true</code> and the element equals the text which
     *   is used to fill an otherwise empty element, the empty string is returned. This is used
     *   when fetching the text of a reading or translation annotation.
     * @see JGlossHTMLDoc#EMPTY_ELEMENT_PLACEHOLDER
     */
    private String getText( Element elem, boolean handleEmptyPlaceholder) {
        try {
            String text = htmlDoc.getText( elem.getStartOffset(), 
                                           elem.getEndOffset() - elem.getStartOffset());
            if (handleEmptyPlaceholder && JGlossHTMLDoc.EMPTY_ELEMENT_PLACEHOLDER.equals( text)) {
	            text = "";
            }
            
            return text;
        } catch (BadLocationException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return "";
        }
    }

    /**
     * Interface for handlers of the different elements.
     */
    private interface ElementHandler {
        void handle(Element elem) throws SAXException ;
    }

    /**
     * Does not create a XML element for the HTML element, only calls 
     * {@link HTMLToSAXParserAdapter#handleChildren(Element) handleChildren}.
     */
    private class StripElementHandler implements ElementHandler {
        @Override
		public void handle(Element elem) throws SAXException {
            handleChildren(elem);
        }
    }

    /**
     * Element handler which creates a XML element identical to the HTML element.
     * Attributes of the HTML element are not copied.
     */
    private class CopyElementHandler implements ElementHandler {
        @Override
		public void handle(Element elem) throws SAXException {
            saxContentHandler.startElement( null, null, elem.getName(),
                                            EMPTY_ATTRIBUTES);
            handleChildren(elem);
            saxContentHandler.endElement( null, null, elem.getName());
        }
    } // class HeadHandler

    /**
     * Creates a XML element with the specified name and calls 
     * {@link HTMLToSAXParserAdapter#handleChildren(Element) handleChildren}.
     */
    private class NameMapperHandler implements ElementHandler {
        private final String elementName;

        NameMapperHandler(String _elementName) {
            elementName = _elementName;
        }

        @Override
		public void handle(Element elem) throws SAXException {
            saxContentHandler.startElement( null, null, elementName,
                                            EMPTY_ATTRIBUTES);
            handleChildren(elem);
            saxContentHandler.endElement( null, null, elementName);
        }
    } // class NameMapperHandler

    /**
     * Creates the head element.
     */
    private class HeadHandler implements ElementHandler {
        @Override
		public void handle( Element head) throws SAXException {
            saxContentHandler.startElement( null, null, JGlossDocument.Elements.HEAD,
                                            EMPTY_ATTRIBUTES);

            saxContentHandler.startElement( null, null, JGlossDocument.Elements.TITLE,
                                            EMPTY_ATTRIBUTES);
            String title = htmlDoc.getTitle();
            if (title != null) {
	            saxContentHandler.characters( title.toCharArray(), 0, title.length());
            }
            saxContentHandler.endElement( null, null, JGlossDocument.Elements.TITLE);
        
            saxContentHandler.startElement( null, null, JGlossDocument.Elements.GENERATOR,
                                            EMPTY_ATTRIBUTES);
            String generator = JGloss.MESSAGES.getString( "jgloss.generator");
            saxContentHandler.characters( generator.toCharArray(), 0, generator.length());
            saxContentHandler.endElement( null, null, JGlossDocument.Elements.GENERATOR);

            saxContentHandler.endElement( null, null, JGlossDocument.Elements.HEAD);
        }
    } // class HeadHandler

    /**
     * Creates annotation elements.
     */
    private class AnnotationHandler implements ElementHandler {
        @Override
		public void handle(Element anno) throws SAXException {
            AttributesImpl a = new AttributesImpl();
            String translation = getText( anno.getElement( 1), true);
            if (translation.length() > 0) {
	            a.addAttribute( null, null, JGlossDocument.Attributes.TRANSLATION,
                                CDATA, translation);
            }
            String base = (String) anno.getAttributes()
                .getAttribute( JGlossHTMLDoc.Attributes.BASE);
            if (base != null && base.length() > 0) {
	            a.addAttribute( null, null, JGlossDocument.Attributes.BASE, CDATA, base);
            }
            String basere = (String) anno.getAttributes()
                .getAttribute( JGlossHTMLDoc.Attributes.BASE_READING);
            if (basere != null && basere.length() > 0) {
	            a.addAttribute( null, null, JGlossDocument.Attributes.BASE_READING, CDATA, basere);
            }
            String type = (String) anno.getAttributes()
                .getAttribute( JGlossHTMLDoc.Attributes.TYPE);
            if (type != null && type.length() > 0) {
	            a.addAttribute( null, null, JGlossDocument.Attributes.TYPE, CDATA, type);
            }

            saxContentHandler.startElement( null, null, JGlossDocument.Elements.ANNOTATION,
                                            a);

            handleElement( anno.getElement( 0)); // word element

            saxContentHandler.endElement( null, null, JGlossDocument.Elements.ANNOTATION);
        }
    } // class AnnotationHandler

    /**
     * Handles a reading/basetext pair.
     */
    private class RBHandler implements ElementHandler {
        @Override
		public void handle( Element rb) throws SAXException {
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
    } // class RBHandler

    /**
     * Handles a paragraph element. The HTML document model inserts a line break character
     * at the end of each paragraph. Since this line break can't be easily filtered on
     * XML export, it is removed here.
     */
    private class PHandler implements ElementHandler {
        @Override
		public void handle(Element p) throws SAXException {
            saxContentHandler.startElement( null, null, HTML.Tag.P.toString(),
                                            EMPTY_ATTRIBUTES);

            for (int i=0; i<p.getElementCount()-1; i++) {
                handleElement(p.getElement(i));
            }

            Element last = p.getElement(p.getElementCount()-1);
            if (last.isLeaf()) {
                // text node, check if last char is a line break
                try {
                    String text = htmlDoc.getText(last.getEndOffset()-1,1);
                    if (text.equals("\n")) {
	                    // strip last char
                        handleText(last, last.getStartOffset(), last.getEndOffset()-1);
                    } else {
	                    // print all element text
                        handleText(last);
                    }
                } catch (BadLocationException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            else {
                handleElement(last);
            }

            saxContentHandler.endElement( null, null, HTML.Tag.P.toString());
        }
    }
} // class HTMLToSAXParserAdapter
