/*
 * Copyright (C) 2001,2002 Michael Koch (tensberg@gmx.net)
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
import jgloss.parser.*;
import jgloss.util.StringTools;

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

public class AnnotationEditor extends JPanel implements MouseListener {
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

    private JGlossDocument doc;
    private JGlossEditor docpane;

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
     * Change the component font in response to preferences changes.
     */
    private PropertyChangeListener fontChangeListener;

    public AnnotationEditor() {
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

        menu = new JMenu( JGloss.messages.getString( "annotationeditor.menu.title"));
        pmenu = new JPopupMenu();
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

        // Add the key bindings for the actions to the annotation editor.
        InputMap im = getInputMap();
        KeyStroke[] strokes = im.allKeys();
        ActionMap am = getActionMap();

        im.put( (KeyStroke) nextAnnotationAction.getValue( Action.ACCELERATOR_KEY),
                nextAnnotationAction.getValue( Action.NAME));
        am.put( nextAnnotationAction.getValue( Action.NAME), nextAnnotationAction);
        im.put( (KeyStroke) previousAnnotationAction.getValue( Action.ACCELERATOR_KEY),
                previousAnnotationAction.getValue( Action.NAME));
        am.put( previousAnnotationAction.getValue( Action.NAME), previousAnnotationAction);

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
    public void setDocument( JGlossDocument _doc, JGlossEditor _docpane) {
        doc = _doc;
        docpane = _docpane;
    }

    /**
     * The currently selected annotation.
     */
    private Annotation annotationSelection = null;
    /**
     * The last selected annotation. This is equal to <CODE>annotationSelection</CODE>, unless no
     * annotation is selected.
     */
    private Annotation lastSelectedAnnotation;

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

    private void updateActions( Annotation anno) {
        if (anno == null) {
            removeAction.setEnabled( false);
            removeDuplicatesAction.setEnabled( false);
            equalizeAnnotationsAction.setEnabled( false);
            addToExclusionsAction.setEnabled( false);
            addToDictionaryAction.setEnabled( false);
            clearTranslationAction.setEnabled( false);
        }
        else {
            removeAction.setEnabled( true);
            removeDuplicatesAction.setEnabled( true);
            equalizeAnnotationsAction.setEnabled( true);
            addToExclusionsAction.setEnabled( true);
            addToDictionaryAction.setEnabled( Dictionaries.getUserDictionary() != null);
            clearTranslationAction.setEnabled( true);
        }
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
    public Annotation makeVisible( int pos) {
        Annotation annotation = model.findAnnotation( pos);
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
    public void makeVisible( Annotation node) {
        if (node != null) {

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
