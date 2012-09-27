/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.dictionary.DictionaryEntryField;
import jgloss.dictionary.ExpressionSearchModes;
import jgloss.dictionary.MatchMode;
import jgloss.dictionary.SearchMode;
import jgloss.dictionary.attribute.ReferenceAttributeValue;
import jgloss.dictionary.attribute.SearchReference;
import jgloss.ui.util.XCVManager;

public class SimpleLookup extends JPanel implements ActionListener, HyperlinkListener {
    /**
     * A dictionary change listener which will update a lookup model if the dictionary list
     * changes, but will not prevent the model from being garbage collected. The model is
     * stored in a weak reference. When the model is garbage collected, the listener is
     * removed from the {@link Dictionaries Dictionaries} listener list.
     */
    private static class WeakDictionaryChangeListener 
        implements DictionaryListChangeListener {
        private final WeakReference<LookupModel> modelRef;

        WeakDictionaryChangeListener( LookupModel model) {
            modelRef = new WeakReference<LookupModel>( model) {
                    @Override
					public boolean enqueue() {
                        boolean enqueued = super.enqueue();
                        if (enqueued) {
	                        Dictionaries.getInstance().removeDictionaryListChangeListener
                                ( WeakDictionaryChangeListener.this);
                        }
                        return enqueued;
                    }
                };
        }

        @Override
		public void dictionaryListChanged() {
            LookupModel model = modelRef.get();
            if (model != null) {
	            model.setDictionaries
                    ( Arrays.asList( Dictionaries.getInstance().getDictionaries()));
            }
        }
    } // class WeakDictionaryChangeListener
    
    private static class SearchOnTextChangeListener implements DocumentListener {

    	private final Timer delayedActionTimer;

    	SearchOnTextChangeListener(ActionListener searchActionListener) {
    		delayedActionTimer = new Timer(500, searchActionListener);
    		delayedActionTimer.setRepeats(false);
    	}

    	@Override
        public void insertUpdate(DocumentEvent e) {
    		delayedActionTimer.restart();
        }

    	@Override
        public void removeUpdate(DocumentEvent e) {
    		delayedActionTimer.restart();
        }

    	@Override
        public void changedUpdate(DocumentEvent e) {
    		delayedActionTimer.restart();
        }

    }
   
    private static final long serialVersionUID = 1L;
    
    private static final String STYLE_SHEET = "/data/lookup-minimal.css";
    
    private final JTextField expression;
    private final LookupModel model;
    
    private final AsynchronousLookupEngine engine;
    private final LookupResultProxy lookupResultProxy;
    private final LookupResultList list;
    
