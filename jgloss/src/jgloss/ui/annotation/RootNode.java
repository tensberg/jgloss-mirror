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

import jgloss.ui.*;
import jgloss.ui.doc.*;

import java.util.*;

import javax.swing.text.Element;
import javax.swing.tree.TreeNode;

/**
 * This is the root node of an annotation tree. Its children are {@link AnnotationNode AnnotationNodes}.
 *
 * @author Michael Koch
 */
class RootNode extends InnerNode {
    /**
     * If an annotation is not found at the specified location, return the node closest to the
     * left. If {@link #findAnnotationIndex(int,int) findAnnotationIndex} is called, this will 
     * return -1 if there is no annotation to the left of the index.
     */
    public static final int BIAS_PREVIOUS = 0;
    /**
     * If an annotation is not found at the specified location, return the node closest to the
     * right. If {@link #findAnnotationIndex(int,int) findAnnotationIndex} is called, this will 
     * return {@link InnerNode#getChildCount() getChildCount()} if there is no annotation to 
     * the right of the index.
     */
    public static final int BIAS_NEXT = 1;
    /**
     * If an annotation is not found at the specified location, return <CODE>null</CODE> or
     * {@link #NO_SUCH_ELEMENT NO_SUCH_ELEMENT}.
     */
    public static final int BIAS_NONE = 2;
    /**
     * Value returned by {@link #findAnnotationIndex(int,int) findAnnotationIndex} if no annotation
     * was found at the specified location and {@link #BIAS_NONE BIAS_NONE} was used.
     */
    public static final int NO_SUCH_ELEMENT = -2;

    /**
     * Index in the array of child trees at which the last successful search for an
     * annotation node terminated. This is
     * remembered because it is propable that the next search will lead to the same node.
     */
    private int searchindex = -1;

    /**
     * The model to which this tree belongs.
     */
    private AnnotationModel model;

    /**
     * Creates a new root node without children.
     */
    public RootNode() {
        super( null, new Vector( 100, 100));
    }

    /**
     * Sets the model to which the tree under this node belongs.
     *
     * @param model The model which this node belongs to.
     */
    public void setModel( AnnotationModel model) {
        this.model = model;
    }

    /**
     * Returns the annotation model this node belongs to.
     *
     * @return The annotation model this node belongs to.
     */
    public AnnotationModel getModel() {
        return model;
    }
    
    /**
     * Returns a string representation of this node. Note that this node should never
     * be visible in the tree.
     *
     * @return A string representation of this node.
     */
    public String toString() { return "AnnotationEditor root"; }

    /**
     * Removes the specified child from the array of children.
     *
     * @param child The child to remove.
     */
    public void remove( TreeNode child) {
        int i = children.indexOf( child);
        if (i != -1)
            remove( i);
    }

    /**
     * Removes the <CODE>AnnotationNode</CODE> which wraps the given annotation element from
     * the array of children.
     *
     * @param e The annotation element the node of which should be removed.
     */
    public void remove( Element e) {
        for ( int i=0; i<children.size(); i++) {
            if (((AnnotationNode) children.elementAt( i)).getAnnotationElement() == e) {
                remove( i);
                break;
            }
        }
    }

    /**
     * Searches for the annotation node which wraps the annotation element which
     * contains the position in the document. If there is no annotation element at
     * the specified location, the node returned will depend on the bias.
     *
     * @param pos A position in the document.
     * @param bias Bias if no annotation element exists at the specified location. One of
     *             {@link #BIAS_PREVIOUS BIAS_PREVIOUS}, {@link #BIAS_NEXT BIAS_NEXT} or
     *             {@link #BIAS_NONE BIAS_NONE}.
     * @return The annotation node for the specified position and bias.
     */
    public AnnotationNode findAnnotation( int pos, int bias) {
        int p = findAnnotationIndex( pos, bias);
        if (p==NO_SUCH_ELEMENT || p<0 || p>=children.size())
            return null;
        
        return (AnnotationNode) children.elementAt( p);
    }
    
