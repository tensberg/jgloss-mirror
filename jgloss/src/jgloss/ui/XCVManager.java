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

import jgloss.JGloss;

import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;

/**
 * Makes the Cut/Copy/Paste actions of a <CODE>JTextComponent</CODE> available for a menu.
 * <p>
 * The XCVManager will generate Cut/Copy/Paste actions which delegate to the corresponding
 * actions of a JTextComponent. The state of the generated actions will be updated according to
 * the state of the text component and the clipboard. 
 * </p><p>
 * An XCVManager can manage more than one
 * text component. In this case, the last component which got the input focus is the active component,
 * and actions will be delegated to it.
 * </p>
 *
 * @author Michael Koch
 */
public class XCVManager {
    /**
     * Action which cuts out the selected part of the document. Delegates to the HTMLDocuments
     * cut action.
     */
    private Action cutAction;
    /**
     * Action which copies the selected part of the document. Delegates to the HTMLDocuments
     * copy action.
     */
    private Action copyAction;
    /**
     * Action which pastes the clipboard content. Delegates to the HTMLDocuments
     * paste action.
     */
    private Action pasteAction;
    private MenuListener editMenuListener;
    /**
     * Cut action of the <CODE>JTextComponent</CODE>.
     */
    private Action delegateeCutAction;
    /**
     * Copy action of the <CODE>JTextComponent</CODE>.
     */
    private Action delegateeCopyAction;
    /**
     * Paste action of the <CODE>JTextComponent</CODE>.
     */
    private Action delegateePasteAction;

    /**
     * Map from source text components to an array of the delegatee cut/copy/paste actions.
     */
    private Map sourceActions;

    /**
     * The currently active text component. The action state will be updated based on the state
     * of this component, and invocations of the actions will be forwarded to this component.
     */
    private JTextComponent activeSource;

    private FocusListener setActiveActionListener;

    public XCVManager( JComboBox source) {
        this( (JTextComponent) source.getEditor().getEditorComponent());
    }

    public XCVManager( JTextComponent source) {
        this.activeSource = source;
        sourceActions = new HashMap( 5);

        cutAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    delegateeCutAction.actionPerformed( e);
                }
            };
        cutAction.setEnabled( false);
        UIUtilities.initAction( cutAction, "editor.menu.cut");
        copyAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    delegateeCopyAction.actionPerformed( e);
                }
            };
        copyAction.setEnabled( false);
        UIUtilities.initAction( copyAction, "editor.menu.copy");
        pasteAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    delegateePasteAction.actionPerformed( e);
                }
            };
        pasteAction.setEnabled( false);
        UIUtilities.initAction( pasteAction, "editor.menu.paste");

        updateActions( activeSource);
        editMenuListener = new MenuListener() {
                public void menuSelected( MenuEvent e) {
                    updateActionState();
                }
                public void menuDeselected( MenuEvent e) {}
                public void menuCanceled( MenuEvent e) {}
            };

        setActiveActionListener = new FocusAdapter() {
                public void focusGained( FocusEvent e) {
                    setActiveSource( (JTextComponent) e.getSource());
                }
            };
        source.addFocusListener( setActiveActionListener);
    }

    public Action getCutAction() { return cutAction; }
    public Action getCopyAction() { return copyAction; }
    public Action getPasteAction() { return pasteAction; }

    /**
     * Returns a menu listener which will update the action state if the menu is selected. This
     * menu should be attached to every menu containing one of the actions generated by this
     * XCVManager.
     */
    public MenuListener getEditMenuListener() { return editMenuListener; }

    public void addManagedComponent( JComboBox source) {
        addManagedComponent( (JTextComponent) source.getEditor().getEditorComponent());
    }

    /**
     * Adds a text field managed by this XCVManager. If the text component gets the focus,
     * it will become the active source and actions invoked by the user will be delegated
     * to this text component.
     */
    public void addManagedComponent( JTextComponent source) {
        updateActions( source);
        source.addFocusListener( setActiveActionListener);
    }

    public void removeManagedComponent( JComboBox source) {
        removeManagedComponent( (JTextComponent) source.getEditor().getEditorComponent());
    }

    /**
     * Removes a component from the list of managed components.
     */
    public void removeManagedComponent( JTextComponent source) {
        sourceActions.remove( source);
        source.removeFocusListener( setActiveActionListener);
    }

    /**
     * Initializes the delegatee cut/copy/paste actions by looking them up from
     * <CODE>DefaultEditorKit</CODE>. This should be called if the editor kit of
     * the <CODE>JTextComponent</CODE> has been changed.
     *
     * @see javax.swing.text.DefaultEditorKit
     */
    public synchronized void updateActions( JTextComponent source) {
        Action[] allActions = source.getActions();
        Action[] actions = new Action[3]; // cut/copy/paste
        for ( int i=0; i<allActions.length; i++) {
            String name = (String) allActions[i].getValue( Action.NAME);
            if (name.equals( DefaultEditorKit.cutAction))
                actions[0] = allActions[i];
            else if (name.equals( DefaultEditorKit.copyAction))
                actions[1] = allActions[i];
            else if (name.equals( DefaultEditorKit.pasteAction))
                actions[2] = allActions[i];
        }
        sourceActions.put( source, actions);

        if (source == activeSource)
            setActiveSource( source); // update current actions
    }

    /**
     * Sets the currently active text field and updates the delegatee actions accordingly.
     */
    protected synchronized void setActiveSource( JTextComponent source) {
        activeSource = source;
        Action[] actions = (Action[]) sourceActions.get( source);
        delegateeCutAction = actions[0];
        delegateeCopyAction = actions[1];
        delegateePasteAction = actions[2];

        updateActionState();
    }

    public synchronized void updateActionState() {
        boolean hasSelection = activeSource.getSelectionStart() != activeSource.getSelectionEnd();
        cutAction.setEnabled( hasSelection && 
                              delegateeCutAction != null &&
                              delegateeCutAction.isEnabled() &&
                              activeSource.isEditable());
        copyAction.setEnabled( hasSelection &&
                               delegateeCopyAction != null &&
                               delegateeCopyAction.isEnabled());
        Transferable t = java.awt.Toolkit.getDefaultToolkit().
            getSystemClipboard().getContents( null);
        boolean hasContent = (t != null &&
                              (t.isDataFlavorSupported( DataFlavor.getTextPlainUnicodeFlavor()) ||
                               t.isDataFlavorSupported( DataFlavor.stringFlavor)));
        
        pasteAction.setEnabled( hasContent &&
                                delegateePasteAction != null &&
                                delegateePasteAction.isEnabled() &&
                                activeSource.isEditable());
    }
} // class XCVManager
