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
            public void start(HTML.Tag t, MutableAttributeSet a) {
                // Force the creation of an implied paragraph if needed.
                // An annotation element must always be enclosed by an explicit or
                // implied paragraph.
                addContent( EMPTY_CHAR_ARRAY, pos, 0, true);

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
                registerTag( AnnotationTags.ANNOTATION, new AnnotationAction());
            registerTag( AnnotationTags.READING, new HTMLDocument.HTMLReader.CharacterAction());
            registerTag( AnnotationTags.KANJI, new HTMLDocument.HTMLReader.CharacterAction());
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
                int i = 0; // index in the list of returned annotations

                List result = parser.parse( data);

                while (i < result.size()) {
                    // get start of next annotation
                    Parser.TextAnnotation ta = (Parser.TextAnnotation) result.get( i);
                    to = ta.getStart() - 1;

                    int talen = ta.getLength();
                    // if this is a conjugated verb, cut off the non-kanji part
                    if (ta instanceof AbstractAnnotation) {
                        AbstractAnnotation tr = (AbstractAnnotation) ta;
                        if (tr.getConjugation() != null) {
                            talen -= tr.getConjugation().getConjugatedForm().length();
                        }
                    }

                    // handle text between annotations
                    if (to >= from) {
                        char[] d = new char[to-from+1];
                        System.arraycopy( data, from, d, 0, d.length);
                        super.handleText( d, pos);
                        pos += d.length;
                    }

                    // handle annotation
                    // get all annotations for this position in the text
                    List annotations = new ArrayList( 5);
                    annotations.add( ta);
                    i++;
                    while (i<result.size() && ((Parser.TextAnnotation) result.get( i))
                           .getStart()<ta.getStart()+talen) {
                        annotations.add( result.get( i));
                        i++;
                    }

                    char[] word = new char[talen];
                    System.arraycopy( data, ta.getStart(), word, 0, word.length);
                    
                    String dictionaryWord = null; // null means same as word
                    // the difference between dictionaryReading and reading is that
                    // the conjugated part will be cut off the reading.
                    String dictionaryReading = null; // null means same as reading
                    String reading = null;
                    String translation = null;
                    if (ta instanceof Reading) {
                        // get reading from annotations
                        Reading r = (Reading) ta;
                        reading = r.getReading();
                        Conjugation c = r.getConjugation();
                        if (c != null) {
                            reading = reading.substring( 0, reading.length() - c
                                                         .getDictionaryForm().length());
                            dictionaryWord = r.getWord();
                            dictionaryReading = r.getReading();
                        }

                        // try to find matching translation
                        for ( int j=1; j<annotations.size(); j++) {
                            Parser.TextAnnotation annotation = (Parser.TextAnnotation) annotations.get( j);
                            if (annotation instanceof Translation) {
                                DictionaryEntry de = ((Translation) annotation).getDictionaryEntry();
                                if (de.getReading()!=null && de.getReading().startsWith( reading)) {
                                    // the comparison is startsWith and not equals because it might
                                    // be an inflected verb.
                                    translation = de.getTranslations()[0];
                                    // the dictionary entry may contain an inflected form the
                                    // document reading didn't
                                    if (((Translation) annotation).getConjugation() != null) {
                                        dictionaryWord = de.getWord();
                                        dictionaryReading = de.getReading();
                                    }
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
                        reading = de.getReading();
                        // if this is a inflected verb, cut off the conjugation part
                        if (tr.getConjugation() != null) {
                            reading = reading.substring( 0, reading.length() - tr.getConjugation()
                                                   .getDictionaryForm().length());

                            dictionaryWord = de.getWord();
                            dictionaryReading = de.getReading();
                        }
                        translation = de.getTranslations()[0];
                    }
                    if (dictionaryReading!=null && dictionaryReading.equals( dictionaryWord))
                        dictionaryReading = null;
                    if (reading == null) 
                        // there has to be at least 1 character for the layout to work
                        reading = " ";
                    else if (reading.equals( new String( word)))
                        // can happpen if hiragana is annotated
                        reading = " ";
                    if (translation == null)
                        translation = " ";

                    // insert the appropriate elements
                    SimpleAttributeSet a = new SimpleAttributeSet();
                    SimpleAttributeSet ana = new SimpleAttributeSet();
                    ana.addAttribute( TEXT_ANNOTATION, annotations);
                    if (dictionaryWord != null)
                        ana.addAttribute( DICTIONARY_WORD, dictionaryWord);
                    if (dictionaryReading != null)
                        ana.addAttribute( DICTIONARY_READING, dictionaryReading);

                    handleStartTag( AnnotationTags.ANNOTATION, ana, pos);

                    handleStartTag( AnnotationTags.READING, a, pos);
                    super.handleText( reading.toCharArray(), pos);
                    pos += reading.length();
                    handleEndTag( AnnotationTags.READING, pos);

                    handleStartTag( AnnotationTags.KANJI, a, pos);
                    super.handleText( word, pos);
                    pos += word.length;
                    handleEndTag( AnnotationTags.KANJI, pos);

                    handleStartTag( AnnotationTags.TRANSLATION, a, pos);
                    super.handleText( translation.toCharArray(), pos);
                    pos += translation.length();
                    handleEndTag( AnnotationTags.TRANSLATION, pos);

                    handleEndTag( AnnotationTags.ANNOTATION, pos);

                    from = ta.getStart() + talen;
                    if (ta instanceof Reading && from<data.length &&
                        parser instanceof ReadingAnnotationParser &&
                        data[from]==((ReadingAnnotationParser) parser).getReadingStart()) {
                        // skip reading annotation in original document
                        from += ((Reading) ta).getReading().length() + 2;
                    }
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
        public void handleError( String errorMsg, int pos) {
            System.err.println( errorMsg + " at " + pos);
        }

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
        this( htmlparser, null, false);
    }

    /**
     * Generates a new JGlossDocument which will use the text parser to add annotations to
     * a loaded document.
     *
     * @param htmlparser The HTML parser to use.
     * @param parser The parser to use to find annotations for some text.
     */
    public JGlossDocument( HTMLEditorKit.Parser htmlparser, Parser parser) {
        this( htmlparser, parser, true);
    }

    /**
     * Generates a new JGlossDocument which will use the text parser to add annotations to
     * a loaded document.
     *
     * @param htmlparser The HTML parser to use.
     * @param parser The parser to use to find annotations for some text.
     * @param addAnnotations <CODE>true</CODE> if annotations should be added when loading a
     *                       document.
     */
    public JGlossDocument( HTMLEditorKit.Parser htmlparser, Parser parser, boolean addAnnotations) {
        setParser( htmlparser);
        this.parser = parser;
        this.addAnnotations = addAnnotations;

        jglossVersion = -1;
        fileFormatMinorVersion = -1;
        fileFormatMajorVersion = -1;
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
