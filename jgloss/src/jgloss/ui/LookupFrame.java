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

import jgloss.dictionary.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class LookupFrame extends JFrame implements ActionListener {
    public static void main( String args[]) throws Exception {
        EDict d1 = new EDict( new java.io.File( "/home/michael/japan/dictionaries/edict"));
        System.err.println( "loading EDICT index");
        if (!d1.loadIndex()) {
            System.err.println( "building EDICT index");
            d1.buildIndex();
        }
        System.err.println( "loading WadokuJT index");
        WadokuJT d2 = new WadokuJT
            ( new java.io.File
              ( "/home/michael/japan/dictionaries/wadoku/WadokuJT.txt"));
        if (!d2.loadIndex()) {
            System.err.println( "building WadokuJT index");
            d2.buildIndex();
        }

        LookupModel model = new LookupModel
            ( java.util.Arrays.asList
              ( new Object[] { ExpressionSearchModes.EXACT,
                               ExpressionSearchModes.PREFIX,
                               ExpressionSearchModes.SUFFIX,
                               ExpressionSearchModes.ANY,
                               DistanceSearchModes.NEAR,
                               DistanceSearchModes.RADIUS }),
              java.util.Arrays.asList
              ( new Object[] { d1, d2 }),
              java.util.Arrays.asList
              ( new Object[] { new AttributeResultFilter() }));

        LookupFrame frame = new LookupFrame( model);
        frame.setSize( 800, 600);
        frame.setVisible( true);
    }

    protected LookupConfigPanel config;
    protected LookupModel model;
    protected LookupEngine engine;
    protected LookupResultList list;
    
    private class SearchThread extends Thread {
        private Object THREAD_LOCK = new Object();
        private boolean terminateThread = false;

        public SearchThread() {
            super( "lookup frame search thread");
            setDaemon( true);
        }

        public void run() {
            synchronized (THREAD_LOCK) {
                while (!terminateThread) try {
                    THREAD_LOCK.wait();
                    
                    if (terminateThread)
                        break;
                    
                    // clear lingering interrupted flag
                    Thread.interrupted();
                    
                    engine.doLookup( (LookupModel) model.clone());
                } catch (InterruptedException ex) { ex.printStackTrace(); }
            }
        }

        public void newSearch() {
            // abort current search (if any)
            SearchThread.this.interrupt();
            // start new search
            synchronized (THREAD_LOCK) {
                THREAD_LOCK.notify();
            }
        }

        public void dispose() {
            terminateThread = true;
            SearchThread.this.interrupt();
            try {
                SearchThread.this.join( 3000);
            } catch (InterruptedException ex) {}
            if (SearchThread.this.isAlive())
                System.err.println( "WARNING: LookupFrame search thread still alive");
        }
    } // class SearchThread
    
    private SearchThread searchThread;

    public LookupFrame( LookupModel _model) {
        super( JGloss.messages.getString( "wordlookup.title"));
        getContentPane().setLayout( new BorderLayout());

        model = _model;
        config = new LookupConfigPanel( model, this);
        list = new LookupResultList();
        engine = new LookupEngine( list);

        searchThread = new SearchThread();
        searchThread.start();

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
        XCVManager xcv = new XCVManager( list.getResultPane());
        xcv.addManagedComponent( config.getSearchExpressionField());
        xcv.addManagedComponent( config.getDistanceField());

        editMenu.add( xcv.getCutAction());
        editMenu.add( xcv.getCopyAction());
        editMenu.add( xcv.getPasteAction());
        editMenu.addMenuListener( xcv.getEditMenuListener());

        //editMenu.addSeparator();
        //editMenu.add( UIUtilities.createMenuItem( PreferencesFrame.showAction));
        bar.add( editMenu);           

        menu = new JMenu( JGloss.messages.getString( "main.menu.help"));
        menu.add( UIUtilities.createMenuItem( AboutFrame.showAction));
        bar.add( menu);

        setJMenuBar( bar);

        config.getSearchExpressionField().requestFocus();

        pack();
    }

    public void actionPerformed( ActionEvent event) {
        searchThread.newSearch();
    }

    public void dispose() {
        super.dispose();
        searchThread.dispose();
    }
} // class LookupFrame
