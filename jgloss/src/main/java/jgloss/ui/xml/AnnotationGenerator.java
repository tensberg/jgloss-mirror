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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.SearchException;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotation;
import jgloss.parser.ReadingAnnotationFilter;
import jgloss.parser.TextAnnotation;
import jgloss.parser.TextAnnotationCompleter;
import jgloss.util.StringTools;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Insert reading/translation annotations into a document. Instances of this class
 * are used to annotate documents when they are imported into JGloss. Text inside of
 * &lt;p&gt; tags is analyzed and the appropriate JGlossDocument annotation tags are inserted.
 * The document structure, including the newly created tags, is passed on to the parent
 * document handler for further processing.
 *
 * @author Michael Koch
 */
class AnnotationGenerator extends DefaultHandler {
	private static final Logger LOGGER = Logger.getLogger(AnnotationGenerator.class.getPackage().getName());
	
	private final DefaultHandler parent;
    private final ReadingAnnotationFilter readingFilter;
    private final Parser parser;
    private final TextAnnotationCompleter taCompleter;

    private final List<ReadingAnnotation> readingsList = new ArrayList<ReadingAnnotation>( 5);
    private boolean annotateText;
    private boolean inP;
    private final AttributesImpl readingAtts = new AttributesImpl(); 

    private final static String CDATA = "CDATA";
    private final static String P_ELEMENT = "p";

    public AnnotationGenerator( DefaultHandler _parent, ReadingAnnotationFilter _readingFilter,
                                Parser _parser, Dictionary[] _dictionaries) {
        parent = _parent;
        readingFilter = _readingFilter;
        parser = _parser;
        taCompleter = new TextAnnotationCompleter( _dictionaries);
        readingAtts.addAttribute( "", "", JGlossDocument.Attributes.READING, CDATA, "");
    }

    @Override
	public void setDocumentLocator( Locator locator) {
        parent.setDocumentLocator( locator);
    }

    @Override
	public void startDocument() throws SAXException {
        parent.startDocument();

        annotateText = true;
        inP = false;
    }

    @Override
	public void endDocument() throws SAXException {
        parent.endDocument();

        parser.reset();
    }

    @Override
	public void startPrefixMapping( String prefix, String uri) throws SAXException {
        parent.startPrefixMapping( prefix, uri);
    }

    @Override
	public void endPrefixMapping( String prefix) throws SAXException {
        parent.endPrefixMapping( prefix);
    }

