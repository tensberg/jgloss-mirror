/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.doc;

import jgloss.*;
import jgloss.dictionary.*;

import java.util.*;

import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * Extends the <CODE>HTMLDocument</CODE> class to deal with documents which contain
 * annotations. It in particular contains methods to manipulate the attributes of the
 * annotation elements and controls the document loading process.
 *
 * @author Michael Koch
 */
public class JGlossDocument extends HTMLDocument {
    /**
     * Name of the attribute of an annotation element which has a list of
     * annotations as value.
     */
    public static final String TEXT_ANNOTATION = "text_annotation";
    /**
     * Name of the attribute of an annotation element which contains
     * the text annotation from the annotations list this element currently links to.
     * This attribute is now deprecated and kept only for compatibility with
     * documents created by JGloss 0.9.1 or earlier.
     */
    public static final String LINKED_ANNOTATION = "linked_annotation";
    /**
     * Name of the attribute of an annotation element which contains the dictionary form
     * of the annotated word. If the attribute is not set, the dictionary form is per
     * definition equal to the annotated word.
     */
    public static final String DICTIONARY_WORD = "dict_word";
    /**
     * Name of the attribute of an annotation element which contains the dictionary form
     * of the reading of the annotated word. If the attribute is not set, the dictionary form is per
     * definition equal to the reading of annotated word.
     */
    public static final String DICTIONARY_READING = "dict_reading";
    /**
     * Key of the attribute of an annotation element which controls if the
     * annotation is hidden.
     */
    public static final String HIDDEN_ATTRIBUTE = "annotation_hidden";
    /**
     * Value of the <CODE>HIDDEN_ATTRIBUTE</CODE> if the annotation should be hidden.
     */
    public static final String HIDDEN_ATTRIBUTE_TRUE = "true";

    /**
     * Parser which is used to generate the annotations for text when a document is
     * imported.
     */
    private Parser parser;
    /**
     * Filter for fetching reading annotations from the document during import.
     */
    private ReadingAnnotationFilter readingFilter;
    /**
     * The instance of the JGlossReader used by this document.
     */
    private JGlossReader reader;
    /**
     * Flag if the reader should add annotations when constructing a document.
     */
    private boolean addAnnotations;
    /**
     * Version of the creator of the parsed document, as seen in the meta generator tag.
     * <code>-1</code> if unknown.
     */
    private int jglossVersion;
    /**
     * Major version of the jgloss file format, as seen in the meta generator tag.
     * <code>-1</code> if unknown.
     */
    private int fileFormatMajorVersion;
    /**
     * Minor version of the jgloss file format, as seen in the meta generator tag.
     * <code>-1</code> if unknown.
     */
    private int fileFormatMinorVersion;

    /**
     * Stores a newline character as a char array. Used by the <CODE>JGlossReader</CODE>.
     */
    private final static char[] NEWLINE = new char[] { '\n' };
    /**
     * Single space character as char array.
     */
    private final static char[] SINGLE_SPACE = new char[] { ' ' };
    
    /**
     * Extends the <CODE>HTMLDocument.HTMLReader</CODE> to support the JGloss extensions
     * to HTML and the addition of annotations while loading a document.
     *
     * @author Michael Koch
     */
    private class JGlossReader extends HTMLDocument.HTMLReader {
        /**
         * The position in the document which is currently loaded.
         */
        private int pos;
        /**
         * Flag if the loading process was aborted. This can happen if the loading thread
         * is interrupted and the parser throws a <CODE>Parser.ParsingInterruptedException</CODE>.
         */
        private boolean aborted = false;
        /**
         * Flag if the last closed tag was a <CODE>AnnotationTags.ANNOTATION</CODE> tag.
         */
        private boolean endAnnotation = false;

        private char[] EMPTY_CHAR_ARRAY = new char[0];
        
