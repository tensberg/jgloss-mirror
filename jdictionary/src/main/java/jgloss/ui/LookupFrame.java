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

package jgloss.ui;

import static jgloss.ui.util.UIUtilities.fitToScreen;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.dictionary.SearchException;
import jgloss.dictionary.attribute.ReferenceAttributeValue;
import jgloss.ui.util.UIUtilities;
import jgloss.ui.util.XCVManager;

/**
 * Frame which ties together a {@link LookupConfigPanel LookupConfigPanel} and a
 * {@link LookupResultList LookupResultList} to do dictionary lookups.
 *
 * @author Michael Koch
 */
public class LookupFrame extends JFrame implements ActionListener, HyperlinkListener,
                                                   DictionaryListChangeListener {
	private static final Logger LOGGER = Logger.getLogger(LookupFrame.class.getPackage().getName());
	
	private static final long serialVersionUID = 1L;
    
	private final LookupConfigPanel config;
    private LookupModel model;
    private final AsynchronousLookupEngine engine;
    private final LookupResultList list;
    private LookupResultCache currentResults;
    
    private List<HistoryItem> history;
    private int historyPosition;
    private static final int MAX_HISTORY_SIZE = 20;
    private final Action historyBackAction;
    private final Action historyForwardAction;
    
    private final Dimension preferredSize;

    private JFrame legendFrame;
    private AttributeLegend legend;

    public LookupFrame( LookupModel _model) {
        super( JGloss.MESSAGES.getString( "wordlookup.title"));
        setIconImages(JGlossLogo.ALL_LOGO_SIZES);
        
        getContentPane().setLayout( new BorderLayout());
        JPanel center = new JPanel();
        center.setLayout( new BorderLayout());
        getContentPane().add( center, BorderLayout.CENTER);
        
        model = _model;
        list = new LookupResultList();
        currentResults = new LookupResultCache( list);
        engine = new AsynchronousLookupEngine( currentResults);
        config = new LookupConfigPanel( model);

        config.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2));
        center.add( config, BorderLayout.NORTH);

        list.setBorder( BorderFactory.createCompoundBorder
                     ( BorderFactory.createTitledBorder( JGloss.MESSAGES.getString( "wordlookup.result")),
                       BorderFactory.createEmptyBorder( 2, 2, 2, 2)));
        list.addHyperlinkListener( this);
        center.add( list, BorderLayout.CENTER);

        // create actions
        /*Action printAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                }
            };
        printAction.setEnabled( false);
        UIUtilities.initAction( printAction, "main.menu.print"); */

        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE);
        addWindowListener( new WindowAdapter() {
                @Override
				public void windowClosing( WindowEvent e) {
                    setVisible(false);
                    JGloss.getApplication().exit();
                }
            });

        // setup menu bar
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu( JGloss.MESSAGES.getString( "main.menu.file"));
        createFileMenuItems( menu);
        /*menu.add( UIUtilities.createMenuItem( printAction));
          menu.addSeparator();*/
        bar.add( menu);

        final JMenu editMenu = new JMenu( JGloss.MESSAGES.getString( "main.menu.edit"));
        XCVManager xcv = new XCVManager();
        xcv.addManagedComponent( config.getSearchExpressionField());
        xcv.addManagedComponent( config.getDistanceField());

        editMenu.add( xcv.getCutAction());
        editMenu.add( xcv.getCopyAction());
        editMenu.add( xcv.getPasteAction());
        editMenu.addMenuListener( xcv.getEditMenuListener());

        editMenu.addSeparator();
        editMenu.add( UIUtilities.createMenuItem( PreferencesFrame.SHOW_ACTION));
        bar.add( editMenu);           

        menu = new JMenu( JGloss.MESSAGES.getString( "main.menu.help"));
        menu.add(UIUtilities.createMenuItem(AboutFrame.getShowAction()));
        bar.add( menu);

        setJMenuBar( bar);

        addWindowFocusListener(new WindowAdapter() {
        	@Override
        	public void windowGainedFocus(WindowEvent e) {
        		config.getSearchExpressionField().requestFocusInWindow();
        	}
		});

        pack();

        preferredSize = new Dimension
            ( Math.max( super.getPreferredSize().width,
                        JGloss.PREFS.getInt( Preferences.WORDLOOKUP_WIDTH, 0)),
              Math.max( super.getPreferredSize().height + 150,
                        JGloss.PREFS.getInt( Preferences.WORDLOOKUP_HEIGHT, 0)));
        
        addComponentListener( new ComponentAdapter() {
                @Override
				public void componentResized( ComponentEvent e) {
                    JGloss.PREFS.set( Preferences.WORDLOOKUP_WIDTH, getWidth());
                    JGloss.PREFS.set( Preferences.WORDLOOKUP_HEIGHT, getHeight());
                }
            });

        history = new ArrayList<HistoryItem>( MAX_HISTORY_SIZE);
        historyBackAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    historyBack();
                }
            };
        UIUtilities.initAction( historyBackAction, "wordlookup.history.back");
        historyBackAction.setEnabled( false);
        historyForwardAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    historyForward();
                }
            };
        UIUtilities.initAction( historyForwardAction, "wordlookup.history.forward");
        historyForwardAction.setEnabled( false);

        Action legendAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    if (legendFrame == null) {
	                    createLegendFrame();
                    }
                    legendFrame.setVisible(true);
                }
            };
        UIUtilities.initAction( legendAction, "wordlookup.showlegend");
        legendAction.setEnabled( true);

        JToolBar toolbar = new JToolBar();
        toolbar.add( historyBackAction);
        toolbar.add( historyForwardAction);
        toolbar.add( legendAction);
        getContentPane().add( toolbar, BorderLayout.NORTH);

        setSize( getPreferredSize());
        
        model.addLookupChangeListener(new SearchOnModelChangeListener(this));
    }

    public void search( String text) {
        if (text == null || text.length()==0) {
	        return;
        }

        model.setSearchExpression( text);
        actionPerformed( null);
    }

    protected void createFileMenuItems( JMenu menu) {
        Action closeAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    setVisible(false);
                    JGloss.getApplication().exit();
                }
            };
        UIUtilities.initAction( closeAction, "main.menu.close");
        menu.add( UIUtilities.createMenuItem( closeAction));
    }

    @Override
	public void actionPerformed( ActionEvent event) {
        if (currentResults.isEmpty()) {
	        // first lookup
            engine.doLookup( model.clone());
        } else {
            // save current state in history
            HistoryItem hi = createHistoryItem();
            addToHistory( hi);
            engine.doLookup( hi.lookupModel); // hi.lookupModel is cloned model
        }
    }

    @Override
	public void dispose() {
        super.dispose();
        engine.dispose();
        Dictionaries.getInstance().removeDictionaryListChangeListener( this);
        if (legendFrame != null) {
	        legendFrame.dispose();
        }
    }

    @Override
	public Dimension getPreferredSize() {
        if (preferredSize == null) {
	        return super.getPreferredSize();
        } else {
	        return preferredSize;
        }
    }

    @Override
	public void hyperlinkUpdate( HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            int colon = e.getDescription().indexOf( ':');
            String protocol = e.getDescription().substring( 0, colon);
            String refKey = e.getDescription().substring( colon+1);
            followReference( protocol, refKey);
        }
    }

    private void followReference( String type, String refKey) {
        if (LookupResultHyperlinker.REFERENCE_PROTOCOL.equals( type)) {
            ReferenceAttributeValue ref = (ReferenceAttributeValue) 
                ((HyperlinkAttributeFormatter.ReferencedAttribute) list.getReference( refKey)).getValue();
            if (ref != null) {
	            try {
	                addToHistory( createHistoryItem());
	                currentResults.setData
	                ( JGloss.MESSAGES.getString( "wordlookup.reference", 
	                                             new Object[] { ref.getReferenceTitle() }),
	                  ref.getReferencedEntries());
	                currentResults.replay();
	            } catch (SearchException ex) {
	                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
	            }
            }
        }
    }

    private void createLegendFrame() {
        if (legend != null) {
	        return;
        }

        legendFrame = new JFrame( JGloss.MESSAGES.getString( "wordlookup.legendframe.title"));
        legend = new AttributeLegend();
        legend.setDictionaries( Dictionaries.getInstance().getDictionaries());
        Dictionaries.getInstance().addDictionaryListChangeListener( this);
        legendFrame.getContentPane().add( legend);
        legendFrame.pack();
		legendFrame.setSize( fitToScreen(legendFrame.getPreferredSize(), legendFrame.getGraphicsConfiguration()));
    }

    @Override
	public void dictionaryListChanged() {
        if (legend != null) {
            synchronized (legend) {
                legend.setDictionaries( Dictionaries.getInstance().getDictionaries());
            }
        }
    }

    private void historyBack() {
        historyPosition--;
        HistoryItem hi = history.get( historyPosition);

        history.set( historyPosition, createHistoryItem());

        if (historyPosition == 0) {
	        historyBackAction.setEnabled( false);
        }
        historyForwardAction.setEnabled( true);
        showHistoryItem( hi);
    }

    private void historyForward() {
        HistoryItem hi = history.get( historyPosition);
        historyPosition++;

        history.set( historyPosition-1, createHistoryItem());

        if (historyPosition == history.size()) {
	        historyForwardAction.setEnabled( false);
        }
        historyBackAction.setEnabled( true);
        showHistoryItem( hi);
    }

    private void addToHistory( HistoryItem hi) {
        history.add( historyPosition, hi);
        historyPosition++;
        history = history.subList( 0, historyPosition);
        if (history.size() > MAX_HISTORY_SIZE) {
            if (historyPosition*2>MAX_HISTORY_SIZE) {
                history.remove( 0);
                historyPosition--;
            } else {
	            history.remove( history.size()-1);
            }
        }
        historyBackAction.setEnabled( true);
        historyForwardAction.setEnabled( false);
    }

    private void showHistoryItem( HistoryItem hi) {
        model = hi.lookupModel;
        config.setModel( model);
        currentResults = hi.resultCache;
        currentResults.replay();
        list.restoreViewState( hi.resultState);
    }

    private HistoryItem createHistoryItem() {
        return new HistoryItem( model.clone(), 
                                currentResults.clone(),
                                list.saveViewState());
    }

    private static class HistoryItem {
        final LookupModel lookupModel;
        final LookupResultCache resultCache;
        final LookupResultList.ViewState resultState;

        HistoryItem( LookupModel _lookupModel, LookupResultCache _resultCache,
                             LookupResultList.ViewState _resultState) {
            lookupModel = _lookupModel;
            resultCache = _resultCache;
            resultState = _resultState;
        }
    }
} // class LookupFrame