    @Override
	public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts)
        throws SAXException {
        parent.startElement( namespaceURI, localName, qName, atts);
        if (P_ELEMENT.equals( localName) || P_ELEMENT.equals( qName)) {
	        inP = true;
        }
    }

    @Override
	public void endElement( String namespaceURI, String localName,
                            String qName) throws SAXException {
        parent.endElement( namespaceURI, localName, qName);
        if (P_ELEMENT.equals( localName) || P_ELEMENT.equals( qName)) {
	        inP = false;
        }
    }

    /**
     * Handle character strings in the document. The string is parsed and annotations are
     * inserted.
     */
    @Override
	public void characters( char[] c, int start, int length) throws SAXException {
        if (!(inP && annotateText)) {
            parent.characters( c, start, length);
            return;
        }

        if (readingFilter != null) {
            readingsList.clear();
            c = readingFilter.filter( c, start, length, readingsList);
            start = 0; // c is now the subarray from c[start] to c[start+length-1]
            length = c.length; // if readings were removed, array is shorter
        }
        // TODO: use readings list

        List<TextAnnotation> annotations = null;
        try {
            annotations = parser.parse( c, start, length);
        } catch (SearchException ex) {
            throw new SAXException( ex);
        }

        int lastEnd = start; // index one after the end of the last annotation
        for (TextAnnotation annotation : annotations) {
            TextAnnotation completedAnnotation = taCompleter.complete(annotation);
            // handle text between annotations
            if (completedAnnotation.getStart() > lastEnd) {
	            parent.characters( c, lastEnd, completedAnnotation.getStart() - lastEnd);
            }
            lastEnd = completedAnnotation.getStart() + completedAnnotation.getLength();
            
            String annotatedWord = new String( c, completedAnnotation.getStart(), completedAnnotation.getLength());

            // start annotation element
            AttributesImpl annoAtts = new AttributesImpl();
            if (completedAnnotation.getTranslation() != null) {
	            annoAtts.addAttribute( "", "", JGlossDocument.Attributes.TRANSLATION,
                                       CDATA, completedAnnotation.getTranslation());
            }
            if (completedAnnotation.getGrammaticalType() != null) {
	            annoAtts.addAttribute( "", "", JGlossDocument.Attributes.TYPE,
                                       CDATA, completedAnnotation.getGrammaticalType());
            }
            if (!annotatedWord.equals( completedAnnotation.getDictionaryForm())) {
	            annoAtts.addAttribute( "", "", JGlossDocument.Attributes.BASE,
                                       CDATA, completedAnnotation.getDictionaryForm());
            }

            // generate reading elements for kanji substrings
            String[][] parts;
            try {
                parts = StringTools.splitWordReading( annotatedWord,
                                                      completedAnnotation.getDictionaryForm(),
                                                      completedAnnotation.getDictionaryFormReading());
            } catch (StringIndexOutOfBoundsException ex) {
                LOGGER.severe( "Warning: unparseable word/reading: " +
                                    annotatedWord + "/" + completedAnnotation.getDictionaryForm() + "/" +
                                    completedAnnotation.getDictionaryFormReading());
                parts = new String[][] { { annotatedWord } };
            }

            if (completedAnnotation.getReading() == null) {
                // derive inflected reading from splitWordReading
                StringBuilder inflectedReading = new StringBuilder( 64);
                for (String[] part : parts) {
                    if (part.length == 1) {
	                    inflectedReading.append( part[0]);
                    } else {
	                    inflectedReading.append( part[1]);
                    }
                }
                completedAnnotation.setReading( inflectedReading.toString());
            }

            if (!completedAnnotation.getReading().equals( completedAnnotation.getDictionaryFormReading())) {
	            annoAtts.addAttribute( "", "", JGlossDocument.Attributes.BASE_READING,
                                       CDATA, completedAnnotation.getDictionaryFormReading());
            }

            parent.startElement( "", "", JGlossDocument.Elements.ANNOTATION, annoAtts);
            int partPosition = completedAnnotation.getStart(); // position of part substring in c array
            for (String[] part : parts) {
                if (part.length == 2) {
                    // reading annotation

                    // if the reading of a kanji substring is not known, it is set
                    // to the kanji substring itself
                    String thisReading = part[1].equals( part[0]) ?
                        "" : part[1];
                    readingAtts.setValue( 0, thisReading);

                    parent.startElement( "", "", JGlossDocument.Elements.RBASE, readingAtts);
                }

                // add the reading base, or part without reading
                parent.characters( c, partPosition, part[0].length());
                partPosition += part[0].length();

                if (part.length == 2) {
                    // end reading element
                    parent.endElement( "", "", JGlossDocument.Elements.RBASE);
                }
            }

            // end annotation element
            parent.endElement( "", "", JGlossDocument.Elements.ANNOTATION);
        }

        // handle remaining unannotated text
        if (lastEnd < start+length) {
	        parent.characters( c, lastEnd, start+length-lastEnd);
        }
    }

    @Override
	public void ignorableWhitespace( char[] ch, int start, int length)
        throws SAXException {
        parent.ignorableWhitespace( ch, start, length);
    }

    @Override
	public void processingInstruction( String target, String data)
        throws SAXException {
        parent.processingInstruction( target, data);
    }

    @Override
	public void skippedEntity( String name) throws SAXException {
        parent.skippedEntity( name);
    }

    @Override
	public void error( SAXParseException e) throws SAXException {
        e.printStackTrace();
    }

    @Override
	public void warning( SAXParseException e) throws SAXException {
        e.printStackTrace();
    }

    @Override
	public InputSource resolveEntity( String publicId, String systemId) throws SAXException, IOException {
        return parent.resolveEntity( publicId, systemId);
    }

    @Override
	public void notationDecl( String name, String publicId, String systemId)
        throws SAXException {
        parent.notationDecl( name, publicId, systemId);
    }

    @Override
	public void unparsedEntityDecl( String name, String publicId, String systemId,
                                    String notationName)
        throws SAXException {
        parent.unparsedEntityDecl( name, publicId, systemId, notationName);
    }
} // class AnnotationGenerator