        /**
         * Handle Annotation tags in the document. Annotation elements need special
         * treatment because they are "inline blocks", something which is not possible in
         * HTML/CSS. The action ensures that the correct element structure is created
         * but explicit or implied paragraphs are not interrupted.
         */
        private class AnnotationAction extends HTMLDocument.HTMLReader.TagAction {
            private boolean forceParagraph;
            
            public AnnotationAction( boolean forceParagraph) {
                this.forceParagraph = forceParagraph;
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                if (forceParagraph) {
                    // Force the creation of an implied paragraph if needed.
                    // An annotation element must always be enclosed by an explicit or
                    // implied paragraph.
                    addContent( EMPTY_CHAR_ARRAY, pos, 0, true);
                }

                a.addAttribute( StyleConstants.NameAttribute, t);
                ElementSpec es = new ElementSpec
                    ( a.copyAttributes(), ElementSpec.StartTagType);
                parseBuffer.addElement( es);
            }
            
            public void end(HTML.Tag t) {
                ElementSpec es = new ElementSpec( null, ElementSpec.EndTagType);
                parseBuffer.addElement( es);
            }
        }
        
        public JGlossReader( int pos) {
            super( pos);
            this.pos = pos;
            addCustomTags( false);
        }

        /**
         * Insert just the fragment of the HTML document delimited by <CODE>insertTag</CODE>.
         */
        public JGlossReader( int pos, int popDepth, int pushDepth, HTML.Tag insertTag) {
            super( pos, popDepth, pushDepth, insertTag);
            this.pos = pos;
            addCustomTags( true);
        }
        
        /**
         * Adds the annotation tags from {@link AnnotationTags AnnotationTags} to the set of tags
         * this <CODE>HTMLReader</CODE> knows how to handle.
         *
         * @param blockCompatible If this is <CODE>true</CODE>, a BlockAction will be used for
         *        annotation elements instead of an AnnotationAction. This is needed because
         *        the canInsertTag, which is needed to correctly handle the case where only a
         *        fragment of the HTML document should be inserted, is private and thus cannot
         *        be called from the AnnotationAction.
         */
        private void addCustomTags( boolean blockCompatible) {
            if (blockCompatible)
                registerTag( AnnotationTags.ANNOTATION, new BlockAction());
            else
                registerTag( AnnotationTags.ANNOTATION, new AnnotationAction( true));
            registerTag( AnnotationTags.WORD, new AnnotationAction( false));
            registerTag( AnnotationTags.READING_BASETEXT, new AnnotationAction( false));
            registerTag( AnnotationTags.READING, new HTMLDocument.HTMLReader.CharacterAction());
            registerTag( AnnotationTags.BASETEXT, new HTMLDocument.HTMLReader.CharacterAction());
            registerTag( AnnotationTags.TRANSLATION,
                         new HTMLDocument.HTMLReader.CharacterAction());
        }

