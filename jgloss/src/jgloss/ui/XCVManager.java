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

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;

/**
 * Makes the Cut/Copy/Paste actions of a <CODE>JTextComponent</CODE> available for a menu.
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

    private JTextComponent source;

    public XCVManager( JComboBox source) {
        this( (JTextComponent) source.getEditor().getEditorComponent());
    }

    public XCVManager( JTextComponent source) {
        this.source = source;

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

        updateActions();
        editMenuListener = new MenuListener() {
                public void menuSelected( MenuEvent e) {
                    updateActions();
                }
                public void menuDeselected( MenuEvent e) {}
                public void menuCanceled( MenuEvent e) {}
            };
    }

    public Action getCutAction() { return cutAction; }
    public Action getCopyAction() { return copyAction; }
    public Action getPasteAction() { return pasteAction; }
    public MenuListener getEditMenuListener() { return editMenuListener; }

    /**
     * Initializes the delegatee cut/copy/paste actions by looking them up from
     * <CODE>DefaultEditorKit</CODE>. This should be called if the editor kit of
     * the <CODE>JTextComponent</CODE> has been changed.
     *
     * @see javax.swing.text.DefaultEditorKit
     */
    public void updateActions() {
        Action[] actions = source.getActions();
        for ( int i=0; i<actions.length; i++) {
            String name = (String) actions[i].getValue( Action.NAME);
            if (name.equals( DefaultEditorKit.cutAction))
                delegateeCutAction = actions[i];
            else if (name.equals( DefaultEditorKit.copyAction))
                delegateeCopyAction = actions[i];
            else if (name.equals( DefaultEditorKit.pasteAction))
                delegateePasteAction = actions[i];
        }
        updateActionState();
    }

    public void updateActionState() {
        boolean hasSelection = source.getSelectionStart() != source.getSelectionEnd();
        cutAction.setEnabled( hasSelection && 
                              delegateeCutAction != null &&
                              delegateeCutAction.isEnabled() &&
                              source.isEditable());
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
                                source.isEditable());
    }
} // class XCVManager
