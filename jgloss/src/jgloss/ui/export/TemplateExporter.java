/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.export;

import jgloss.*;
import jgloss.dictionary.CharacterEncodingDetector;
import jgloss.ui.doc.*;
import jgloss.ui.annotation.*;

import java.io.*;
import java.util.*;
import java.text.*;

import javax.swing.text.*;

/**
 * Export an annotated JGloss document using a format defined in a template.
 * <p>
 * A template is a text file, which can contain <em>variables</em> and 
 * <em>insertion sections</em>. A variable has the form %name%. It can appear anywhere
 * in the input text and is replaced by its value when the document is exported.
 * </p><p>
 * An insertion section is marked by <code>%section-name</code> and
 * <code>%end section-name</code>, each on a single line. Between these markers are the
 * <em>pattern definitions</em>. On export the insertion section is replaced by the
 * document text or annotation list, with the patterns applied to the annotations.
 * </p><p>
 * A pattern definition has the form <code>%pattern-name pattern</code>.
 * <code>pattern</code> can contain variable definitions and the escape sequences 
 * <code>\\, \t, \n and \r</code>, which have the same meaning as in a Java string.
 * What patterns can appear in an insertion section depends on the section definition.
 * </p>
 *
 * @author Michael Koch
 */
public abstract class TemplateExporter {
    /**
     * Marks an encoding declaration in a template file 
     * read by {@link #getReader(InputStream) getReader}.
     */
    public static final String ENCODING_HEADER = "% encoding:";

    /**
     * Character which marks the beginning of a variable.
     */
    public static final char VARIABLE_START_MARKER = '%';
    /**
     * Character which marks the end of a variable.
     */
    public static final char VARIABLE_END_MARKER = '%';
    /**
     * Variable: replaced by the file name of the exported document.
     */
    public static final String DOCUMENT_FILENAME = "%document-filename%";
    /**
     * Variable: replaced by the time and date of the document generation.
     */
    public static final String GENERATION_TIME = "%generation-time%";
    /**
     * Variable: replaced by the title of the document.
     */
    public static final String DOCUMENT_TITLE = "%document-title%";
    /**
     * Variable: replaced by the longest dictionary word in the document.
     */
    public static final String LONGEST_WORD = "%longest-word%";
    /**
     * Variable: replaced by the longest dictionary reading in the document.
     */
    public static final String LONGEST_READING = "%longest-reading%";

    /**
     * Text insertion section start marker. The document text is inserted in its place.<br>
     * Allowed pattern definitions:
     * <ul>
     * <li>{@link #READING_PATTERN READING_PATTERN}</li>
     * <li>{@link #TRANSLATION_PATTERN TRANSLATION_PATTERN}</li>
     * <li>{@link #LINE_BREAK_PATTERN LINE_BREAK_PATTERN}</li>
     * <li>{@link #PARAGRAPH_START_PATTERN PARAGRAPH_START_PATTERN}</li>
     * <li>{@link #PARAGRAPH_END_PATTERN PARAGRAPH_END_PATTERN}</li>
     * </ul>
     */
    public static final String BEGIN_TEXT = "%document-text";
    /**
     * Annotation list insertion section start marker. The list of annotations is inserted in 
     * its place.
     * Allowed pattern definitions:
     * <ul>
     * <li>{@link #TRANSLATION_PATTERN TRANSLATION_PATTERN}</li>
     * <li>{@link #PARAGRAPH_START_PATTERN PARAGRAPH_START_PATTERN}</li>
     * <li>{@link #PARAGRAPH_END_PATTERN PARAGRAPH_END_PATTERN}</li>
     * </ul>
     */
    public static final String BEGIN_ANNOTATION_LIST = "%annotation-list";
    /**
     * Insertion section end marker.
     */
    public static final String END = "%end";
    /**
     * Variable: replaced with the word part when used in a {@link #READING_PATTERN reading pattern}.
     */
    public static final String WORD = "%word%";
    /**
     * Variable: replaced with the dictionary word when used in a 
     * {@link #TRANSLATION_PATTERN translation pattern}.
     */
    public static final String DICTIONARY_WORD = "%dictionary-word%";
    /**
     * Variable: replaced with the dictionary part when used in a {@link #READING_PATTERN reading pattern}.
     */
    public static final String READING = "%reading%";
    /**
     * Variable: replaced with the dictionary reading when used in a 
     * {@link #TRANSLATION_PATTERN translation pattern}.
     */
    public static final String DICTIONARY_READING = "%dictionary-reading%";
    /**
     * Variable: replaced with the translation when used in a 
     * {@link #TRANSLATION_PATTERN translation pattern}.
     */
    public static final String TRANSLATION = "%translation%";
    /**
     * Variable: replaced with the paragraph number when used in a 
     * {@link #PARAGRAPH_START_PATTERN paragraph start pattern}.
     */
    public static final String PARAGRAPH_NUMBER = "%paragraph-number%";