    /**
     * Returns the child index of the annotation node which wraps the annotation element which
     * contains the position in the document. If there is no annotation element at
     * the specified location, the index returned will depend on the bias.
     *
     * @param pos A position in the document.
     * @param bias Bias if no annotation element exists at the specified location. One of
     *             {@link #BIAS_PREVIOUS BIAS_PREVIOUS}, {@link #BIAS_NEXT BIAS_NEXT} or
     *             {@link #BIAS_NONE BIAS_NONE}.
     * @return The child index for the specified position and bias.
     */
    public int findAnnotationIndex( int pos, int bias) {
        if (children.size() == 0) {
            switch (bias) {
            case BIAS_NONE:
                return NO_SUCH_ELEMENT;
            case BIAS_PREVIOUS:
                return -1;
            case BIAS_NEXT:
                return 0;
            default:
                throw new IllegalArgumentException( "Invalid bias " + bias);
            }
        }
         
        // the finishing index of the previous search is remebered. This will make the
        // search faster if the method is called for close locations. Since this method
        // is often called after mouse movement events, this is likely to be the case.
        if (searchindex<0 || searchindex>=children.size())
            searchindex = children.size() / 2;

        // do a binary search
        AnnotationNode out;
        boolean found = false;
        int min = 0;
        int max = children.size()-1;
        do {
            out = (AnnotationNode) children.elementAt( searchindex);
            if (pos < out.getAnnotationElement().getStartOffset()) {
                if (searchindex == min) // no annotation for this position
                    switch (bias) {
                    case BIAS_PREVIOUS:
                        return searchindex - 1;
                    case BIAS_NEXT:
                        return searchindex;
                    case BIAS_NONE:
                        return NO_SUCH_ELEMENT;
                    default:
                        throw new IllegalArgumentException( "Invalid bias " + bias);
                    }
                else if (((AnnotationNode) children.elementAt( searchindex-1))
                         .getAnnotationElement().getEndOffset()-1 < pos)
                    // pos is between two annotation elements
                    switch (bias) {
                    case BIAS_PREVIOUS:
                        return searchindex - 1;
                    case BIAS_NEXT:
                        return searchindex;
                    case BIAS_NONE:
                        return NO_SUCH_ELEMENT;
                    default:
                        throw new IllegalArgumentException( "Invalid bias " + bias);
                    }
                else {
                    max = searchindex - 1;
                    searchindex = min + (searchindex-min) / 2;
                }
            }
            else if (pos > out.getAnnotationElement().getEndOffset()-1) {
                if (searchindex == max) // no annotation for this position
                    switch (bias) {
                    case BIAS_PREVIOUS:
                        return searchindex;
                    case BIAS_NEXT:
                        return searchindex + 1;
                    case BIAS_NONE:
                        return NO_SUCH_ELEMENT;
                    default:
                        throw new IllegalArgumentException( "Invalid bias " + bias);
                    }
                else if (((AnnotationNode) children.elementAt( searchindex+1))
                         .getAnnotationElement().getStartOffset() > pos)
                    switch (bias) {
                    case BIAS_PREVIOUS:
                        return searchindex;
                    case BIAS_NEXT:
                        return searchindex + 1;
                    case BIAS_NONE:
                        return NO_SUCH_ELEMENT;
                    default:
                        throw new IllegalArgumentException( "Invalid bias " + bias);
                    }
                else {
                    min = searchindex + 1;
                    searchindex = searchindex + (max-searchindex)/2 + 1;
                }
            }
            else
                found = true;
        } while (!found);

        return searchindex;
    }

    /**
     * Returns the root node of the tree. The node will return itself.
     *
     * @return The root of the tree (which is this node).
     */
    public RootNode getRootNode() { return this; }    

    /**
     * Returns an iteration over all children.
     *
     * @return An iteration over all children.
     */
    public Iterator getChildren() {
        return children.iterator();
    }

} // class RootNode
