/*
 * Copyright (C) 2001,2002 Michael Koch (tensberg@gmx.net)
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

import jgloss.ui.xml.JGlossDocument;
import jgloss.dictionary.*;
import jgloss.parser.*;
import jgloss.util.StringTools;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

import javax.swing.text.*;
import javax.swing.text.html.*;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.dom.DOMSource;

/**
 * Extends the <CODE>HTMLDocument</CODE> class to deal with documents which contain
 * annotations. It in particular contains methods to manipulate the attributes of the
 * annotation elements and controls the document loading process.
 *
 * @author Michael Koch
 */
public class JGlossHTMLDoc extends HTMLDocument {
    /**
     * Name of the attribute of an annotation element which contains the dictionary form
     * of the annotated word. If the attribute is not set, the dictionary form is per
     * definition equal to the annotated word.
     */
    public static final String DICTIONARY_WORD = "base";
    /**
     * Name of the attribute of an annotation element which contains the dictionary form
     * of the reading of the annotated word. If the attribute is not set, the dictionary form is per
     * definition equal to the reading of annotated word.
     */
    public static final String DICTIONARY_READING = "baseReading";

    private JGlossDocument baseDoc;

    private JGlossReader reader;

    private Transformer jglossDocTransformer;
    private SAXResult docTransformTarget;

    /**
     * Manages the property change listeners, which are notified of document title changes.
     */
    private PropertyChangeSupport listeners = new PropertyChangeSupport( this);

    private static TransformerFactory transformerFactory;
    private static Templates jglossToHTMLTemplate;

    static {
        try {
            transformerFactory = TransformerFactory.newInstance();
            jglossToHTMLTemplate = transformerFactory.newTemplates
                ( new StreamSource( JGlossHTMLDoc.class.getResourceAsStream
                                    ( "/data/JGlossToHTML.xslt")));
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace();
        }
    }

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
         *        be called from the AnnotationActiendon.
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
            super.handleText( data, pos);
        }

        /**
         * Handles a closing tag. 
         *
         * @param t The tag which is closed.
         * @param pos Position in the document.
         */
        public void handleEndTag( HTML.Tag t, int pos) {
            if (t.equals( HTML.Tag.P)) {
                // make sure end of lines are handled correctly in layout by inserting a '\n'
                // This is needed when an annotation at the end of a paragraph is removed.
                super.addContent( NEWLINE, 0, 1, false);
                pos++;
            }
            
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
            return pos;
        }
    }

    public JGlossHTMLDoc( HTMLEditorKit.Parser _htmlparser) {
        setParser( _htmlparser);
    }

    public void setJGlossDocument( JGlossDocument _baseDoc) {
        baseDoc = _baseDoc;

        try {
            jglossDocTransformer = jglossToHTMLTemplate.newTransformer();
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace();
        }
        docTransformTarget = new SAXResult( new SAXToHTMLParserAdapter( getReader( 0),
                                                                        JGlossEditorKit.getDTD()));
        try {
            jglossDocTransformer.transform( new DOMSource( baseDoc.getDOMDocument()),
                                            docTransformTarget);
        } catch (TransformerException ex) {
            ex.printStackTrace();
        }

        baseDoc.linkWithHTMLDoc( this);
    }

    public JGlossDocument getJGlossDocument() { return baseDoc; }

    /**
     * Returns a reader for a document at the given position. This will return a new instance
     * of {@link JGlossReader JGlossReader}.
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
     * Sets an attribute to a new value. This method is used because writeLock must be called
     * and that method is not public.
     *
     * @param attr The attribute set to change.
     * @param key Key of the attribute.
     * @param value New value of the attribute; or <CODE>null</CODE> to remove the attribute.
     * @param fireChanged <CODE>true</CODE> if a document changed event should be triggered.
     */
    public void setAttribute( MutableAttributeSet attr, Object key, Object value, boolean fireChanged) {
        writeLock();
        if (value != null)
            attr.addAttribute( key, value);
        else
            attr.removeAttribute( key);
        writeUnlock();

        if (fireChanged)
            fireChangedUpdate( new DefaultDocumentEvent
                ( 0, 0, javax.swing.event.DocumentEvent.EventType.CHANGE));
    }

    /**
     * Switches strict HTML parsing on or off.
     *
     * @see JGlossEditorKit.JGlossParser#setStrict(boolean)
     */
    public void setStrictParsing( boolean strict) {
        ((JGlossEditorKit.JGlossParserWrapper) getParser()).setStrict( strict);
    }

    public List getAnnotationElements() {
        List out = new ArrayList( 10);
        readLock();
        try {
            findElements( getDefaultRootElement(), AnnotationTags.ANNOTATION, out);
        } finally {
            readUnlock();
        }

        return out;
    }

    private void findElements( Element elem, HTML.Tag tag, List elemList) {
        if (elem.getName().equals( tag.toString())) {
            elemList.add( elem);
            // don't recurse over children, it is assumed that the elements can't have children
            // of the same type
        }
        else {
            // recurse over children
            for ( int i=0; i<elem.getElementCount(); i++)
                findElements( elem.getElement( i), tag, elemList);
        }
    }

    /**
     * Adds a listener which will be notified of changes to the document title.
     */
    public void addPropertyChangeListener( PropertyChangeListener listener) {
        listeners.addPropertyChangeListener( listener);
    }

    /**
     * Removes a previously added document change listener.
     */
    public void removePropertyChangeListener( PropertyChangeListener listener) {
        listeners.removePropertyChangeListener( listener);
    }

    /**
     * Sets the title of the document. This modifies the <CODE>DocumentTitle</CODE> property
     * of the Document object. This also fires a property change event.
     */
    public void setTitle( String title) {
        String oldTitle = getTitle();
        if (oldTitle!=null && !oldTitle.equals( title) || oldTitle==null && title!=null) {
            putProperty( Document.TitleProperty, title);
            listeners.firePropertyChange( Document.TitleProperty, oldTitle, title);
        }
    }

    /**
     * Returns the title of the document. This is the <CODE>DocumentTitle</CODE> property.
     */
    public String getTitle() {
        return (String) getProperty( Document.TitleProperty);
    }
} // class JGlossDocument
