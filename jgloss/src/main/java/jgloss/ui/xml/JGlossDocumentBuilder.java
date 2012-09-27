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
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jgloss.dictionary.Dictionary;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Build JGloss XML documents from input sources.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
public class JGlossDocumentBuilder {
	private static final Logger LOGGER = Logger.getLogger(JGlossDocumentBuilder.class.getPackage().getName());
	
	private final SAXParser xmlParser;

    private final DocumentGenerator docGen = new DocumentGenerator();

    public JGlossDocumentBuilder() {
        try {
            xmlParser = SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception ex) {
            // SAXException or ParserConfigurationException
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public JGlossDocument build( Reader text, boolean detectLineBreaks,
                                 ReadingAnnotationFilter readingFilter, Parser parser,
                                 Dictionary[] dictionaries) throws IOException, SAXException {
        AnnotationGenerator handler = new AnnotationGenerator( docGen, readingFilter, parser,
                                                               dictionaries);
                                                               
        InputSource inputSource = new InputSource( new JGlossifyReader( text, null, detectLineBreaks));                                                               
        
        // TODO: Progress Bar: this step takes very long:
        xmlParser.parse( inputSource, handler);
        
        JGlossDocument result = new JGlossDocument( docGen.getGeneratedDocument());
        return result;
    }
} // class JGlossDocumentBuilder
