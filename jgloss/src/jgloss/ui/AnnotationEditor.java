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

package jgloss.ui;

import jgloss.*;
import jgloss.ui.doc.*;
import jgloss.ui.annotation.*;
import jgloss.dictionary.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTreeUI;

/**
 * Editor for annotations in a document. For each annotation the editor will show all
 * reading annotation and translations which were found during parsing. The user can select
 * which reading and which translation should be used, or edit a new value by hand.
 *
 * @author Michael Koch
 */
public class AnnotationEditor extends JTree implements TreeSelectionListener, MouseListener {
    /**
     * Editor for the reading or translation for a specific annotation. The editor consist of a
     * static label which describes the type of entry and an editable component for the
     * actual entry.
     *
     * @author Michael Koch
     */
    private class AnnotationTreeCellEditor extends DefaultTreeCellEditor {
        private Box box;
        private JLabel label;
        private Component glue;

        /**
         * Creates a new editor with its look taken from the component returned by the
         * specified renderer.
         *
         * @param renderer The tree cell renderer on which the look for the editor should be based.
         */
        public AnnotationTreeCellEditor( DefaultTreeCellRenderer renderer) {
            super( AnnotationEditor.this, renderer);
        }

        /**
         * Creates a container in which the editing component will be placed. Additionally the
         * descriptive label will be placed in the container.
         *
         * @return The newly created container.
         */
        protected Container createContainer() {
            Container c = super.createContainer();
            box = Box.createHorizontalBox();
            label = new JLabel();
            return c;
        }

        public Component getTreeCellEditorComponent( JTree tree, Object value,
                                                     boolean isSelected,
                                                     boolean expanded, boolean leaf, int row) {
            Container c = (Container) super.getTreeCellEditorComponent( tree, value, isSelected, expanded,
                                                                        leaf, row);

            canEdit = canEdit && (value instanceof ReadingTranslationNode);
            if (canEdit) {
                ReadingTranslationNode node = (ReadingTranslationNode) value;
                label.setFont( c.getFont());
                label.setForeground( c.getForeground());
                label.setText( node.getDescription());
                String text = node.getText();

                // replace the single whitespace which is only there for layout purposes with 
                // the empty string.
                if (text.equals( " "))
                    text = "";
                ((JTextField) editingComponent).setText( text);
                
                box.removeAll();
                box.add( label);
                box.add( editingComponent); // editingComponent was reset by superclass
                box.setSize( Math.min( box.getPreferredSize().width, 200),
                             box.getPreferredSize().height);
                editingComponent = box; // will be added to container by superclass
            }

            return c;
        }
    }

    /**
     * The model which this JTree displays. Kept here as member variable to prevent the constant
     * typecasting.
     */
    private AnnotationModel model;
    /**
     * JGlossEditor which displays the document containing the annotations managed by this 
     * AnnotationEditor.
     */
    private JGlossEditor docpane;
    /**
     * The EditorKit which created the document.
     */
    private JGlossEditorKit kit;
    /**
     * The document which is displayed by <CODE>docpane</CODE> and contains the annotations
     * managed by this AnnotationEditor.
     */
    private JGlossDocument doc;

    /**
     * Action which sets the reading of the selected annotation to the currently selected reading.
     */
    private Action useReadingAction;
    /**
     * Action which sets the translation of the selected annotation to the currently selected
     * translation.
     */
    private Action useTranslationAction;
    /**
     * Action which hides the currently selected annotation.
     */
    private Action hideAction;
    /**
     * Action which removes the currently selected annotation.
     */
    private Action removeAction;
    /**
     * Action which will hide all annotations which are a duplicate of a previous annotation.
     */
    private Action hideDuplicatesAction;
    /**
     * Action which will un-hide all annotations.
     */
    private Action unhideAction;

    /**
     * Menu which contains the annotation-specific actions.
     */
    private JMenu menu;
    /**
     * Popup menu which contains annotation-specific actions.
     */
    private JPopupMenu pmenu;

