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

import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.DTD;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Adapt a SAX XML parser callback to the <code>HTMLEditorKit.ParserCallback</code> interface.
 *
 * @author Michael Koch
 */
class SAXToHTMLParserAdapter extends DefaultHandler {
    private HTMLEditorKit.ParserCallback htmlHandler;
    private DTD dtd;
    private int position;
    
    public SAXToHTMLParserAdapter( HTMLEditorKit.ParserCallback _htmlHandler,
                                   DTD _dtd) {
        htmlHandler = _htmlHandler;
        dtd = _dtd;
    }

    public void startDocument() throws SAXException {
        position = 0;
    }

    public void endDocument() throws SAXException {
        htmlHandler.handleEndOfLineString( "\n");
        try {
            htmlHandler.flush();
        } catch (BadLocationException ex) {
            throw new SAXException( ex);
        }
    }

    private HTML.Tag getTag( String qName) {
        HTML.Tag tag = HTML.getTag( qName);
        if (tag == null) 
            // special JGloss tag
            tag = AnnotationTags.getAnnotationTagEqualTo( qName);
        return tag;
    }

    public void startElement( String uri, String localName, String qName,
                              Attributes attributes) throws SAXException {
        MutableAttributeSet htmlAtts = new SimpleAttributeSet();
        for ( int i=0; i<attributes.getLength(); i++) {
            htmlAtts.addAttribute( attributes.getQName( i), attributes.getValue( i));
        }
        
        HTML.Tag tag = getTag( qName);
        if (dtd.getElement( qName).isEmpty())
            htmlHandler.handleSimpleTag( tag, htmlAtts, position);
        else
            htmlHandler.handleStartTag( tag, htmlAtts, position);
        position += qName.length();
    }

    public void endElement( String uri, String localName, String qName)
        throws SAXException {
        if (!dtd.getElement( qName).isEmpty()) {
            htmlHandler.handleEndTag( getTag( qName), position);
            position += qName.length();
        }
    }

    public void characters( char[] c, int start, int length) throws SAXException {
        // don't forward ignorable whitespace
        // TODO: implement real ignorable whitespace handling
        if (length==1 && c[start]=='\n')
            return;
        
        if (start>0 || start+length<c.length) {
            char[] ctemp = new char[length];
            System.arraycopy( c, start, ctemp, 0, length);
            c = ctemp;
        }
        htmlHandler.handleText( c, position);

        position += length;
    }

    public void warning( SAXParseException e) throws SAXException {
        htmlHandler.handleError( "WARNING: " + e.getMessage(), position);
    }

    public void error( SAXParseException e) throws SAXException {
        htmlHandler.handleError( "ERROR: " + e.getMessage(), position);   
    }

    public void fatalError( SAXParseException e) throws SAXException {
        htmlHandler.handleError( "FATAL ERROR: " + e.getMessage(), position);
        throw e;
    }
} // class SAXToHTMLParserAdapter
