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

import static jgloss.ui.SetDistanceListener.DISTANCE_FORMAT;
import static jgloss.util.ObjectUtils.isEqual;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jgloss.JGloss;
import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntryField;
import jgloss.dictionary.MatchMode;
import jgloss.dictionary.SearchFieldSelection;
import jgloss.dictionary.SearchMode;
import jgloss.ui.util.UIUtilities;

/**
 * User configuration for dictionary lookups.
 *
 * @author Michael Koch
 */
public class LookupConfigPanel extends JPanel implements View<LookupModel>, LookupChangeListener {
	private class DictionaryChoiceActionListener implements ActionListener {
    	@Override
    	public void actionPerformed(ActionEvent e) {
            int choice = dictionaryChoice.getSelectedIndex();
            if (choice >= 0) { 
            	if (model.isDictionaryEnabled( choice)) {
            		model.selectDictionary( dictionaryChoice.getSelectedIndex(), true);
            	} else {
            		List<Dictionary> dictionaries = model.getSelectedDictionaries();
            		if (!dictionaries.isEmpty()) {
            			dictionaryChoice.setSelectedItem( dictionaries.get( 0));
            		} // else : no dictionary in model, ignore
            	}
            }
    	}
    }
    
	private class DictionaryCellRenderer implements ListCellRenderer {
        ListCellRenderer parent;

        public DictionaryCellRenderer( ListCellRenderer _parent) {
            parent = _parent;
        }

        @Override
		public Component getListCellRendererComponent( JList list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            boolean isEnabled = index<0 ||
                index>=model.getDictionaryCount() ||
                model.isDictionaryEnabled( index);

            Component out = parent.getListCellRendererComponent
                ( list, value, index, isSelected && isEnabled, cellHasFocus);
            out.setEnabled( isEnabled);

            return out;
        }
    } // class DictionaryCellRenderer
	
    private static final long serialVersionUID = 1L;

    static final Logger LOGGER = Logger.getLogger(LookupConfigPanel.class.getPackage().getName());
    
	private LookupModel model;

    private final JRadioButton[] searchModes;

    private final JCheckBox[] searchFields;
    private final JRadioButton[] matchModes;

    private final JCheckBox[] filters;

    private final JRadioButton dictionary;
    private final JRadioButton allDictionaries;
    private final JComboBox dictionaryChoice;

    private final JTextField expression;
    private final JTextField distance;

    private final SetSearchExpressionListener searchExpressionListener;

    private final SetDistanceListener setDistanceListener;