    /**
     * An Enumeration containing no elements.
     */
    private final static Enumeration EMPTY_ENUMERATION = new Vector(1).elements();

    /**
     * Flag if this is the first time the tree is expanded. This is used for optimizing the
     * <CODE>getExpandedDescendants</CODE> method.
     */
    private boolean isInitialExpansion = true;
    /**
     * Flag if getExpandedDescendants should return an empty enumeration. This is used together
     * with <CODE>isInitialExpansion</CODE> to speed up the initial expansion of the tree.
     */
    private boolean returnEmptyEnumeration = false;

    /**
     * Creates a new annotation editor. The editor is initially empty and not associated
     * to a document containing annotations. Call {@link #setDocument(Element,JGlossEditor) setDocument}
     * to set the document to edit. The constructor will create a new empty AnnotationModel.
     */
    public AnnotationEditor() {
        setLargeModel( true);
        model = new AnnotationModel() {
                public void valueForPathChanged( TreePath path, Object newValue) {
                    ((ReadingTranslationNode) path.getLastPathComponent()).setText( newValue.toString());
                }
            };
        setModel( model);
        ((DefaultTreeSelectionModel) getSelectionModel())
            .setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION);
        model.addTreeModelListener( new TreeModelListener() {
                private void checkCollapseNode( TreePath tp) {
                    // collapse an annotation node if it was hidden through a user action
                    if (tp.getLastPathComponent() instanceof AnnotationNode) {
                        if (((AnnotationNode) tp.getLastPathComponent()).isHidden()) {
                            collapsePath( tp);
                        }
                        else {
                            expandAll( tp);
                        }
                    }
                }

                public void treeNodesChanged( TreeModelEvent e) {
                    TreePath tp = e.getTreePath();
                    if (e.getChildren() == null) {
                        checkCollapseNode( tp);
                    }
                    else {
                        Object[] children = e.getChildren();
                        for ( int i=0; i<children.length; i++)
                            checkCollapseNode( tp.pathByAddingChild( children[i]));
                    }
                }

                public void treeNodesInserted( TreeModelEvent e) {}
                public void treeNodesRemoved( TreeModelEvent e) {}
                public void treeStructureChanged( TreeModelEvent e) {}
            });

        // Prevent the tree cell renderer from drawing the collapse/uncollapse icons in the
        // tree because they don't make much sense for this application.
        // The DefaultTreeCellRenderer seems to accept null for a new value, but it is not
        // documented, so I use an empty icon.
        Icon nullIcon = new Icon() {
                public int getIconWidth() { return 0; }
                public int getIconHeight() { return 0; }
                public void paintIcon( java.awt.Component c, java.awt.Graphics g, int x, int y) {}
            };
        DefaultTreeCellRenderer r = new DefaultTreeCellRenderer();
        r.setLeafIcon( nullIcon);
        r.setOpenIcon( nullIcon);
        r.setClosedIcon( nullIcon);
        this.setCellRenderer( r);
        if (this.getUI() instanceof BasicTreeUI) {
            BasicTreeUI ui = (BasicTreeUI) this.getUI();
            ui.setCollapsedIcon( nullIcon);
            ui.setExpandedIcon( nullIcon);
        }
        this.setCellEditor( new AnnotationTreeCellEditor( r));
        setEditable( true);

        this.setRootVisible( false);
        this.setShowsRootHandles( false);

        addTreeSelectionListener( this);

