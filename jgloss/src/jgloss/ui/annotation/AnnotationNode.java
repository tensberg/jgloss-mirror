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
 * An annotation node wraps an annotation element in a document. It has a {@link WordNode WordNode}
 * or {@link ReadingTextNode ReadingTextNode}, a {@link TranslationTextNode TranslationTextNode}
 * and a {@link DictionaryFormNode DictionaryFormNode} as children.
 * It additionally has
 * child nodes for the reading and translations found by the dictionary lookups during
 * parsing.
 *
 * @author Michael Koch
 */
public class AnnotationNode extends InnerNode {
    /**
     * Index of the first dictionary node child of this node.
     */
    private final static int DICTIONARY_NODE_OFFSET = 3;

    /**
     * The annotation element this node wraps.
     */
    private Element annotation;
    /**
     * Manages the readings of the annotated word.
     */
    private WordNode word;
    /**
     * The child which displays the current translation annotation.
     */
    private TranslationTextNode translation;
    /**
     * Child which displays the dictionary form of the anntated word.
     */
    private DictionaryFormNode dictionaryform;
    
    /**
     * The text which this node displays in the tree.
     */
    private String nodeText;

    /**
     * Returns the text of the reading/annotation node.
     */
    public static String getElementText( Element element) {
        try {
            return element.getDocument().getText( element.getStartOffset(),
                                                  element.getEndOffset()-
                                                  element.getStartOffset());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a new annotation node for an annotation element. This will also create all
     * child nodes of the annotation.
     *
     * @param parent Parent of this node. This should be the root of the tree.
     * @param annotation The annotation element this node wraps.
     */
    public AnnotationNode( InnerNode parent, Element annotation) {
        super( parent, new Vector( ((List) annotation.getAttributes().getAttribute
                                   ( JGlossDocument.TEXT_ANNOTATION)).size()+3));
        // Note that the child elements are guraranteed to exist by the way the element is constructed
        // in JGlossDocument.JGlossReader.
        this.annotation = annotation;

        word = new WordNode( this, annotation.getElement( 0)) {
                public void updateReadingText() {
                    super.updateReadingText();
                    if (AnnotationNode.this != null) // is null while still in constructor
                        AnnotationNode.this.updateNodeText( true);
                }
            };
        if (word.getReadingCount() == 1)
            // add single reading directly to tree
            children.add( word.getReadings()[0]);
        else
            // add reading subtree
            children.add( word);
        
        translation = new TranslationTextNode( this) {
                public void setText( String text) {
                    super.setText( text);
                    updateNodeText( true);
                }
            };
        children.add( translation);
        dictionaryform = new DictionaryFormNode( this);
        children.add( dictionaryform);

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
                    else {
                        // Test if the translations of the new node are equal to the translations
                        // of the previous node and if so, remove the translations from the previous
                        // node. This is not the most elegant solution to do this, but the
                        // cost should be negligible.
                        TranslationNode tnp = (TranslationNode) dn.getChildAt( dn.getChildCount()-1);
                        String[] t1 = t.getDictionaryEntry().getTranslations();
                        String[] t2 = tnp.getTranslation().getDictionaryEntry().getTranslations();
                        if (t1.length == t2.length) {
                            boolean equal = true;
                            for ( int j=0; j<t1.length; j++) {
                                if (!t1[j].equals( t2[j])) {
                                    equal = false;
                                    break;
                                }
                            }
                            if (equal)
                                tnp.removeAll();
                        }
                    }
                    dn.add( new TranslationNode( dn, t));
                }
                else if (o instanceof Reading) {
                    add( new ReadingAnnotationNode( this, (Reading) o));
                }
                // else: unhandled annotation type, ignore
            }
        }

        updateNodeText( false);
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
        String out = word.getReading() + "\n" + translation.toString();
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

    public WordNode getWordNode() { return word; }
    /**
     * Returns the child node which manages the current translation annotation.
     *
     * @return The translation annotation child.
     */
    public TranslationTextNode getTranslationNode() { return translation; }

    /**
     * Returns the node which displays the dictionary form of the annotated word.
     */
    public DictionaryFormNode getDictionaryFormNode() { return dictionaryform; }

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
            updateNodeText( true);
        }
    }

    /**
     * Removes this annotation from the document. The document listener of the
     * annotation model will remove this node from the model in reaction to the change.
     */
    public void removeAnnotation() {
        try {
            JGlossDocument doc = ((JGlossDocument) annotation.getDocument());
            doc.setOuterHTML( annotation, word.getWord());
            // remove the newline which the stupid HTMLDocument.insertHTML insists on adding
            doc.remove( annotation.getEndOffset()-1, 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set the reading and dictionary form from the annotation.
     *
     * @param annotation The annotation from which to take the word and reading.
     */
    public void setWordFrom( AbstractAnnotation annotation) {
        String newword = annotation.getWord();
        String newreading = annotation.getReading();
        if (newreading == null ||
            newreading.equals( newword))
            newreading = "";
        dictionaryform.setWord( newword);
        dictionaryform.setReading( newreading);

        // only set the reading if the annotated word is not all hiragana
        boolean isHiragana = true;
        String annotatedWord = word.getWord();
        for ( int i=0; i<annotatedWord.length(); i++) {
            if (!StringTools.isHiragana( annotatedWord.charAt( i))) {
                isHiragana = false;
                break;
            }
        }
        if (!isHiragana)
            word.setReading( newword, newreading);
    }

    /**
     * Set the reading, translation and dictionary form to the first dictionary entry. Does nothing
     * if no annotation exists.
     */
    public void selectFirstAnnotation() {
        if (getChildCount() <= DICTIONARY_NODE_OFFSET) // no annotation
            return;

        // No child bounds checking is done because if the tree is constructed correctly,
        // all child accesses should succeed.

        TreeNode tn = getChildAt( DICTIONARY_NODE_OFFSET); // first dictionary node

        TreeNode tnc = tn.getChildAt( 0);
        if (tnc instanceof TranslationNode) {
            setWordFrom( ((TranslationNode) tnc).getTranslation());
            
            // If there are several readings with identical translations, the
            // TranslationLeafNodes are only attached to the last TranslationNode.
            // Search this TranslationNode.
            
            int i = 1;
            while (tnc.getChildCount() == 0)
                tnc = (TranslationNode) tn.getChildAt( i++);
            
            tnc = tnc.getChildAt( 0); // first translationLeafNode
            getTranslationNode().setText( ((TranslationLeafNode) tnc).getTranslation());
        }
        else if (tnc instanceof ReadingAnnotationNode)
            setWordFrom( ((ReadingAnnotationNode) tnc).getReading());
    }

    /**
     * Sets the new node text. This should be called if the attributes of the annotation element
     * have changed (for example after being hidden).
     */
    private void updateNodeText( boolean fireNodeChanged) {
        String newNodeText = word.getWord();
        String reading = word.getReading();
        if (reading.length() > 0)
            newNodeText += " \uff08" + reading + "\uff09";
        String translation = getTranslationNode().getText();
        if (translation.length() > 0)
            newNodeText += " " + translation;

        if (isHidden())
            newNodeText += JGloss.messages.getString( "annotationeditor.entry.hidden");

        if (!newNodeText.equals( nodeText)) {
            nodeText = newNodeText;
            if (fireNodeChanged)
                getRootNode().getModel().nodeChanged( this);
        }
    }
} // class AnnotationNode