        /**
         * Handles the addition of text to the document. If either {@link #addAnnotations addAnnotations}
         * is <CODE>false</CODE>, the <CODE>JGlossDocument</CODE> has no {@link #parser parser} or the
         * loading was aborted, this will forward the call to the superclass. Otherwise the text will
         * be put through the parser and for the annotations which the parser returns the appropriate
         * annotation element will be inserted in the text.
         *
         * @param data Text data to insert in the document.
         * @param pos Position in the document.
         */
        public void handleText( char[] data, int pos) {
            this.pos = pos;

            if (!addAnnotations || parser==null || aborted) {
                super.handleText( data, pos);
                return;
            }
            
            try {
                int from = 0; // from offset of the data currently worked on
                int to = 0; // to offset of the data currently worked on
                
                List readings = new ArrayList( 10);
                int readingsindex = 0;
                if (readingFilter != null)
                    data = readingFilter.filter( data, readings);

                List result = parser.parse( data);
                int resultindex = 0; // index in the list of returned annotations
                
                while (readingsindex<readings.size() || resultindex<result.size()) {
                    // Get the next annotation. This can be either an annotation generated
                    // by the parser or a Reading from the reading annotation filter, or both if
                    // they overlap. If a reading
                    // is overlapped by a parser annotation (lies between ta.start and 
                    // ta.start + ta.lenght, a new reading is generated spanning the whole parser
                    // annotation.
                    Parser.TextAnnotation ta;
                    List annotations = new ArrayList( 5);
                    if (resultindex < result.size()) {
                        ta = (Parser.TextAnnotation) result.get( resultindex);
                        resultindex++;
                        if (readingsindex < readings.size()) {
                            Reading r = (Reading) readings.get( readingsindex);
                            readingsindex++;
                            if (r.getStart() < ta.getStart()) { 
                                // reading comes first, skip parser annotation for this iteration
                                ta = r;
                                resultindex--;
                            }
                            else if (r.getStart()==ta.getStart() && r.getLength()>=ta.getLength()) {
                                // handle reading and text annotation at same time
                                ta = r;
                                resultindex--;
                                // current parser annotation will be re-added to annotation list
                                // when all annotations for this position in the text are added
                            }
                            else if (r.getStart() >= ta.getStart()+ta.getLength()) {
                                // reading starts after current annotation, skip reading in this
                                // iteration
                                readingsindex--;
                            }
                            else if (r.getStart()+r.getLength() > ta.getStart()+ta.getLength()) {
                                // Reading and parser annotation overlap. Give preference to reading
                                // and skip annotation.
                                ta = r;
                                // skip all additional parser annotations which start before reading
                                while (resultindex<result.size() && 
                                       ((Parser.TextAnnotation) result.get( resultindex))
                                       .getStart()<ta.getStart()) {
                                    resultindex++;
                                }
                            }
                            else {
                                // reading lies in parser annotation, grow reading annoation to
                                // area of parser annotation.
                                StringBuffer readingbuf = new StringBuffer( ta.getLength() +
                                                                            r.getReading().length());
                                int beforeindex = ta.getStart();
                                boolean moreReadings = false;
                                do {
                                    // add text before reading start
                                    readingbuf.append( data, beforeindex, r.getStart()-beforeindex);
                                    // add reading
                                    readingbuf.append( r.getReading());
                                    // Parser annotation may overlap several reading annotations
                                    // (for example for compound words). Check if the next Reading
                                    // is overlapped too.
                                    moreReadings = false;
                                    beforeindex = r.getStart() + r.getLength();
                                    if (readingsindex < readings.size()) {
                                        Reading r2 = (Reading) readings.get( readingsindex);
                                        if (r2.getStart() < ta.getStart() + ta.getLength()) {
                                            r = r2;
                                            readingsindex++;
                                            moreReadings = true;
                                        }
                                    }
                                } while (moreReadings);
                                // add text after reading end
                                readingbuf.append( data, beforeindex, ta.getStart() +
                                                   ta.getLength() - (r.getStart()+r.getLength()));

                                // create new compound reading
                                final String word = new String( data, ta.getStart(), ta.getLength());
                                final String reading = readingbuf.toString();
                                final jgloss.dictionary.Dictionary dictionary = r.getWordReadingPair()
                                    .getDictionary();
                                r = new Reading( ta.getStart(), ta.getLength(), new WordReadingPair() {
                                        public String getWord() { return word; }
                                        public String getReading() { return reading; }
                                        public jgloss.dictionary.Dictionary getDictionary() { 
                                            return dictionary;
                                        }
                                    });

                                // now r.start==ta.start && r.length==ta.length
                                // add new reading to annotation list
                                ta = r; // ta must refer to first annotation
                                resultindex--;
                                // current parser annotation will be re-added to annotation list
                                // when all annotations for this position in the text are added
                            }
                        }
                    }
                    else {
                        // no more parser annotations, just add readings
                        ta = (Parser.TextAnnotation) readings.get( readingsindex);
                        readingsindex++;
                    }

                    annotations.add( ta);
                    to = ta.getStart() - 1;
                    int talen = ta.getLength();
                    
                    // get all annotations for this position in the text
                    // the tests above guarantee that there can be no more readings at this position
                    while (resultindex<result.size() && ((Parser.TextAnnotation) result.get( resultindex))
                           .getStart()<ta.getStart()+talen) {
                        annotations.add( result.get( resultindex));
                        resultindex++;
                    }
                    
                    // handle text between annotations
                    if (to >= from) {
                        char[] d = new char[to-from+1];
                        System.arraycopy( data, from, d, 0, d.length);
                        super.handleText( d, pos);
                        pos += d.length;
                    }
                    
                    // handle annotations
                    String word = new String( data, ta.getStart(), talen);
                    String reading = null; // reading (in dictionary form)
                    String dictionaryWord = null; // null means same as word
                    String dictionaryReading = null; // null means same as reading
                    String translation = null;
                    if (ta instanceof Reading && !(ta instanceof Translation)) {
                        // get reading from annotations
                        Reading r = (Reading) ta;
                        reading = r.getReading(); // dictionary form of reading, real reading for
                                                  // inflected verbs will be derived further down
                        if (reading == null)
                            // this happens if the annotated word is hiragana and an inflected form.
                            // In this case, the Reading contains the base reading as word, and
                            // r.getReading() returns null
                            reading = r.getWord();
                        if (!r.getWord().equals( word)) {
                            dictionaryWord = r.getWord();
                            dictionaryReading = r.getReading();
                        }

                        // try to find matching translation
                        String hiraganaReading = StringTools.toHiragana( reading);
                        for ( int j=1; j<annotations.size(); j++) {
                            Parser.TextAnnotation annotation = (Parser.TextAnnotation) annotations.get( j);
                            if (annotation instanceof Translation) {
                                DictionaryEntry de = ((Translation) annotation).getDictionaryEntry();
                                String dr = de.getReading();
                                if (dr == null)
                                    dr = de.getWord();
                                if (dr != null &&
                                    StringTools.toHiragana( dr).startsWith( hiraganaReading)) {
                                    // the comparison is startsWith and not equals because it might
                                    // be an inflected verb.
                                    translation = de.getTranslations()[0];
                                    break;
                                }
                            }
                        }
                        // if nothing found, don't trust any of the translations
                    }
                    else if (annotations.size() >= 1) {
                        // pick the first reading and translation
                        Translation tr = (Translation) ta;
                        DictionaryEntry de = tr.getDictionaryEntry();
                        reading = de.getReading(); // dictionary form of reading, real reading for
                                                   // inflected verbs will be derived further down
                        translation = de.getTranslations()[0];
                        if (!de.getWord().equals( word)) {
                            dictionaryWord = de.getWord();
                            dictionaryReading = de.getReading();
                        }
                    }

                    if (reading!=null && reading.equals( StringTools.toHiragana( word)))
                        // can happen if katakana or hiragana is annotated
                        reading = null;
                    if (translation == null)
                        translation = " "; // translation must always be set to a non-empty string

                    // insert the appropriate elements
                    SimpleAttributeSet a = new SimpleAttributeSet();
                    SimpleAttributeSet ana = new SimpleAttributeSet();
                    ana.addAttribute( TEXT_ANNOTATION, annotations);
                    if (dictionaryWord != null)
                        ana.addAttribute( DICTIONARY_WORD, dictionaryWord);
                    if (dictionaryReading != null)
                        ana.addAttribute( DICTIONARY_READING, dictionaryReading);

                    handleStartTag( AnnotationTags.ANNOTATION, ana, pos);

                    handleStartTag( AnnotationTags.WORD, a, pos);

                    // The "reading" variable is set to the dictionary form of the
                    // reading of the word, even if word itself is a hiragana string.
                    // Hiragana words should not be annotated
                    if (reading==null || !StringTools.containsKanji( word)) {
                        // There has to always be at least one reading.
                        // Insert word with a single reading/base pair with an empty reading.
                        handleStartTag( AnnotationTags.READING_BASETEXT, a, pos);
                        handleStartTag( AnnotationTags.READING, a, pos);
                        super.handleText( SINGLE_SPACE, pos);
                        pos++;
                        handleEndTag( AnnotationTags.READING, pos);
                        handleStartTag( AnnotationTags.BASETEXT, a, pos);
                        super.handleText( word.toCharArray(), pos);
                        pos += word.length();
                        handleEndTag( AnnotationTags.BASETEXT, pos);
                        handleEndTag( AnnotationTags.READING_BASETEXT, pos);
                    }
                    else {
                        // The "reading" variable is set to the dictionary form of the
                        // reading of the word. The inflected form of the reading is
                        // implicitly derived by the splitWordReading method.
                        String base = dictionaryWord;
                        if (base == null)
                            base = word;
                        String[][] wr = StringTools.splitWordReading( word, base, reading);
                        if (wr.length==1 && wr[0].length==1) {
                            // Single word without reading. Since there has to be at least one
                            // reading, use an empty string as reading.
                            wr[0] = new String[] { wr[0][0], " " };
                        }
                        for ( int j=0; j<wr.length; j++) {
                            if (wr[j].length == 1) {
                                // word without reading, don't generate annotation
                                handleStartTag( AnnotationTags.BASETEXT, a, pos);
                                super.handleText( wr[j][0].toCharArray(), pos);
                                pos += wr[j][0].length();
                                handleEndTag( AnnotationTags.BASETEXT, pos);
                            }
                            else {
                                // word with reading, create reading annotation
                                handleStartTag( AnnotationTags.READING_BASETEXT, a, pos);
                                handleStartTag( AnnotationTags.READING, a, pos);
                                super.handleText( wr[j][1].toCharArray(), pos);
                                pos += wr[j][1].length();
                                handleEndTag( AnnotationTags.READING, pos);
                                handleStartTag( AnnotationTags.BASETEXT, a, pos);
                                super.handleText( wr[j][0].toCharArray(), pos);
                                pos += wr[j][0].length();
                                handleEndTag( AnnotationTags.BASETEXT, pos);
                                handleEndTag( AnnotationTags.READING_BASETEXT, pos);
                            }
                        }
                    }

                    handleEndTag( AnnotationTags.WORD, pos);

                    handleStartTag( AnnotationTags.TRANSLATION, a, pos);
                    super.handleText( translation.toCharArray(), pos);
                    pos += translation.length();
                    handleEndTag( AnnotationTags.TRANSLATION, pos);

                    handleEndTag( AnnotationTags.ANNOTATION, pos);
                    
                    from = ta.getStart() + talen;
                }
                
                // add remaining text
                if (from < data.length) {
                    char[] d = new char[data.length-from];
                    System.arraycopy( data, from, d, 0, d.length);
                    super.handleText( d, pos);
                }
            } catch (SearchException ex) {
                // if the parser detected that the thread was interrupted,
                // abort the whole import process
                if (ex instanceof ParsingInterruptedException) {
                    aborted = true;
                }
                else
                    ex.printStackTrace();
                
                super.handleText( data, pos);
            }
        }

