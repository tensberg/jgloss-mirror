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
import jgloss.dictionary.attribute.ReferenceAttributeValue;
import jgloss.dictionary.attribute.SearchReference;
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
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(LookupFrame.class.getPackage().getName());
	
	private final LookupConfigPanel config;
    private LookupModel model;
    private final AsynchronousLookupEngine engine;
    private final LookupResultList list;
    
    private final Dimension preferredSize;

    private final LookupHistory lookupHistory = new LookupHistory(this);
    
    private JFrame legendFrame;
    private AttributeLegend legend;

	private final SearchOnModelChangeListener searchOnModelChange = new SearchOnModelChangeListener(this);
	
	private final PerformSearchPreconditions performSearchPreconditions = new PerformSearchPreconditions();

    public LookupFrame( LookupModel _model) {
        super( JGloss.MESSAGES.getString( "wordlookup.title"));
        setIconImages(JGlossLogo.ALL_LOGO_SIZES);
        
        getContentPane().setLayout( new BorderLayout());
        JPanel center = new JPanel();
        center.setLayout( new BorderLayout());
        getContentPane().add( center, BorderLayout.CENTER);
        
        model = _model;
        list = new LookupResultList();
        engine = new AsynchronousLookupEngine(list);
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

        pack();
		config.getSearchExpressionField().requestFocusInWindow();

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
        toolbar.add(lookupHistory.getHistoryBackAction());
        toolbar.add(lookupHistory.getHistoryForwardAction());
        toolbar.add( legendAction);
        getContentPane().add( toolbar, BorderLayout.NORTH);

        setSize( getPreferredSize());
        
        model.addLookupChangeListener(searchOnModelChange);
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
        if (performSearchPreconditions.performSearch(model)) {
            engine.doLookup(model, new Runnable() {
                @Override
                public void run() {
                    LOGGER.severe("adding state for model " + model);
                    lookupHistory.addCurrentState(createHistoryItem());
                }
            });
        } else {
            LOGGER.finer("not performing search because preconditions of model not fulfilled");
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
            if (ref instanceof SearchReference) {
            	model.followReference((SearchReference) ref);
            } else {
            	throw new IllegalArgumentException("unsupported reference type " + ref);
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

    void showHistoryItem( HistoryItem hi) {
        model = hi.getLookupModel().clone();
        
        // ignore model changes triggered by setModel
        model.removeLookupChangeListener(searchOnModelChange);
        config.setModel( model);
        list.restoreViewState( hi.getResultState());
        model.addLookupChangeListener(searchOnModelChange);
    }

    private HistoryItem createHistoryItem() {
        return new HistoryItem( model.clone(), 
                                list.saveViewState());
    }
} // class LookupFrame
