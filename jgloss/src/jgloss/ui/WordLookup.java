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
import java.util.Iterator;

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
                    getFrame().show();
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

    protected final static String STYLE =
        "body { font-size: 12pt; color: black; background-color: white; }\n";

    protected WordLookupPanel wordlookup;

    /**
     * Open recent JGloss document menu item.
     */
    private JMenu openRecent;

    public WordLookup() {
        super( JGloss.messages.getString( "wordlookup.title"));

        wordlookup = new WordLookupPanel( false);
        getContentPane().setLayout( new GridLayout( 1, 1));
        getContentPane().add( wordlookup);

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
} // class WordLookup