        /**
         * Handles an opening tag. This adds special treatments for JGloss-specific
         * attributes.
         *
         * @param t The tag which is opened
         * @param a Attributes of the tag.
         * @param pos Position in the document.
         */
        public void handleStartTag( HTML.Tag t, MutableAttributeSet a, int pos) {
            // In a JGloss document the annotation attribute will be encoded as a
            // string. Decode it here.
            if (a.isDefined( TEXT_ANNOTATION) && 
                a.getAttribute( TEXT_ANNOTATION) instanceof String) {
                decodeAnnotation( a);
            }

            if (fileFormatMajorVersion == 1) {
                // handle JGloss 0.9.2 import
                // annotation format was <anno><reading>...</><kanji>...</></translation>...</></>
                // a word element with a single READING_BASETEXT element is created, which contains
                // the reading element, and the kanji element as BASETEXT
                if (t.toString().equals( "anno")) {
                    MutableAttributeSet empty = new SimpleAttributeSet();
                    super.handleStartTag( t, a, pos);
                    super.handleStartTag( AnnotationTags.WORD, empty, pos);
                    super.handleStartTag( AnnotationTags.READING_BASETEXT, empty, pos);
                    return;
                }
                else if (t.toString().equals( "kanji")) {
                    super.handleStartTag( AnnotationTags.BASETEXT, a, pos);
                    return;
                }
            }
            
            super.handleStartTag( t, a, pos);
        }

