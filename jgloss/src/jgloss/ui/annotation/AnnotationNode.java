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

package jgloss.ui.annotation;

import jgloss.*;
import jgloss.dictionary.*;
import jgloss.ui.*;
import jgloss.ui.doc.*;

import java.util.*;

import javax.swing.text.*;
import javax.swing.tree.*;

/**
 * An annotation node wraps an annotation element in a document. It has two
 * {@link ReadingTranslationNode ReadingTranslationNodes} as children, which allow
 * the edit of the current reading and translation annotation. It additionally has
 * child nodes for the reading and translations found by the dictionary lookups during
 * parsing.
 *
 * @author Michael Koch
 * @see ReadingTranslationNode
 * @see ReadingAnnotationNode
 * @see DictionaryNode
 */
public class AnnotationNode extends InnerNode {
    /**
     * The annotation element this node wraps.
     */
    private Element annotation;
    /**
     * The document text contained in the kanji element.
     */
    private String kanjiText;
    /**
     * The child which displays the current reading annotation.
     */
    private ReadingTranslationNode reading;
    /**
     * The child which displays the current translation annotation.
     */
    private ReadingTranslationNode translation;

    /**
     * The text which this node displays in the tree.
     */
    private String nodeText;

    /**
     * Creates a new annotation node for an annotation element. This will also create all
     * child nodes of the annotation.
     *
     * @param parent Parent of this node. This should be the root of the tree.
     * @param annotation The annotation element this node wraps.
     */
    public AnnotationNode( InnerNode parent, Element annotation) {
        super( parent, new Vector( ((List) annotation.getAttributes().getAttribute
                                   ( JGlossDocument.TEXT_ANNOTATION)).size()+2));
        // Note that the child elements are guraranteed to exist by the way the element is constructed
        // in JGlossDocument.JGlossReader.
        this.annotation = annotation;
        Element readingElement = annotation.getElement( 0);
        Element kanjiElement = annotation.getElement( 1);
        Element translationElement = annotation.getElement( 2);
        
        reading = new ReadingTranslationNode( this, readingElement, true);
        translation = new ReadingTranslationNode( this, translationElement, false);
        children.add( reading);
        children.add( translation);

        List annotations = (java.util.List) annotation.getAttributes().getAttribute
            ( JGlossDocument.TEXT_ANNOTATION);

        if (annotations != null) {
            // sort the translation by the dictionaries they appear in
            HashMap dictionaryNodes = new HashMap();
            for ( Iterator i=annotations.iterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o instanceof Translation) {
                    Translation t = (Translation) o;
                    String d = t.getDictionaryEntry().getDictionary().getName();
                    DictionaryNode dn = (DictionaryNode) dictionaryNodes.get( d);
                    if (dn == null) {
                        dn = new DictionaryNode( this, d);
                        add( dn);
                        dictionaryNodes.put( d, dn);
                    }
                    dn.add( new TranslationNode( dn, t));
                }
                else if (o instanceof Reading) {
                    add( new ReadingAnnotationNode( this, (Reading) o));
                }
                // else: unhandled annotation type, ignore
            }
        }
        try {
            this.kanjiText = kanjiElement.getDocument().getText( kanjiElement.getStartOffset(),
                                                                 kanjiElement.getEndOffset()-
                                                                 kanjiElement.getStartOffset());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }

