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

import java.io.*;
import java.awt.*;
import java.util.*;

import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.DocumentParser;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.text.html.parser.AttributeList;
import javax.swing.text.html.parser.ContentModel;
import javax.swing.text.html.parser.TagElement;

/**
 * The <CODE>JGlossEditorKit</CODE> is an extension of the <CODE>HTMLEditorKit</CODE> 
 * with several additions to manage the generation and display of JGloss documents.
 * It also has to work around the shortcomings in the HTML document API.<BR>
 * It contains the views for the annotation
 * elements and its subelements as subclasses, since they share some view attribute state with the
 * <CODE>JGlossEditorKit</CODE> instance.
 *
 * @author Michael Koch
 */
public class JGlossEditorKit extends HTMLEditorKit {
    /**
     * The factory used to create views for document elements.
     */
    private JGlossFactory viewFactory = new JGlossFactory();
    /**
     * The parser used to annotate text.
     */
    private jgloss.dictionary.Parser parser;

    /**
     * Flag if the view should be in compact mode.
     */
    private boolean compactView;
    /**
     * Flag if reading annotations should be shown.
     */
    private boolean showReading;
    /**
     * Flag if translation annotations should be shown.
     */
    private boolean showTranslation;
    /**
     * Flag if annotations should be added when reading a document.
     */
    private boolean addAnnotations;

    /**
     * The DTD used when reading HTML documents. This DTD is customized to understand
     * the {@link AnnotationTags AnnotationTags}.
     */
    private static DTD dtd = null;

    /**
     * TagElements are used by the HTML parser to inquire properties of an element. This class
     * adds support for the annotation elements of JGloss.
     *
     * @author Michael Koch
     */
    private static class JGlossTagElement extends TagElement {
        /**
         * The tag this element wraps.
         */
        private HTML.Tag htmlTag;

        /**
         * Creates a new TagElement which wraps the given element. If the HTML tag represented
         * by this element is an instance of HTML.UnknownTag it will be replaced by the
         * equivalent unque annotation tag.
         */
        public JGlossTagElement( javax.swing.text.html.parser.Element e, boolean fictional) {
            super( e, fictional);

            // Get the real annotation tag. Unfortunately the creation of the HTML.UnknownTags
            // cannot be prevented, so we have to work around it.
            htmlTag = super.getHTMLTag();
            if (htmlTag instanceof HTML.UnknownTag)
                htmlTag = AnnotationTags.getAnnotationTagEqualTo( htmlTag);
        }

        public HTML.Tag getHTMLTag() {
            return htmlTag;
        }
        
        public boolean breaksFlow() {
            return htmlTag.breaksFlow();
        }

        public boolean isPreformatted() {
            return htmlTag.isPreformatted();
        }
    }

    /**
     * Instance of <CODE>HTMLEditorKit.Parser</CODE>, which will forward parse requests to a
     * {@link JGlossEditorKit.JGlossParser JGlossParser}. <CODE>JGlossParser</CODE> is derived from
     * <CODE>DocumentParser</CODE>, which prevents it from also being a 
     * <CODE>HTMLEditorKit.Parser</CODE>.
     *
     * @author Michael Koch
     */
    private class JGlossParserWrapper extends HTMLEditorKit.Parser {
        /**
         * The parser to which parse requests will be forwarded.
         */
        private JGlossParser parser;

        /**
         * Creates a new wrapper with an associated <CODE>JGlossParser</CODE>.
         */
        public JGlossParserWrapper() {
            parser = new JGlossParser();
        }

        /**
         * Parse a document. This forwards the request to the underlying parser.
         */
        public void parse( Reader r, HTMLEditorKit.ParserCallback cb,
                           boolean ignoreCharset) throws IOException {
            parser.parse( r, cb, ignoreCharset);
        }
    }

    /**
     * Parser for JGloss documents.
     *
     * @author Michael Koch
     */
    private class JGlossParser extends DocumentParser {
        /**
         * Constructs a new parser. This will use the DTD modified for JGloss tags
         * from {@link JGlossEditorKit#getDTD() getDTD}.
         *
         */
        public JGlossParser() {
            super( getDTD());
        }