    public LookupConfigPanel( LookupModel _model) {
        setLayout( new GridBagLayout());

        // search mode setup
        SearchMode[] _searchModes = _model.getSearchModes();
        searchModes = new JRadioButton[_searchModes.length];
        ButtonGroup modesGroup = new ButtonGroup();
        JPanel modesPanel = new JPanel( new GridLayout( 0, 2));
        for ( int i=0; i<_searchModes.length; i++) {
            SearchMode mode = _searchModes[i];
            JRadioButton button = new JRadioButton( new SelectSearchModeAction(this, mode));
            modesGroup.add( button);
            modesPanel.add( button);
            searchModes[i] = button;
        }
        modesPanel = UIUtilities.createFlexiblePanel( modesPanel, false);
        modesPanel.setBorder( BorderFactory.createCompoundBorder 
                              ( BorderFactory.createTitledBorder
                                ( JGloss.MESSAGES.getString( "wordlookup.searchoptions")),
                                BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // search fields setup
        JPanel fieldsPanel = new JPanel( new GridLayout( 0, 1));
        searchFields = new JCheckBox[3];
        searchFields[0] = new JCheckBox(new SelectSearchFieldAction(this, DictionaryEntryField.WORD));
        fieldsPanel.add( searchFields[0]);

        searchFields[1] = new JCheckBox(new SelectSearchFieldAction(this, DictionaryEntryField.READING));
        fieldsPanel.add( searchFields[1]);

        searchFields[2] = new JCheckBox(new SelectSearchFieldAction(this, DictionaryEntryField.TRANSLATION));
        fieldsPanel.add( searchFields[2]);
        
        fieldsPanel = UIUtilities.createFlexiblePanel( fieldsPanel, false);
        fieldsPanel.setBorder( BorderFactory.createCompoundBorder
                               ( BorderFactory.createTitledBorder
                                 ( JGloss.MESSAGES.getString
                                   ( "wordlookup.searchfield")),
                                 BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // match mode setup
        JPanel matchPanel = new JPanel( new GridLayout( 0, 1));
        ButtonGroup matchmodeGroup = new ButtonGroup();
        matchModes = new JRadioButton[2];
        matchModes[0] = new JRadioButton(new SelectMatchModeAction(this, MatchMode.FIELD));
        matchmodeGroup.add( matchModes[0]);
        matchPanel.add( matchModes[0]);

        matchModes[1] = new JRadioButton(new SelectMatchModeAction(this, MatchMode.WORD));
        matchmodeGroup.add( matchModes[1]);
        matchPanel.add( matchModes[1]);
        
        matchPanel = UIUtilities.createFlexiblePanel( matchPanel, false);
        matchPanel.setBorder( BorderFactory.createCompoundBorder
                              ( BorderFactory.createTitledBorder
                                ( JGloss.MESSAGES.getString
                                  ( "wordlookup.matchmode")),
                                BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // dictionary selection setup
        dictionaryChoice = new JComboBox();
        dictionaryChoice.setRenderer( new DictionaryCellRenderer( dictionaryChoice.getRenderer()));
        // avoid layout flickering by setting a reasonably sized prototype value
        dictionaryChoice.setPrototypeDisplayValue( "aaaaaaaaaaaaaaaaaaaaaaaa");
        dictionaryChoice.addActionListener( new DictionaryChoiceActionListener());
        dictionaryChoice.setEditable( false);

        ButtonGroup dictionaries = new ButtonGroup();
        dictionary = new JRadioButton(new SelectAllDictionariesAction(this, false));
        dictionaries.add( dictionary);
        dictionary.addChangeListener( new ChangeListener() {
                @Override
				public void stateChanged( ChangeEvent e) {
                    dictionaryChoice.setEnabled( dictionary.isSelected());
                }
            });
        allDictionaries = new JRadioButton(new SelectAllDictionariesAction(this, true));
        dictionaries.add( allDictionaries);

        JPanel dictionaryPanel = new JPanel( new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        dictionaryPanel.add( dictionary, c);
        c = (GridBagConstraints) c.clone();
        c.fill = GridBagConstraints.HORIZONTAL;
        dictionaryPanel.add( dictionaryChoice, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        dictionaryPanel.add( allDictionaries, c);

        dictionaryPanel = UIUtilities.createFlexiblePanel( dictionaryPanel, false);
        dictionaryPanel.setBorder( BorderFactory.createCompoundBorder
                                   ( BorderFactory.createTitledBorder
                                     ( JGloss.MESSAGES.getString
                                       ( "wordlookup.dictionaryselection")),
                                     BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // filter selection setup
        JPanel filterPanel = new JPanel( new GridLayout( 0, 1));
        LookupResultFilter[] _filters = _model.getFilters();
        filters = new JCheckBox[_filters.length];
        for ( int i=0; i<_filters.length; i++) {
            JCheckBox box = new JCheckBox( new SelectFilterAction(this, _filters[i]));
            filterPanel.add( box);
            filters[i] = box;
        }
        filterPanel = UIUtilities.createFlexiblePanel( filterPanel, false);
        filterPanel.setBorder( BorderFactory.createCompoundBorder
                               ( BorderFactory.createTitledBorder
                                 ( JGloss.MESSAGES.getString
                                   ( "wordlookup.filter")),
                                 BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // input field setup
        JPanel inputPanel = new JPanel( new GridBagLayout());
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 5;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        GridBagConstraints c2 = (GridBagConstraints) c.clone();
        c2.weightx = 1;
        GridBagConstraints c3 = (GridBagConstraints) c.clone();
        c3.fill = GridBagConstraints.NONE;
        c3.weightx = 0;

        expression = new JTextField();
        JLabel expressionDescription = 
            new JLabel( JGloss.MESSAGES.getString( "wordlookup.enterexpression"));
        expressionDescription.setDisplayedMnemonic
            ( JGloss.MESSAGES.getString( "wordlookup.enterexpression.mk").charAt( 0));
        expressionDescription.setLabelFor( expression);
        inputPanel.add( expressionDescription, c3);
        inputPanel.add( expression, c);
        searchExpressionListener = new SetSearchExpressionListener(_model);
        expression.getDocument().addDocumentListener(searchExpressionListener);

        inputPanel.add( Box.createHorizontalStrut( 4), c3);
		distance = new JTextField();
		setDistanceListener = new SetDistanceListener(_model);
        distance.getDocument().addDocumentListener(setDistanceListener);

        // layout the panel
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        c.gridheight = 1;
        this.add( modesPanel, c);
        this.add( dictionaryPanel, c);
        if (filters.length > 0) {
	        this.add( filterPanel, c);
        }
        this.add( fieldsPanel, c);
        this.add( matchPanel, c);
        
        c = (GridBagConstraints) c.clone();
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = (filters.length == 0) ? 4 : 5;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add( inputPanel, c);

        // update font if prefs change
        UIManager.getDefaults().addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                @Override
				public void propertyChange( java.beans.PropertyChangeEvent e) { 
                    if (e.getPropertyName().equals( "TextField.font")) {
                        expression.setFont( (Font) e.getNewValue());
                    }
                }
            });

        setModel( _model);
    }

    /**
     * Update the list of dictionaries. This method is called after the dictionary list has
     * been changed in the preferences window.
     */
    private void updateDictionaryChoice() {
        Dictionary[] d = model.getDictionaries();
        dictionaryChoice.removeAllItems();
        if (d.length == 0) {
            dictionaryChoice.addItem( JGloss.MESSAGES.getString( "wordlookup.nodictionary"));
            dictionaryChoice.setEnabled( false);
        }
        else {
            dictionaryChoice.setEnabled( true);
            for (Dictionary element : d) {
                dictionaryChoice.addItem( element);
            }
        }
    }

    @Override
    public LookupModel getModel() { return model; }

    public final void setModel( LookupModel _model) {
        if (isEqual(model, _model)) {
	        return;
        }

        if (model != null) {
	        model.removeLookupChangeListener( this);
        }

        LookupModel oldModel = model;
        model = _model;
        model.setMultiDictionarySelectionMode( false);

        updateSearchModeSelection();
        updateSearchFieldSelection();
        updateFilterSelection();
        updateDictionaryChoice();
        updateDictionaryAvailability();
        updateDictionarySelection();
        updateInputAvailability();
        updateInputSelection();
        
        model.addLookupChangeListener( this);
        firePropertyChange(MODEL_PROPERTY_NAME, oldModel, model);
    }

	@Override
    public void addModelChangeListener(PropertyChangeListener modelChangeListener) {
		addPropertyChangeListener(MODEL_PROPERTY_NAME, modelChangeListener);
    }

    private void updateSearchFieldSelection() {
        SearchFieldSelection sf = model.getSearchFields();
        searchFields[0].setSelected( sf.isSelected( DictionaryEntryField.WORD));
        searchFields[1].setSelected( sf.isSelected( DictionaryEntryField.READING));
        searchFields[2].setSelected( sf.isSelected( DictionaryEntryField.TRANSLATION));

        matchModes[0].setSelected( sf.isSelected( MatchMode.FIELD));
        matchModes[1].setSelected( sf.isSelected( MatchMode.WORD));
    }

    private void updateSearchModeSelection() {
        int selectedIndex = model.getSelectedSearchModeIndex();
        if (selectedIndex != -1) {
	        searchModes[selectedIndex].setSelected( true);
        }
    }

    private void updateFilterSelection() {
        for ( int i=0; i<filters.length; i++) {
            filters[i].setSelected( model.isFilterSelected( i) && model.isFilterEnabled( i));
        }
    }

    private void updateDictionarySelection() {
        if (model.isAllDictionariesSelected()) {
            allDictionaries.setSelected( true);
            dictionaryChoice.setEnabled( false);
        }
        else {
            dictionary.setSelected( true);
            dictionaryChoice.setEnabled( true);
        }
        // since the model is not in multi-selection mode, there should always be
        // exactly one selection
        dictionaryChoice.setSelectedItem( model.getSelectedDictionaries().get( 0));
    }

    private void updateDictionaryAvailability() {
    }

    private void updateInputSelection() {
        String newExpression = model.getSearchExpression();
        if (!newExpression.equals(expression.getText())) {
            // The listener has to be removed because changing the text will generate a
            // delete event followed by an insert event, triggering two model updates
            // through the searchExpressionListener.
            expression.getDocument().removeDocumentListener(searchExpressionListener);
        	expression.setText( newExpression);
        	expression.getDocument().addDocumentListener(searchExpressionListener);
        }
        
        String newDistance = DISTANCE_FORMAT.format( model.getDistance());
        if (!newDistance.equals(distance.getText())) {
            distance.getDocument().removeDocumentListener(setDistanceListener);
        	distance.setText( newDistance);
        	distance.getDocument().addDocumentListener(setDistanceListener);
        }
    }

    private void updateInputAvailability() {
        expression.setEnabled( model.isSearchExpressionEnabled());
        distance.setEnabled( model.isDistanceEnabled());
    }

    @Override
	public void stateChanged( final LookupChangeEvent event) {
    	EventQueue.invokeLater(new Runnable() {
	        @Override
            public void run() {
	        	updateStateLater(event);
	        }
        });
    }

	private void updateStateLater(LookupChangeEvent event) {
        if (event.hasChanged( LookupChangeEvent.DICTIONARY_AVAILABILITY)) {
	        updateDictionaryAvailability();
        }
        if (event.hasChanged( LookupChangeEvent.FILTER_AVAILABILITY)) {
            // the selection state of the JCheckBoxes also depends on the availability
            updateFilterSelection();
        }
        if (event.hasChanged( LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY)) {
	        updateInputAvailability();
        }
        if (event.hasChanged( LookupChangeEvent.DICTIONARY_LIST_CHANGED)) {
            updateDictionaryChoice();
            updateDictionarySelection();
        }

        if (event.hasChanged( LookupChangeEvent.SEARCH_MODE_SELECTION)) {
        	updateSearchModeSelection();
        }
        if (event.hasChanged( LookupChangeEvent.DICTIONARY_SELECTION)) {
        	updateDictionarySelection();
        }
        if (event.hasChanged( LookupChangeEvent.MULTI_DICTIONARY_MODE)) {
        	updateDictionarySelection();
        }
        if (event.hasChanged( LookupChangeEvent.FILTER_SELECTION)) {
        	updateFilterSelection();
        }
        if (event.hasChanged( LookupChangeEvent.SEARCH_FIELDS_SELECTION)) {
        	updateSearchFieldSelection();
        }
        if (event.hasChanged( LookupChangeEvent.SEARCH_PARAMETERS)) {
        	updateInputSelection();
        }
    }

    public JTextField getSearchExpressionField() {
        return expression;
    }

    public JTextField getDistanceField() {
        return distance;
    }
} // class LookupConfigPanel
