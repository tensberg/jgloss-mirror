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
import jgloss.dictionary.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Frontend for looking up single words in the dictionaries.
 *
 * @author Michael Koch
 */
public class WordLookup extends JFrame {
    /**
     * Action which displays the word lookup window.
     */
    public final static Action showAction;

    static {
        showAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    getFrame().tempDisableSearchClipboard();
                    getFrame().setVisible( true);
                }
            };
        UIUtilities.initAction( showAction, "main.menu.wordlookup");
    }

    /**
     * Instance of the dialog used in the show action.
     */
    private static WordLookup instance;

    public static synchronized WordLookup getFrame() {
        if (instance == null)
            instance = new WordLookup();

        return instance;
    }

    protected WordLookupPanel wordlookup;

    /**
     * Open recent JGloss document menu item.
     */
    private JMenu openRecent;

    private WindowListener searchClipboardListener = new WindowAdapter() {
            public void windowActivated( WindowEvent e) {
                searchClipboardContent();
            }
        };

    /**
     * Maximum character length of clipboard content for automatic search.
     *
     * @see #searchClipboardContent()
     */
    private final static int MAX_CLIPBOARD_LOOKUP_LENGTH = 50;
    
    private String lastClipboardContent;

    public WordLookup() {
        super( JGloss.messages.getString( "wordlookup.title"));

        // don't auto-search the creation-time clipboard content
        lastClipboardContent = getClipboardContent();
        if (lastClipboardContent == null)
            lastClipboardContent = "";

        // create clipboard lookup switch UI element
        JPanel p = new JPanel( new GridLayout( 1, 1));
        
        final JCheckBox clipboard = new JCheckBox( JGloss.messages.getString
                                                   ( "wordlookup.clipboard.search"));
        clipboard.setSelected( JGloss.prefs.getBoolean( Preferences.WORDLOOKUP_SEARCHCLIPBOARD, false));
        if (JGloss.prefs.getBoolean( Preferences.WORDLOOKUP_SEARCHCLIPBOARD, false))
            addWindowListener( searchClipboardListener);

        clipboard.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e) {
                    if (clipboard.isSelected()) {
                        WordLookup.this.addWindowListener( searchClipboardListener);
                        searchClipboardContent();
                    }
                    else {
                        WordLookup.this.removeWindowListener( searchClipboardListener);
                    }
                    JGloss.prefs.set( Preferences.WORDLOOKUP_SEARCHCLIPBOARD, clipboard.isSelected());
                }
            });
        p.add( clipboard);
        p.setBorder( BorderFactory.createCompoundBorder
                    ( BorderFactory.createTitledBorder
                      ( JGloss.messages.getString( "wordlookup.clipboard")),
                      BorderFactory.createEmptyBorder( 2, 2, 2, 2)));        

        wordlookup = new WordLookupPanel( p);
        getContentPane().setLayout( new GridLayout( 1, 1));
        getContentPane().add( wordlookup);
        getRootPane().setDefaultButton( wordlookup.getSearchButton());

        // create actions
        /*Action printAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                }
            };
        printAction.setEnabled( false);
        UIUtilities.initAction( printAction, "main.menu.print"); */
        Action closeAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    hide();
                    JGloss.exit();
                }
            };
        UIUtilities.initAction( closeAction, "main.menu.close");

        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE);
        addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent e) {
                    hide();
                    JGloss.exit();
                }
            });

        // setup menu bar
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu( JGloss.messages.getString( "main.menu.file"));
        menu.add( UIUtilities.createMenuItem( JGlossFrame.actions.importDocument));
        menu.add( UIUtilities.createMenuItem( JGlossFrame.actions.importClipboard));
        addWindowListener( JGlossFrame.actions.importClipboardListener);
        menu.addMenuListener( JGlossFrame.actions.importClipboardListener);
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( JGlossFrame.actions.open));
        openRecent = JGlossFrame.OPEN_RECENT.createMenu( JGlossFrame.actions.openRecentListener);
        menu.add( openRecent);
        menu.addSeparator();
        /*menu.add( UIUtilities.createMenuItem( printAction));
          menu.addSeparator();*/
        menu.add( UIUtilities.createMenuItem( closeAction));
        bar.add( menu);

        final JMenu editMenu = new JMenu( JGloss.messages.getString( "editor.menu.edit"));
        XCVManager xcv = wordlookup.getXCVManager();
        editMenu.add( xcv.getCutAction());
        editMenu.add( xcv.getCopyAction());
        editMenu.add( xcv.getPasteAction());
        editMenu.addMenuListener( xcv.getEditMenuListener());

        editMenu.addSeparator();
        editMenu.add( UIUtilities.createMenuItem( PreferencesFrame.showAction));
        bar.add( editMenu);           

        menu = new JMenu( JGloss.messages.getString( "main.menu.help"));
        menu.add( UIUtilities.createMenuItem( AboutFrame.showAction));
        bar.add( menu);

        setJMenuBar( bar);

        wordlookup.getExpressionField().requestFocus();

        pack();
    }

    /**
     * Looks up the selected text.
     *
     * @param text The word which should be looked up.
     */
    public void search( String text) {
        wordlookup.search( text);
    }

    /**
     * Saves the preferences in addition to hiding the frame.
     */
    public void hide() {
        super.hide();

        wordlookup.savePreferences();
    }

    public void dispose() {
        super.dispose();
        JGlossFrame.OPEN_RECENT.removeMenu( openRecent);
        openRecent = null;
    }

    /**
     * Get the cliboard content as string. If the clipboard content is not available or can't be
     * accessed as string, <code>null</code> is returned.
     */
    private String getClipboardContent() {
        Transferable t = getToolkit().getSystemClipboard().getContents( this);

        if (t != null) try {
            return (String) t.getTransferData( DataFlavor.stringFlavor);
        } catch (IOException ex) {
            /* content no longer available */
        } catch (UnsupportedFlavorException ex) {
            /* clipboard content not a string */
        }

        return null;
    }

    /**
     * Automatically search the clipboard content. The clipboard content is searched if the
     * content has changed since the last call and some constraints on the content length and
     * form are met.
     */
    private void searchClipboardContent() {
        String data = getClipboardContent();

        if (data != null) {
            if (data.equals( lastClipboardContent))
                return;
            lastClipboardContent = data;
            if (data.length() > MAX_CLIPBOARD_LOOKUP_LENGTH)
                return;
            // single kana characters lead to long searches if startsWith is selected, don't use
            // them for search
            if (data.length() == 1 && StringTools.isKana( data.charAt( 0)))
                return;
            
            // Line break characters in clipboard content will confuse the 
            // expression JComboBox. Filter them.
            for ( int i=0; i<data.length(); i++) {
                char c = data.charAt( i);
                if (c=='\r' || c=='\n') {
                    // discard everything from the line break on
                    data = data.substring( 0, i);
                    break;
                }
            }

            search( data);
        }
    }

    /**
     * Prevent {@link #searchClipboardContent() searchClipboardContent} from searching the
     * current content.
     */
    private void tempDisableSearchClipboard() {
        String data = getClipboardContent();
        if (data != null)
            lastClipboardContent = data;
    }
} // class WordLookup