    /**
     * Pattern name: the pattern is applied to every word/reading pair.
     */
    public static final String READING_PATTERN = "%reading";
    /**
     * Pattern name: the pattern is applied to every translation.
     */
    public static final String TRANSLATION_PATTERN = "%translation";
    /**
     * Pattern name: the pattern is applied to every line break.
     */
    public static final String LINE_BREAK_PATTERN = "%line-break";
    /**
     * Pattern name: the pattern is applied to every paragraph start.
     */
    public static final String PARAGRAPH_START_PATTERN = "%paragraph-start";
    /**
     * Pattern name: the pattern is applied to every paragraph end.
     */
    public static final String PARAGRAPH_END_PATTERN = "%paragraph-end";

    /**
     * Object inserted in the {@link #parsedText parsedText} list for every line
     * break in the text.
     */
    protected static Object LINE_BREAK_MARKER = new Object();
    /**
     * Object inserted in the {@link #parsedText parsedText} list for every
     * paragraph end in the text. An <code>Integer</code> object with the paragraph
     * number is inserted for a paragraph start.
     */
    protected static Object PARAGRAPH_END_MARKER = new Object();

    /**
     * Representation of a word/reading pair in the segmented document.
     */
    protected static class Reading {
        private String[] data;

        public Reading( String word, String ruby) {
            data = new String[] { word, ruby };
        }

        public String getWord() { return data[0]; }
        public String getReading() { return data[1]; }
        public String[] asArray() { return data; }
    } // class Reading

    /**
     * Representation of a translation in the segmented document.
     */
    protected static class Translation {
        private String[] data;

        public Translation( String dictionaryWord, String dictionaryReading, String translation) {
            data = new String[] { dictionaryWord, dictionaryReading, translation };
        }

        public String getDictionaryWord() { return data[0]; }
        public String getDictionaryReading() { return data[1]; }
        public String getTranslation() { return data[2]; }
        public String[] asArray() { return data; }
    } // class Translation

    /**
     * List of the document text, split in normal text, text with ruby, annotations and paragraph markers.
     * Normal text between two annotations is stored as a string. 
     * Text with ruby is stored as {@link Reading Reading} instance. An annotation is stored
     * as a {@link Translation Translation} instance. For a paragraph start, an <code>Integer</code>  
     * with the paragraph number is stored.
     */
    protected List parsedText = null;

    protected String longestDictionaryWord = "";
    protected String longestDictionaryReading = "";

    /**
     * Creates a new template exporter instance.
     */
    protected TemplateExporter() {}

    /**
     * Prepare the document for export. The document text is parsed by calling 
     * {@link #parseText(JGlossDocument,AnnotationModel,boolean) parseText} and the variable map
     * is initialized. Subclasses should call this method from their <code>export</code> method,
     * insert additional variables to the returned set and then call the
     * {@link #export(Reader,Writer,Map) export} method of this class.
     *
     * @returns The map with initialized variables.
     */
    protected Map prepareExport( JGlossDocument doc, String documentName, AnnotationModel model,
                                 boolean writeHidden) {
        parseText( doc, model, writeHidden);
        Map variables = new HashMap( 37);
        variables.put( DOCUMENT_FILENAME, documentName);
        variables.put( GENERATION_TIME, DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.SHORT)
                       .format( new Date( System.currentTimeMillis())));
        variables.put( DOCUMENT_TITLE, escape( doc.getTitle()));
        variables.put( LONGEST_WORD, longestDictionaryWord);
        variables.put( LONGEST_READING, longestDictionaryReading);

        // variable substititions used to construct MessageFormat objects from ruby and
        // translation patterns
        variables.put( WORD, "{0}");
        variables.put( READING, "{1}");
        variables.put( DICTIONARY_WORD, "{0}");
        variables.put( DICTIONARY_READING, "{1}");
        variables.put( TRANSLATION, "{2}");
        variables.put( PARAGRAPH_NUMBER, "{0}");

