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

import jgloss.JGloss;
import jgloss.ui.doc.JGlossDocument;

import javax.swing.text.*;

/**
 * A dictionary form node displays the dictionary form and reading for an annotated word.
 * Its parent is a {@link AnnotationNode AnnotationNode} and it has two 
 * {@link EditableTextNode EditableTextNodes} as children which display word and reading.
 *
 * @author Michael Koch
 */
public class DictionaryFormNode extends InnerNode {
    /**
     * The text which the node displays in the tree.
     */
    private String nodeText;

    /**
     * Child which displays the word in dictionary form.
     */
    private EditableTextNode wordNode;
    /**
     * Child which displays the reading in dictionary form.
     */
    private EditableTextNode readingNode;

    /**
     * Creates a new reading or annotation annotation node which wraps the given element.
     *
     * @param parent Parent of this node. Usually a {@link AnnotationNode AnnotationNode}.
     */
    public DictionaryFormNode( AnnotationNode parent) {
        super( parent, new java.util.Vector( 2));
        
        AttributeSet attr = parent.getAnnotationElement().getAttributes();
        String word = (String) attr.getAttribute( JGlossDocument.DICTIONARY_WORD);
        if (word == null)
            word = parent.getWordText(); // per definition
        String reading = (String) attr.getAttribute( JGlossDocument.DICTIONARY_READING);
        if (reading == null)
            reading = parent.getReadingNode().getText(); // per definition

        wordNode = new EditableTextNode( this, JGloss.messages.getString
                                         ( "annotationeditor.dictionaryform.word"), word) {
                public void setText( String text) {
                    super.setText( text);
                    updateNodeText();

                    // Update the dictionary word attribute of the annotation element.
                    // The attribute will only be set if the dictionary form is different from
                    // the form in the document.
                    AnnotationNode anno = (AnnotationNode) DictionaryFormNode.this.getParent();
                    if (text.equals( anno.getWordText()))
                        text = null; // delete attribute
                    Element ae = anno.getAnnotationElement();
                    ((JGlossDocument) ae.getDocument()).setAttribute
                        ( (MutableAttributeSet) ae.getAttributes(),
                          JGlossDocument.DICTIONARY_WORD, text);
                }
            };
        children.add( wordNode);
        readingNode = new EditableTextNode( this, JGloss.messages.getString
                                            ( "annotationeditor.dictionaryform.reading"), reading) {
                public void setText( String text) {
                    super.setText( text);
                    updateNodeText();

                    // Update the dictionary reading attribute of the annotation element.
                    AnnotationNode anno = (AnnotationNode) DictionaryFormNode.this.getParent();
                    // don't test for equality with the reading here since the reading might change
                    Element ae = anno.getAnnotationElement();
                    ((JGlossDocument) ae.getDocument()).setAttribute
                        ( (MutableAttributeSet) ae.getAttributes(),
                          JGlossDocument.DICTIONARY_READING, text);
                }
            };
        children.add( readingNode);
        updateNodeText();
    }

    /**
     * Returns a string representation of this node. This is a short description plus
     * the dictionary word and reading.
     */
    public String toString() {
        return nodeText;
    }

    /**
     * Returns the dictionary form of the word represented by this node.
     */
    public String getWord() {
        return wordNode.getText();
    }

    /**
     * Sets the dictionary form of the word represented by this node.
     */
    public void setWord( String word) {
        wordNode.setText( word);
    }

    /**
     * Returns the dictionary form of the reading of the word represented by this node.
     */
    public String getReading() {
        return readingNode.getText();
    }

    /**
     * Sets the dictionary form of the reading of the word represented by this node.
     */
    public void setReading( String reading) {
        readingNode.setText( reading);
    }

    /**
     * Updates the node text. This method should be called after the dictionary word or
     * reading has changed. A <code>nodeChanged</code> event will be fired for this node.
     */
    private void updateNodeText() {
        nodeText = JGloss.messages.getString( "annotationeditor.dictionaryform");

        String wordreading = wordNode.getText();
        if (readingNode.getText().length() > 0) {
            if (wordreading.length() > 0)
                wordreading += " ";
            wordreading += "\uff08" + readingNode.getText() + "\uff09";
        }
        if (wordreading.length() == 0)
            wordreading = JGloss.messages.getString( "annotationeditor.dictionaryform.none");
        nodeText += wordreading;

        getRootNode().getModel().nodeChanged( this);
    }
} // class ReadingTranslationNode
