/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.ui.html;

import static jgloss.ui.html.AnnotationTags.READING;
import static jgloss.ui.html.AnnotationTags.TRANSLATION;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.BlockView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.InlineView;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.parser.AttributeList;
import javax.swing.text.html.parser.ContentModel;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.DTDConstants;
import javax.swing.text.html.parser.DocumentParser;
import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.text.html.parser.TagElement;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.xml.JGlossDocument;

/**
 * The <CODE>JGlossEditorKit</CODE> is an extension of the
 * <CODE>HTMLEditorKit</CODE> with several additions to manage the generation,
 * display and manipulation of JGloss documents. It also has to work around the
 * shortcomings in the HTML document API. It contains the views for the
 * annotation elements and its subelements as subclasses, since they share some
 * view attribute state with the <CODE>JGlossEditorKit</CODE> instance.
 * <P>
 * The DTD element definition for an annotation element is
 * <CODE>(#PCDATA|READING|BASETEXT|TRANSLATION)*</CODE>. Additional constraints
 * which are not enforceable through the DTD but used throughout JGloss are that
 * each reading element is followed by a kanji element, and there is exactly one
 * translation element which is the last child of the annotation element.
 * </P>
 *
 * @author Michael Koch
 */
public class JGlossEditorKit extends HTMLEditorKit {
    private static final Logger LOGGER = Logger.getLogger(JGlossEditorKit.class.getPackage().getName());

    private static final long serialVersionUID = 1L;

    private static final String JGLOSS_STYLE_SHEET = "/data/jgloss.css";

    /**
     * The factory used to create views for document elements.
     */
    private final JGlossFactory viewFactory = new JGlossFactory();

    /**
     * Flag if reading annotations should be shown.
     */
    private boolean showReading;
    /**
     * Flag if translation annotations should be shown.
     */
    private boolean showTranslation;

    /**
     * Annotation view used as reference for calculating the height of the
     * unannotated views.
     */
    private AnnotationView referenceAnnotationView;

    /**
     * Reading view used as reference for calculating the y offset of the
     * unannoated views.
     */
    private ReadingTranslationView referenceReadingView;

    /**
     * The DTD used when reading HTML documents. This DTD is customized to
     * understand the {@link AnnotationTags AnnotationTags}.
     */
    private static DTD dtd = null;
    /**
     * Standard style for rendering JGloss documents. Used instead of the usual
     * HTML style sheet.
     */
    private static StyleSheet jglossStyleSheet = null;

    /**
     * TagElements are used by the HTML parser to inquire properties of an
     * element. This class adds support for the annotation elements of JGloss.
     */
    private static class JGlossTagElement extends TagElement {
        /**
         * The tag this element wraps.
         */
        private HTML.Tag htmlTag;

        /**
         * Creates a new TagElement which wraps the given element. If the HTML
         * tag represented by this element is an instance of HTML.UnknownTag it
         * will be replaced by the equivalent unque annotation tag.
         */
        public JGlossTagElement(javax.swing.text.html.parser.Element e, boolean fictional) {
            super(e, fictional);

            // Get the real annotation tag. Unfortunately the creation of the
            // HTML.UnknownTags
            // cannot be prevented, so we have to work around it.
            htmlTag = super.getHTMLTag();
            if (htmlTag instanceof HTML.UnknownTag) {
                htmlTag = AnnotationTags.getAnnotationTagEqualTo(htmlTag);
            }
        }

        @Override
        public HTML.Tag getHTMLTag() {
            return htmlTag;
        }

        @Override
        public boolean breaksFlow() {
            return htmlTag.breaksFlow();
        }

        @Override
        public boolean isPreformatted() {
            return htmlTag.isPreformatted();
        }
    }

    /**
     * Instance of <CODE>HTMLEditorKit.Parser</CODE>, which will forward parse
     * requests to a {@link JGlossEditorKit.JGlossParser JGlossParser}.
     * <CODE>JGlossParser</CODE> is derived from <CODE>DocumentParser</CODE>,
     * which prevents it from also being a <CODE>HTMLEditorKit.Parser</CODE>.
     */
    class JGlossParserWrapper extends HTMLEditorKit.Parser {
        /**
         * The parser to which parse requests will be forwarded.
         */
        private final JGlossParser parser;

