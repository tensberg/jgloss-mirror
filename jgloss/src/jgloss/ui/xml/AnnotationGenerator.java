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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.SearchException;
import jgloss.parser.Parser;
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
    private DefaultHandler parent;
    private ReadingAnnotationFilter readingFilter;
    private Parser parser;
    private TextAnnotationCompleter taCompleter;

    private List readingsList = new ArrayList( 5);
    private boolean annotateText;
    private boolean inP;
    private AttributesImpl readingAtts = new AttributesImpl(); 

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

    public void setDocumentLocator( Locator locator) {
        parent.setDocumentLocator( locator);
    }

    public void startDocument() throws SAXException {
        parent.startDocument();

        annotateText = true;
        inP = false;
    }

    public void endDocument() throws SAXException {
        parent.endDocument();
        parser.reset();
    }

    public void startPrefixMapping( String prefix, String uri) throws SAXException {
        parent.startPrefixMapping( prefix, uri);
    }

    public void endPrefixMapping( String prefix) throws SAXException {
        parent.endPrefixMapping( prefix);
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts)
        throws SAXException {
        parent.startElement( namespaceURI, localName, qName, atts);
        if (P_ELEMENT.equals( localName) || P_ELEMENT.equals( qName))
            inP = true;
    }

    public void endElement( String namespaceURI, String localName,
                            String qName) throws SAXException {
        parent.endElement( namespaceURI, localName, qName);
        if (P_ELEMENT.equals( localName) || P_ELEMENT.equals( qName))
            inP = false;
    }

    /**
     * Handle character strings in the document. The string is parsed and annotations are
     * inserted.
     */
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

        List annotations = null;
        try {
            annotations = parser.parse( c, start, length);
        } catch (SearchException ex) {
            throw new SAXException( ex);
        }

        int lastEnd = start; // index one after the end of the last annotation
        for ( Iterator i=annotations.iterator(); i.hasNext(); ) {
            TextAnnotation anno = taCompleter.complete( (TextAnnotation) i.next());
            // handle text between annotations
            if (anno.getStart() > lastEnd)
                parent.characters( c, lastEnd, anno.getStart() - lastEnd);
            lastEnd = anno.getStart() + anno.getLength();
            
            String annotatedWord = new String( c, anno.getStart(), anno.getLength());

            // start annotation element
            AttributesImpl annoAtts = new AttributesImpl();
            if (anno.getTranslation() != null)
                annoAtts.addAttribute( "", "", JGlossDocument.Attributes.TRANSLATION,
                                       CDATA, anno.getTranslation());
            if (anno.getGrammaticalType() != null)
                annoAtts.addAttribute( "", "", JGlossDocument.Attributes.TYPE,
                                       CDATA, anno.getGrammaticalType());
            if (!annotatedWord.equals( anno.getDictionaryForm()))
                annoAtts.addAttribute( "", "", JGlossDocument.Attributes.BASE,
                                       CDATA, anno.getDictionaryForm());

            // generate reading elements for kanji substrings
            String[][] parts;
            try {
                parts = StringTools.splitWordReading( annotatedWord,
                                                      anno.getDictionaryForm(),
                                                      anno.getDictionaryFormReading());
            } catch (StringIndexOutOfBoundsException ex) {
                System.err.println( "Warning: unparseable word/reading: " +
                                    annotatedWord + "/" + anno.getDictionaryForm() + "/" +
                                    anno.getDictionaryFormReading());
                parts = new String[][] { { annotatedWord } };
            }

            if (anno.getReading() == null) {
                // derive inflected reading from splitWordReading
                StringBuffer inflectedReading = new StringBuffer( 64);
                for ( int j=0; j<parts.length; j++) {
                    if (parts[j].length == 1)
                        inflectedReading.append( parts[j][0]);
                    else
                        inflectedReading.append( parts[j][1]);
                }
                anno.setReading( inflectedReading.toString());
            }

            if (!anno.getReading().equals( anno.getDictionaryFormReading()))
                annoAtts.addAttribute( "", "", JGlossDocument.Attributes.BASE_READING,
                                       CDATA, anno.getDictionaryFormReading());

            parent.startElement( "", "", JGlossDocument.Elements.ANNOTATION, annoAtts);
            int partPosition = anno.getStart(); // position of part substring in c array
            for ( int j=0; j<parts.length; j++) {
                if (parts[j].length == 2) {
                    // reading annotation

                    // if the reading of a kanji substring is not known, it is set
                    // to the kanji substring itself
                    String thisReading = parts[j][1].equals( parts[j][0]) ?
                        "" : parts[j][1];
                    readingAtts.setValue( 0, thisReading);

                    parent.startElement( "", "", JGlossDocument.Elements.RBASE, readingAtts);
                }

                // add the reading base, or part without reading
                parent.characters( c, partPosition, parts[j][0].length());
                partPosition += parts[j][0].length();

                if (parts[j].length == 2) {
                    // end reading element
                    parent.endElement( "", "", JGlossDocument.Elements.RBASE);
                }
            }

            // end annotation element
            parent.endElement( "", "", JGlossDocument.Elements.ANNOTATION);
        }

        // handle remaining unannotated text
        if (lastEnd < start+length)
            parent.characters( c, lastEnd, start+length-lastEnd);
    }

    public void ignorableWhitespace( char[] ch, int start, int length)
        throws SAXException {
        parent.ignorableWhitespace( ch, start, length);
    }

    public void processingInstruction( String target, String data)
        throws SAXException {
        parent.processingInstruction( target, data);
    }

    public void skippedEntity( String name) throws SAXException {
        parent.skippedEntity( name);
    }

    public void error( SAXParseException e) throws SAXException {
        e.printStackTrace();
    }

    public void warning( SAXParseException e) throws SAXException {
        e.printStackTrace();
    }

    public InputSource resolveEntity( String publicId, String systemId) throws SAXException {
        return parent.resolveEntity( publicId, systemId);
    }

    public void notationDecl( String name, String publicId, String systemId)
        throws SAXException {
        parent.notationDecl( name, publicId, systemId);
    }

    public void unparsedEntityDecl( String name, String publicId, String systemId,
                                    String notationName)
        throws SAXException {
        parent.unparsedEntityDecl( name, publicId, systemId, notationName);
    }
} // class AnnotationGenerator