    public SimpleLookup( Component[] additionalControls, LookupResultHyperlinker hyperlinker) {
        model = new LookupModel
            ( Arrays.asList( new SearchMode[] { ExpressionSearchModes.EXACT,
                                            ExpressionSearchModes.PREFIX,
                                            ExpressionSearchModes.ANY }),
              Arrays.asList( Dictionaries.getInstance().getDictionaries()),
              Collections.<LookupResultFilter> emptyList());
        Dictionaries.getInstance().addDictionaryListChangeListener
            ( new WeakDictionaryChangeListener( model));
        model.selectAllDictionaries( true);
        model.selectSearchField( DictionaryEntryField.WORD, true);
        model.selectSearchField( DictionaryEntryField.READING, true);
        model.selectSearchField( DictionaryEntryField.TRANSLATION, true);
        model.selectMatchMode( MatchMode.FIELD, true);

        setLayout( new BorderLayout());

        JPanel controls = new JPanel();
        controls.setLayout( new GridBagLayout());
        GridBagConstraints fixedC = new GridBagConstraints();
        fixedC.anchor = GridBagConstraints.WEST;
        fixedC.gridy = 0;
        fixedC.fill = GridBagConstraints.NONE;
        GridBagConstraints expandableC = new GridBagConstraints();
        expandableC.anchor = GridBagConstraints.WEST;
        expandableC.gridy = 0;
        expandableC.fill = GridBagConstraints.HORIZONTAL;
        expandableC.weightx = 1.0f;

        expression = new JTextField();
        JLabel expressionDescription = 
            new JLabel( JGloss.MESSAGES.getString( "wordlookup.enterexpression"));
        expressionDescription.setDisplayedMnemonic
            ( JGloss.MESSAGES.getString( "wordlookup.enterexpression.mk").charAt( 0));
        expressionDescription.setLabelFor( expression);
        controls.add( expressionDescription, fixedC);
        controls.add( expression, expandableC);
        expression.addActionListener(this);

        if (additionalControls != null) {
            for (Component additionalControl : additionalControls) {
	            controls.add( additionalControl, fixedC);
            }
        }

        this.add( controls, BorderLayout.NORTH);

        list = new LookupResultList( 100, SimpleLookup.class.getResource( STYLE_SHEET), false,
                                     hyperlinker);
        list.addHyperlinkListener( this);
        new HyperlinkKeyNavigator(new Color
                                  (Math.max(0, JGloss.PREFS.getInt
                                            (Preferences.ANNOTATION_HIGHLIGHT_COLOR, 0xcccccc))))
            .setTargetEditor(list.getResultPane());

        this.add( list, BorderLayout.CENTER);
        lookupResultProxy = new LookupResultProxy(list);
        engine = new AsynchronousLookupEngine( lookupResultProxy);
        
        expression.getDocument().addDocumentListener(new SearchOnTextChangeListener(this));
    }

    public void addLookupResultHandler(LookupResultHandler handler) {
        lookupResultProxy.addHandler(handler);
    }

    public void search( String text) {
        if (text == null || text.length()==0) {
	        return;
        }

        if (!text.equals(expression.getText())) {
        	expression.setText( text);
        }

        final LookupModel modelClone = model.clone();
        modelClone.setSearchExpression( text);

        // Try a lookup with each search mode until at least one entry is found.
        // To do this in asynchronous search mode, a runnable is created which is executed
        // after a search ends. If the search did not find any results, the runnable selects
        // the next search mode and repeats the search.
        final Iterator<SearchMode> searchModes = Arrays.asList(modelClone.getSearchModes()).iterator();
        modelClone.selectSearchMode(searchModes.next());
        engine.doLookup( modelClone, new Runnable() {
                @Override
				public void run() {
                    if (list.getEntryCount() == 0 && searchModes.hasNext()) {
                    	model.selectSearchMode(searchModes.next());
                    	engine.doLookup( modelClone, this);
                    }
                }
            });
    }

    public void addHyperlinkListener( HyperlinkListener listener) {
        list.addHyperlinkListener( listener);
    }

    public void removeHyperlinkListener( HyperlinkListener listener) {
        list.removeHyperlinkListener( listener);
    }

    public void addToXCVManager( XCVManager manager) {
        manager.addManagedComponent( expression);
        list.addToXCVManager( manager);
    }

    @Override
	public void actionPerformed( ActionEvent e) {
        search( expression.getText());
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

    /**
     * Return the component used to display the lookup result.
     */
    public LookupResultList getLookupResultList() { 
        return list;
    }

    protected void followReference( String type, String refKey) {
        if (LookupResultHyperlinker.REFERENCE_PROTOCOL.equals( type)) {
            ReferenceAttributeValue ref = (ReferenceAttributeValue) 
                ((HyperlinkAttributeFormatter.ReferencedAttribute) list.getReference( refKey)).getValue();
            if (ref instanceof SearchReference) {
            	SearchReference searchRef = (SearchReference) ref;
            	model.selectAllDictionaries(false);
            	model.selectDictionary(searchRef.getDictionary());
            	model.setSearchExpression(searchRef.getReference());
            	model.setSearchFieldSelection(searchRef.getSearchFieldSelection());
            } else {
            	throw new IllegalArgumentException("unsupported reference type " + ref);
            }
        }
    }
} // class SimpleLookup