        /**
         * Make sure the document charset is always ignored. The Parser has a bug where it
         * throws an exception when the charset of the reader is identical to that given in
         * a http-equiv charset header in the document instead of when they are different.
         * This parser will always ignore the charset. It will also clear the cache
         * of the jgloss.dictionary.Parser when parsing is finished.
         *
         * @param r Reader from which the document is read.
         * @param cb Callback to forward document construction requests to.
         * @param ignoreCharset <CODE>true</CODE> if charset declarations in the HTML document
         *        should be ignored. This parameter will be ignored.
         * @exception IOException
         */
        public void parse( Reader r, HTMLEditorKit.ParserCallback cb,
                           boolean ignoreCharset) throws IOException {
            long t = System.currentTimeMillis();
            super.parse( r, cb, true);
            if (parser != null) {
                t = System.currentTimeMillis() - t;
                if (parser.getLookups()>10 && t>500) {
                    // print some statistics 
                    System.err.println( "time: " + t/1000f);
                    System.err.println( "dictionary lookups: " + parser.getLookups());
                    System.err.println( "cache hits: " + parser.getCacheHits());
                    System.err.println( "cache gc: " + parser.getGarbageCollected());
                    parser.clearCache();
                }
            }
        }

        /**
         * Creates a {@link JGlossEditorKit.JGlossTagElement JGlossTagElement} which wraps the
         * given element. This is necessary because the parser superclass does not know how to
         * handle the custom annotation tags.
         *
         * @param e The element this should wrap.
         * @param fictional <CODE>true</CODE> if the element does not exist in the original document.
         * @return The new tag element.
         */
        protected TagElement makeTag( javax.swing.text.html.parser.Element e, boolean fictional) {
            return new JGlossTagElement( e, fictional);
        }
    }

    /**
     * Factory which creates views for elements in the JGloss document.
     *
     * @author Michael Koch
     */
    private class JGlossFactory extends HTMLEditorKit.HTMLFactory {
        /**
         * Creates a new factory.
         *
         */
        public JGlossFactory() {}

        /**
         * Creates a view which can render the element.
         *
         * @param elem The element to create the view for.
         * @return The newly created view.
         */
        public View create( Element elem) {
            Object name = elem.getAttributes().getAttribute( StyleConstants.NameAttribute);
            AttributeSet a = elem.getAttributes();
            if (name.equals( AnnotationTags.ANNOTATION))
                return new AnnotationView( elem);
            else if (a.isDefined( AnnotationTags.KANJI) || a.isDefined( AnnotationTags.KANJI.getId()))
                return new KanjiView( elem);
            else if (a.isDefined( AnnotationTags.TRANSLATION) || 
                     a.isDefined( AnnotationTags.TRANSLATION.getId()))
                return new ReadingTranslationView( elem, AnnotationTags.TRANSLATION);
            else if (a.isDefined( AnnotationTags.READING) || a.isDefined( AnnotationTags.READING.getId())) {
                return new ReadingTranslationView( elem, AnnotationTags.READING);
            }
            else {
                View v = super.create( elem);
                if (v instanceof InlineView)
                    // The inline views do not have the text aligned to work properly with the
                    // annotation view. Replace it with a vertically adjusted view.
                    return new VAdjustedView( elem);
                else
                    return v;
            }
        }
    }

    /**
     * A view which renders an annotation. The view is a block, but will not cause a
     * break in the flow.
     *
     * @author Michael Koch
     */
    public class AnnotationView extends BlockView {
        /**
         * Creates a new view for an annotation element.
         *
         * @param elem The element rendered by the view.
         */
        public AnnotationView( Element elem) {
            super( elem, View.Y_AXIS);
        }

        /**
         * Returns the minimum span of the view. Overridden to take into account the
         * state of <CODE>compactView</CODE>.
         */
        public float getMinimumSpan( int axis) {
            if (compactView && axis==View.X_AXIS) {
                // Only take enough space for the kanji subelement.
                for ( int i=0; i<getViewCount(); i++) {
                    if (getView( i) instanceof KanjiView) {
                        return getView( i).getMinimumSpan( axis);
                    }
                }
            }

            return super.getMinimumSpan( axis);
        }

        /**
         * Returns the preferred span of the view. Overridden to take into account the
         * state of <CODE>compactView</CODE>.
         */
        public float getPreferredSpan( int axis) {
            if (compactView && axis==View.X_AXIS) {
                // Only take enough space for the kanji subelement. The reading and annotation
                // will still draw even outside of the allocated space, but if two annotations
                // are close to eachother, the annotations can overlap.
                for ( int i=0; i<getViewCount(); i++) {
                    if (getView( i) instanceof KanjiView) {
                        return getView( i).getPreferredSpan( axis);
                    }
                }
            }

            return super.getPreferredSpan( axis);
        }

        /**
         * Returns the maximum span of the view. Overridden to prevent it to grow beyond
         * the preferred span.
         */
        public float getMaximumSpan( int axis) {
            // Prevent the view from filling the whole line by not allowing it to grow
            return getPreferredSpan( axis);
        }