        updateNodeText();
    }

    /**
     * Returns the annotation element this node wraps.
     *
     * @return The annotation element this node wraps.
     */
    public Element getAnnotationElement() {
        return annotation;
    }

    /**
     * Returns the text of this annotation. This is the current reading annotation and
     * translation annotation plus any translation entries.
     *
     * @return The text of the annotation.
     */
    public String getAnnotationText() {
        String out = reading.toString() + "\n" + translation.toString();
        for ( int i=2; i<children.size(); i++) {
            Object child = children.elementAt( i);
            if (child instanceof DictionaryNode) {
                out += "\n" + ((DictionaryNode) child).getText();
            }
            else
                out += "\n" + child.toString();
        }
        return out;
    }

    /**
     * Returns a string representation of this node.
     *
     * @return A string representation of this node.
     */
    public String toString() {
        return nodeText;
    }

    /**
     * Returns the text of the kanji part of this annotation.
     *
     * @return The text of the kanji part.
     */
    public String getKanjiText() { return kanjiText; }

    /**
     * Returns the child node which manages the current reading annotation.
     *
     * @return The reading annotation child.
     */
    public ReadingTranslationNode getReadingNode() { return reading; }
    /**
     * Returns the child node which manages the current translation annotation.
     *
     * @return The translation annotation child.
     */
    public ReadingTranslationNode getTranslationNode() { return translation; }

    /**
     * Returns <CODE>true</CODE> if the annotation is hidden (not displayed in the
     * document).
     *
     * @return <CODE>true</CODE> if the annotation is hidden.
     */
    public boolean isHidden() {
        return ((JGlossDocument) annotation.getDocument()).isHidden( annotation);
    }

    /**
     * Sets the hidden status of the annotation. If the annotation is hidden, the reading
     * and translation will not be displayed in the document.
     *
     * @param hidden <CODE>true</CODE> if this node should be hidden.
     */
    public void setHidden( boolean hidden) {
        setHidden( hidden, true);
    }

    /**
     * Sets the hidden status of the annotation. If the annotation is hidden, the reading
     * and translation will not be displayed in the document.
     *
     * @param hidden <CODE>true</CODE> if this node should be hidden.
     * @param updateLayout <CODE>true</CODE> if the document layout should be updated. If you want
     *        to hide several nodes set this to <CODE>true</CODE> only for the last node.
     */
    public void setHidden( boolean hidden, boolean updateLayout) {
        if (hidden != isHidden()) {
            RootNode root = getRootNode();
            ((JGlossDocument) annotation.getDocument()).setHidden( annotation, hidden);
            if (updateLayout) {
                // force docpane to be re-layouted. Unfortunately I have not found a better
                // way to do this.
                ((JGlossDocument) annotation.getDocument()).getStyleSheet()
                    .addRule( AnnotationTags.ANNOTATION.getId() + " { }");
            }
            updateNodeText();
            root.getModel().nodeChanged( this);
        }
    }

    /**
     * Removes this annotation from the document. The document listener of the
     * annotation model will remove this node from the model in reaction to the change.
     */
    public void removeAnnotation() {
        try {
            JGlossDocument doc = ((JGlossDocument) annotation.getDocument());
            doc.setOuterHTML( annotation, kanjiText);
            // remove the newline which the stupid HTMLDocument.insertHTML insists on adding
            doc.remove( annotation.getEndOffset()-1, 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the linked text annotation of the annotation element this node wraps.
     * This is one of the text annotations in the list of annotations for this element.
     * It is usually set to the annotation from which the current settings of the
     * reading and translation annotations are taken.
     *
     * @return The linked annotation, or <CODE>null</CODE> if there is no linked annotation.
     */
    public Parser.TextAnnotation getLinkedAnnotation() {
        return (Parser.TextAnnotation) annotation.getAttributes().getAttribute
            (JGlossDocument.LINKED_ANNOTATION);
    }

    /**
     * Sets the linked text annotation of the annotation element this node wraps.
     * This is one of the text annotations in the list of annotations for this element.
     * It is usually set to the annotation from which the current settings of the
     * reading and translation annotations are taken.
     *
     * @return The new linked annotation.
     */
    public void setLinkedAnnotation( Parser.TextAnnotation linkedAnnotation) {
        ((JGlossDocument) annotation.getDocument()).setLinkedAnnotation( annotation,
                                                                         linkedAnnotation);
    }

    /**
     * Removes the linked text annotation from this annotation.
     */
    public void removeLinkedAnnotation() {
        ((JGlossDocument) annotation.getDocument()).setLinkedAnnotation( annotation, null);
    }

    /**
     * Sets the new node text. This should be called if the attributes of the annotation element
     * have changed (for example after being hidden).
     */
    private void updateNodeText() {
        nodeText = kanjiText + ":";
        if (isHidden())
            nodeText += JGloss.messages.getString( "annotationeditor.entry.hidden");
    }
} // class AnnotationNode
