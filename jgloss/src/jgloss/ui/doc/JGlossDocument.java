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
     */
    public static final String LINKED_ANNOTATION = "linked_annotation";
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

        public JGlossReader( int pos) {
            super( pos);
            this.pos = pos;
            addCustomTags();
        }

        public JGlossReader( int pos, int popDepth, int pushDepth, HTML.Tag insertTag) {
            super( pos, popDepth, pushDepth, insertTag);
            this.pos = pos;
            addCustomTags();
        }
        
        /**
         * Adds the annotation tags from {@link AnnotationTags AnnotationTags} to the set of tags
         * this <CODE>HTMLReader</CODE> knows how to handle.
         */
        private void addCustomTags() {
            registerTag( AnnotationTags.ANNOTATION, new HTMLDocument.HTMLReader.BlockAction());
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
                    Parser.TextAnnotation linked = ta;
                    to = ta.getStart() - 1;

                    int talen = ta.getLength();
                    // if this is a conjugated verb, cut off the non-kanji part
                    if (ta instanceof Translation) {
                        Translation tr = (Translation) ta;
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
                    
                    String reading = null;
                    String translation = null;
                    if (ta instanceof Reading) {
                        // get reading from annotations
                        reading = ((Reading) ta).getReading();
                        // try to find matching translation
                        for ( int j=1; j<annotations.size(); j++) {
                            Parser.TextAnnotation annotation = (Parser.TextAnnotation) annotations.get( j);
                            if (annotation instanceof Translation) {
                                DictionaryEntry de = ((Translation) annotation).getDictionaryEntry();
                                if (de.getReading()!=null && de.getReading().startsWith( reading)) {
                                    // the comparison is startsWith and not equals because it might
                                    // be an inflected verb.
                                    linked = annotation;
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
                        reading = de.getReading();
                        // if this is a inflected verb, cut off the conjugation part
                        if (tr.getConjugation() != null)
                            reading = reading.substring( 0, reading.length() - tr.getConjugation()
                                                   .getDictionaryForm().length());
                        translation = de.getTranslations()[0];
                    }
                    if (reading == null) // there has to be at least 1 character for the layout to work
                        reading = " ";
                    if (translation == null)
                        translation = " ";

                    // insert the appropriate elements
                    SimpleAttributeSet a = new SimpleAttributeSet();
                    SimpleAttributeSet ana = new SimpleAttributeSet();
                    ana.addAttribute( TEXT_ANNOTATION, annotations);
                    ana.addAttribute( LINKED_ANNOTATION, linked);

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
                    if (ta instanceof Reading) {
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
                if (ex instanceof Parser.ParsingInterruptedException) {
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
                // See addContent()
                super.addContent( NEWLINE, 0, 1, false);
                pos++;
            }
            else if (t.equals( AnnotationTags.ANNOTATION))
                endAnnotation = true;
            super.handleEndTag( t, pos);
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

        /**
         * Adds some content. Overridden to handle a special case when a
         * <CODE>AnnotationTags.ANNOTATION</CODE> tag is closed.
         *
         * @param data The content to add.
         * @param offs Offset in the data.
         * @param length Length of the data to add.
         * @param gi Flags if an embedded &lt;P&gt; should be added when necessary.
         */
        protected void addContent( char[] data, int offs, int length, boolean gi) {
            // For a correct calculation of the cursor position, it is necessary to insert
            // a newline after a block element. Unfortunately, the superclass inserts this
            // newline as the last character of the Annotation-Element. The following
            // statement prevents this. To prevent linebreaks in normal text, a \n
            // is only inserted at a closing </p> tag. This is done in the above
            // handleEndTag().
            if (endAnnotation && data.length==1 && data[0]=='\n' && offs==0 && length==1 && !gi) {
                endAnnotation = false;
                return;
            }

            super.addContent( data, offs, length, gi);
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
        int linkedIndex = -1;
        if (attr.isDefined( LINKED_ANNOTATION))
            linkedIndex = annotations.indexOf( attr.getAttribute( LINKED_ANNOTATION));

        attr.addAttribute( TEXT_ANNOTATION,
                           TextAnnotationCodec.encode( annotations));
        if (linkedIndex != -1)
            attr.addAttribute( LINKED_ANNOTATION,
                               String.valueOf( linkedIndex));
        else // no linked annotation or linked annotation not found
            attr.removeAttribute( LINKED_ANNOTATION);
            
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
        if (attr.isDefined( LINKED_ANNOTATION)) {
            try {
                attr.addAttribute( LINKED_ANNOTATION, annotations.get
                                   ( Integer.parseInt( (String) attr.getAttribute( LINKED_ANNOTATION))));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        writeUnlock();
    }

    /**
     * Sets an attribute to a new value.
     *
     * @param attr The attribute set to change.
     * @param key Key of the attribute.
     * @param value New value of the attribute.
     */
    public void setAttribute( MutableAttributeSet attr, Object key, Object value) {
        writeLock();
        attr.addAttribute( key, value);
        writeUnlock();
    }

    /**
     * Sets the linked annotation of an annotation element. The linked annotation is one
     * of the text annotations from the list of annotations for this element.
     * This method is needed because a write lock is needed for the
     * attribute set to be changed and the method has protected access.
     *
     * @param annotation The element for which to set the attribute.
     * @param linkedAnnotation The annotation the element should be linked to.
     */
    public void setLinkedAnnotation( Element annotation, Parser.TextAnnotation linkedAnnotation) {
        writeLock();
        MutableAttributeSet attr = (MutableAttributeSet) annotation.getAttributes();

        if (linkedAnnotation != null)
            attr.addAttribute( LINKED_ANNOTATION, linkedAnnotation);
        else
            attr.removeAttribute( LINKED_ANNOTATION);
        writeUnlock();
    }
} // class JGlossDocument
