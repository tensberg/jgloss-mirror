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
 * Extends the HTML document class to handle JGloss document annotations. 
 * Each JGloss html document is linked to a {@link jgloss.ui.xml.JGlossDocument JGloss XML document}.
 * The JGloss HTML document is generated from the JGloss XML document via a XSLT style sheet.
 * If the HTML document is changed, the XML document is marked as invalid and is re-generated
 * from the HTML document via class {@link HTMLToSAXParserAdapter HTMLToSAXParserAdapter}.
 * The HTML document is used whenever the document content or structure needs to be changed.
 * The XML document is used when an external well-defined representation is needed, such as
 * when saving or exporting the document.
 *
 * @author Michael Koch
 */
public class JGlossHTMLDoc extends HTMLDocument {
    public interface Attributes {
        /**
         * Name of the attribute of an annotation element which contains the dictionary form
         * of the annotated word. If the attribute is not set, the dictionary form is per
         * definition equal to the annotated word.
         */
        String BASE = "base";
        /**
         * Name of the attribute of an annotation element which contains the dictionary form
         * of the reading of the annotated word. If the attribute is not set, the dictionary form is per
         * definition equal to the reading of annotated word.
         */
        String BASE_READING = "basere";
        /**
         * Name of the attribute of an annotation element which contains the grammatical type.
         */
        String TYPE = "type";
        /**
         * Name of the attribute of an rb element which contains the reading found in a
         * document during import.
         */
        String DOCREADING = "docre";
    }

    public static final String EMPTY_ELEMENT_PLACEHOLDER = " ";

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

    /**
     * Set the JGloss XML document to which this HTML document corresponds. The HTML document
     * will be generated from the XML document when the method is called. Changes to the
     * HTML document will be propagated to the XML document.
     */
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
     * of the Document object. Calling this method fires a property change event.
     */
    public void setTitle( String title) {
        String oldTitle = getTitle();
        if (oldTitle!=null && !oldTitle.equals( title) || oldTitle==null && title!=null) {
            putProperty( Document.TitleProperty, title);
            listeners.firePropertyChange( Document.TitleProperty, oldTitle, title);
        }
    }

    /**
     * Returns the title of the document. It is stored in the <CODE>DocumentTitle</CODE> property.
     */
    public String getTitle() {
        return (String) getProperty( Document.TitleProperty);
    }

    /**
     * Currently not implemented, don't use.
     */
    public String getUnannotatedText( int start, int end) {
        return new UnannotatedTextFetcher().getText(start, end).toString();
    }

    public Element getParagraphElement(int offset) {
        Element para = getDefaultRootElement();
        while (!para.isLeaf() && 
               !para.getAttributes().getAttribute(StyleConstants.NameAttribute)
               .equals(AnnotationTags.ANNOTATION)) {
            para = para.getElement(para.getElementIndex(offset));
        }

        // when the loop terminates, para is a child of the paragraph element
        return para.getParentElement();
    }

    /**
     * Returns the annotation element spanning the given element offset. If the offset is not
     * in an annotation, <code>null</code> is returned.
     */
    public Element getAnnotationElement(int offset) {
        Element anno = getDefaultRootElement();
        while (!anno.isLeaf() && 
               !anno.getAttributes().getAttribute(StyleConstants.NameAttribute)
               .equals(AnnotationTags.ANNOTATION)) {
            anno = anno.getElement(anno.getElementIndex(offset));
        }

        if (anno.isLeaf())
            return null;
        else
            return anno;
    }