        /**
         * Creates a new wrapper with an associated <CODE>JGlossParser</CODE>.
         */
        public JGlossParserWrapper() {
            parser = new JGlossParser();
        }

        /**
         * Parse a document. This forwards the request to the underlying parser.
         */
        @Override
        public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharset) throws IOException {
            parser.parse(r, cb, ignoreCharset);
        }

        /**
         * Sets strict parsing mode on the wrapped <CODE>JGlossParser</CODE>.
         *
         * @see JGlossEditorKit.JGlossParser#setStrict(boolean)
         */
        public void setStrict(boolean strict) {
            parser.setStrict(strict);
        }
    }

    /**
     * Parser for JGloss documents.
     */
    private class JGlossParser extends DocumentParser {
        /**
         * Constructs a new parser. This will use the DTD modified for JGloss
         * tags from {@link JGlossEditorKit#getDTD() getDTD}.
         *
         */
        public JGlossParser() {
            super(getDTD());

            // Swing 1.4 changes how whitespace is coalesced in class
            // javax.swing.text.html.parser.Parser. The new behavior breaks
            // reading of JGloss documents
            // with tags containing only a single space character (e. g. <trans>
            // </trans>). To switch
            // back to the old behavior, the protected member strict must be
            // changed to true.
            strict = true;
        }

        /**
         * Make sure the document charset is always ignored. The Parser has a
         * bug where it throws an exception when the charset of the reader is
         * identical to that given in a http-equiv charset header in the
         * document instead of when they are different. This parser will always
         * ignore the charset. It will also clear the cache of the
         * jgloss.dictionary.Parser when parsing is finished.
         *
         * @param r
         *            Reader from which the document is read.
         * @param cb
         *            Callback to forward document construction requests to.
         * @param ignoreCharset
         *            <CODE>true</CODE> if charset declarations in the HTML
         *            document should be ignored. This parameter will be
         *            ignored.
         * @exception IOException
         */
        @Override
        public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharset) throws IOException {
            super.parse(r, cb, true);
        }

        /**
         * Sets strict parsing mode. If strict parsing mode is set to
         * <CODE>true</CODE> (default), SGML specification conformance is
         * enforced, otherwise incorrect content is handled mimicking the
         * popular browsers' behavior. Strict mode is needed when a JGloss
         * document is loaded, while non-strict mode is needed when it is
         * edited.
         */
        public void setStrict(boolean strict) {
            this.strict = strict;
        }

        /**
         * Creates a {@link JGlossEditorKit.JGlossTagElement JGlossTagElement}
         * which wraps the given element. This is necessary because the parser
         * superclass does not know how to handle the custom annotation tags.
         *
         * @param e
         *            The element this should wrap.
         * @param fictional
         *            <CODE>true</CODE> if the element does not exist in the
         *            original document.
         * @return The new tag element.
         */
        @Override
        protected TagElement makeTag(javax.swing.text.html.parser.Element e, boolean fictional) {
            return new JGlossTagElement(e, fictional);
        }

        @Override
        protected void handleError(int ln, String errorMsg) {
            LOGGER.warning("error parsing JGloss document at line " + ln + ": " + errorMsg);
        }
    }

    /**
     * Factory which creates views for elements in the JGloss document.
     */
    private class JGlossFactory extends HTMLEditorKit.HTMLFactory {
        /**
         * Creates a new factory.
         *
         */
        public JGlossFactory() {
        }

        /**
         * Creates a view which can render the element.
         *
         * @param elem
         *            The element to create the view for.
         * @return The newly created view.
         */
        @Override
        public View create(Element elem) {
            Object name = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            AttributeSet a = elem.getAttributes();
            if (name.equals(AnnotationTags.ANNOTATION)) {
                AnnotationView annotationView = new AnnotationView(elem);
                if (referenceAnnotationView == null) {
                    referenceAnnotationView = annotationView;
                }
                return annotationView;
            } else if (name.equals(AnnotationTags.WORD)) {
                return new WordView(elem);
            } else if (name.equals(AnnotationTags.READING_BASETEXT)) {
                return new ReadingBaseView(elem);
            } else if (a.isDefined(AnnotationTags.BASETEXT) || a.isDefined(AnnotationTags.BASETEXT.getId())) {
                return new BaseView(elem);
            } else if (a.isDefined(AnnotationTags.TRANSLATION) || a.isDefined(AnnotationTags.TRANSLATION.getId())) {
                return new ReadingTranslationView(elem, AnnotationTags.TRANSLATION);
            } else if (a.isDefined(AnnotationTags.READING) || a.isDefined(AnnotationTags.READING.getId())) {
                ReadingTranslationView readingView = new ReadingTranslationView(elem, AnnotationTags.READING);
                if (referenceReadingView == null) {
                    referenceReadingView = readingView;
                }
                return readingView;
            } else {
                View v = super.create(elem);
                if (v instanceof InlineView && !name.equals(HTML.Tag.BR)) {
                    // The inline views do not have the text aligned to work
                    // properly with the
                    // annotation view. Replace it with a vertically adjusted
                    // view.
                    return new UnannotatedTextView(elem);
                } else {
                    return v;
                }
            }
        }
    }

    /**
     * A view which renders an annotation. The view is a block, but will not
     * cause a break in the flow. Child views are laid out vertically.
     */
    public class AnnotationView extends BlockView {
        /**
         * Last parent view which was a LogicalView. See
         * {@link #setParent(View) setParent}.
         */
        private View logicalViewParent = null;

        /**
         * Creates a new view for an annotation element.
         *
         * @param elem
         *            The element rendered by the view.
         */
        public AnnotationView(Element elem) {
            super(elem, View.Y_AXIS);
        }

        /**
         * Fix setting of <CODE>null</CODE> parent with Swing 1.4.
         * <CODE>AnnotationViews</CODE> can be referenced from two parents at
         * the same time: a <CODE>FlowView.LogicalView</CODE> and a
         * <CODE>ParagraphView.Row</CODE>. If the <CODE>AnnotationView</CODE> is
         * removed from the <CODE>ParagraphView.Row</CODE> by calling
         * <CODE>setParent(null)</CODE>, the <CODE>LogicalView</CODE> has to be
         * made parent again for the layout to work.
         */
        @Override
        public void setParent(View parent) {
            if (parent == null && logicalViewParent != null) {
                // Instead of removing the parent, test if the logical view
                // should be made
                // parent again.
                if (getParent() == logicalViewParent) {
                    // The annotationView is removed from the logical view
                    // parent
                    logicalViewParent = null;
                } else {
                    // Test if the AnnotationView is still a child of the
                    // logical view.
                    for (int i = 0; i < logicalViewParent.getViewCount(); i++) {
                        if (logicalViewParent.getView(i) == this) {
                            // Still a child: reset parent view to logical view.
                            parent = logicalViewParent;
                        }
                    }
                }
            }

            if (parent != null) {
                // Save logical view parent. Since the class
                // FlowView.LogicalView is package-private,
                // the test has to be done via class name check.
                if (parent.getClass().getName().equals("javax.swing.text.FlowView$LogicalView")) {
                    logicalViewParent = parent;
                }
            }

            super.setParent(parent);
        }

        /**
         * Returns the maximum span of the view. Overridden to prevent it
         * growing beyond the preferred span.
         */
        @Override
        public float getMaximumSpan(int axis) {
            return getPreferredSpan(axis);
        }
    } // class AnnotationView

    /**
     * A view which renders a reading/kanji element. The view is a block, but
     * will not cause a break in the flow. Child views are laid out vertically.
     */
    class ReadingBaseView extends BlockView {
        /**
         * Creates a new view for a reading/kanji element.
         *
         * @param elem
         *            The element rendered by the view.
         */
        public ReadingBaseView(Element elem) {
            super(elem, View.Y_AXIS);
        }

        /**
         * Returns the maximum span of the view. Overridden to prevent it to
         * grow beyond the preferred span.
         */
        @Override
        public float getMaximumSpan(int axis) {
            return getPreferredSpan(axis);
        }

        /**
         * Returns the alignment of this view. Overridden because the alignment
         * has to be changed if the annotation is not displayed.
         */
        @Override
        public float getAlignment(int axis) {
            if (axis == View.Y_AXIS) {
                if (!showReading) {
                    return getView(1).getAlignment(axis);
                }
            }

            return super.getAlignment(axis);
        }
    } // class ReadingBaseView

    /**
     * A view displaying a word with reading annotations. The view is a block,
     * but will not cause a break in the flow. Child views are laid out
     * horizontally.
     */
    class WordView extends BlockView {
        public WordView(Element elem) {
            super(elem, View.X_AXIS);
        }

        /**
         * Centers the view along the x-axis.
         */
        @Override
        public float getAlignment(int axis) {
            if (axis == View.X_AXIS) {
                return 0.5f;
            } else {
                return super.getAlignment(axis);
            }
        }
    } // class WordView

    /**
     * A view which renders a reading or a translation element. These views are
     * placed above or below the normal flow of text and centered vertically on
     * the annotated text fragment.
     */
    class ReadingTranslationView extends InlineView {
        /**
         * Type of this view. Either {@link AnnotationTags#READING READING} or
         * {@link AnnotationTags#TRANSLATION TRANSLATION}.
         */
        private final AnnotationTags type;

        /**
         * Creates a new view for the specified element with the given type.
         *
         * @param elem
         *            The element this view renders.
         * @param type
         *            Type of this view. Either {@link AnnotationTags#READING
         *            READING} or {@link AnnotationTags#TRANSLATION TRANSLATION}
         *            .
         */
        public ReadingTranslationView(Element elem, AnnotationTags type) {
            super(elem);
            this.type = type;
        }

        /**
         * Paint this view. Overridden to change the allocation so that the view
         * is horizontally centered on the annotated text fragment. This means
         * the view will draw outside of its allocated space, but this is
         * usually not a problem.
         *
         * @param g
         *            Graphics object used for drawing.
         * @param allocation
         *            The allocation the view should be rendered into.
         */
        @Override
        public void paint(Graphics g, Shape allocation) {
            if (isHidden()) {
                return;
            }

            if (getParent() == null) {
                return;
            }

            super.paint(g, allocation);
        }

        @Override
        public float getMinimumSpan(int axis) {
            float span;

            if (isHidden()) {
                span = 0;
            } else {
                span = super.getMinimumSpan(axis);
            }

            return span;
        }

        @Override
        public float getPreferredSpan(int axis) {
            float span;

            if (isHidden()) {
                span = 0;
            } else {
                span = super.getPreferredSpan(axis);
            }

            return span;
        }

        @Override
        public float getMaximumSpan(int axis) {
            float span;

            if (isHidden()) {
                span = 0;
            } else {
                span = super.getMaximumSpan(axis);
            }

            return span;
        }

        /**
         * Returns if the annotation is currently hidden. It is hidden if all
         * reading/translations annotations are globally hidden by a call to
         * {@link JGlossEditorKit#showReading(boolean) showReading}/
         * {@link JGlossEditorKit#showTranslation(boolean) showTranslation}.
         *
         * @return <CODE>true</CODE> if the annotation is hidden.
         */
        public boolean isHidden() {
            return READING.equals(type) && !showReading || TRANSLATION.equals(type) && !showTranslation;
        }

    } // class ReadingTranslationView

    /**
     * Inline view with {@link #getBreakWeight(int, float, float)} optimized for
     * better performance with Japanese text.
     */
    class OptimizedBreakInlineView extends InlineView {

        /**
         * Creates a new view for the element.
         *
         * @param elem
         *            The element rendered by this view.
         */
        public OptimizedBreakInlineView(Element elem) {
            super(elem);
        }

        /**
         * Overridden to always break the line at the last char.
         */
        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            /*
             * J2SE 1.4 introduces a new, more sophisticated way to choose where
             * to break a line with class
             * <CODE>java.text.RuleBasedBreakIterator</CODE>.
             * <CODE>GlyphView.getBreakWeight</CODE> uses this class to decide
             * where to break a line. Unfortunately, this is a major performance
             * bottleneck. Since the utility of this way of breaking lines for
             * Japanese text is dubious at best, it is disabled in the
             * overridden method. Unfortunately there is no cleaner API do do
             * this.
             */

            if (axis == View.X_AXIS) {
                checkPainter();
                int p0 = getStartOffset();
                int p1 = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                if (p1 == p0) {
                    return View.BadBreakWeight;
                } else {
                    return View.GoodBreakWeight;
                }
            } else {
                return super.getBreakWeight(axis, pos, len);
            }
        }
    }

    /**
     * Inline view for text which is not annotated. Span calculation and
     * painting are overridden to make the text align with the basetext of the
     * {@link AnnotationView} elements.
     */
    class UnannotatedTextView extends OptimizedBreakInlineView {

        public UnannotatedTextView(Element elem) {
            super(elem);
        }

        /**
         * Overridden to make the view as high as the
         * {@link JGlossEditorKit#referenceAnnotationView
         * referenceAnnotationView}.
         */
        @Override
        public float getMinimumSpan(int axis) {
            float minimumSpan;
            if (axis == Y_AXIS && referenceAnnotationView != null) {
                minimumSpan = referenceAnnotationView.getMinimumSpan(axis);
            } else {
                minimumSpan = super.getMinimumSpan(axis);
            }
            return minimumSpan;
        }

        /**
         * Overridden to make the view as high as the
         * {@link JGlossEditorKit#referenceAnnotationView
         * referenceAnnotationView}.
         */
        @Override
        public float getPreferredSpan(int axis) {
            float preferredSpan;
            if (axis == Y_AXIS && referenceAnnotationView != null) {
                preferredSpan = referenceAnnotationView.getPreferredSpan(axis);
            } else {
                preferredSpan = super.getPreferredSpan(axis);
            }
            return preferredSpan;
        }

        /**
         * Overridden to make the view as high as the
         * {@link JGlossEditorKit#referenceAnnotationView
         * referenceAnnotationView}.
         */
        @Override
        public float getMaximumSpan(int axis) {
            float maximumSpan;
            if (axis == Y_AXIS && referenceAnnotationView != null) {
                maximumSpan = referenceAnnotationView.getMaximumSpan(axis);
            } else {
                maximumSpan = super.getMaximumSpan(axis);
            }
            return maximumSpan;
        }

        /**
         * Overridden to make the alignment across the Y axis identical to the
         * {@link JGlossEditorKit#referenceAnnotationView
         * referenceAnnotationView}.
         */
        @Override
        public float getAlignment(int axis) {
            float alignment;
            if (axis == Y_AXIS && referenceAnnotationView != null) {
                alignment = referenceAnnotationView.getAlignment(axis);
            } else {
                alignment = super.getAlignment(axis);
            }
            return alignment;
        }

        /**
         * Overridden to paint the text at the same y location as the basetext
         * of a annotation view. This is done by offsetting the y location by
         * the height of the {@link JGlossEditorKit#referenceReadingView} if
         * readings are shown. Since the height of the view is as large as the
         * annotation view, there is enough room for it.
         */
        @Override
        public void paint(Graphics g, Shape a) {
            Rectangle bounds = a.getBounds();
            if (showReading && referenceReadingView != null) {
                bounds.y += referenceReadingView.getPreferredSpan(Y_AXIS);
            }
            super.paint(g, bounds);
        }

    }

    /**
     * The BaseView renders the part of the annotation element which contains
     * the original text which is annotated. This class currently does not
     * change the behavior of the superclass but makes the view easily
     * recognizable using the <CODE>instanceof</CODE> operator.
     */
    class BaseView extends OptimizedBreakInlineView {
        /**
         * Creates a new view for a kanji element.
         *
         * @param elem
         *            The kanji element which is rendered by this view.
         */
        public BaseView(Element elem) {
            super(elem);
        }

    } // class BaseView

    /**
     * Creates a new editor kit for JGloss documents.
     *
     * @param compactView
     *            <CODE>true</CODE> if compact view mode should be used.
     * @param showReading
     *            <CODE>true</CODE> if reading annotations should be visible.
     * @param showTranslation
     *            <CODE>true</CODE> if translation annotations should be
     *            visible.
     */
    public JGlossEditorKit(boolean _showReading, boolean _showTranslation) {
        showReading = _showReading;
        showTranslation = _showTranslation;
    }

    /**
     * Sets the visibility of reading annotations. If this is set to
     * <CODE>false</CODE>, the reading annotations will not be shown and will
     * not take screen space.
     *
     * @param showReading
     *            <CODE>true</CODE> if reading annotations should be shown.
     */
    public void showReading(boolean showReading) {
        this.showReading = showReading;
    }

    /**
     * Sets the visibility of translation annotations. If this is set to
     * <CODE>false</CODE>, the translation annotations will not be shown and
     * will not take screen space.
     *
     * @param showTranslation
     *            <CODE>true</CODE> if translation annotations should be shown.
     */
    public void showTranslation(boolean showTranslation) {
        this.showTranslation = showTranslation;
    }

    /**
     * Returns the MIME content type of the JGloss documents. This is set in the
     * JGloss message resource bundle under "jgloss.mimetype".
     *
     * @return The MIME content type.
     */
    @Override
    public String getContentType() {
        return JGloss.MESSAGES.getString("jgloss.mimetype");
    }

    /**
     * Returns an HTML parser for loading a document. This will return an
     * instance of {@link JGlossParserWrapper JGlossParserWrapper}.
     *
     * @return An instance of {@link JGlossParserWrapper JGlossParserWrapper}.
     */
    @Override
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
    @Override
    public Document createDefaultDocument() {
        StyleSheet styles = new StyleSheet();
        styles.addStyleSheet(getStyleSheet());
        JGlossHTMLDoc doc = new JGlossHTMLDoc(styles, getParser());
        return doc;
    }

    /**
     * Returns the view factory which creates views for a JGloss document.
     *
     * @return The view factory.
     */
    @Override
    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    /**
     * Initializes and returns the DTD used for parsing JGloss documents. This
     * is a modified 3.2 DTD which allows annotation elements anywhere #pcdata
     * is allowed.
     *
     * @return The DTD for parsing JGloss documents.
     */
    public static DTD getDTD() {
        // The DTD used by the JDK1.3 is a HTML 3.2 DTD. Unfortunately it is
        // neither
        // possible to use no DTD at all nor to make the parser simply accept
        // unknown tags.
        // The two alternatives are creating a completely new DTD or hacking the
        // existing one,
        // which I am doing here.

        if (dtd == null) {
            try {
                // make sure that the default DTD is initialized:
                new ParserDelegator();

                // As of JDK1.3, the name of the default DTD of the
                // ParserDelegator is hardcoded
                // in the source, and there is no way for other program to
                // inquire it,
                // so I put it in the preferences to make it easily changeable.
                dtd = DTD.getDTD(JGloss.PREFS.getString(Preferences.DTD_DEFAULT));

                // there seems to be a bug in the parser where the REQUIRED
                // content attribute is
                // not recognized even if is there. This only leads to a call of
                // handleError and
                // otherwise has no effect, but I remove the REQUIRED modifier
                // anyway
                AttributeList al = dtd.getElement("meta").getAttributes();
                while (al != null) {
                    if (al.getName().equals("content")) {
                        al.modifier = 0;
                    }
                    al = al.getNext();
                }

                // add custom elements
                // #pcdata*
                ContentModel pcdata = new ContentModel('*', new ContentModel(dtd.pcdata), null);
                dtd.defineElement(AnnotationTags.READING.getId(), DTDConstants.MODEL, false, false, pcdata, null, null,
                                null);
                dtd.defineElement(AnnotationTags.BASETEXT.getId(), DTDConstants.MODEL, false, false, pcdata, null,
                                null, null);
                dtd.defineElement(AnnotationTags.TRANSLATION.getId(), DTDConstants.MODEL, false, false, pcdata, null,
                                null, null);
                javax.swing.text.html.parser.Element reading = dtd.getElement(AnnotationTags.READING.getId());
                javax.swing.text.html.parser.Element base = dtd.getElement(AnnotationTags.BASETEXT.getId());
                javax.swing.text.html.parser.Element translation = dtd.getElement(AnnotationTags.TRANSLATION.getId());

                al = new AttributeList(JGlossHTMLDoc.Attributes.BASE, DTDConstants.CDATA, 0, null, null, al);
                al = new AttributeList(JGlossHTMLDoc.Attributes.BASE_READING, DTDConstants.CDATA, 0, null, null, al);

                // The content model of <anno> should really be (word &
                // translation)*,
                // but character level attributes created by a <font> or <a
                // href> tag can be embedded
                // in an annotation element when the document is written by the
                // HTMLWriter,
                // so we have to use DTD.ANY.
                dtd.defineElement(AnnotationTags.ANNOTATION.getId(), DTDConstants.ANY, false, false, null, null, null,
                                al);
                dtd.defineElement(AnnotationTags.WORD.getId(), DTDConstants.ANY, false, false, null, null, null, null);
                dtd.defineElement(AnnotationTags.READING_BASETEXT.getId(), DTDConstants.ANY, false, false, null, null,
                                null, null);

                javax.swing.text.html.parser.Element annotation = dtd.getElement(AnnotationTags.ANNOTATION.getId());
                javax.swing.text.html.parser.Element word = dtd.getElement(AnnotationTags.WORD.getId());
                javax.swing.text.html.parser.Element reading_base = dtd.getElement(AnnotationTags.READING_BASETEXT
                                .getId());

                // (anno | word | reading_base | reading | base | translation |
                // #pcdata*)
                ContentModel annotationmodel = new ContentModel('|', new ContentModel('|',
                                new ContentModel(annotation),
                                new ContentModel('|', new ContentModel(word), new ContentModel('|', new ContentModel(
                                                reading_base), new ContentModel('|', new ContentModel(reading),
                                                new ContentModel('|', new ContentModel(base), new ContentModel('|',
                                                                new ContentModel(translation), new ContentModel('*',
                                                                                new ContentModel(dtd.pcdata)))))))));
                // allow an annotation element or any of its subelements
                // anywhere the DTD allows #pcdata
                for (javax.swing.text.html.parser.Element e : dtd.elements) {
                    if (e != annotation && e != word && e != reading_base && e != reading && e != base
                                    && e != translation) {
                        updateContentModel(dtd, e.getContent(), annotationmodel);
                    }
                }
            } catch (java.io.IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        return dtd;
    }

    /**
     * Change a content model by replacing all occurrences of #pcdata with a new
     * model. This is used to insert the annotation element model wherever
     * #pcdata is allowed in the DTD.
     *
     * @param dtd
     *            The DTD to which the content model belongs.
     * @param cm
     *            The content model which will be changed.
     * @param annotationmodel
     *            The model which will replace any occurrences of #pcdata.
     * @param return <CODE>true</CODE> if the content model was changed.
     */
    private static boolean updateContentModel(DTD dtd, ContentModel cm, ContentModel annotationmodel) {
        if (cm == null) {
            return false;
        }

        boolean changed = false;
        do {
            if (cm.content instanceof ContentModel) {
                ContentModel cm2 = (ContentModel) cm.content;
                if (cm2.type == 0 && cm2.content == dtd.pcdata) {
                    cm.content = annotationmodel;
                    changed = true;
                } else {
                    changed |= updateContentModel(dtd, (ContentModel) cm.content, annotationmodel);
                }
            }
            cm = cm.next;
        } while (cm != null);
        return changed;
    }

    @Override
    public void setStyleSheet(StyleSheet _jglossStyleSheet) {
        jglossStyleSheet = _jglossStyleSheet;
    }

    @Override
    public StyleSheet getStyleSheet() {
        if (jglossStyleSheet == null) {
            jglossStyleSheet = new StyleSheet();
            try {
                InputStream is = JGlossEditorKit.class.getResourceAsStream(JGLOSS_STYLE_SHEET);
                Reader r = new BufferedReader(new InputStreamReader(is));
                jglossStyleSheet.loadRules(r, null);
                r.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        return jglossStyleSheet;
    }
} // class JGlossEditorKit
