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
 *
 * @author Michael Koch
 */
public abstract class TemplateExporter {
    public static final String ENCODING_HEADER = "% encoding:";

    public static final char VARIABLE_START_MARKER = '%';
    public static final char VARIABLE_END_MARKER = '%';
    public static final String DOCUMENT_FILENAME = "%document-filename%";
    public static final String GENERATION_TIME = "%generation-time%";
    public static final String DOCUMENT_TITLE = "%document-title%";
    public static final String LONGEST_WORD = "%longest-word%";
    public static final String LONGEST_READING = "%longest-reading%";

    public static final String BEGIN_TEXT = "%begin text";
    public static final String BEGIN_ANNOTATION_LIST = "%begin annotation-list";
    public static final String END = "%end";
    public static final String WORD = "%word%";
    public static final String DICTIONARY_WORD = "%dictionary-word%";
    public static final String READING = "%reading%";
    public static final String DICTIONARY_READING = "%dictionary-reading%";
    public static final String TRANSLATION = "%translation%";
    public static final String PARAGRAPH_NUMBER = "%paragraph-number%";

    public static final String RUBY_PATTERN = "%ruby";
    public static final String TRANSLATION_PATTERN = "%translation";
    public static final String PARAGRAPH_PATTERN = "%paragraph";

    protected static class Ruby {
        private String[] data;

        public Ruby( String word, String ruby) {
            data = new String[] { word, ruby };
        }

        public String getWord() { return data[0]; }
        public String getReading() { return data[1]; }
        public String[] asArray() { return data; }
    } // class Ruby

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
     * Text with ruby is stored as {@link Ruby Ruby} instance. An annotation is stored
     * as a {@link Translation Translation} instance. For a paragraph start, an <code>Integer</code>  
     * with the paragraph number is stored.
     */
    protected List parsedText = null;

    protected String longestDictionaryWord = "";
    protected String longestDictionaryReading = "";

    public TemplateExporter() {}

    protected Map prepareExport( JGlossDocument doc, String documentName, AnnotationModel model,
                                 boolean writeHidden) {
        parseText( doc, model, writeHidden);
        Map variables = new HashMap( 37);
        variables.put( DOCUMENT_FILENAME, documentName);
        variables.put( GENERATION_TIME, DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.SHORT)
                       .format( new Date( System.currentTimeMillis())));
        variables.put( DOCUMENT_TITLE, doc.getTitle());
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

    protected void export( Reader template, Writer out, Map variables) throws IOException {
        LineNumberReader templateLines = new LineNumberReader( template);
        String line;
        while ((line = templateLines.readLine()) != null) {
            if (line.startsWith( BEGIN_TEXT))
                handleInsertText( templateLines, out, variables);
            else if (line.startsWith( BEGIN_ANNOTATION_LIST))
                handleInsertAnnotationList( templateLines, out, variables);
            else
                handleTemplateLine( line, out, variables);
        }
    }

    protected void handleTemplateLine( String line, Writer out, Map variables) throws IOException {
        out.write( substituteVariables( line, variables));
        out.write( '\n');
    }

    protected void handleInsertText( LineNumberReader template, Writer out,
                                     Map variables) throws IOException {
        int startLineNumber = template.getLineNumber();

        String line;
        String rubyPattern = null;
        String translationPattern = null;
        try {
            while (!(line = template.readLine()).startsWith( END)) {
                String pattern = null;
                pattern = matchPatternDefinition( line, template.getLineNumber(),
                                                  RUBY_PATTERN, rubyPattern);
                if (pattern != null)
                    rubyPattern = pattern;
                else {
                    pattern = matchPatternDefinition
                        ( line, template.getLineNumber(), TRANSLATION_PATTERN, translationPattern);
                    if (pattern != null)
                        translationPattern = pattern;
                }
                if (pattern == null)
                    // one of the patterns must have matched,
                    // all other line contents are an error
                    throw new IOException
                        ( JGloss.messages.getString
                          ( "export.template.unknownline",
                            new Object[] { new Integer( template.getLineNumber()) }));
            }
        } catch (NullPointerException ex) {
            // template ended before %end marker was found
            throw new IOException( JGloss.messages.getString
                                   ( "export.template.unmatchedbegin",
                                     new Object[] { BEGIN_TEXT, new Integer( startLineNumber) }));
        }

        // build message formats for ruby and translation pattern
        MessageFormat rubyFormat = null;
        if (rubyPattern != null)
            rubyFormat = new MessageFormat( substituteVariables
                                            ( quoteMessageFormat( rubyPattern), variables));
        MessageFormat translationFormat = null;
        if (translationPattern != null)
            translationFormat = new MessageFormat( substituteVariables
                                                   ( quoteMessageFormat( translationPattern), variables));

        // write the output text
        for ( Iterator i=parsedText.iterator(); i.hasNext(); ) {
            Object next = i.next();
            if (next instanceof String) {
                // normal text
                out.write( (String) next);
            }
            else if (next instanceof Ruby) {
                Ruby ruby = (Ruby) next;
                if (rubyFormat != null)
                    out.write( rubyFormat.format( ruby.asArray(), new StringBuffer(), null).toString());
                else
                    out.write( ruby.getWord());
            }
            else if (next instanceof Translation) {
                Translation translation = (Translation) next;
                if (translationFormat != null)
                    out.write( translationFormat.format( translation.asArray(), new StringBuffer(), null)
                               .toString());
            }
        }
        out.write( '\n');
    }