        /**
         * Handles a closing tag. 
         *
         * @param t The tag which is closed.
         * @param pos Position in the document.
         */
        public void handleEndTag(HTML.Tag t, int pos) {
            if (t.equals( HTML.Tag.P)) {
                // make sure end of lines are handled correctly in layout by inserting a '\n'
                // This is needed when an annotation at the end of a paragraph is removed.
                super.addContent( NEWLINE, 0, 1, false);
                pos++;
            }
            else if (t.equals( AnnotationTags.ANNOTATION))
                endAnnotation = true;

            if (fileFormatMajorVersion == 1) {
                // handle JGloss 0.9.2 import
                // annotation format was <anno><reading>...</><kanji>...</></translation>...</></>
                if (t.toString().equals( "kanji")) {
                    super.handleEndTag( AnnotationTags.BASETEXT, pos);
                    super.handleEndTag( AnnotationTags.READING_BASETEXT, pos);
                    super.handleEndTag( AnnotationTags.WORD, pos);
                    return;
                }
            }
            
            super.handleEndTag( t, pos);
        }

        /**
         * Handle the meta "generator" tag, which sets the file version.
         */
        public void handleSimpleTag( HTML.Tag t, MutableAttributeSet a, int pos) {
            if (t.equals( HTML.Tag.META) &&
                a.containsAttribute( HTML.Attribute.NAME, "generator")) {
                Integer[] version = JGlossWriter.parseFileVersionString
                    ( (String) a.getAttribute( HTML.Attribute.CONTENT));
                if (version != null) {
                    jglossVersion = version[0].intValue();
                    fileFormatMajorVersion = version[1].intValue();
                    fileFormatMinorVersion = version[2].intValue();
                }
            }

            super.handleSimpleTag( t, a, pos);
        }

