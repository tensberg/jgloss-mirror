/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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
import jgloss.Preferences;

import jgloss.dictionary.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class LookupFrame extends JFrame implements ActionListener {
    protected LookupConfigPanel config;
    protected LookupModel model;
    protected AsynchronousLookupEngine engine;
    protected LookupResultList list;
    
    protected Dimension preferredSize;

    public LookupFrame( LookupModel _model) {
        super( JGloss.messages.getString( "wordlookup.title"));

        getContentPane().setLayout( new BorderLayout());

        model = _model;
        config = new LookupConfigPanel( model, this);
        list = new LookupResultList();
        engine = new AsynchronousLookupEngine( list);

        config.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2));
        getContentPane().add( config, BorderLayout.NORTH);

        list.setBorder( BorderFactory.createCompoundBorder
                     ( BorderFactory.createTitledBorder( JGloss.messages.getString( "wordlookup.result")),
                       BorderFactory.createEmptyBorder( 2, 2, 2, 2)));
        getContentPane().add( list, BorderLayout.CENTER);
        getRootPane().setDefaultButton( config.getSearchButton());

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
                    if (JGloss.exit())
                        dispose();
                }
            };
        UIUtilities.initAction( closeAction, "main.menu.close");

        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE);
        addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent e) {
                    hide();
                    if (JGloss.exit())
                        dispose();
                }
            });

        // setup menu bar
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu( JGloss.messages.getString( "main.menu.file"));
        //menu.add( UIUtilities.createMenuItem( JGlossFrame.actions.importDocument));
        //menu.add( UIUtilities.createMenuItem( JGlossFrame.actions.importClipboard));
        //addWindowListener( JGlossFrame.actions.importClipboardListener);
        //menu.addMenuListener( JGlossFrame.actions.importClipboardListener);
        //menu.addSeparator();
        //menu.add( UIUtilities.createMenuItem( JGlossFrame.actions.open));
        //openRecent = JGlossFrame.OPEN_RECENT.createMenu( JGlossFrame.actions.openRecentListener);
        //menu.add( openRecent);
        //menu.addSeparator();
        /*menu.add( UIUtilities.createMenuItem( printAction));
          menu.addSeparator();*/
        menu.add( UIUtilities.createMenuItem( closeAction));
        bar.add( menu);

        final JMenu editMenu = new JMenu( JGloss.messages.getString( "editor.menu.edit"));
        XCVManager xcv = new XCVManager( config.getSearchExpressionField());
        xcv.addManagedComponent( config.getDistanceField());

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

        config.getSearchExpressionField().requestFocus();

        pack();

        preferredSize = new Dimension
            ( Math.max( super.getPreferredSize().width,
                        JGloss.prefs.getInt( Preferences.WORDLOOKUP_WIDTH, 0)),
              Math.max( super.getPreferredSize().height + 150,
                        JGloss.prefs.getInt( Preferences.WORDLOOKUP_HEIGHT, 0)));
        
        addComponentListener( new ComponentAdapter() {
                public void componentResized( ComponentEvent e) {
                    JGloss.prefs.set( Preferences.WORDLOOKUP_WIDTH, getWidth());
                    JGloss.prefs.set( Preferences.WORDLOOKUP_HEIGHT, getHeight());
                }
            });
    }

    public void actionPerformed( ActionEvent event) {
        engine.doLookup( (LookupModel) model.clone());
    }

    public void dispose() {
        super.dispose();
        engine.dispose();
    }

    public Dimension getPreferredSize() {
        if (preferredSize == null)
            return super.getPreferredSize();
        else
            return preferredSize;
    }
} // class LookupFrame