    /**
     * Adds a new annotation for a part of the document. Since it is not possible to nest
     * annotation elements, all annotations lying in that interval will be removed first. The
     * annotation also cannot span paragraphs, so the interval will be shortened as needed.
     *
     * @param start Start offset of the interval to annotate.
     * @param end End offset of the interval to annotate.
     * @param editorKit Editor kit needed to insert the newly generated annotation element.
     */
    public void addAnnotation(int start, int end, JGlossEditorKit editorKit) {
        try {
            // find smallest enclosing element of start and don't allow annotation to
            // cross it.
            Element paragraph = getParagraphElement( start);
            end = Math.min( end, paragraph.getEndOffset()-1);

            // The start and end offsets will move around while we change the document.
            // So wrap them in position objects, which adapt to changes.
            Position startp = createPosition( start);
            Position endp = createPosition( end);
            
            // remove any annotations in the area
            removeAnnotations(start, end);

            // The interval now only contains document plain text.
            start = startp.getOffset();
            end = endp.getOffset();
            String text = getText( start, end-start);

            // work around Document construction quirks
            boolean paragraphSpaceInserted = false;
            if (start == paragraph.getStartOffset()) {
                insertAfterStart( paragraph, "&nbsp;");
                start++;
                startp = createPosition( start);
                end = endp.getOffset();
                paragraphSpaceInserted = true;
            }

            // If we insert new text directly after an annotation, the new annotation will 
            // be made a child of the first one, which is not what we want. So insert an
            // additional character if needed.
            Element sae = getAnnotationElement(startp.getOffset()-1);
            if (sae != null) {
                insertAfterEnd( sae, "&nbsp;");
            }
            Element eae = getAnnotationElement(endp.getOffset()+1);
            if (eae != null) {
                insertBeforeStart( eae, "&nbsp;");
                // the nbsp is inserted before endp, move endp to the left of the nbsp
                endp = createPosition(endp.getOffset()-1);
            }

            // remove the old text
            remove( startp.getOffset(), endp.getOffset()-startp.getOffset());

            // construct the new annotation and insert it

            // Split word in base/readings. Add a reading/base pair for every kanji substring of the
            // word and a base element for every other substring. If there is no kanji substring, add
            // a reading for the whole string since there has to be at least one reading.
            StringBuffer wordhtml = new StringBuffer(128);
            int baseStart = 0;
            boolean hasReading = false;
            boolean needsReading = needsReading( text, 0);
            for ( int baseEnd=1; baseEnd<=text.length(); baseEnd++) {
                if (needsReading) {
                    if (baseEnd==text.length() || !needsReading(text, baseEnd)) {
                        hasReading = true;

                        appendTag(wordhtml, AnnotationTags.READING_BASETEXT, false);

                        appendTag(wordhtml, AnnotationTags.READING, false);
                        wordhtml.append(' ');
                        appendTag(wordhtml, AnnotationTags.READING, true);

                        appendTag(wordhtml, AnnotationTags.BASETEXT, false);
                        wordhtml.append(text.substring( baseStart, baseEnd));
                        appendTag(wordhtml, AnnotationTags.BASETEXT, true);

                        appendTag(wordhtml, AnnotationTags.READING_BASETEXT, true);
                        needsReading = false;
                        baseStart = baseEnd;
                    }
                }
                else if (baseEnd==text.length() || needsReading(text, baseEnd)) {
                    appendTag(wordhtml, AnnotationTags.BASETEXT, false);
                    wordhtml.append(text.substring( baseStart, baseEnd));
                    appendTag(wordhtml, AnnotationTags.BASETEXT, true);
                    needsReading = true;
                    baseStart = baseEnd;
                }
            }

            StringBuffer html = new StringBuffer(128);
            html.append("<html><body><p>");
            appendTag(html, AnnotationTags.ANNOTATION, false);
            appendTag(html, AnnotationTags.WORD, false);

            if (hasReading) {
                html.append(wordhtml);
            }
            else {
                // must have at least one reading, create it
                appendTag(html, AnnotationTags.READING_BASETEXT, false);

                appendTag(html, AnnotationTags.READING, false);
                html.append(' ');
                appendTag(html, AnnotationTags.READING, true);

                html.append(wordhtml);

                appendTag(html, AnnotationTags.READING_BASETEXT, true);
            }

            appendTag(html, AnnotationTags.WORD, true);
            appendTag(html, AnnotationTags.TRANSLATION, false);
            html.append(' ');
            appendTag(html, AnnotationTags.TRANSLATION, true);
            appendTag(html, AnnotationTags.ANNOTATION, true);

            // The insertion will create a new annotation element and trigger a document changed
            // event. The AnnotationListSynchronizer will react to this by creating a new
            // annotation node.
            // Unfortunately the HTMLDocument has no method for inserting HTML text at an
            // arbitrary location. So we will have to use an editor kit.
            editorKit.insertHTML( this, startp.getOffset(), html.toString(),
                                  0, 0, AnnotationTags.ANNOTATION);

            // remove the '\n\n' which the HTMLEditorKit insists on inserting
            remove( endp.getOffset()-2, 2);

            // remove the additional characters which were inserted above to work around
            // document construction quirks
            if (eae != null)
                remove( eae.getStartOffset()-1, 1);
            if (sae != null)
                remove( sae.getEndOffset(), 1);
            if (paragraphSpaceInserted)
                remove( paragraph.getStartOffset(), 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Test if a japanese character needs a reading annotation.
     */
    private static boolean needsReading( String text, int pos) {
        char c = text.charAt( pos);
        return StringTools.isKanji( c) ||
            // handle special case with infix katakana 'ke', which is read as 'ka' or 'ga'
            // when used as counter or in place names, as in ikkagetsu or Sakuragaoka
            (c=='\u30f6' || c=='\u30b1') && 
            pos>0 && StringTools.isKanji( text.charAt( pos-1)) && 
            pos+1<text.length() && StringTools.isKanji( text.charAt( pos+1));
    }

    /**
     * Adds an opening or closing HTML tag to a string buffer.
     */
    private static void appendTag(StringBuffer buf, AnnotationTags tag, boolean endTag) {
        buf.append('<');
        if (endTag)
            buf.append('/');
        buf.append(tag.getId());
        buf.append('>');
    }

    /**
     * Remove all annotations which intersect the given region of the document.
     */
    public void removeAnnotations(int start, int end) {
        new AnnotationRemover().removeAnnotations(start, end);
    }

    /**
     * Remove an annotation element. This will replace the annotation element and the text it
     * spans with just the unannotated plain text.
     */
    public void removeAnnotationElement(Element annotation) {
        // get unannotated text
        Element word = annotation.getElement(0);
        Segment textSegment = new Segment();
        StringBuffer unannotatedText = new StringBuffer(word.getEndOffset()-word.getStartOffset());
        for (int i=0; i<word.getElementCount(); i++) {
            Element basetext = word.getElement(i);
            if (basetext.getName().equals(AnnotationTags.READING_BASETEXT.getId()))
                basetext = basetext.getElement(1);
            try {
                getText(basetext.getStartOffset(),
                        basetext.getEndOffset()-basetext.getStartOffset(),
                        textSegment);
            } catch (BadLocationException ex) { ex.printStackTrace(); }
            unannotatedText.append(textSegment.array, textSegment.offset, textSegment.count);
        }

        // remove annotation element
        try {
            setOuterHTML(annotation, unannotatedText.toString());
        } catch (Exception ex) { ex.printStackTrace(); }
        // remove the newline which the stupid HTMLDocument.insertHTML insists on adding
        try {
            remove(annotation.getEndOffset()-1, 1);
        } catch (BadLocationException ex) { ex.printStackTrace(); }
    }

    /**
     * Recursively process the elements spanning a region of the document tree.
     */
    private abstract class TreeWalker {
        protected Position start;
        protected Position end;
        protected boolean writeLock;
        
        /**
         * Initialize the tree walker.
         *
         * @param _writeLock If <code>true</code>, the tree walker will aquire a write lock on the
         *        HTML document prior to recursing over the tree, otherwise a read lock will be used.
         */
        protected TreeWalker(boolean _writeLock) {
            writeLock = _writeLock;
        }

        /**
         * Starts the walk over the tree. The start and end offsets specify the region of elements
         * covered by the algorithm. This method will first aquire a read or write lock on the
         * HTML document, call {@link #walk(Element) walk} with the default root element and afterwards
         * release the lock held on the document.
         */
        protected void startWalk(int _start, int _end) {
            try {
                start = JGlossHTMLDoc.this.createPosition(_start);
                end = JGlossHTMLDoc.this.createPosition(_end);
            } catch (BadLocationException ex) { ex.printStackTrace(); }

            if (writeLock)
                JGlossHTMLDoc.this.writeLock();
            else
                JGlossHTMLDoc.this.readLock();

            try {
                walk(JGlossHTMLDoc.this.getDefaultRootElement());
            } finally {
                if (writeLock)
                    JGlossHTMLDoc.this.writeUnlock();
                else
                    JGlossHTMLDoc.this.readUnlock();
            }
        }

        protected void walk(Element elem) {
            if (elem.getStartOffset() >= end.getOffset() || elem.getEndOffset() <= start.getOffset())
                return;

            boolean recurse = processElement(elem);
            if (recurse)
                recurse(elem);
        }

        /**
         * Iterates forward over all children and calls {@link #walk(Element) walk} for every child.
         * This method is called by {@link #walk(Element) walk} if {@link #processElement(Element)
         * processElement} returns <code>true</code>.
         */
        protected void recurse(Element elem) {
            for ( int i=0; i<elem.getElementCount(); i++)
                walk(elem.getElement( i));
        }

        /**
         * Process the element of the tree in some way. This method will only be called for an element
         * if its region (the area between its start and end offset) intersects with the region
         * specified by the {@link #start start} and {@link #end end} offsets of the walker object.
         * If the element has children, the return value of the method determines if the algorithm
         * should recurse over the children.
         *
         * @return <code>true</code> if the tree walker should recurse over the children of the element.
         */
        protected abstract boolean processElement(Element elem);
    } // class TreeWalker

    /**
     * Remove all annotation elements intersecting a span of text in the document.
     */
    private class AnnotationRemover extends TreeWalker {
        AnnotationRemover() {
            super(true);
        }

        public void removeAnnotations(int _start, int _end) {
            startWalk(_start, _end);
        }

        protected boolean processElement(Element elem) {
            if (elem.getName().equals(AnnotationTags.ANNOTATION.getId())) {
                removeAnnotationElement(elem);
                return false;
            }
            else {
                return true;
            }
        }

        /**
         * Recurse over children backwards because children may be removed while the algorithm
         * runs, changing the forward iteration order.
         */
        protected void recurse(Element elem) {
            for ( int i=elem.getElementCount()-1; i>=0; i--)
                walk(elem.getElement( i));            
        }
    } // class AnnotationRemover

    /**
     * Fetch a span of text from the document, leaving out text with is part of an annotation.
     */
    private class UnannotatedTextFetcher extends TreeWalker {
        private StringBuffer text;
        private Segment textSegment;

        UnannotatedTextFetcher() {
            super(false);
            textSegment = new Segment();
            textSegment.setPartialReturn(true);
        }

        /**
         * Returns the unannotated text between the given start and end offsets.
         */
        public StringBuffer getText( int _start, int _end) {
            text = new StringBuffer(_end - _start);
            startWalk(_start, _end);
            return text;
        }

        protected boolean processElement(Element elem) {
            if (elem.getName().equals(HTML.Tag.CONTENT.toString())) {
                // element spanning some text, add if the text is not part of an annotation
                AttributeSet as = elem.getAttributes();
                if (!as.isDefined( AnnotationTags.READING) &&
                    !as.isDefined( AnnotationTags.TRANSLATION)) {
                    // copy the part of the element text which intersects with the requested
                    // text span to the string buffer
                    int offset = Math.max(start.getOffset(),elem.getStartOffset());
                    int length = Math.min(end.getOffset(),elem.getEndOffset())-offset;
                    try {
                        // partial return is activated for the text segment, so we have
                        // to iterate until all segment fragments are copied
                        while (length > 0) {
                            JGlossHTMLDoc.this.getText(offset,length,textSegment);
                            text.append(textSegment.array,textSegment.offset,textSegment.count);
                            offset += textSegment.count;
                            length -= textSegment.count;
                        }
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                }
                return false;
            }
            else {
                return true; // recurse over children
            }
        }
    } // class UnannotatedTextFetcher
} // class JGlossDocument