        /**
         * Handles an error in paring the document. This will print the error to
         * <CODE>System.err</CODE>.
         *
         * @param errorMsg The error message.
         * @param pos Position at which the error occured.
         */
        /*public void handleError( String errorMsg, int pos) {
            System.err.println( errorMsg + " at " + pos);
            }*/

        /**
         * Returns the current position in the loading document.
         *
         * @return The current position.
         */
        public int getParsePosition() {
            return (parser != null) ? pos + parser.getParsePosition() : pos;
        }
    }

    /**
     * Generates a new JGlossDocument which will never add annotations to a loaded document.
     *
     * @param htmlparser The HTML parser to use.
     */
    public JGlossDocument( HTMLEditorKit.Parser htmlparser) {
        this( htmlparser, null, null, false);
    }

    /**
     * Generates a new JGlossDocument which will use the text parser to add annotations to
     * a loaded document.
     *
     * @param htmlparser The HTML parser to use.
     * @param filter Filter for fetching the reading annotations from a parsed document.
     * @param parser The parser to use to find annotations for some text.
     */
    public JGlossDocument( HTMLEditorKit.Parser htmlparser, Parser parser, ReadingAnnotationFilter filter) {
        this( htmlparser, parser, filter, true);
    }

    /**
     * Generates a new JGlossDocument which will use the text parser to add annotations to
     * a loaded document.
     *
     * @param htmlparser The HTML parser to use.
     * @param parser The parser to use to find annotations for some text.
     * @param filter Filter for fetching the reading annotations from a parsed document.
     * @param addAnnotations <CODE>true</CODE> if annotations should be added when loading a
     *                       document.
     */
    public JGlossDocument( HTMLEditorKit.Parser htmlparser, Parser parser, ReadingAnnotationFilter filter,
                           boolean addAnnotations) {
        setParser( htmlparser);
        this.parser = parser;
        this.readingFilter = filter;
        this.addAnnotations = addAnnotations;

        if (addAnnotations) {
            // create new JGloss file
            jglossVersion = JGlossWriter.JGLOSS_VERSION;
            fileFormatMajorVersion = JGlossWriter.FILE_FORMAT_MAJOR_VERSION;
            fileFormatMinorVersion = JGlossWriter.FILE_FORMAT_MINOR_VERSION;
        }
        else {
            // read version from existing JGloss file
            jglossVersion = -1;
            fileFormatMajorVersion = -1;
            fileFormatMinorVersion = -1;
        }
    }

