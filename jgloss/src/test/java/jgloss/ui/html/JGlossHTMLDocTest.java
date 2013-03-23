/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 */

package jgloss.ui.html;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLUnit.buildControlDocument;
import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.StyleSheet;

import jgloss.ui.xml.JGlossDocument;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class JGlossHTMLDocTest {
    private static final Properties expectedResults = initExpectedResults();

    private JGlossDocument jglossDoc;

    private final JGlossHTMLDoc doc = new JGlossHTMLDoc(new StyleSheet(), new JGlossParserWrapper());

    private final JGlossEditorKit editorKit = new JGlossEditorKit(true, true);

    @BeforeClass
    public static void configureXMLUnit() {
        XMLUnit.setNormalize(true);
    }

    private static Properties initExpectedResults() {
        Properties expectedResults = new Properties();

        try {
            expectedResults.load(new InputStreamReader(JGlossHTMLDocTest.class
                            .getResourceAsStream(JGlossHTMLDocTest.class.getSimpleName() + ".properties"), "UTF-8"));
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }

        return expectedResults;
    }

    @Before
    public void initJGlossDoc() throws IOException, SAXException, BadLocationException {
        jglossDoc = new JGlossDocument(new InputSource(JGlossHTMLDocTest.class.getResourceAsStream("/jgloss2.jgloss")));
        doc.setJGlossDocument(jglossDoc);
        Element root = doc.getDefaultRootElement();
        assertThat(root).isNotNull();
        assertThat(root.getName()).isEqualTo("html");
        doc.setStrictParsing(false);
    }

    @Test
    public void testRemoveAnnotations() throws IOException, SAXException, BadLocationException {
        doc.removeAnnotations(0, doc.getLength());

        assertXMLEqual(buildControlDocument(getExpectedResult("testRemoveAnnotations")), jglossDoc.getDOMDocument());
    }

    @Test
    public void testAddAnnotation() throws IOException, BadLocationException, SAXException {
        doc.addAnnotation(77, 80, editorKit);
        dumpDocument();

        assertXMLEqual(buildControlDocument(getExpectedResult("testAddAnnotation")), jglossDoc.getDOMDocument());
    }

    @Test
    public void testAddAnnotationSingleKanji() throws IOException, BadLocationException, SAXException {
        doc.addAnnotation(77, 78, editorKit);
        dumpDocument();

        assertXMLEqual(buildControlDocument(getExpectedResult("testAddAnnotationSingleKanji")),
                        jglossDoc.getDOMDocument());
    }

    @Test
    public void testAddAnnotationAfterOther() throws SAXException, IOException, BadLocationException {
        doc.addAnnotation(76, 78, editorKit);

        assertXMLEqual(buildControlDocument(getExpectedResult("testAddAnnotationAfterOther")),
                        jglossDoc.getDOMDocument());
    }

    @Test
    public void testAnnotateAll() throws SAXException, IOException, BadLocationException {
        doc.addAnnotation(66, 100, editorKit);
        dumpDocument();

        assertXMLEqual(buildControlDocument(getExpectedResult("testAnnotateAll")), jglossDoc.getDOMDocument());
    }

    @Test
    public void testGetUnannotatedText() {
        assertThat(doc.getUnannotatedText(0, doc.getLength())).isEqualTo("bazq漢う字x。");
    }

    private String getExpectedResult(String key) {
        String expectedResult = expectedResults.getProperty(key + ".expected");
        assertThat(expectedResult).isNotNull();
        return expectedResult;
    }

    private void dumpDocument() throws IOException, BadLocationException {
        // System.out.println("HTML");
        // StringWriter docString = new StringWriter();
        // new HTMLWriter(docString, doc).write();
        // System.out.println(docString);
        //
        // System.out.println("XML");
        // Document domdoc = jglossDoc.getDOMDocument();
        // DOMImplementationLS domImplementation = (DOMImplementationLS)
        // domdoc.getImplementation();
        // LSSerializer lsSerializer = domImplementation.createLSSerializer();
        // System.out.println(lsSerializer.writeToString(domdoc));
    }
}
