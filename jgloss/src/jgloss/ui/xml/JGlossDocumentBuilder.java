package jgloss.ui.xml;

import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;
import jgloss.dictionary.Dictionary;

import java.io.Reader;
import java.io.IOException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import jgloss.ui.html.JGlossEditorKit;
import jgloss.ui.html.JGlossHTMLDoc;

public class JGlossDocumentBuilder {
    private SAXParser xmlParser;
    private DocumentGenerator docGen;

    public JGlossDocumentBuilder() {
        try {
            xmlParser = SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception ex) {
            // SAXException or ParserConfigurationException
            ex.printStackTrace();
        }
        docGen = new DocumentGenerator();
    }

    public JGlossDocument build( Reader text, boolean detectLineBreaks,
                                 ReadingAnnotationFilter readingFilter, Parser parser,
                                 Dictionary[] dictionaries) throws IOException, SAXException {
        AnnotationGenerator annoGen = new AnnotationGenerator( docGen, readingFilter, parser,
                                                               dictionaries);
        xmlParser.parse( new InputSource( new JGlossifyReader( text, null, detectLineBreaks)),
                         annoGen);
        return new JGlossDocument( docGen.getGeneratedDocument());
    }
} // class JGlossDocumentBuilder