    /**
     * Returns a reader for a document at the given position. This will return a new instance
     * of {@link JGlossDocument.JGlossReader JGlossReader}.
     *
     * @param pos Position in the document.
     * @return A reader for a document.
     */
    public HTMLEditorKit.ParserCallback getReader( int pos) {
        reader = new JGlossReader( pos);
        return reader;
    }

    /**
     * Returns a reader for a document at the given position. This will return a new instance
     * of {@link JGlossDocument.JGlossReader JGlossReader}.
     *
     * @param pos Position in the document.
     * @param popDepth The number of ElementSpec.EndTagTypes to generate before inserting
     * @param pushDepth The number of ElementSpec.StartTagTypes with a direction of 
     *                  ElementSpec.JoinNextDirection that should be generated before
     *                  inserting, but after the end tags have been generated.
     * @param insertTag The first tag to start inserting into document.
     * @return The new reader.
     */
    public HTMLEditorKit.ParserCallback getReader( int pos, int popDepth, int pushDepth,
                                                   HTML.Tag insertTag) {
        return new JGlossReader( pos, popDepth, pushDepth, insertTag);
    }

    /**
     * Returns the parser used to find annotation for text.
     *
     * @return The parser used to find annotation for text.
     */
    public Parser getDictionaryParser() {
        return parser;
    }

    /**
     * Returns the position in the document the current reader is at. If no reader is currently
     * active, this will return 0.
     *
     * @return The position in the document the current reader is at.
     */
    public int getParsePosition() {
        if (reader != null)
            return reader.getParsePosition();
        else
            return 0;
    }

    /**
     * Sets if annotations should be added when reading a document.
     *
     * @param addAnnotations <CODE>true</CODE> if annotations should be added.
     */
    public void setAddAnnotations( boolean addAnnotations) {
        this.addAnnotations = addAnnotations;
    }

    /**
     * Sets the visibility status of an annotation element. If an annotation element is
     * hidden, neither the reading nor the translation is shown. If you want to change the
     * visibility of all annotation elements, use {@link JGlossEditorKit#showReading(boolean)
     * showReading} and {@link JGlossEditorKit#showTranslation(boolean) showTranslation}
     * in class <CODE>JGlossEditorKit</CODE>.
     *
     * @param element The annotation element the visibility of which should be changed.
     * @param hidden <CODE>true</CODE> if the annotation should be hidden.
     */
    public void setHidden( Element element, boolean hidden) {
        writeLock();
        if (hidden) {
            ((MutableAttributeSet) element.getAttributes()).addAttribute
                ( HIDDEN_ATTRIBUTE, HIDDEN_ATTRIBUTE_TRUE);
        }
        else {
            ((MutableAttributeSet) element.getAttributes()).removeAttribute
                ( HIDDEN_ATTRIBUTE);
        }
        writeUnlock();
    }

    /**
     * Returns the visibility status of an annotation element.
     *
     * @param element The annotation element.
     * @return <CODE>true</CODE> if the element is hidden.
     */
    public boolean isHidden( Element element) {
        return HIDDEN_ATTRIBUTE_TRUE.equals
            (element.getAttributes().getAttribute( HIDDEN_ATTRIBUTE));
    }