        /**
         * Returns the state of the {@link JGlossDocument#HIDDEN_ATTRIBUTE hidden attribute}
         * of the annotation element rendered by this view.
         *
         * @return <CODE>true</CODE> if the hidden attribute is set
         */
        public boolean isAnnotationHidden() {
            if (JGlossDocument.HIDDEN_ATTRIBUTE_TRUE.equals
                (getElement().getAttributes()
                 .getAttribute( JGlossDocument.HIDDEN_ATTRIBUTE)))
                return true;

            return false;
        }

        /**
         * Returns the alignment of this view. Overridden because the alignment
         * has to be changed if the annotation is hidden.
         */
        public float getAlignment( int axis) {
            if ((!showReading || isAnnotationHidden()) && axis==View.Y_AXIS)
                return -0.15f;
            else
                return super.getAlignment( axis);
        }
    } // class AnnotationView

    /**
     * A view which renders a reading or a translation element. These views are placed
     * above or below the normal flow of text and centered vertically on the annotated text
     * fragment.
     *
     * @author Michael Koch
     */
    class ReadingTranslationView extends InlineView {
        /**
         * Type of this view. Either {@link AnnotationTags#READING READING} or
         * {@link AnnotationTags#TRANSLATION TRANSLATION}.
         */
        private AnnotationTags type;

        /**
         * Creates a new view for the specified element with the given type.
         *
         * @param elem The element this view renders.
         * @param type Type of this view. Either {@link AnnotationTags#READING READING} or
         *             {@link AnnotationTags#TRANSLATION TRANSLATION}.
         */
        public ReadingTranslationView( Element elem, AnnotationTags type) {
            super( elem);
            this.type = type;
        }

        /**
         * Paint this view. Overridden to change the allocation so that the view
         * is vertically centered on the annotated text fragment. This means the view
         * will draw outside of its allocated space, but this is usually not a problem.
         * If the annotation parent is currently hidden, the method will do nothing.
         *
         * @param g Graphics object used for drawing.
         * @param allocation The allocation the view should be rendered into.
         */
        public void paint( Graphics g, Shape allocation) {
            if (isHidden())
                return;

            if (allocation instanceof Rectangle) {
                Rectangle a = (Rectangle) allocation;
                float xs = getParent().getMinimumSpan( View.X_AXIS);
                if (a.getWidth() > xs) {
                    a.setRect( a.getX() + (xs - a.getWidth())/2, a.getY(),
                               a.getWidth(), a.getHeight());
                    allocation = a;
                }
            }

            super.paint( g, allocation);
        }

        /**
         * Returns the minimum span. If the annotation is currently hidden, it will not
         * take any space and therefore return 0.
         */
        public float getMinimumSpan( int axis) {
            if (isHidden())
                return 0;

            return super.getMinimumSpan( axis);
        }

        /**
         * Returns the preferred span. If the annotation is currently hidden, it will not
         * take any space and therefore return 0.
         */
        public float getPreferredSpan( int axis) {
            if (isHidden())
                return 0;

            return super.getPreferredSpan( axis);
        }

        /**
         * Returns if the annotation is currently hidden. It is hidden if either
         * all reading/translations annotations are globally hidden by a call to
         * {@link JGlossEditorKit#showReading(boolean) showReading}/
         * {@link JGlossEditorKit#showTranslation(boolean) showTranslation}, or if the
         * hidden attribute of the parent annotation element is set.
         *
         * @return <CODE>true</CODE> if the annotation is hidden.
         */
        public boolean isHidden() {
            if ((type==AnnotationTags.READING)&&!showReading ||
                (type==AnnotationTags.TRANSLATION)&&!showTranslation)
                return true;

            if (!(getParent() instanceof AnnotationView)) {
                // The parent of a ReadingTranslationView should always be
                // an AnnotationView. Nevertheless I got exceptions where
                // the parent was a javax.swing.text.ParagraphView$Row.
                return false;
            }

            return ((AnnotationView) getParent()).isAnnotationHidden();
        }
    } // class ReadingTranslationView

    /**
     * The KanjiView renders the part of the annotation element which contains the original text
     * which is annotated. This class currently does not change the behavior of the superclass
     * but makes the view easily recognizable.
     * with the <CODE>instanceof</CODE> operator.
     *
     * @author Michael Koch
     */
    class KanjiView extends InlineView {
        /**
         * Creates a new view for a kanji element.
         *
         * @param elem The kanji element which is rendered by this view.
         */
        public KanjiView( Element elem) {
            super( elem);
        }
    } // class KanjiView