        return variables;
    }

    /**
     * Export the document by reading the template and substituting all special sections.
     */
    protected void export( Reader template, Writer out, Map variables) throws IOException {
        LineNumberReader templateLines = new LineNumberReader( template);
        templateLines.setLineNumber( 1); // start counting at 1 and not 0
        String line;
        while ((line = templateLines.readLine()) != null) {
            if (line.startsWith( BEGIN_TEXT))
                handleInsertionSection( templateLines, out, variables, true, true, true, true);
            else if (line.startsWith( BEGIN_ANNOTATION_LIST))
                handleInsertionSection( templateLines, out, variables, false, false, true, true);
            else
                handleTemplateLine( line, out, variables);
        }
    }

    /**
     * Handle a normal template line by substituting all variables and writing it to the output
     * writer.
     */
    protected void handleTemplateLine( String line, Writer out, Map variables) throws IOException {
        out.write( substituteVariables( line, variables));
        out.write( '\n');
    }

    /**
     * Handle an insertion section by reading the patterns and writing the selected parts 
     * of the document text to the output writer.
     */
    protected void handleInsertionSection( LineNumberReader template, Writer out,
                                           Map variables, boolean writeText, boolean writeReadings,
                                           boolean writeTranslations, boolean writeParagraphs)
        throws IOException {
        int startLineNumber = template.getLineNumber();

        String line;
        String readingPattern = null;
        String translationPattern = null;
        String lineBreakPattern = null;
        String paragraphStartPattern = null;
        String paragraphEndPattern = null;
        try {
            while (!(line = template.readLine()).startsWith( END)) {
                String pattern = null;
                if (writeReadings) {
                    pattern = matchPatternDefinition( line, template.getLineNumber(),
                                                      READING_PATTERN, readingPattern);
                    if (pattern != null)
                        readingPattern = pattern;
                }

                if (pattern==null && writeTranslations) {
                    pattern = matchPatternDefinition
                        ( line, template.getLineNumber(), TRANSLATION_PATTERN, translationPattern);
                    if (pattern != null)
                        translationPattern = pattern;
                }

                if (pattern==null && writeText) {
                    pattern = matchPatternDefinition
                        ( line, template.getLineNumber(), LINE_BREAK_PATTERN, lineBreakPattern);
                    if (pattern != null)
                        lineBreakPattern = pattern;
                }

                if (pattern==null && (writeText || writeParagraphs)) {
                    pattern = matchPatternDefinition
                        ( line, template.getLineNumber(), PARAGRAPH_START_PATTERN, paragraphStartPattern);
                    if (pattern != null)
                        paragraphStartPattern = pattern;
                }

                if (pattern==null && (writeText || writeParagraphs)) {
                    pattern = matchPatternDefinition
                        ( line, template.getLineNumber(), PARAGRAPH_END_PATTERN, paragraphEndPattern);
                    if (pattern != null)
                        paragraphEndPattern = pattern;
                }

                if (pattern == null)
                    // one of the patterns must have matched,
                    // all other line contents are an error
                    throw new TemplateException
                        ( JGloss.messages.getString
                          ( "export.template.unknownline",
                            new Object[] { new Integer( template.getLineNumber()) }),
                          template.getLineNumber());
            }
        } catch (NullPointerException ex) {
            // template ended before %end marker was found
            throw new TemplateException( JGloss.messages.getString
                                         ( "export.template.unmatchedbegin",
                                           new Object[] { BEGIN_TEXT, new Integer( startLineNumber) }),
                                         startLineNumber);
        }

        // replace line end and paragraph patterns by default values
        if (lineBreakPattern == null)
            lineBreakPattern = defaultLineBreakPattern();
        lineBreakPattern = substituteVariables( quoteMessageFormat( lineBreakPattern), variables);
        if (paragraphEndPattern == null)
            paragraphEndPattern = defaultParagraphEndPattern();
        paragraphEndPattern = substituteVariables( quoteMessageFormat( paragraphEndPattern), variables);

        // build message formats for reading, translation and paragraph start pattern
        MessageFormat readingFormat = null;
        if (readingPattern != null)
            readingFormat = new MessageFormat( substituteVariables
                                               ( quoteMessageFormat( readingPattern), variables));
        MessageFormat translationFormat = null;
        if (translationPattern != null)
            translationFormat = new MessageFormat( substituteVariables
                                                   ( quoteMessageFormat( translationPattern), variables));
        MessageFormat paragraphStartFormat = null;
        Integer[] paragraphNumber = null;
        if (paragraphStartPattern == null)
            paragraphStartPattern = defaultParagraphStartPattern();
        paragraphStartFormat = new MessageFormat( substituteVariables
                                                  ( quoteMessageFormat( paragraphStartPattern), variables));
        paragraphNumber = new Integer[1];

        // write the output text
        for ( Iterator i=parsedText.iterator(); i.hasNext(); ) {
            Object next = i.next();
            if (next instanceof String) {
                // normal text
                if (writeText)
                    out.write( (String) next);
            }
            else if (next instanceof Reading) {
                if (writeReadings) {
                    Reading reading = (Reading) next;
                    if (readingFormat != null)
                        out.write( readingFormat.format( reading.asArray(), new StringBuffer(), null)
                                   .toString());
                    else
                        out.write( reading.getWord());
                }
            }
            else if (next instanceof Translation) {
                if (writeTranslations) {
                    Translation translation = (Translation) next;
                    if (translationFormat != null)
                        out.write( translationFormat.format( translation.asArray(), new StringBuffer(), null)
                                   .toString());
                }
            }
            else if (next instanceof Integer) { // paragraph start marker
                if (writeText || writeParagraphs) {
                    paragraphNumber[0] = (Integer) next;
                    out.write( paragraphStartFormat.format( paragraphNumber, new StringBuffer(), null)
                               .toString());
                }
            }
            else if (next == PARAGRAPH_END_MARKER) {
                if (writeText || writeParagraphs)
                    out.write( paragraphEndPattern);
            }
            else if (next == LINE_BREAK_MARKER) {
                if (writeText)
                    out.write( lineBreakPattern);
            }
        }
    }

    /**
     * Create a list of text segments. The text is segmented in normal text, word/ruby pairs,
     * translations and paragraph starts.
     *
     * @param doc The document to write.
     * @param model Annotation model which contains the annotations of the document.
     * @param writeHidden <CODE>true</CODE> if annotations marked as hidden should be written.
     * @see #parsedText
     */
    protected void parseText( JGlossDocument doc, AnnotationModel model, boolean writeHidden) {
        parsedText = new ArrayList( model.getAnnotationCount()*3 + 30);
        int paragraph = 1; // current paragraph
        // text starts with a paragraph
        parsedText.add( new Integer( paragraph));

        String text = null;
        try {
            text = doc.getText( 0, doc.getLength());
        } catch (BadLocationException ex) {}
        
        // root element is HTML, child 0 is HEAD, child 1 is body. Ignore text in HEAD by
        // initializing prevend to start of body
        Element body = doc.getDefaultRootElement().getElement( 1); 
        int prevend = body.getStartOffset(); // end offset of the previous annotation
        for ( int i=0; i<model.getAnnotationCount(); i++) {
            AnnotationNode annotation = model.getAnnotationNode( i);
            Element ae = annotation.getAnnotationElement();

            // add text between annotations
            if (prevend < ae.getStartOffset())
                paragraph = addTextAndParagraphs( text.substring( prevend, ae.getStartOffset()), paragraph);
            prevend = ae.getEndOffset();

            // handle reading and word text
            Element wordelement = ae.getElement( 0);
            for ( int j=0; j<wordelement.getElementCount(); j++) {
                Element child = wordelement.getElement( j);
                if (child.getElementCount() == 2) { // READING_BASETEXT element
                    String word = escape( text.substring( child.getElement( 1).getStartOffset(),
                                                          child.getElement( 1).getEndOffset()));
                    String reading = escape( text.substring( child.getElement( 0).getStartOffset(),
                                                             child.getElement( 0).getEndOffset()));
                    if (reading.length()==0 || reading.equals( " ") ||
                        (!writeHidden && annotation.isHidden()))
                        parsedText.add( word);
                    else
                        parsedText.add( new Reading( word, reading));
                }
                else { // BASETEXT element
                    parsedText.add( escape( text.substring( child.getStartOffset(),
                                                            child.getEndOffset())));
                }
            }

            // handle translation text
            String translation = escape( text.substring( ae.getElement( 1).getStartOffset(), 
                                                         ae.getElement( 1).getEndOffset()));
            String dictionaryWord = escape( annotation.getDictionaryFormNode().getWord());
            if (longestDictionaryWord.length() < dictionaryWord.length())
                longestDictionaryWord = dictionaryWord;
            String dictionaryReading = escape( annotation.getDictionaryFormNode().getReading());
            if (longestDictionaryReading.length() < dictionaryReading.length())
                longestDictionaryReading = dictionaryReading;
            if (!(translation.length()==0 || translation.equals( " "))) {
                parsedText.add( new Translation
                    ( dictionaryWord, dictionaryReading, translation));
            }
        }
        // add the remaining text
        if (prevend < text.length())
            addTextAndParagraphs( text.substring( prevend, text.length()), paragraph);

        // close last paragraph
        if (parsedText.get( parsedText.size()-1) != PARAGRAPH_END_MARKER)
            parsedText.add( PARAGRAPH_END_MARKER);
    }

    /**
     * Add unannotated text, line break and paragraph markers to the parsed text list.
     * A \n in the document text is interpreted as paragraph marker. 
     *
     * @param text The text, possibly containing paragraph markers.
     * @param paragraphNumber Number of the current paragraph.
     * @return New paragraph number.
     */
    protected int addTextAndParagraphs( String text, int paragraphNumber) {
        text = escape( text);
        int from = 0;
        int to;
        while ((to = text.indexOf( '\n', from)) != -1) {
            String p = text.substring( from, to);
            if (p.length() > 0) {
                if (parsedText.get( parsedText.size()-1) == PARAGRAPH_END_MARKER) {
                    // p starts a new paragraph
                    paragraphNumber++;
                    // add paragraph start marker
                    parsedText.add( new Integer( paragraphNumber));
                }
                parsedText.add( p);
            }
            // if the \n is immediately followed by another \n, it is considered
            // to mark a paragraph break, otherwise a line break
            to++;
            if (to<text.length() && text.charAt( to)=='\n') {
                // paragraph break
                parsedText.add( PARAGRAPH_END_MARKER);
                // skip over additional \n
                while (to<text.length() && text.charAt( to)=='\n')
                    to++;
            }
            else {
                // line break
                parsedText.add( LINE_BREAK_MARKER);
            }

            from = to;
        }
        // add remaining text (if any)
        if (from < text.length()) {
            if (parsedText.get( parsedText.size()-1) == PARAGRAPH_END_MARKER) {
                // p starts a new paragraph
                paragraphNumber++;
                // add paragraph start marker
                parsedText.add( new Integer( paragraphNumber));
            }
            parsedText.add( text.substring( from));
        }
        
        return paragraphNumber;
    }

    /**
     * Test if the line is a pattern definition of the specified pattern, and return the pattern.
     *
     * @param line Line from the template which may be a pattern definition.
     * @param lineNumber Current line number of the pattern reader.
     * @param name Pattern name including the pattern start marker "% ".
     * @param definedPattern Value of the pattern if it is already defined. If this is not
     *        <code>null</code> and the line redefines the pattern, a 
     *        {@link TemplateException TemplateException} is thrown.
     * @return The pattern, if it was defined, otherwise <code>null</code>.
     * @exception TemplateException if an already defined pattern is redefined.
     */
    protected String matchPatternDefinition( String line, int lineNumber, String name, 
                                             String definedPattern) throws TemplateException {
        if (line.toLowerCase().startsWith( name)) {
            if (definedPattern != null)
                throw new TemplateException
                    ( JGloss.messages.getString
                      ( "export.template.patternredefined",
                        new Object[] { new Integer( lineNumber) }),
                      lineNumber);
            return unescapePattern( line.substring( name.length() + 1));
        }

        return null;
    }

    /**
     * Substitute variables in a line with their values. Variable delimiters are
     * {@link #VARIABLE_START_MARKER VARIABLE_START_MARKER} and
     * {@link #VARIABLE_END_MARKER VARIABLE_END_MARKER}.
     *
     * @param line Line, possibly with variables to replace.
     * @param substitutions Mapping from variable names to values.
     */
    protected String substituteVariables( String line, Map substitutions) {
        StringBuffer linebuf = null;
        int variableEnd = line.lastIndexOf( VARIABLE_END_MARKER);
        while (variableEnd != -1) {
            int variableStart = line.lastIndexOf( VARIABLE_START_MARKER, variableEnd-1);
            if (variableStart == -1)
                break;

            // determine which variable matches
            String replacement = null;
            int length = variableEnd - variableStart + 1;

            replacement = (String) substitutions.get( line.substring( variableStart, variableEnd+1));
            if (replacement != null) {
                if (linebuf == null)
                    linebuf = new StringBuffer( line);
                linebuf.replace( variableStart, variableEnd+1, replacement);
            }

            variableEnd = line.lastIndexOf( VARIABLE_END_MARKER, variableStart - 1);
        }

        if (linebuf != null)
            return linebuf.toString();
        else
            return line; // no substitutions
    }

    /**
     * Escapes any special characters specific to the export format. This implementation simply
     * returns the input string.
     *
     * @param in The string to escape.
     * @return The escaped string.
     */
    protected String escape( String in) { return in; }

    /**
     * Default pattern inserted in the output for line breaks if none is specified in the template.
     *
     * @return A single \n.
     */
    protected String defaultLineBreakPattern() { return "\n"; }

    /**
     * Default pattern inserted in the output for paragraph starts if none is specified in the template.
     *
     * @return The empty string.
     */
    protected String defaultParagraphStartPattern() { return ""; }

    /**
     * Default pattern inserted in the output for paragraph ends if none is specified in the template.
     *
     * @return "\n\n".
     */
    protected String defaultParagraphEndPattern() { return "\n\n"; }

    /**
     * Creates a reader which reads a template definition from an input stream. If the template
     * starts with a {@link #ENCODING_HEADER character encoding declaration} line, this encoding
     * will be used and the declaration line will be skipped by the reader. Otherwise the
     * {@link jgloss.dictionary.CharacterEncodingDetector character encoding detector} will be used
     * to create the reader.
     */
    public static InputStreamReader getReader( InputStream template) throws IOException {
        // look at the start of the input stream to find a encoding declaration
        int BUF_SIZE = ENCODING_HEADER.length() + 10;
        byte[] buf = new byte[BUF_SIZE];
        PushbackInputStream in = new PushbackInputStream( template, BUF_SIZE);
        int length = in.read( buf);
        // Encoding declaration is all-ASCII, which should be safe with any encoding
        String encoding = new String( buf, 0, length, "ASCII");
        if (encoding.toLowerCase().startsWith( ENCODING_HEADER)) {
            // find end of line
            int nl = encoding.indexOf( '\n');
            int cr = encoding.indexOf( '\r');
            int eol = nl; // end of first input line
            int sol; // start of second input line
            if (nl == -1)
                eol = cr;
            else if (cr != -1)
                eol = Math.min( nl, cr);
            if (cr+1 == nl) // DOS line ending
                sol = nl + 1;
            else
                sol = eol + 1;
            if (eol != -1) { // else: encoding line broken, go with CharacterEncodingDetector
                encoding = encoding.substring( 0, eol);
                in.unread( buf, sol, length-sol); // unread everything except encoding line
                // extract encoding name
                encoding = encoding.substring( ENCODING_HEADER.length()).trim();

                return new InputStreamReader( in, encoding);
            }
        }

        in.unread( buf, 0, length);
        return CharacterEncodingDetector.getReader( in);
    }

    /**
     * Escapes the characters in the string which have a special meaning when used as a
     * <code>MessageFormat</code>.
     */
    protected static String quoteMessageFormat( String format) {
        StringBuffer out = new StringBuffer( format.length() + 10);
        out.append( format);
        for ( int i=out.length()-1; i>=0; i--) {
            switch (out.charAt( i)) {
            case '\'':
            case '{':
                out.insert( i+1, '\'');
                out.insert( i, '\'');
                break;
            }
        }

        return out.toString();
    }

    /**
     * Replaces backslash-escapes in a pattern with the represented characters.
     */
    protected String unescapePattern( String pattern) {
        StringBuffer out = new StringBuffer( pattern);
        for ( int i=0; i<out.length()-1; i++) {
            if (out.charAt( i) == '\\') {
                char replacement = out.charAt( i+1);
                switch (replacement) {
                case 'n':
                    replacement = '\n';
                    break;
                case 'r':
                    replacement = '\r';
                    break;
                case 't':
                    replacement = '\t';
                    break;
                }
                out.deleteCharAt( i);
                out.setCharAt( i, replacement);
            }
        }

        return out.toString();
    }
} // class TemplateExporter
