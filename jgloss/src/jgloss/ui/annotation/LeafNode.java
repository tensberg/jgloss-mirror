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

import javax.swing.tree.TreeNode;

/**
 * Implementation of <CODE>TreeNode</CODE> which is suitable for nodes which don't have children.
 *
 * @author Michael Koch
 */
abstract class LeafNode implements TreeNode {
    /**
     * Parent of this node.
     */
    protected InnerNode parent;

    /**
     * Creates a new leaf node with the specified parent.
     *
     * @param parent Parent of this node.
     */
    protected LeafNode( InnerNode parent) {
        this.parent = parent;
    }

    /**
     * Returns an enumeration of this nodes children. Returns <CODE>null</CODE> since
     * <CODE>LeafNodes</CODE> never have children.
     *
     * @return <CODE>null</CODE> since
     *         <CODE>LeafNodes</CODE> never have children.
     */
    public Enumeration children() { return null; }
    /**
     * Returns <CODE>false</CODE> since
     * <CODE>LeafNodes</CODE> never have children.
     *
     * @return <CODE>false</CODE> since
     *         <CODE>LeafNodes</CODE> never have children.
     */
    public boolean getAllowsChildren() { return false; }
    /**
     * Returns the child at the specified index. This will throw an <CODE>IllegalArgumentException</CODE>
     * since <CODE>LeafNodes</CODE> never have children.
     *
     * @param childIndex Index of the child.
     * @return Nothing.
     * @exception IllegalArgumentException always, since <CODE>LeafNodes</CODE> never have children.
     */
    public TreeNode getChildAt( int childIndex) { 
        throw new IllegalArgumentException( "This node is a leaf");
    }
    /**
     * Returns the number of children.
     *
     * @return Always <CODE>0</CODE>, since <CODE>LeafNodes</CODE> never have children..
     */
    public int getChildCount() { return 0; }
    /**
     * Returns the index of the specified node in the child array.
     *
     * @param node A tree node.
     * @return Always <CODE>-1</CODE>, since <CODE>LeafNodes</CODE> never have children.
     */
    public int getIndex( TreeNode node) { return -1; }
    /**
     * Returns the parent of this node.
     *
     * @return The parent of this node.
     */
    public TreeNode getParent() { return parent; }
    /**
     * Returns <CODE>true</CODE> if this node is a leaf.
     *
     * @return Always <CODE>true</CODE>.
     */
    public boolean isLeaf() { return true; }

    /**
     * Returns the root node of the tree to which this node belongs.
     *
     * @return The root node.
     */
    public RootNode getRootNode() { return parent.getRootNode(); }
} // class LeafNode