    protected void handleInsertAnnotationList( LineNumberReader template, Writer out,
                                               Map variables) throws IOException {
        int startLineNumber = template.getLineNumber();

        String line;
        String translationPattern = null;
        String paragraphPattern = null;
        try {
            while (!(line = template.readLine()).startsWith( END)) {
                String pattern;
                pattern = matchPatternDefinition
                    ( line, template.getLineNumber(), TRANSLATION_PATTERN, translationPattern);
                if (pattern != null)
                    translationPattern = pattern;
                else {
                    pattern = matchPatternDefinition
                        ( line, template.getLineNumber(), PARAGRAPH_PATTERN, paragraphPattern);
                    if (pattern != null)
                        paragraphPattern = pattern;
                }
                if (pattern == null)
                    // one of the patterns must have matched,
                    // all other line contents are an error
                    throw new IOException
                        ( JGloss.messages.getString
                          ( "export.template.unknownline",
                            new Object[] { new Integer( template.getLineNumber()) }));
            }
        } catch (NullPointerException ex) {
            // template ended before %end marker was found
            throw new IOException( JGloss.messages.getString
                                   ( "export.template.unmatchedbegin",
                                     new Object[] { BEGIN_TEXT, new Integer( startLineNumber) }));
        }

        // build message formats for ruby and translation pattern
        MessageFormat translationFormat = null;
        if (translationPattern != null)
            translationFormat = new MessageFormat( substituteVariables
                                                   ( quoteMessageFormat( translationPattern), variables));
        MessageFormat paragraphFormat = null;
        Integer[] paragraphNumber = null;
        if (paragraphPattern != null) {
            paragraphFormat = new MessageFormat( substituteVariables
                                                 ( quoteMessageFormat( paragraphPattern), variables));
            paragraphNumber = new Integer[1];
        }

        // write the output text
        for ( Iterator i=parsedText.iterator(); i.hasNext(); ) {
            Object next = i.next();
            if (next instanceof Translation) {
                Translation translation = (Translation) next;
                if (translationFormat != null)
                    out.write( translationFormat.format( translation.asArray(), new StringBuffer(), null)
                               .toString());
            }
            else if (next instanceof Integer) {
                if (paragraphPattern != null) {
                    paragraphNumber[0] = (Integer) next;
                    out.write( paragraphFormat.format( paragraphNumber, new StringBuffer(), null)
                               .toString());
                }
            }
        }
        out.write( '\n');
    }

    /**
     * Create a list of text segments. The text is segmented in normal text, word/ruby pairs
     * and translations.
     *
     * @param doc The document to write.
     * @param model Annotation model which contains the annotations of the document.
     * @param writeHidden <CODE>true</CODE> if annotations marked as hidden should be written.
     * @see #parsedText
     */
    protected void parseText( JGlossDocument doc, AnnotationModel model, boolean writeHidden) {
        parsedText = new ArrayList( model.getAnnotationCount()*3 + 30);
        String text = null;
        try {
            text = doc.getText( 0, doc.getLength());
        } catch (BadLocationException ex) {}
        
        int paragraph = 1; // current paragraph
        int prevend = 0; // end offset of the previous annotation
        for ( int i=0; i<model.getAnnotationCount(); i++) {
            AnnotationNode annotation = model.getAnnotationNode( i);
            Element ae = annotation.getAnnotationElement();

            // add text between annotations
            if (prevend < ae.getStartOffset())
                parsedText.add( escape( text.substring( prevend, ae.getStartOffset())));
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
                        parsedText.add( new Ruby( word, reading));
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
            parsedText.add( escape( text.substring( prevend, text.length())));

    }

    protected String matchPatternDefinition( String line, int lineNumber, String name, 
                                             String definedPattern) throws IOException {
        if (line.toLowerCase().startsWith( name)) {
            if (definedPattern != null)
                throw new IOException
                    ( JGloss.messages.getString
                      ( "export.template.patternredefined",
                        new Object[] { new Integer( lineNumber) }));
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
     * @param substitutions Mapping from variable names to substitutions.
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
    protected String escape( String in) {
        return in;
    }

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

    protected static String quoteMessageFormat( String format) {
        System.err.println( format);
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

        System.err.println( out.toString());
        return out.toString();
    }

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