    /**
     * Encode the annotation list as string and add them to the attribute set
     * as {@link #TEXT_ANNOTATION TEXT_ANNOTATION}.
     * This method is needed because a write lock is needed for the
     * attribute set to be changed and the method has protected access.
     *
     * @param attr The attribute set to which the attribute should be added.
     * @see TextAnnotationCodec#encode(List)
     */
    public void encodeAnnotation( MutableAttributeSet attr) {
        writeLock();
        List annotations = (List) attr.getAttribute( TEXT_ANNOTATION);
        attr.addAttribute( TEXT_ANNOTATION,
                           TextAnnotationCodec.encode( annotations));
        writeUnlock();
    }

    /**
     * Decodes the {@link #TEXT_ANNOTATION TEXT_ANNOTATION} attribute from a string.
     * This method is needed because a write lock is needed for the
     * attribute set to be changed and the method has protected access.
     *
     * @param attr The attribute set to which the attribute should be added.
     */
    public void decodeAnnotation( MutableAttributeSet attr) {
        writeLock();
        List annotations = TextAnnotationCodec.decodeList( (String) attr.getAttribute( TEXT_ANNOTATION));
        attr.addAttribute( TEXT_ANNOTATION, annotations);

        // import linked annotation from JGloss 0.9.1 documents
        if (attr.isDefined( LINKED_ANNOTATION)) {
            try {
                int i = Integer.parseInt( (String) attr.getAttribute( LINKED_ANNOTATION));
                if (i>=0 && i<annotations.size()) {
                    Object o = annotations.get( i);
                    if (o instanceof AbstractAnnotation) {
                        String word = ((AbstractAnnotation) o).getWord();
                        if (word != null)
                            attr.addAttribute( DICTIONARY_WORD, word);
                        String reading = ((AbstractAnnotation) o).getReading();
                        if (reading!=null && !reading.equals( word))
                            attr.addAttribute( DICTIONARY_READING, reading);
                    }
                }
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
            attr.removeAttribute( LINKED_ANNOTATION);

            // Set file version. LINKED_ANNOTATION attributes were only used in JGloss 0.9 and
            // 0.9.1, which had no file versioning mechanism.
            jglossVersion = 91;
            fileFormatMajorVersion = 1;
            fileFormatMinorVersion = 0;
        }

        writeUnlock();
    }

    /**
     * Sets an attribute to a new value. This method is used because writeLock must be called
     * and this method is not public.
     *
     * @param attr The attribute set to change.
     * @param key Key of the attribute.
     * @param value New value of the attribute; or <CODE>null</CODE> to remove the attribute.
     */
    public void setAttribute( MutableAttributeSet attr, Object key, Object value) {
        writeLock();
        if (value != null)
            attr.addAttribute( key, value);
        else
            attr.removeAttribute( key);
        writeUnlock();

        fireChangedUpdate( new DefaultDocumentEvent
            ( 0, 0, javax.swing.event.DocumentEvent.EventType.CHANGE));
    }

    /**
     * Returns the version of JGloss which created the parsed document. This is initialized from
     * the meta "generator" tag in the document. If there was no generator tag in the parsed
     * document, <code>-1</code> will be returned.
     */
    public int getJGlossVersion() {
        return jglossVersion;
    }

    /**
     * Returns the major version number of the JGloss file format of the parsed document. 
     * This is initialized from
     * the meta "generator" tag in the document. If there was no generator tag in the parsed
     * document, <code>-1</code> will be returned.
     */
    public int getFileFormatMajorVersion() {
        return fileFormatMajorVersion;
    }

    /**
     * Returns the minor version number of the JGloss file format of the parsed document. 
     * This is initialized from
     * the meta "generator" tag in the document. If there was no generator tag in the parsed
     * document, <code>-1</code> will be returned.
     */
    public int getFileFormatMinorVersion() {
        return fileFormatMinorVersion;
    }
} // class JGlossDocument
