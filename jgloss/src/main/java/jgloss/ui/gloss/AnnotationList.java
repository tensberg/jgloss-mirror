/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.gloss;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Element;

import jgloss.JGloss;
import jgloss.ui.ExclusionList;
import jgloss.ui.UIUtilities;
import jgloss.ui.annotation.Annotation;
import jgloss.ui.annotation.AnnotationListModel;
import jgloss.ui.html.JGlossHTMLDoc;
import jgloss.util.StringTools;

public class AnnotationList extends JList implements MouseListener, ListSelectionListener {
    private static final long serialVersionUID = 1L;

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
        private final KeyListener delegatee;

        public KeyEventDelegator( KeyListener delegatee) {
            this.delegatee = delegatee;
            if (delegators == null) {
	            delegators = new LinkedList<KeyEventDelegator>();
            }
            delegators.add( this);
        }
        
        public KeyListener getDelegatee() { return delegatee; }

        @Override
		public void keyPressed( KeyEvent event) {
            delegatee.keyPressed( event);
        }

        @Override
		public void keyReleased( KeyEvent event) {
            delegatee.keyReleased( event);
        }

        @Override
		public void keyTyped( KeyEvent event) {
            // ignore this event
            // FIXME: if the keyTyped event is ever used for something useful in later Java releases,
            // this will break.
        }
    } // class KeyEventDelegator

    private List<KeyEventDelegator> delegators;

    /**
     * Find the delegator which was created for a delegatee and remove it from the list of
     * delegators for this instance.
     */
    private KeyEventDelegator releaseDelegator( KeyListener delegatee) {
        for (Iterator<KeyEventDelegator> i=delegators.iterator(); i.hasNext(); ) {
            KeyEventDelegator delegator = i.next();
            if (delegator.getDelegatee().equals(delegatee)) {
                i.remove();
                return delegator;
            }
        }
        
        return null;
    }

    private AnnotationListModel annotationList;

    /**
     * Menu which contains the annotation-specific actions.
     */
    private final JMenu menu;
    /**
     * Popup menu which contains annotation-specific actions.
     */
    private final JPopupMenu pmenu;

    /**
     * Action which removes the currently selected annotation.
     */
    private final Action removeAction;
    /**
     * Action which adds the word of the selected annotation to the list of excluded words.
     */
    private final Action addToExclusionsAction;
    /**
     * Action which adds the selected annotation to the user dictionary.
     */
    private final Action addToDictionaryAction;
    /**
     * Action which will set the current reading/translation text to the empty string.
     */
    private final Action clearTranslationAction;

    public AnnotationList() {
        setSelectionMode( ListSelectionModel.SINGLE_SELECTION);

        // remove the currently selected annotation
        removeAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    removeSelectedAnnotation();
                }
            };
        UIUtilities.initAction( removeAction, "annotationeditor.menu.remove");
        removeAction.setEnabled( false);
        // add the word of the selected annotation to the list of excluded words
        // The word is added as it appears in the text and in its dictionary form
        addToExclusionsAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    addSelectionToExclusions();
                }
            };
        UIUtilities.initAction( addToExclusionsAction, "annotationeditor.menu.addtoexclusions");
        addToExclusionsAction.setEnabled( false);
        // add the selected annotation to the user dictionary
        addToDictionaryAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    addSelectionToDictionary();
                }
            };
        UIUtilities.initAction( addToDictionaryAction, "annotationeditor.menu.addtodictionary");
        addToDictionaryAction.setEnabled( false);

        menu = new JMenu( JGloss.MESSAGES.getString( "annotationeditor.menu.title"));
        pmenu = new JPopupMenu();
        menu.add( UIUtilities.createMenuItem( removeAction));
        pmenu.add( removeAction);
        menu.add( UIUtilities.createMenuItem( addToExclusionsAction));
        pmenu.add( addToExclusionsAction);
        menu.add( UIUtilities.createMenuItem( addToDictionaryAction));
        pmenu.add( addToDictionaryAction);

        addMouseListener( this);

        // add keyboard shortcuts for editing the tree
        // set reading/translation text to the empty string
        clearTranslationAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    Annotation selection = (Annotation) getSelectedValue();
                    if (selection != null) {
	                    selection.setTranslation( null);
                    }
                }
            };
        UIUtilities.initAction( clearTranslationAction, "annotationeditor.action.cleartranslation");
        clearTranslationAction.setEnabled( false);
        // select the following annotation
        Action nextAnnotationAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    int selection = getSelectedIndex();
                    if (selection>-1 && selection<annotationList.getAnnotationCount()-1) {
	                    setSelectedIndex( selection+1);
                    }
                }
            };
        UIUtilities.initAction( nextAnnotationAction, "annotationeditor.action.next");
        // select the previous annotation
        Action previousAnnotationAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    int selection = getSelectedIndex();
                    if (selection > 0) {
	                    setSelectedIndex( selection-1);
                    }
                }
            };
        UIUtilities.initAction( previousAnnotationAction, "annotationeditor.action.previous");

        // Add the key bindings for the actions to the annotation editor.
        InputMap im = new InputMap();
        im.setParent(getInputMap());
        setInputMap(WHEN_FOCUSED, im);
        ActionMap am = new ActionMap();
        am.setParent(getActionMap());
        setActionMap(am);

        im.put( (KeyStroke) nextAnnotationAction.getValue( Action.ACCELERATOR_KEY),
                nextAnnotationAction.getValue( Action.NAME));
        am.put( nextAnnotationAction.getValue( Action.NAME), nextAnnotationAction);
        im.put( (KeyStroke) previousAnnotationAction.getValue( Action.ACCELERATOR_KEY),
                previousAnnotationAction.getValue( Action.NAME));
        am.put( previousAnnotationAction.getValue( Action.NAME), previousAnnotationAction);

        im.put( KeyStroke.getKeyStroke( JGloss.MESSAGES.getString
                                        ( "annotationeditor.action.cleartranslation.ak")),
                clearTranslationAction.getValue( Action.NAME));
        am.put( clearTranslationAction.getValue( Action.NAME), clearTranslationAction);
        im.put( KeyStroke.getKeyStroke( JGloss.MESSAGES.getString( "annotationeditor.action.remove.ak")),
                removeAction.getValue( Action.NAME));
        am.put( removeAction.getValue( Action.NAME), removeAction);
        im.put( KeyStroke.getKeyStroke( JGloss.MESSAGES.getString
                                        ( "annotationeditor.action.addtoexclusions.ak")),
                addToExclusionsAction.getValue( Action.NAME));
        am.put( addToExclusionsAction.getValue( Action.NAME), addToExclusionsAction);
        im.put( KeyStroke.getKeyStroke( JGloss.MESSAGES.getString
                                        ( "annotationeditor.action.addtodictionary.ak")),
                addToDictionaryAction.getValue( Action.NAME));
        am.put( addToDictionaryAction.getValue( Action.NAME), addToDictionaryAction);

        addListSelectionListener(this);
    }

    public void setAnnotationListModel( AnnotationListModel _model) {
        annotationList = _model;
        setModel( new AnnotationListModelAdapter( _model));
    }

    public AnnotationListModel getAnnotationListModel() {
        return annotationList;
    }

    /**
     * Display context menu if needed.
     *
     * @param e The mouse event.
     */
    @Override
	public void mouseClicked(MouseEvent e) {
      checkPopupMenu( e);
    }

    /**
     * Display context menu if needed.
     *
     * @param e The mouse event.
     */
    @Override
	public void mousePressed(MouseEvent e) {
      checkPopupMenu( e);
    }

    /**
     * Display context menu if needed.
     *
     * @param e The mouse event.
     */
    @Override
	public void mouseReleased(MouseEvent e) {
      checkPopupMenu( e);
    }

    @Override
	public void mouseEntered(MouseEvent e) {}
    @Override
	public void mouseExited(MouseEvent e) {}

    /**
     * Display a context menu for the current position if the mouse event is a popup trigger.
     * This will make the node under the mouse pointer the new selection.
     *
     * @param e The mouse event to react to.
     */
    private void checkPopupMenu( MouseEvent e) {
        if (e.isPopupTrigger()) {
            int selection = locationToIndex( e.getPoint());
            if (selection != -1) {
                setSelectedIndex( selection);
                showContextMenu( this, e.getX(), e.getY());
            }
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

        pmenu.show( invoker, x - sc.x, y - sc.y);
    }

    /**
     * Wraps the handler in a {@link AnnotationList.KeyEventDelegator KeyEventDelegator}.
     */
    @Override
	public void addKeyListener( KeyListener handler) {
        if (handler.getClass().getName().equals( "javax.swing.plaf.basic.BasicListUI$KeyHandler")) {
            super.addKeyListener( new KeyEventDelegator( handler));
        } else {
	        super.addKeyListener( handler);
        }
    }

    /**
     * Removes the {@link AnnotationList.KeyEventDelegator KeyEventDelegator} which wraps the handler.
     */
    @Override
	public void removeKeyListener( KeyListener handler) {
        if (handler.getClass().getName().equals( "javax.swing.plaf.basic.BasicListUI$KeyHandler")) {
            KeyListener delegator = releaseDelegator( handler);
            if (delegator != null) {
	            super.removeKeyListener( delegator);
            } else {
	            super.removeKeyListener( handler);
            }
        }
    }

    @Override
	public void setSelectedIndex(int index) {
        
        super.setSelectedIndex(index);
        // Scroll to visible
        ensureIndexIsVisible(index);
    }

    /**
     * Adapts the state of the annotation-specific actions in response to changes in the selection.
     */
    @Override
	public void valueChanged(ListSelectionEvent e) {
        boolean annoSelected = (getSelectedIndex() != -1); // HERE
        removeAction.setEnabled(annoSelected);
        addToExclusionsAction.setEnabled(annoSelected);
        addToDictionaryAction.setEnabled(annoSelected);
        clearTranslationAction.setEnabled(annoSelected);
    }

    /**
     * Removes the currently selected annotation.
     */
    private void removeSelectedAnnotation() {
        int selection = getSelectedIndex();
        if (selection != -1) {
            Element annotation = annotationList.getAnnotation(selection)
                .getAnnotationElement();
            ((JGlossHTMLDoc) annotation.getDocument()).removeAnnotationElement(annotation);
        }

        // select the next annotation (or previous, if this was the last annotation)
        selection = Math.min(selection, getModel().getSize()-1);
        if (selection >= 0) {
	        setSelectedIndex(selection);
        }
    }

    /**
     * Adds the word of the currently selected annotation to the list of exclusions.
     */
    private void addSelectionToExclusions() {
        Annotation selection = (Annotation) getSelectedValue();
        if (selection == null) {
	        return;
        }

        String word = selection.getAnnotatedText();
        String dictionaryWord = selection.getDictionaryForm();

        // Some words are written as hiragana in the text and appear as katakana
        // in the dictionary or vice versa. In this case add the word as it appears
        // in the text as well as the dictionary form.
        if (dictionaryWord.length() > 0) {
            if (StringTools.isHiragana( word.charAt( 0)) &&
                StringTools.isKatakana( dictionaryWord.charAt( 0)) ||
                StringTools.isKatakana( word.charAt( 0)) &&
                StringTools.isHiragana( dictionaryWord.charAt( 0))) {
	            ExclusionList.addWord( word);
            }
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

        if (!isKana || selection.getDictionaryFormReading().length()==0) {
	        ExclusionList.addWord( dictionaryWord);
        } else {
	        ExclusionList.addWord( selection.getDictionaryFormReading());
        }
    }

    /**
     * Adds the currently selected annotation to the user dictionary.
     */
    private void addSelectionToDictionary() {
        Annotation selection = (Annotation) getSelectedValue();
        if (selection == null) {
	        return;
        }
        String translation = selection.getTranslation();
        if (translation==null || translation.length()==0) {
	        return;
        }
        String word = selection.getDictionaryForm();
        if (word==null || word.length()==0) {
	        return;
        }
        String reading = selection.getDictionaryFormReading();
        if (reading!=null && (reading.length()==0 || reading.equals( word))) {
	        reading = null;
        /*try {
          Dictionaries.getUserDictionary().addEntry( word, reading, translation);
          } catch (NullPointerException ex) {}*/
        }
    }
} // class AnnotationList