        // use currently selected reading for this annotation
        useReadingAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    String reading;
                    Parser.TextAnnotation annotation;
                    if (tn instanceof TranslationNode) {
                        annotation = ((TranslationNode) tn).getTranslation();
                        reading = ((TranslationNode) tn).getReading();
                    }
                    else {
                        annotation = ((ReadingAnnotationNode) tn).getReading();
                        reading = ((ReadingAnnotationNode) tn).getReadingText();
                    }

                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();
                    ((AnnotationNode) tn).getReadingNode().setText( reading);
                    ((AnnotationNode) tn).setLinkedAnnotation( annotation);
                }
            };
        JGlossFrame.initAction( useReadingAction, "annotationeditor.menu.usereading");
        useReadingAction.setEnabled( false);
        // use currently selected translation for this annotation
        useTranslationAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    String translation = ((TranslationLeafNode) tn).getTranslation();

                    Parser.TextAnnotation annotation = ((TranslationNode) tn.getParent())
                        .getTranslation();

                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();
                    ((AnnotationNode) tn).getTranslationNode().setText( translation);
                    ((AnnotationNode) tn).setLinkedAnnotation( annotation);
                }
            };
        JGlossFrame.initAction( useTranslationAction, "annotationeditor.menu.usetranslation");
        useTranslationAction.setEnabled( false);
        // hide/unhide currently selected annotation
        hideAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();
                    AnnotationNode krn = (AnnotationNode) tn;
                    krn.setHidden( !krn.isHidden());
                    if (getSelectionPath()!=null)
                        updateHideAction( (TreeNode) getSelectionPath().getLastPathComponent());
                }
            };
        JGlossFrame.initAction( hideAction, "annotationeditor.menu.hide");
        hideAction.setEnabled( false);
        // remove the currently selected annotation
        removeAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();
                    AnnotationNode krn = (AnnotationNode) tn;
                    int index = model.getIndexOfChild( model.getRoot(), krn);
                    krn.removeAnnotation();
                    // select the following annotation node (or previous if it was the last)
                    if (index >= model.getChildCount( model.getRoot()))
                        index--;
                    if (index >= 0) {
                        TreeNode[] path = new TreeNode[2];
                        path[0] = (TreeNode) model.getRoot();
                        path[1] = (TreeNode) model.getChild( model.getRoot(), index);
                        setSelectionPath( new TreePath( path));
                    }
                }
            };
        JGlossFrame.initAction( removeAction, "annotationeditor.menu.remove");
        removeAction.setEnabled( false);
        // Hide all annotations which are a duplicate of a previous annotation.
        // A duplicate has the same kanji, reading and translation.
        hideDuplicatesAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    // stores already seen annotations
                    Set nodes = new HashSet();
                    for ( Iterator i=model.getAnnotationNodes(); i.hasNext(); ) {
                        AnnotationNode node = (AnnotationNode) i.next();
                        if (!node.isHidden()) {
                            String key = node.getKanjiText() + "%" +
                                node.getReadingNode().getText() + "%" +
                                node.getTranslationNode().getText();
                            if (nodes.contains( key))
                                node.setHidden( true, false);
                            else
                                nodes.add( key);
                        }
                    }
                    // force docpane to be re-layouted. Unfortunately I have not found a better
                    // way to do this.
                    doc.getStyleSheet().addRule( AnnotationTags.ANNOTATION.getId() + " { }");
                }
            };
        JGlossFrame.initAction( hideDuplicatesAction, "annotationeditor.menu.hideduplicates");
        hideDuplicatesAction.setEnabled( false);
        // make all hidden annotations visible
        unhideAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    for ( Iterator i=model.getAnnotationNodes(); i.hasNext(); ) {
                        AnnotationNode node = (AnnotationNode) i.next();
                        if (node.isHidden()) {
                            node.setHidden( false, false);
                        }
                    }
                    // force docpane to be re-layouted. Unfortunately I have not found a better
                    // way to do this.
                    doc.getStyleSheet().addRule( AnnotationTags.ANNOTATION.getId() + " { }");
                }
            };
        JGlossFrame.initAction( unhideAction, "annotationeditor.menu.unhide");
        unhideAction.setEnabled( false);

        menu = new JMenu( JGloss.messages.getString( "annotationeditor.menu.title"));
        pmenu = new JPopupMenu();
        menu.add( JGlossFrame.createMenuItem( useReadingAction));
        pmenu.add( useReadingAction);
        menu.add( JGlossFrame.createMenuItem( useTranslationAction));
        pmenu.add( useTranslationAction);
        menu.add( JGlossFrame.createMenuItem( hideAction));
        pmenu.add( hideAction);
        menu.add( JGlossFrame.createMenuItem( removeAction));
        pmenu.add( removeAction);
        menu.addSeparator();
        //pmenu.addSeparator();
        menu.add( JGlossFrame.createMenuItem( hideDuplicatesAction));
        //pmenu.add( hideDuplicatesAction); // this is not a context-sensitive action
        menu.add( JGlossFrame.createMenuItem( unhideAction));
        //pmenu.add( unhideAction);

        addMouseListener( this);

        // add keyboard shortcuts for editing the tree
        // select the following annotation
        Action nextAnnotationAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreePath tp = getSelectionPath();
                    TreeNode annotation = null;
                    if (tp != null) {
                        annotation = (TreeNode) tp.getLastPathComponent();
                        while (!(annotation instanceof AnnotationNode))
                            annotation = annotation.getParent();
                        int index = model.getIndexOfChild( model.getRoot(), annotation);
                        if (index < model.getChildCount( model.getRoot()))
                            annotation = (TreeNode) model.getChild( model.getRoot(), index+1);
                        else // last annotation, no new selection
                            annotation = null;
                     }
                    else if (!((TreeNode) model.getRoot()).isLeaf()) {
                        annotation = (TreeNode) model.getChild( model.getRoot(), 0);
                    }

                    if (annotation != null) {
                        LinkedList l = ((AnnotationNode) annotation).getPathToLastDescendant();
                        l.addFirst( model.getRoot());
                        TreeNode[] tn = new TreeNode[l.size()];
                        tn = (TreeNode[]) l.toArray( tn);
                        scrollPathToVisible( new TreePath( tn));

                        tn = new TreeNode[2];
                        tn[0] = (TreeNode) model.getRoot();
                        tn[1] = annotation;
                        tp = new TreePath( tn);
                        setSelectionPath( tp);
                        // make sure the viewport view is at x position 0 so the selected node is
                        // fully visible
                        ((JViewport) getParent()).setViewPosition( new Point
                            ( 0, ((JViewport) getParent()).getViewPosition().y));
                    }
                }
            };
        JGlossFrame.initAction( nextAnnotationAction, "annotationeditor.action.next");
        // select the previous annotation
        Action previousAnnotationAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreePath tp = getSelectionPath();
                    TreeNode annotation = null;
                    if (tp != null) {
                        annotation = (TreeNode) tp.getLastPathComponent();
                        while (!(annotation instanceof AnnotationNode))
                            annotation = annotation.getParent();
                        int index = model.getIndexOfChild( model.getRoot(), annotation);
                        if (index > 0)
                            annotation = (TreeNode) model.getChild( model.getRoot(), index-1);
                        else // first annotation, no new selection
                            annotation = null;
                     }
                    else if (!((TreeNode) model.getRoot()).isLeaf()) {
                        annotation = (TreeNode) model.getChild( model.getRoot(), 
                                                                model.getChildCount( model.getRoot())-1);
                    }

                    if (annotation != null) {
                        TreeNode[] tn = new TreeNode[2];
                        tn[0] = annotation.getParent();
                        tn[1] = annotation;
                        tp = new TreePath( tn);
                        setSelectionPath( tp);
                        scrollPathToVisible( tp);
                    }
                }
            };
        JGlossFrame.initAction( previousAnnotationAction, "annotationeditor.action.previous");
        // do something useful, based on the currenty selected node
        Action metaAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreePath tp = getSelectionPath();
                    if (tp != null) {
                        TreeNode node = (TreeNode) tp.getLastPathComponent();
                        if (node instanceof AnnotationNode)
                            hideAction.actionPerformed( e);
                        else if (node instanceof ReadingAnnotationNode ||
                                 node instanceof TranslationNode)
                            useReadingAction.actionPerformed( e);
                        else if (node instanceof TranslationLeafNode)
                            useTranslationAction.actionPerformed( e);
                        else if (node instanceof ReadingTranslationNode)
                            startEditingAtPath( tp);
                    }
                }
            };
        JGlossFrame.initAction( metaAction, "annotationeditor.action.meta");
        InputMap im = getInputMap();
        ActionMap am = getActionMap();
        im.put( (KeyStroke) nextAnnotationAction.getValue( Action.ACCELERATOR_KEY),
                nextAnnotationAction.getValue( Action.NAME));
        am.put( nextAnnotationAction.getValue( Action.NAME), nextAnnotationAction);
        im.put( (KeyStroke) previousAnnotationAction.getValue( Action.ACCELERATOR_KEY),
                previousAnnotationAction.getValue( Action.NAME));
        am.put( previousAnnotationAction.getValue( Action.NAME), previousAnnotationAction);
        im.put( (KeyStroke) metaAction.getValue( Action.ACCELERATOR_KEY),
                metaAction.getValue( Action.NAME));
        am.put( metaAction.getValue( Action.NAME), metaAction);
        im.put( KeyStroke.getKeyStroke( JGloss.messages.getString( "annotationeditor.action.remove.ak")),
                removeAction.getValue( Action.NAME));
        am.put( removeAction.getValue( Action.NAME), removeAction);
        im.put( KeyStroke.getKeyStroke( JGloss.messages.getString( "annotationeditor.action.hide.ak")),
                hideAction.getValue( Action.NAME));
        am.put( hideAction.getValue( Action.NAME), hideAction);
    }

    /**
     * Set the document the annotations of which will be managed by this editor. This method can
     * only be called once per instance of AnnotationEditor. This method will fill the AnnotationModel
     * with the annotations from this document.
     *
     * @param rootElement Root element of the document containing annotations.
     * @param docpane The editor which displays the annotated document.
     */
    public void setDocument( Element rootElement, JGlossEditor docpane) {
        this.docpane = docpane;
        this.kit = (JGlossEditorKit) docpane.getEditorKit();
        this.doc = (JGlossDocument) docpane.getDocument();
        hideDuplicatesAction.setEnabled( true);
        unhideAction.setEnabled( true);
        model.setDocument( doc);
    }

    /**
     * Expands all paths in the tree so that all nodes are visible.
     */
    public synchronized void expandAll() {
        if (isInitialExpansion)
            returnEmptyEnumeration = true;

        if (!((TreeNode) model.getRoot()).isLeaf())
            expandAll( new TreePath( model.getRoot()));

        if (isInitialExpansion) {
            isInitialExpansion = false;
            returnEmptyEnumeration = false;
        }
    }

    /**
     * Expands all descendants of all annotation nodes which are not hidden.
     */
    public synchronized void expandNonHidden() {
        if (isInitialExpansion)
            returnEmptyEnumeration = true;

        for ( Iterator i=model.getAnnotationNodes(); i.hasNext(); ) {
            AnnotationNode a = (AnnotationNode) i.next();
            if (!a.isHidden())
                expandAll( a);
        }

        if (isInitialExpansion) {
            isInitialExpansion = false;
            returnEmptyEnumeration = false;
        }
    }

    /**
     * Expands all paths in the tree passing through a node.
     *
     * @param node Node which will be expanded.
     */
    public void expandAll( TreeNode node) {
        LinkedList path = new LinkedList();
        path.add( node);
        TreeNode parent = node.getParent();
        while (parent != null) {
            path.addFirst( parent);
            parent = parent.getParent();
        }
        expandAll( new TreePath( path.toArray()));
    }

    /**
     * Expands all paths in the tree passing through the node which is last in the path.
     *
     * @param path Contains the path from root to the node inclusive.
     */
    private void expandAll( TreePath path) {
        this.expandPath( path);

        boolean childrenAreLeaves = true;
        TreeNode child = null;
        for ( Enumeration e=((TreeNode) path.getLastPathComponent()).children(); e.hasMoreElements(); ) {
            child = (TreeNode) e.nextElement();
            if (!child.isLeaf()) {
                childrenAreLeaves = false;
                expandAll( path.pathByAddingChild( child));
            }
        }

        // child is never null since this method will not be called for leaves.
        // It doesn't matter which child we use for the expansion since they are all leaves.
        if (childrenAreLeaves) {
            this.expandPath( path.pathByAddingChild( child));
        }
    }

    /**
     * Returns an enumeration of expanded descendants. This is overridden to optimize
     * the initial expansion of the tree from {@link #expandAll() expandAll} or
     * {@link #expandNonHidden() expandNonHidden}. Profiling has shown that this method
     * is slowing down the expansion extremely.
     * If one of this methods is called for
     * the first time, it will set {@link #returnEmptyEnumeration} to <CODE>true</CODE>,
     * because it is known that no nodes are expanded at that time. This method will
     * then return an empty enumeration. 
     */
    public Enumeration getExpandedDescendants( TreePath parent) {
        if (returnEmptyEnumeration)
            return EMPTY_ENUMERATION;
        else
            return super.getExpandedDescendants( parent);
    }

    /**
     * The currently selected annotation node.
     */
    private AnnotationNode annotationSelection = null;

    /**
     * Adapt the display to a new selection.
     *
     * @param e The tree selection event.
     */
    public void valueChanged( TreeSelectionEvent e) {
        if (e.getNewLeadSelectionPath() != null &&
            e.getNewLeadSelectionPath() != e.getOldLeadSelectionPath()) {
            TreeNode tn = (TreeNode) e.getNewLeadSelectionPath().getLastPathComponent();

            updateActions( tn);

            TreeNode tn2 = tn;
            while (!(tn2 instanceof AnnotationNode)) {
                tn2 = tn2.getParent();
            }

            if (tn2 != annotationSelection) {
                annotationSelection = (AnnotationNode) tn2;
                int start = annotationSelection.getAnnotationElement().getStartOffset();
                int end = annotationSelection.getAnnotationElement().getEndOffset();
                docpane.makeVisible( start, end);
                docpane.highlightText( start, end);
            }
        }
    }

    /**
     * Update the state of the actions to adapt to the given tree node.
     *
     * @param tn The tree node to adapt the actions to.
     */
    private void updateActions( TreeNode tn) {
        if (tn == null) {
            useReadingAction.setEnabled( false);
            useTranslationAction.setEnabled( false);
            hideAction.setEnabled( false);
            removeAction.setEnabled( false);
        }
        else {
            useReadingAction.setEnabled(tn instanceof ReadingAnnotationNode ||
                                     tn instanceof TranslationNode);
            useTranslationAction.setEnabled(tn instanceof TranslationLeafNode);
            hideAction.setEnabled( true);
            removeAction.setEnabled( true);

            updateHideAction( tn);
        }
    }

    /**
     * Adapt the hide action to the given node.
     *
     * @param tn The node to adapt to.
     */
    private void updateHideAction( TreeNode tn) {
        while (!(tn instanceof AnnotationNode)) {
            tn = tn.getParent();
        }
        hideAction.putValue( Action.NAME,
                             JGloss.messages.getString( ((AnnotationNode) tn).isHidden() ?
                                                        "annotationeditor.menu.show" : 
                                                        "annotationeditor.menu.hide"));
    }

    /**
     * Returns the menu which contains the annotation-specific action. This menu can
     * be integrated in the application menu bar. The menu is unique and thus cannot be
     * used in more than one menu bar.
     *
     * @return The menu.
     */
    public JMenu getMenu() {
        return menu;
    }

    /**
     * Scrolls the display of the annotation editor so that the annotation for the
     * given position in the document is visible. If no annotation exists at the
     * position, nothing will be done.
     *
     * @param pos A position in the document which contains the annotations.
     */
    public void makeVisible( int pos) {
        makeVisible( model.findAnnotation( pos));
    }

    /**
     * Scrolls the display of the annotation editor so that the given annotation node is
     * visible. This will uncollapse the split pane, if the annotation editor is contained in one.
     *
     * @param node The annotation node which will be made visible.
     */
    public void makeVisible( AnnotationNode node) {
        if (node != null) {
            TreeNode[] path = new TreeNode[2];
            path[0] = (TreeNode) model.getRoot();
            path[1] = node;
            TreePath tp = new TreePath( path);
            
            // make node first visible entry in scroller
            Rectangle r = getPathBounds( tp);
            Component parent = getParent(); // JViewPort
            if (parent!=null && parent instanceof JViewport) {
                JViewport port = (JViewport) parent;
                port.setViewPosition( new Point
                    ( 0, Math.max( 0, Math.min( r.y, port.getViewSize().height -
                                                port.getSize().height))));
                }

            // uncollapse split pane if neccessary
            if (parent!=null && !(parent instanceof JSplitPane))
                parent = parent.getParent(); // JScrollPane
            if (parent!=null && !(parent instanceof JSplitPane))
                parent = parent.getParent(); // JSplitPane
            if (parent!=null && parent instanceof JSplitPane) {
                JSplitPane split = (JSplitPane) parent;
                if (split.getDividerLocation() >= split.getMaximumDividerLocation()) {
                    if (split.getLastDividerLocation() >= split.getMaximumDividerLocation())
                        split.resetToPreferredSizes();
                    else
                        split.setDividerLocation( split.getLastDividerLocation());
                }
            }
        }
    }

    /**
     * Makes the annotation node for the annotation at the given position in the document
     * the selected node. This will not scroll the display to the new selection. If no
     * annotation exists at the specified position, nothing will be done.
     *
     * @param pos The position in the document for which the annotation should be selected.
     */
    public void selectAnnotation( int pos) {
        selectNode( model.findAnnotation( pos));
    }

    /**
     * Makes the given tree node the new selection. This will not scroll the display to 
     * the new selection.
     *
     * @param node The tree node.
     */
    public void selectNode( TreeNode node) {
        if (node == null)
            return;

        LinkedList path = new LinkedList();
        while (node != null) {
            path.addFirst( node);
            node = node.getParent();
        }

        TreeNode[] tp = new TreeNode[path.size()];
        tp = (TreeNode[]) path.toArray( tp);
        setSelectionPath( new TreePath( tp));
    }

    /**
     * Display context menu if needed.
     *
     * @param e The mouse event.
     */
    public void mouseClicked(MouseEvent e) {
      checkPopupMenu( e);
    }

    /**
     * Display context menu if needed.
     *
     * @param e The mouse event.
     */
    public void mousePressed(MouseEvent e) {
      checkPopupMenu( e);
    }

    /**
     * Display context menu if needed.
     *
     * @param e The mouse event.
     */
    public void mouseReleased(MouseEvent e) {
      checkPopupMenu( e);
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    /**
     * Display a context menu for the current position if the mouse event is a popup trigger.
     * This will make the node under the mouse pointer the new selection.
     *
     * @param e The mouse event to react to.
     */
    private void checkPopupMenu( MouseEvent e) {
        if (e.isPopupTrigger()) {
            TreePath path = getPathForLocation( e.getX(), e.getY());
            if (path != null) {
                setSelectionPath( path);
                pmenu.show( this, e.getX(), e.getY());
            }
        }
    }

    /**
     * Returns a context menu with annotation-specific actions. This menu can then be displayed
     * to allow the user to manipulate entries. To use the context menu, first call
     * <CODE>selectNode</CODE> to adapt the context menu to this node and then display it.
     *
     * @return The context menu.
     */
    public JPopupMenu getContextMenu() {
        return pmenu;
    }
} // class AnnotationEditor
