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

/**
 * A node which displays a static description and some editable text.
 *
 * @author Michael Koch
 */
public class EditableTextNode extends LeafNode {
    /**
     * The text which can be changed.
     */
    protected String text;
    /**
     * The static descriptive text of this node.
     */
    protected String description;

    /**
     * Creates a new reading or annotation annotation node which wraps the given element.
     *
     * @param parent Parent of this node. Usually a {@link AnnotationNode AnnotationNode}.
     * @param description Immutable description for this node.
     * @param text Editable text.
     */
    public EditableTextNode( InnerNode parent, String description, String text) {
        super( parent);
        
        if (text == null)
            text = "";
        this.text = text;
        this.description = description;
    }

    /**
     * Returns a string representation of this node. This is the description plus the 
     * text.
     */
    public String toString() {
        return description + text;
    }

    /**
     * Returns the static descriptive text for this node. This does not contain the changeable
     * text part.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the changeable text of this node.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the changeable text to a new value. A <code>nodeChanged</code> event will be
     * fired for this node.
     *
     * @param text The new value.
     */
    public void setText( String text) {
        if (text == null)
            text = "";

        this.text = text;
        getRootNode().getModel().nodeChanged( this);
    }
} // class ReadingTranslationNode
