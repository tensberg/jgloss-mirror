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

import java.util.*;

import javax.swing.tree.*;

/**
 * Implementation of <CODE>TreeNode</CODE> which is suitable for nodes which have children.
 *
 * @author Michael Koch
 */
abstract class InnerNode implements TreeNode {
    /**
     * Parent of this node.
     */
    protected InnerNode parent;
    /**
     * Children of this node.
     */
    protected Vector children;

    /**
     * Creates a new inner node with the specified parent and children.
     *
     * @param parent Parent of this node.
     * @param children Children of this node. Must not be <CODE>null</CODE>.
     */
    protected InnerNode( InnerNode parent, Vector children) {
        this.parent = parent;
        this.children = children;
    }

    /**
     * Returns an enumeration over all children.
     *
     * @return An enumeration over all children.
     */
    public Enumeration children() { return children.elements(); }
    /**
     * Returns if this node allows children. This will always return <CODE>true</CODE>.
     *
     * @return <CODE>true</CODE> since this node always allows children.
     */
    public boolean getAllowsChildren() { return true; }
    /**
     * Returns the child at the given index.
     *
     * @param childIndex Index of the child in the vector of children.
     * @return The child.
     * @exception java.lang.ArrayIndexOutOfBoundsException if no child exists at the specified
     *            index.
     */
    public TreeNode getChildAt( int childIndex) { 
        return (TreeNode) children.elementAt( childIndex);
    }
    /**
     * Returns the number of children of this node.
     *
     * @return The number of children.
     */
    public int getChildCount() { return children.size(); }
    /**
     * Returns the index of a child in the vector of children.
     *
     * @param node Node to search.
     * @return The index in the vector of children, or -1 if the node is not found.
     */
    public int getIndex( TreeNode node) { return children.indexOf( node); }
    /**
     * Returns the parent of this node.
     *
     * @return The parent of this node.
     */
    public TreeNode getParent() { return parent; }
    /**
     * Returns <CODE>true</CODE> if this node does not have children.
     *
     * @return <CODE>true</CODE> if this node does not have children.
     */
    public boolean isLeaf() { return (getChildCount() == 0); }

    /**
     * Adds a node to the vector of children.
     *
     * @param node The node to add to the children.
     */
    public void add( TreeNode node) {
        children.add( node);
    }

    /**
     * Inserts a node in the vector of children.
     *
     * @param node The node to insert.
     * @param pos Index at which to insert the node.
     * @exception java.lang.ArrayIndexOutOfBoundsException if the index was invalid.
     */
    public void insert( TreeNode node, int pos) {
        children.insertElementAt( node, pos);
    }

    /**
     * Removes the child at the given index. This will call the <CODE>nodesWereRemoved</CODE> method of
     * the annotation model.
     *
     * @param index Index of the child to remove.
     * @exception java.lang.ArrayIndexOutOfBoundsException if the index was invalid.
     */
    public void remove( int index) {
        remove( index, index);
    }

    /**
     * Removes an interval of children. This will call the <CODE>nodesWereRemoved</CODE> method of
     * the annotation model.
     *
     * @param start Index of the first child to remove.
     * @param end Index of the last child to remove.
     * @exception java.lang.ArrayIndexOutOfBoundsException if the index range was invalid.
     */
    public void remove( int start, int end) {
        int[] indices = new int[end-start+1];
        Object[] childArray = new Object[end-start+1];

        for ( int i=end; i>=start; i--) {
            indices[i-start] = i;
            childArray[i-start] = children.remove( i);
            getRootNode().getModel().nodesWereRemoved( this, indices, childArray);
        }
    }

    /**
     * Returns a list with all nodes from this node to the last descendant.
     *
     * @return A list with all nodes from this node to the last descendant.
     */
    public LinkedList getPathToLastDescendant() {
        if (children.isEmpty()) {
            LinkedList l = new LinkedList();
            l.add( this);
            return l;
        }

        Object child = children.lastElement();
        if (child instanceof InnerNode) {
            LinkedList l = ((InnerNode) child).getPathToLastDescendant();
            l.addFirst( this);
            return l;
        }
        else {
            LinkedList l = new LinkedList();
            l.add( this);
            l.add( child);
            return l;
        }
    }

    /**
     * Returns the root node of the tree to which this node belongs.
     *
     * @return The root node.
     */
    public RootNode getRootNode() {
        return parent.getRootNode();
    }
} // class InnerNode