    /**
     * Creates a view in which the vertical alignment of text is changed. This is neccessary for
     * normal text to be correctly aligned with annotation views.
     *
     * @author Michael Koch
     */
    class VAdjustedView extends InlineView {
        /**
         * Creates a new view for the element.
         *
         * @param elem The element rendered by this view.
         */
        public VAdjustedView( Element elem) {
            super( elem);
        }
        
        /**
         * Returns the alignment. The vertical alignment is changed to make the text align properly
         * with annotation views.
         *
         * @param axis The axis for which the alignment is inquired.
         * @return The alignment.
         */
        public float getAlignment( int axis) {
            if (axis == View.Y_AXIS)
                return -0.15f;
            else
                return super.getAlignment( axis);
        }
        
    } // class VAdjustedView


    /**
     * Creates a new editor kit for JGloss documents.
     *
     * @param parser Parser for finding annotations when a document is loaded. The parser will be used
     *               when creating the default document.
     * @param addAnnotations Flag if annotations should be added when a document is loaded. This will
     *                       be used when the default document is created.
     * @param compactView <CODE>true</CODE> if compact view mode should be used.
     * @param showReading <CODE>true</CODE> if reading annotations should be visible.
     * @param showTranslation <CODE>true</CODE> if translation annotations should be visible.
     */
    public JGlossEditorKit( jgloss.dictionary.Parser parser, boolean addAnnotations, boolean compactView,
                            boolean showReading, boolean showTranslation) {
        super();
        this.parser = parser;
        this.compactView = compactView;
        this.showReading = showReading;
        this.showTranslation = showTranslation;
        this.addAnnotations = addAnnotations;
    }

    /**
     * Sets the compact view mode. In compact view, each annotation element gets only enough 
     * horizontal space. This means that the normal text part of the annotation will align with
     * the rest of the rest of the text, but also that annotations could overlap if they need
     * more space.
     *
     * @param compactView <CODE>true</CODE> if compact view mode should be set.
     */
    public void setCompactView( boolean compactView) {
        this.compactView = compactView;
    }

    /**
     * Sets the visibility of reading annotations. If this is set to <CODE>false</CODE>, the reading
     * annotations will not be shown and will not take screen space.
     *
     * @param showReading <CODE>true</CODE> if reading annotations should be shown.
     */
    public void showReading( boolean showReading) {
        this.showReading = showReading;
    }

    /**
     * Sets the visibility of translation annotations. If this is set to <CODE>false</CODE>, the
     * translation annotations will not be shown and will not take screen space.
     *
     * @param showTranslation <CODE>true</CODE> if translation annotations should be shown.
     */
    public void showTranslation( boolean showTranslation) {
        this.showTranslation = showTranslation;
    }

    /**
     * Returns the current view mode.
     *
     * @return <CODE>true</CODE> if compact view is enabled.
     */
    public boolean isCompactView() {
        return compactView;
    }

    /**
     * Returns the MIME content type of the JGloss documents. This is set in the JGloss message
     * resource bundle under "jgloss.mimetype".
     *
     * @return The MIME content type.
     */
    public String getContentType() {
        return JGloss.messages.getString( "jgloss.mimetype");
    }

    /**
     * Returns an HTML parser for loading a document. This will return an instance of
     * {@link JGlossParserWrapper JGlossParserWrapper}.
     *
     * @return An instance of {@link JGlossParserWrapper JGlossParserWrapper}.
     */
    public HTMLEditorKit.Parser getParser() {
        return new JGlossParserWrapper();
    }

    /**
     * Creates a new document. This will return an instance of 
     * {@link JGlossDocument JGlossDocument} initialized with the text parser
     * passed to the constructor of this JGlossEditorKit.
     *
     * @return The new document.
     */
    public Document createDefaultDocument() {
        return new JGlossDocument( getParser(), parser, addAnnotations);
    }

