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

import jgloss.dictionary.*;
import jgloss.parser.*;

import java.util.*;

import javax.swing.tree.TreeNode;

/**
 * Node for a dictionary entry. Parent will be a {@link DictionaryNode DictionaryNode}, children
 * are {@link TranslationLeafNode TranslationLeafNodes}. The node will display the word and the
 * reading entry as they appear in the dictionary. Each translation will have a child of this
 * node.
 *
 * @author Michael Koch
 */
public class TranslationNode extends InnerNode {
    /**
     * The word and reading as it appears in the dictionary entry.
     */
    private String word;
    /**
     * Translation this node represents.
     */
    private Translation translation;
    /**
     * The reading which will be used as reading annotation for the annotation node ancestor.
     * This will be the reading only of the kanji part of the word.
     */
    private String reading;

    /**
     * Creates a new node for the given translation. For each translation word a 
     * {@link TranslationLeafNode TranslationLeafNode} child will be added. 
     *
     * @param parent Parent of this node. Should be a {@link DictionaryNode DictionaryNode}.
     * @param translation The translation this node represents.
     */
    public TranslationNode( InnerNode parent, Translation translation) {
        super( parent, null);

        this.translation = translation;
        DictionaryEntry d = translation.getDictionaryEntry();
        word = d.getWord();
        if (d.getReading() != null) 
            word += "\uff08" + d.getReading() + "\uff09"; // Japanese brackets
        children = new Vector( d.getTranslations().size());
        for ( Iterator i=d.getTranslations().iterator(); i.hasNext(); )
            children.addElement( new TranslationLeafNode( this, i.next().toString()));

        // construct the reading. This is what will be used when selecting this translation as
        // reading annotation.
        if (translation.getConjugation() == null) {
            reading = d.getReading(); // may be null for katakana
        }
        else {
            word += " (" + translation.getConjugation().getType() + ")";
            reading = d.getReading(); // may be null if inflected form equals dictionary form
                                      // for annotated hiragana words
            // cut off inflection
            if (reading != null)
                reading = reading.substring( 0, reading.length() - 
                                             translation.getConjugation().getDictionaryForm().length());
        }
    }

    /**
     * Returns a string representation of this node. This is the word of the dictionary entry.
     *
     * @return A string representation.
     */
    public String toString() { return word; }
    /**
     * Returns the translation annotation this node represents.
     *
     * @return The translation annotation this node represents.
     */
    public Translation getTranslation() { return translation; }
    /**
     * Returns the reading of the kanji part of this word.
     *
     * @return The reading of this entry.
     */
    public String getReading() { return reading; }

    /**
     * Returns the descriptive text of this translation. This is the word of this entry plus all
     * translation.
     *
     * @return The descriptive text.
     */
    public String getText() {
        String out = word;
        for ( Enumeration e=children(); e.hasMoreElements(); ) {
            out += "\n    " + e.nextElement().toString();
        }
        return out;
    }
} // class TranslationNode
