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
import java.beans.PropertyChangeListener;

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
     * Variant of the default tree cell editor which returns an increased preferred width
     * for {@link jgloss.ui.annotation.EditableTextNode editable text nodes}. This is neccessary
     * because otherwise readings and translations won't display if the text length is increased.
     */
    private class AnnotationTreeCellRenderer extends DefaultTreeCellRenderer {
        /**
         * This is set to <CODE>true</CODE> for editable text nodes to signal a change
         * of the preferred width.
         */
        boolean modifyWidth;

        public AnnotationTreeCellRenderer() {
            super();
            modifyWidth = false;
        }

        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            if (modifyWidth && preferred!=null)
                preferred.width += 60;
            return preferred;
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf, int row,
                                                      boolean hasFocus) {
            modifyWidth = (value instanceof EditableTextNode);
            return super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row, hasFocus);
        }
    } // class AnnotationTreeCellRenderer

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
         * Prevent editing nodes other than EditableTextNodes.
         */
        public boolean isCellEditable( EventObject event) {
            if (event != null &&
                event.getSource() == AnnotationEditor.this &&
                event instanceof MouseEvent) {
                TreePath path = tree.getPathForLocation( ((MouseEvent)event).getX(),
                                                         ((MouseEvent)event).getY());
                if (path != null && 
                    !(path.getLastPathComponent() instanceof EditableTextNode))
                    return false;
            }

            return super.isCellEditable( event);
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

            if (value instanceof EditableTextNode) {
                EditableTextNode node = (EditableTextNode) value;
                label.setFont( c.getFont());
                label.setForeground( c.getForeground());
                label.setText( node.getDescription());
                String text = node.getText();
                
                JTextField textfield = ((JTextField) editingComponent);
                textfield.setText( text);
                textfield.setColumns( text.length() + 5); // leave space for editing
                
                box.removeAll();
                box.add( label);
                box.add( editingComponent); // editingComponent was reset by superclass
                editingComponent = box; // will be added to container by superclass
                prepareForEditing();
            }
            
            return c;
        }
    }

    /**
     * Delegator for key events which only forwards <CODE>keyPressed</CODE> and <CODE>keyReleased</CODE>
     * events. In Swing 1.4, the <CODE>BasicTreeUI</CODE> key listener adds first letter navigation, 
     * triggered by a <CODE>keyTyped</CODE> event. Since this behavior conflicts with the 
     * <CODE>AnnotationEditor</CODE> keyboard navigation, the listener is wrapped by this delegator,
     * which ignores the <CODE>keyTyped</CODE> events.
     */
    private class KeyEventDelegator implements KeyListener {
        /**
         * Fetch the delegator created for the given delegatee. This will remove the delegator
         * from the map of created delegators.
         */
        private KeyListener delegatee;

        public KeyEventDelegator( KeyListener delegatee) {
            this.delegatee = delegatee;
            if (delegators == null)
                delegators = new LinkedList();
            delegators.add( this);
        }
        
        public KeyListener getDelegatee() { return delegatee; }

        public void keyPressed( KeyEvent event) {
            delegatee.keyPressed( event);
        }

        public void keyReleased( KeyEvent event) {
            delegatee.keyReleased( event);
        }

        public void keyTyped( KeyEvent event) {
            // ignore this event
            // FIXME: if the keyTyped event is ever used for something useful in later releases,
            // this will break.
        }
    } // class

    private java.util.List delegators;

    /**
     * Find the delegator which was created for a delegatee and remove it from the list of
     * delegators for this instance.
     */
    private KeyEventDelegator releaseDelegator( KeyListener delegatee) {
        for ( Iterator i=delegators.iterator(); i.hasNext(); ) {
            KeyEventDelegator delegator = (KeyEventDelegator) i.next();
            if (delegator.getDelegatee() == delegatee) {
                i.remove();
                return delegator;
            }
        }
        
        return null;
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
     * Action which removes all duplicates of the currently selected annotation.
     */
    private Action removeDuplicatesAction;
    /**
     * Action which will make all annotations of the current word in the document equal.
     */
    private Action equalizeAnnotationsAction;
    /**
     * Action which adds the word of the selected annotation to the list of excluded words.
     */
    private Action addToExclusionsAction;
    /**
     * Action which adds the selected annotation to the user dictionary.
     */
    private Action addToDictionaryAction;
    /**
     * Action which will hide all annotations which are a duplicate of a previous annotation.
     */
    private Action hideDuplicatesAction;
    /**
     * Action which will un-hide all annotations.
     */
    private Action unhideAction;
    /**
     * Action which will set the current reading/translation text to the empty string.
     */
    private Action clearTranslationAction;

    /**
     * Menu which contains the annotation-specific actions.
     */
    private JMenu menu;
    /**
     * Popup menu which contains annotation-specific actions.
     */
    private JPopupMenu pmenu;
    /**
     * Flag if the use translation action is currently added to the popup menu.
     */
    private boolean pmenuTranslationAdded = false;
    /**
     * Flag if the use reading action is currently added to the popup menu.
     */
    private boolean pmenuReadingAdded = false;
    /**
     * Change the component font in response to preferences changes.
     */
    private PropertyChangeListener fontChangeListener;

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
                    ((EditableTextNode) path.getLastPathComponent()).setText( newValue.toString());
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
        DefaultTreeCellRenderer r = new AnnotationTreeCellRenderer();
        r.setLeafIcon( nullIcon);
        r.setOpenIcon( nullIcon);
        r.setClosedIcon( nullIcon);
        setCellRenderer( r);
        setRowHeight( r.getPreferredSize().height);
        if (this.getUI() instanceof BasicTreeUI) {
            BasicTreeUI ui = (BasicTreeUI) this.getUI();
            ui.setCollapsedIcon( nullIcon);
            ui.setExpandedIcon( nullIcon);
        }
        this.setCellEditor( new AnnotationTreeCellEditor( r));
        this.setEditable( true);

        this.setRootVisible( false);
        this.setShowsRootHandles( false);

        addTreeSelectionListener( this);

        // use currently selected reading for this annotation
        useReadingAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    AbstractAnnotation annotation;
                    if (tn instanceof TranslationNode) {
                        annotation = ((TranslationNode) tn).getTranslation();
                    }
                    else {
                        annotation = ((ReadingAnnotationNode) tn).getReading();
                    }

                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();

                    ((AnnotationNode) tn).setWordFrom( annotation);
                }
            };
        UIUtilities.initAction( useReadingAction, "annotationeditor.menu.usereading");
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
                }
            };
        UIUtilities.initAction( useTranslationAction, "annotationeditor.menu.usetranslation");
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
        UIUtilities.initAction( hideAction, "annotationeditor.menu.hide");
        hideAction.setEnabled( false);
        // remove the currently selected annotation
        removeAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();
                    AnnotationNode selection = (AnnotationNode) tn;
                    int index = model.getIndexOfChild( model.getRoot(), selection);
                    selection.removeAnnotation();

                    // select the following annotation node (or previous if it was the last)
                    if (index >= model.getChildCount( model.getRoot()))
                        index--;
                    if (index >= 0) {
                        TreeNode[] path = new TreeNode[2];
                        path[0] = (TreeNode) model.getRoot();
                        path[1] = (TreeNode) model.getChild( model.getRoot(), index);
                        setSelectionPath( new TreePath( path));
                    }
                    else {
                        updateActions( null);
                    }
                }
            };
        UIUtilities.initAction( removeAction, "annotationeditor.menu.remove");
        removeAction.setEnabled( false);
        // Removes all annotations which are a duplicate of the currently selected annotation.
        // A duplicate has the same kanji, reading and translation.
        removeDuplicatesAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();
                    AnnotationNode selection = (AnnotationNode) tn;

                    String kanji = selection.getWordNode().getWord();
                    String reading = selection.getWordNode().getReading();
                    String translation = selection.getTranslationNode().getText();

                    // Remove all duplicates except the current selection. We can't delete the
                    // current selection now because we need to calculate the new selection later.
                    // Removing annotations while iterating will throw a ConcurrentModificationException.
                    LinkedList duplicates = new LinkedList();
                    for ( Iterator i=model.getAnnotationNodes(); i.hasNext(); ) {
                        AnnotationNode node = (AnnotationNode) i.next();
                        if (node!=selection &&
                            kanji.equals( node.getWordNode().getWord()) &&
                            reading.equals( node.getWordNode().getReading()) &&
                            translation.equals( node.getTranslationNode().getText()))
                            duplicates.add( node);
                    }
                    for ( Iterator i=duplicates.iterator(); i.hasNext(); )
                        ((AnnotationNode) i.next()).removeAnnotation();

                    int index = model.getIndexOfChild( model.getRoot(), selection);
                    // remove the selected node
                    selection.removeAnnotation();
                    // Select the following annotation node (or previous if it was the last)
                    if (index >= model.getChildCount( model.getRoot()))
                        index--;
                    if (index >= 0) {
                        TreeNode[] path = new TreeNode[2];
                        path[0] = (TreeNode) model.getRoot();
                        path[1] = (TreeNode) model.getChild( model.getRoot(), index);
                        setSelectionPath( new TreePath( path));
                    }
                    else {
                        updateActions( null);
                    }

                }
            };
        UIUtilities.initAction( removeDuplicatesAction, "annotationeditor.menu.removeduplicates");
        removeDuplicatesAction.setEnabled( false);
        // Makes all annotations with the same word as the current use the same reading and
        // translation.
        equalizeAnnotationsAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();
                    AnnotationNode selection = (AnnotationNode) tn;

                    String word = selection.getWordNode().getWord();
                    String reading = selection.getWordNode().getReading();
                    String translation = selection.getTranslationNode().getText();
                    String dfword = selection.getDictionaryFormNode().getWord();
                    String dfreading = selection.getDictionaryFormNode().getReading();

                    // change all annotations with identical word
                    LinkedList duplicates = new LinkedList();
                    for ( Iterator i=model.getAnnotationNodes(); i.hasNext(); ) {
                        AnnotationNode node = (AnnotationNode) i.next();
                        if (node != selection &&
                            word.equals( node.getWordNode().getWord())) {
                            node.getWordNode().setReading( reading);
                            node.getTranslationNode().setText( translation);
                            node.getDictionaryFormNode().setWord( dfword);
                            node.getDictionaryFormNode().setReading( dfreading);
                        }
                    }
                }
            };
        UIUtilities.initAction( equalizeAnnotationsAction, "annotationeditor.menu.equalize");
        equalizeAnnotationsAction.setEnabled( false);
        // add the word of the selected annotation to the list of excluded words
        // The word is added as it appears in the text and in its dictionary form
        addToExclusionsAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();
                    AnnotationNode selection = (AnnotationNode) tn;
                    String word = selection.getWordNode().getWord();
                    String dictionaryWord = selection.getDictionaryFormNode().getWord();

                    // Some words are written as hiragana in the text and appear as katakana
                    // in the dictionary or vice versa. In this case add the word as it appears
                    // in the text as well as the dictionary form.
                    if (dictionaryWord.length() > 0) {
                        if (StringTools.isHiragana( word.charAt( 0)) &&
                            StringTools.isKatakana( dictionaryWord.charAt( 0)) ||
                            StringTools.isKatakana( word.charAt( 0)) &&
                            StringTools.isHiragana( dictionaryWord.charAt( 0)))
                            ExclusionList.addWord( word);
                    }

                    // if the annotated word is all kana, add the dictionary reading,
                    // else add the dictionary word.
                    boolean isKana = true;
                    for ( int i=0; i<word.length(); i++) {
                        if (!(StringTools.isHiragana( word.charAt( i)) ||
                              StringTools.isKatakana( word.charAt( i)))) {
                            isKana = false;
                            break;
                        }
                    }

                    if (!isKana || selection.getDictionaryFormNode().getReading().length()==0)
                        ExclusionList.addWord( selection.getDictionaryFormNode().getWord());
                    else
                        ExclusionList.addWord( selection.getDictionaryFormNode().getReading());
                }
            };
        UIUtilities.initAction( addToExclusionsAction, "annotationeditor.menu.addtoexclusions");
        addToExclusionsAction.setEnabled( false);
        // add the selected annotation to the user dictionary
        addToDictionaryAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    while (!(tn instanceof AnnotationNode))
                        tn = tn.getParent();
                    AnnotationNode selection = (AnnotationNode) tn;
                    String translation = selection.getTranslationNode().getText();
                    if (translation.length() == 0)
                        return;
                    String word = selection.getDictionaryFormNode().getWord();
                    if (word.length() == 0)
                        return;
                    String reading = selection.getDictionaryFormNode().getReading();
                    if (reading.length() == 0 || reading.equals( word))
                        reading = null;
                    try {
                        Dictionaries.getUserDictionary().addEntry( word, reading, translation);
                    } catch (NullPointerException ex) {}
                }
            };
        UIUtilities.initAction( addToDictionaryAction, "annotationeditor.menu.addtodictionary");
        addToDictionaryAction.setEnabled( false);
        // Hide all annotations which are a duplicate of a previous annotation.
        // A duplicate has the same kanji, reading and translation.
        hideDuplicatesAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    // stores already seen annotations
                    Set nodes = new HashSet();
                    for ( Iterator i=model.getAnnotationNodes(); i.hasNext(); ) {
                        AnnotationNode node = (AnnotationNode) i.next();
                        if (!node.isHidden()) {
                            String key = node.getWordNode().getWord() + "%" +
                                node.getWordNode().getReading() + "%" +
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
        UIUtilities.initAction( hideDuplicatesAction, "annotationeditor.menu.hideduplicates");
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
        UIUtilities.initAction( unhideAction, "annotationeditor.menu.unhide");
        unhideAction.setEnabled( false);

        menu = new JMenu( JGloss.messages.getString( "annotationeditor.menu.title"));
        pmenu = new JPopupMenu();
        menu.add( UIUtilities.createMenuItem( useReadingAction));
        menu.add( UIUtilities.createMenuItem( useTranslationAction));

        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( hideAction));
        pmenu.add( hideAction);
        menu.add( UIUtilities.createMenuItem( removeAction));
        pmenu.add( removeAction);
        menu.add( UIUtilities.createMenuItem( removeDuplicatesAction));
        pmenu.add( removeDuplicatesAction);
        menu.add( UIUtilities.createMenuItem( equalizeAnnotationsAction));
        pmenu.add( equalizeAnnotationsAction);
        menu.add( UIUtilities.createMenuItem( addToExclusionsAction));
        pmenu.add( addToExclusionsAction);
        menu.add( UIUtilities.createMenuItem( addToDictionaryAction));
        pmenu.add( addToDictionaryAction);
        
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( hideDuplicatesAction));
        menu.add( UIUtilities.createMenuItem( unhideAction));

        addMouseListener( this);

        // add keyboard shortcuts for editing the tree
        // set reading/translation text to the empty string
        clearTranslationAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    TreeNode tn = (TreeNode) getSelectionPath().getLastPathComponent();
                    if (tn instanceof ReadingTranslationNode)
                        ((ReadingTranslationNode) tn).setText( "");
                    else if (tn instanceof AnnotationNode)
                        ((ReadingTranslationNode) ((AnnotationNode) tn).getChildAt( 1)).setText( "");
                }
            };
        UIUtilities.initAction( clearTranslationAction, "annotationeditor.action.cleartranslation");
        clearTranslationAction.setEnabled( false);
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
                        if (index < model.getChildCount( model.getRoot())-1)
                            annotation = (TreeNode) model.getChild( model.getRoot(), index+1);
                        else // last annotation, no new selection
                            annotation = null;
                     }
                    else if (!((TreeNode) model.getRoot()).isLeaf()) {
                        annotation = (TreeNode) model.getChild( model.getRoot(), 0);
                    }

                    if (annotation != null) {
                        LinkedList l;
                        if (((AnnotationNode) annotation).isHidden()) {
                            // only show annotation node
                            l = new LinkedList();
                            l.add( annotation);
                        }
                        else {
                            // uncollapse complete tree under annotation node
                            l = ((AnnotationNode) annotation).getPathToLastDescendant();
                        }
                        l.addFirst( model.getRoot());
                        TreeNode[] tn = new TreeNode[l.size()];
                        tn = (TreeNode[]) l.toArray( tn);
                        scrollPathToVisible( new TreePath( tn));

                        tn = new TreeNode[2];
                        tn[0] = (TreeNode) model.getRoot();
                        tn[1] = annotation;
                        tp = new TreePath( tn);
                        setSelectionPath( tp);
                        scrollPathToVisible( tp);
                        // make sure the viewport view is at x position 0 so the selected node is
                        // fully visible
                        ((JViewport) getParent()).setViewPosition( new Point
                            ( 0, ((JViewport) getParent()).getViewPosition().y));
                    }
                }
            };
        UIUtilities.initAction( nextAnnotationAction, "annotationeditor.action.next");
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
        UIUtilities.initAction( previousAnnotationAction, "annotationeditor.action.previous");
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
                        else if (node instanceof EditableTextNode)
                            startEditingAtPath( tp);
                        else if (node instanceof DictionaryFormNode) {
                            if (isExpanded( tp))
                                collapsePath( tp);
                            else
                                expandPath( tp);
                        }
                    }
                }
            };
        UIUtilities.initAction( metaAction, "annotationeditor.action.meta");

        // Add the key bindings for the actions to the annotation editor.
        // Since the metaAction uses a "released SPACE" and a "SPACE" action is also defined,
        // the "SPACE" action has to be overridden. Since the "SPACE" action is not defined
        // in the JTree's input map but in one of the parents and I don't want to mess up the
        // keybindings too much, I add a dummy binding for "SPACE" which does nothing.
        // A "released SPACE" has to be used because otherwise a space character is added when
        // text editing is started.
        InputMap im = getInputMap();
        KeyStroke[] strokes = im.allKeys();
        KeyStroke metaStroke = (KeyStroke) metaAction.getValue( Action.ACCELERATOR_KEY);
        for ( int i=0; i<strokes.length; i++) {
            if (strokes[i].getKeyCode() == metaStroke.getKeyCode())
                im.put( strokes[i], "dummy");
        }
        ActionMap am = getActionMap();
        am.put( "dummy", new AbstractAction() { public void actionPerformed( ActionEvent e) {} });

        im.put( (KeyStroke) nextAnnotationAction.getValue( Action.ACCELERATOR_KEY),
                nextAnnotationAction.getValue( Action.NAME));
        am.put( nextAnnotationAction.getValue( Action.NAME), nextAnnotationAction);
        im.put( (KeyStroke) previousAnnotationAction.getValue( Action.ACCELERATOR_KEY),
                previousAnnotationAction.getValue( Action.NAME));
        am.put( previousAnnotationAction.getValue( Action.NAME), previousAnnotationAction);
        im.put( metaStroke, metaAction.getValue( Action.NAME));
        am.put( metaAction.getValue( Action.NAME), metaAction);

        im.put( KeyStroke.getKeyStroke( JGloss.messages.getString
                                        ( "annotationeditor.action.cleartranslation.ak")),
                clearTranslationAction.getValue( Action.NAME));
        am.put( clearTranslationAction.getValue( Action.NAME), clearTranslationAction);
        im.put( KeyStroke.getKeyStroke( JGloss.messages.getString( "annotationeditor.action.remove.ak")),
                removeAction.getValue( Action.NAME));
        am.put( removeAction.getValue( Action.NAME), removeAction);
        im.put( KeyStroke.getKeyStroke( JGloss.messages.getString
                                        ( "annotationeditor.action.removeduplicates.ak")),
                removeDuplicatesAction.getValue( Action.NAME));
        am.put( removeDuplicatesAction.getValue( Action.NAME), removeDuplicatesAction);
        im.put( KeyStroke.getKeyStroke( JGloss.messages.getString
                                        ( "annotationeditor.action.equalize.ak")),
                equalizeAnnotationsAction.getValue( Action.NAME));
        am.put( equalizeAnnotationsAction.getValue( Action.NAME), equalizeAnnotationsAction);
        im.put( KeyStroke.getKeyStroke( JGloss.messages.getString
                                        ( "annotationeditor.action.addtoexclusions.ak")),
                addToExclusionsAction.getValue( Action.NAME));
        am.put( addToExclusionsAction.getValue( Action.NAME), addToExclusionsAction);
        im.put( KeyStroke.getKeyStroke( JGloss.messages.getString
                                        ( "annotationeditor.action.addtodictionary.ak")),
                addToDictionaryAction.getValue( Action.NAME));
        am.put( addToDictionaryAction.getValue( Action.NAME), addToDictionaryAction);
        im.put( KeyStroke.getKeyStroke( JGloss.messages.getString( "annotationeditor.action.hide.ak")),
                hideAction.getValue( Action.NAME));
        am.put( hideAction.getValue( Action.NAME), hideAction);

        // update display if user changed font
        fontChangeListener = new PropertyChangeListener() {
                public void propertyChange( java.beans.PropertyChangeEvent e) { 
                    if (e.getPropertyName().equals( "Tree.font")) {
                        setFont( (Font) e.getNewValue());
                    }
                }
            };
        UIManager.getDefaults().addPropertyChangeListener( fontChangeListener);
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
     * This method has one exception: DictionaryFormNodes will not be expanded.
     *
     * @param path Contains the path from root to the node inclusive.
     */
    private void expandAll( TreePath path) {
        TreeNode child = null;
        boolean hasExpandedChild = false;
        for ( Enumeration e=((TreeNode) path.getLastPathComponent()).children(); e.hasMoreElements(); ) {
            child = (TreeNode) e.nextElement();
            if (!(child.isLeaf() || child instanceof DictionaryFormNode)) {
                expandAll( path.pathByAddingChild( child));
                hasExpandedChild = true;
            }
        }
        if (!hasExpandedChild)
            this.expandPath( path);
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
     * The last selected annotation. This is equal to <CODE>annotationSelection</CODE>, unless no
     * annotation is selected.
     */
    private AnnotationNode lastSelectedAnnotation;

    /**
     * Adapt the display to a new selection. In response to a node selected in the annotation
     * tree, the annotation the node belongs to will become the selected annotation. This annotation
     * will be highlighted in the linked {@link JGlossEditor JGlossEditor}, the editor will be
     * scrolled to this location and the caret will be placed at the beginning of the annotation.
     * The status of the annotation manipulation actions will be updated according to the current
     * selection.
     *
     * @param e The tree selection event.
     */
    public void valueChanged( TreeSelectionEvent e) {
        if (e.getNewLeadSelectionPath() != null) {
            if (e.getNewLeadSelectionPath() != e.getOldLeadSelectionPath()) {
                TreeNode tn = (TreeNode) e.getNewLeadSelectionPath().getLastPathComponent();                
                updateActions( tn);
                
                TreeNode selection = tn;
                while (!(selection instanceof AnnotationNode)) {
                    selection = selection.getParent();
                }
                
                if (selection != annotationSelection) {
                    if (annotationSelection != null) {
                        // collapse old selection
                        collapsePath( new TreePath( new Object[] { model.getRoot(), annotationSelection }));
                        // the whole tree is collapsed now, clear the cache of toggled 
                        // paths to conserve memory
                        clearToggledPaths();
                    }
                    // expand new selection
                    annotationSelection = (AnnotationNode) selection;
                    lastSelectedAnnotation = annotationSelection;
                    expandAll( annotationSelection);
                    makeVisible( annotationSelection);
                    
                    int start = annotationSelection.getAnnotationElement().getStartOffset();
                    int end = annotationSelection.getAnnotationElement().getEndOffset();
                    docpane.makeVisible( start, end);
                    docpane.highlightText( start, end);
                    // Move the position of the caret to the start of the annotation. This
                    // works around the problem where the document scrolls to a different
                    // location if some text is changed, which happens because the DefaultCaret 
                    // will scroll to the caret location after document changes.
                    docpane.setCaretPosition( start);
                }
            }
        }
        else if (e.getOldLeadSelectionPath() != null) {
            // clear selection
            updateActions( null);
            if (annotationSelection != null) {
                // collapse old selection
                collapsePath( new TreePath( new Object[] { model.getRoot(), annotationSelection }));
                // the whole tree is collapsed now, clear the cache of toggled 
                // paths to conserve memory
                clearToggledPaths();
                annotationSelection = null;

                docpane.removeHighlight();
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
            removeDuplicatesAction.setEnabled( false);
            equalizeAnnotationsAction.setEnabled( false);
            addToExclusionsAction.setEnabled( false);
            addToDictionaryAction.setEnabled( false);
            clearTranslationAction.setEnabled( false);

            // update context sensitive popup menu
            if (pmenuReadingAdded || pmenuTranslationAdded) {
                pmenu.remove( 0);
                pmenu.remove( 0); // separator
                pmenuReadingAdded = false;
                pmenuTranslationAdded = false;
            }
        }
        else {
            useReadingAction.setEnabled(tn instanceof ReadingAnnotationNode ||
                                        tn instanceof TranslationNode);
            useTranslationAction.setEnabled(tn instanceof TranslationLeafNode);
            hideAction.setEnabled( true);
            removeAction.setEnabled( true);
            removeDuplicatesAction.setEnabled( true);
            equalizeAnnotationsAction.setEnabled( true);
            addToExclusionsAction.setEnabled( true);
            addToDictionaryAction.setEnabled( Dictionaries.getUserDictionary() != null);
            clearTranslationAction.setEnabled( tn instanceof ReadingTranslationNode ||
                                               tn instanceof AnnotationNode);

            updateHideAction( tn);

            // update context sensitive popup menu
            if (pmenuReadingAdded && !(tn instanceof ReadingAnnotationNode ||
                                       tn instanceof TranslationNode) ||
                pmenuTranslationAdded && !(tn instanceof TranslationLeafNode)) {
                pmenu.remove( 0);
                pmenu.remove( 0); // separator
                pmenuReadingAdded = false;
                pmenuTranslationAdded = false;
            }

            if (!pmenuReadingAdded && (tn instanceof ReadingAnnotationNode||
                                       tn instanceof TranslationNode)) {
                pmenu.insert( useReadingAction, 0);
                pmenu.insert( new JPopupMenu.Separator(), 1);
                pmenuReadingAdded = true;
            }
            else if (!pmenuTranslationAdded && tn instanceof TranslationLeafNode) {
                pmenu.insert( useTranslationAction, 0);
                pmenu.insert( new JPopupMenu.Separator(), 1);
                pmenuTranslationAdded = true;
            }
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
     * @return The annotation made visible, or <CODE>null</CODE> if there is no annotation at the position.
     */
    public AnnotationNode makeVisible( int pos) {
        AnnotationNode annotation = model.findAnnotation( pos);
        if (annotation != null)
            makeVisible( annotation);
        return annotation;
    }

    /**
     * Scrolls the display of the annotation editor so that the given annotation node is
     * visible. This will uncollapse the split pane if the annotation editor is contained in one.
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
     * @return The selected annotation, or <CODE>null</CODE> if there is no annotation at the position.
     */
    public AnnotationNode selectAnnotation( int pos) {
        AnnotationNode annotation = model.findAnnotation( pos);
        if (annotation != null)
            selectNode( annotation);
        else {
            clearSelection();
        }
        return annotation;
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
                showContextMenu( this, e.getX(), e.getY());
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

    /**
     * Shows the context menu with annotation-specific actions. The coordinates are given in the
     * coordinate system of the invoker and are changed such that the menu will fit on the screen.
     */
    public void showContextMenu( Component invoker, int x, int y) {
        Point sc = invoker.getLocationOnScreen();
        x += sc.x;
        y += sc.y;
        Dimension size = pmenu.getSize();
        Rectangle screen = invoker.getGraphicsConfiguration().getBounds();
        // If the menu has not been shown yet, the size will be 0, even if validate()
        // and pack() were called.
        if (size.width == 0 && size.height == 0) {
            // move the menu off-screen
            pmenu.setLocation( screen.x + screen.width + 1, 0);
            pmenu.setVisible( true);
            size = pmenu.getSize();
            pmenu.setVisible( false);
        }

        // fit menu on the screen
        if (x + size.width > screen.x + screen.width)
            x = screen.x + screen.width - size.width;
        if (y + size.height > screen.y + screen.height)
            y = screen.y + screen.height - size.height;

        pmenu.show( invoker, x - sc.x, y - sc.y);
    }
    
    /**
     * Returns the currently selected annotation, or <CODE>null</CODE> if no annotation is
     * selected.
     */
    public AnnotationNode getSelectedAnnotation() {
        return annotationSelection;
    }

    /**
     * Returns the annotation which was most recently selected.
     */
    public AnnotationNode getLastSelectedAnnotation() {
        return lastSelectedAnnotation;
    }

    /**
     * Wraps the handler in a <CODE>KeyEventDelegator</CODE>.
     *
     * @see AnnotationEditor.KeyEventDelegator
     */
    public void addKeyListener( KeyListener handler) {
        if (handler instanceof BasicTreeUI.KeyHandler) {
            super.addKeyListener( new KeyEventDelegator( handler));
        }
        else
            super.addKeyListener( handler);
    }

    /**
     * Removes the <CODE>KeyEventDelegator</CODE> which wraps the handler.
     *
     * @see AnnotationEditor.KeyEventDelegator
     */
    public void removeKeyListener( KeyListener handler) {
        if (handler instanceof BasicTreeUI.KeyHandler) {
            KeyListener delegator = releaseDelegator( handler);
            if (delegator != null)
                super.removeKeyListener( delegator);
            else
                super.removeKeyListener( handler);
        }
    }

    /**
     * Release resources held by the annotation editor.
     */
    public void dispose() {
        UIManager.getDefaults().removePropertyChangeListener( fontChangeListener);
    }
} // class AnnotationEditor