    /**
     * Returns the view factory which creates views for a JGloss document.
     *
     * @return The view factory.
     */
    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    /**
     * Initializes and returns the DTD used for parsing JGloss documents. This is
     * a modified 3.2 DTD which allows annotation elements anywhere #pcdata is allowed.
     *
     * @return The DTD for parsing JGloss documents.
     */
    private static DTD getDTD() {
        // The DTD used by the JDK1.3 is a HTML 3.2 DTD. Unfortunately it is neither
        // possible to use no DTD at all nor to make the parser simply accept unknown tags.
        // The two alternatives are creating a completely new DTD or hacking the existing one,
        // which I am doing here.

        if (dtd == null) try {
            // make sure that the default DTD is initialized:
            new ParserDelegator();

            // As of JDK1.3, the name of the default DTD of the ParserDelegator is hardcoded
            // in the source, and there is no way for other program to inquire it,
            // so I put it in the preferences to make it easily changeable.
            dtd = DTD.getDTD( JGloss.prefs.getString( Preferences.DTD_DEFAULT));

            // there seems to be a bug in the parser where the REQUIRED content attribute is
            // not recognized even if is there. This only leads to a call of handleError and
            // otherwise has no effect, but I remove the REQUIRED modifier anyway
            AttributeList al = dtd.getElement( "meta").getAttributes();
            while (al != null) {
                if (al.getName().equals( "content"))
                    al.modifier = 0;
                al = al.getNext();
            }
            
            // add custom elements
            // #pcdata*
            ContentModel pcdata = new ContentModel( '*', 
                                                    new ContentModel( dtd.pcdata),
                                                    null);
            javax.swing.text.html.parser.Element reading = 
                dtd.defineElement( AnnotationTags.READING.getId(),
                                   DTD.MODEL, false, false, pcdata, null, null, null);
            javax.swing.text.html.parser.Element kanji = 
                dtd.defineElement( AnnotationTags.KANJI.getId(),
                                   DTD.MODEL, false, false, pcdata, null, null, null);
            javax.swing.text.html.parser.Element translation = 
                dtd.defineElement( AnnotationTags.TRANSLATION.getId(),
                                   DTD.MODEL, false, false, pcdata, null, null, null);
            
            al = new AttributeList( JGlossDocument.HIDDEN_ATTRIBUTE, DTD.CDATA, 
                                    0, null, null, null);
            // LINKED_ANNOTATION is only kept for compatibility with JGloss 0.9.1 documents
            al = new AttributeList( JGlossDocument.LINKED_ANNOTATION, DTD.CDATA, 0, null, null, al);
            al = new AttributeList( JGlossDocument.DICTIONARY_WORD, DTD.CDATA, 0, null, null, al);
            al = new AttributeList( JGlossDocument.DICTIONARY_READING, DTD.CDATA, 0, null, null, al);
            al = new AttributeList( JGlossDocument.TEXT_ANNOTATION, DTD.CDATA, 0, null, null, al);
            
            // The content model of <anno> should really be (reading & kanji & translation),
            // but character level attributes created by a <font> or <a href> tag can be embedded
            // in an annotation element when the document is written by the HTMLWriter,
            // so we have to use DTD.ANY.
            dtd.defineElement( AnnotationTags.ANNOTATION.getId(),
                               DTD.ANY, false, false, null, null, null, al);

            javax.swing.text.html.parser.Element annotation = dtd.getElement
                ( AnnotationTags.ANNOTATION.getId());

            // (anno | reading | kanji | translation | #pcdata*)
            ContentModel annotationmodel = new ContentModel( '|', new ContentModel
                ( 0, annotation,
                  new ContentModel( 0, reading, new ContentModel
                      ( 0, kanji, new ContentModel
                          ( 0, translation, new ContentModel( '*',
                                                              new ContentModel( dtd.pcdata), null))))));
            // allow an annotation element or any of its subelements anywhere the DTD allows #pcdata
            for ( Iterator i=dtd.elements.iterator(); i.hasNext(); ) {
                javax.swing.text.html.parser.Element e =
                    (javax.swing.text.html.parser.Element) i.next();
                if (e!=annotation && e!=reading && e!=kanji && e!=translation) {
                    updateContentModel( dtd, e.getContent(), annotationmodel);
                }
            }
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }

        return dtd;
    }

    /**
     * Change a content model by replacing all occurrences of #pcdata with a new model.
     * This is used to insert the annotation element model wherever #pcdata is allowed in the
     * DTD.
     *
     * @param dtd The DTD to which the content model belongs.
     * @param cm The content model which will be changed.
     * @param annotationmodel The model which will replace any occurrences of #pcdata.
     * @param return <CODE>true</CODE> if the content model was changed.
     */
    private static boolean updateContentModel( DTD dtd, ContentModel cm, 
                                               ContentModel annotationmodel) {
        if (cm == null)
            return false;

        boolean changed = false;
        do {
            if (cm.content instanceof ContentModel) {
                ContentModel cm2 = (ContentModel) cm.content;
                if (cm2.type==0 && cm2.content==dtd.pcdata) {
                    cm.content = annotationmodel;
                    changed = true;
                }
                else
                    changed |= updateContentModel( dtd, (ContentModel) cm.content, annotationmodel);
            }
            cm = cm.next;
        } while (cm != null);
        return changed;
    }
} // class JGlossEditorKit
