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
import jgloss.dictionary.attribute.ReferenceAttributeValue;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.*;

public class LookupFrame extends JFrame implements ActionListener, HyperlinkListener {
    protected LookupConfigPanel config;
    protected LookupModel model;
    protected AsynchronousLookupEngine engine;
    protected LookupResultList list;
    protected LookupResultCache referencedResults;
    
    protected List history;
    protected int historyPosition;
    protected static final int MAX_HISTORY_SIZE = 20;
    protected Action historyBackAction;
    protected Action historyForwardAction;
    
    protected Dimension preferredSize;

    public LookupFrame( LookupModel _model) {
        super( JGloss.messages.getString( "wordlookup.title"));

        getContentPane().setLayout( new BorderLayout());
        JPanel center = new JPanel();
        center.setLayout( new BorderLayout());
        getContentPane().add( center, BorderLayout.CENTER);
        
        model = _model;
        config = new LookupConfigPanel( model, this);
        list = new LookupResultList();
        engine = new AsynchronousLookupEngine( list);

        config.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2));
        center.add( config, BorderLayout.NORTH);

        list.setBorder( BorderFactory.createCompoundBorder
                     ( BorderFactory.createTitledBorder( JGloss.messages.getString( "wordlookup.result")),
                       BorderFactory.createEmptyBorder( 2, 2, 2, 2)));
        list.addHyperlinkListener( this);
        center.add( list, BorderLayout.CENTER);
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

        history = new ArrayList( MAX_HISTORY_SIZE);
        historyBackAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    historyBack();
                }
            };
        UIUtilities.initAction( historyBackAction, "wordlookup.history.back");
        historyBackAction.setEnabled( false);
        historyForwardAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    historyForward();
                }
            };
        UIUtilities.initAction( historyForwardAction, "wordlookup.history.forward");
        historyForwardAction.setEnabled( false);
        JToolBar toolbar = new JToolBar();
        toolbar.add( historyBackAction);
        toolbar.add( historyForwardAction);
        getContentPane().add( toolbar, BorderLayout.NORTH);
    }

    public void actionPerformed( ActionEvent event) {
        LookupModel clonedModel = (LookupModel) model.clone();
        addToHistory( new HistoryItem( clonedModel, referencedResults, list.saveViewState()));
        referencedResults = null;
        engine.doLookup( clonedModel);
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

    protected void historyBack() {
        historyPosition--;
        HistoryItem hi = (HistoryItem) history.get( historyPosition);

        history.set( historyPosition, new HistoryItem( (LookupModel) model.clone(), 
                                                       referencedResults, list.saveViewState()));

        if (historyPosition == 0)
            historyBackAction.setEnabled( false);
        historyForwardAction.setEnabled( true);
        showHistoryItem( hi);
    }

    protected void historyForward() {
        HistoryItem hi = (HistoryItem) history.get( historyPosition);
        historyPosition++;

        history.set( historyPosition-1, new HistoryItem( (LookupModel) model.clone(), 
                                                         referencedResults, list.saveViewState()));

        if (historyPosition == history.size())
            historyForwardAction.setEnabled( false);
        historyBackAction.setEnabled( true);
        showHistoryItem( hi);
    }

    protected void addToHistory( HistoryItem hi) {
        history.add( historyPosition, hi);
        historyPosition++;
        history = history.subList( 0, historyPosition);
        if (history.size() > MAX_HISTORY_SIZE) {
            if (historyPosition*2>MAX_HISTORY_SIZE) {
                history.remove( 0);
                historyPosition--;
            }
            else
                history.remove( history.size()-1);
        }
        historyBackAction.setEnabled( true);
        historyForwardAction.setEnabled( false);
    }

    protected void showHistoryItem( HistoryItem hi) {
        model = hi.lookupModel;
        config.setModel( model);
        hi.replay( list, engine);
        referencedResults = hi.resultCache;
    }

    public void hyperlinkUpdate( HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            int colon = e.getDescription().indexOf( ':');
            String protocol = e.getDescription().substring( 0, colon);
            String refKey = e.getDescription().substring( colon+1);
            followReference( protocol, refKey);
        }
    }

    protected void followReference( String type, String refKey) {
        ReferenceAttributeValue ref = list.getReference( refKey);
        if (ref != null) try {
            addToHistory( new HistoryItem( (LookupModel) model.clone(), referencedResults,
                                           list.saveViewState()));
            referencedResults = new LookupResultCache
                ( JGloss.messages.getString( "wordlookup.reference." + type, 
                                             new Object[] { ref.getReferenceTitle() }),
                      ref.getReferencedEntries());
            referencedResults.replay( list);
        } catch (SearchException ex) {
            ex.printStackTrace();
        }
    }

    private static class HistoryItem {
        private LookupModel lookupModel;
        private LookupResultCache resultCache;
        private LookupResultList.ViewState resultState;

        private HistoryItem( LookupModel _lookupModel, LookupResultCache _resultCache,
                             LookupResultList.ViewState _resultState) {
            lookupModel = _lookupModel;
            resultCache = _resultCache;
            resultState = _resultState;
        }

        public void replay( final LookupResultList list, AsynchronousLookupEngine engine) {
            Runnable listRestorer = new Runnable() {
                    public void run() {
                        if (!EventQueue.isDispatchThread())
                            EventQueue.invokeLater( this);
                        else
                            list.restoreViewState( resultState);
                    }
                };
            
            if (resultCache != null) {
                resultCache.replay( list);
                listRestorer.run();
            }
            else {
                engine.doLookup( lookupModel, listRestorer);
            }
        }
    }
} // class LookupFrame
