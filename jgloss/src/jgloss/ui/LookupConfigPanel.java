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

import java.util.List;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

public class LookupConfigPanel extends JPanel implements LookupChangeListener,
                                                         ActionListener {
    protected LookupModel model;

    protected JRadioButton[] searchModes;
    protected final static String SEARCH_MODE_ACTION_COMMAND = "searchModes";

    protected JCheckBox[] searchFields;
    protected final static String SEARCH_FIELD_ACTION_COMMAND = "searchFields";
    protected final static String SEARCH_FIELD_KEY = "searchFieldsKey";
    protected JRadioButton[] matchModes;
    protected final static String MATCH_MODE_ACTION_COMMAND = "matchModes";
    protected final static String MATCH_MODE_KEY = "matchModesKey";

    protected JCheckBox[] filters;
    protected final static String FILTER_ACTION_COMMAND = "filters";

    protected JRadioButton dictionary;
    protected JRadioButton allDictionaries;
    protected final static String DICTIONARY_ACTION_COMMAND = "dictionaries";
    protected JComboBox dictionaryChoice;
    protected final static String DICTIONARY_CHOICE_ACTION_COMMAND = "dictionaryChoice";

    protected AutoSearchComboBox expression;
    protected JTextField distance;

    protected boolean enableActionEvents = false;
    protected boolean enableSelectionEvents = false;

    protected JButton search;
    protected ActionListener searchAction;

    protected class DictionaryCellRenderer implements ListCellRenderer {
        ListCellRenderer parent;

        public DictionaryCellRenderer( ListCellRenderer _parent) {
            parent = _parent;
        }

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
    
    public LookupConfigPanel( LookupModel _model) {
        this( _model, null);
    }

    public LookupConfigPanel( LookupModel _model, ActionListener _searchAction) {
        setLayout( new GridBagLayout());

        // search mode setup
        SearchMode[] _searchModes = _model.getSearchModes();
        searchModes = new JRadioButton[_searchModes.length];
        ButtonGroup modesGroup = new ButtonGroup();
        JPanel modesPanel = new JPanel( new GridLayout( 0, 1));
        for ( int i=0; i<_searchModes.length; i++) {
            SearchMode mode = _searchModes[i];
            JRadioButton button = new JRadioButton( mode.getName());
            button.setToolTipText( mode.getDescription());
            button.setActionCommand( SEARCH_MODE_ACTION_COMMAND);
            button.addActionListener( this);
            modesGroup.add( button);
            modesPanel.add( button);
            searchModes[i] = button;
        }
        modesPanel = UIUtilities.createFlexiblePanel( modesPanel, false);
        modesPanel.setBorder( BorderFactory.createCompoundBorder 
                              ( BorderFactory.createTitledBorder
                                ( JGloss.messages.getString( "wordlookup.searchoptions")),
                                BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // search fields setup
        JPanel fieldsPanel = new JPanel( new GridLayout( 0, 1));
        searchFields = new JCheckBox[3];
        searchFields[0] = new JCheckBox();
        searchFields[0].setActionCommand( SEARCH_FIELD_ACTION_COMMAND);
        searchFields[0].addActionListener( this);
        searchFields[0].putClientProperty( SEARCH_FIELD_KEY, DictionaryEntryField.WORD);
        UIUtilities.initButton( searchFields[0], "wordlookup.searchfield.word");
        fieldsPanel.add( searchFields[0]);
        searchFields[1] = new JCheckBox();
        searchFields[1].setActionCommand( SEARCH_FIELD_ACTION_COMMAND);
        searchFields[1].addActionListener( this);
        searchFields[1].putClientProperty( SEARCH_FIELD_KEY, DictionaryEntryField.READING);
        UIUtilities.initButton( searchFields[1], "wordlookup.searchfield.reading");
        fieldsPanel.add( searchFields[1]);
        searchFields[2] = new JCheckBox();
        searchFields[2].setActionCommand( SEARCH_FIELD_ACTION_COMMAND);
        searchFields[2].addActionListener( this);
        searchFields[2].putClientProperty( SEARCH_FIELD_KEY, DictionaryEntryField.TRANSLATION);
        UIUtilities.initButton( searchFields[2], "wordlookup.searchfield.translation");
        fieldsPanel.add( searchFields[2]);
        fieldsPanel = UIUtilities.createFlexiblePanel( fieldsPanel, false);
        fieldsPanel.setBorder( BorderFactory.createCompoundBorder
                               ( BorderFactory.createTitledBorder
                                 ( JGloss.messages.getString
                                   ( "wordlookup.searchfield")),
                                 BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // match mode setup
        JPanel matchPanel = new JPanel( new GridLayout( 0, 1));
        ButtonGroup matchmodeGroup = new ButtonGroup();
        matchModes = new JRadioButton[2];
        matchModes[0] = new JRadioButton();
        matchModes[0].setActionCommand( MATCH_MODE_ACTION_COMMAND);
        matchModes[0].addActionListener( this);
        matchModes[0].putClientProperty( MATCH_MODE_KEY, MatchMode.FIELD);
        UIUtilities.initButton( matchModes[0], "wordlookup.matchmode.field");
        matchmodeGroup.add( matchModes[0]);
        matchPanel.add( matchModes[0]);
        matchModes[1] = new JRadioButton();
        matchModes[1].setActionCommand( MATCH_MODE_ACTION_COMMAND);
        matchModes[1].addActionListener( this);
        matchModes[1].putClientProperty( MATCH_MODE_KEY, MatchMode.WORD);
        UIUtilities.initButton( matchModes[1], "wordlookup.matchmode.word");
        matchmodeGroup.add( matchModes[1]);
        matchPanel.add( matchModes[1]);
        matchPanel = UIUtilities.createFlexiblePanel( matchPanel, false);
        matchPanel.setBorder( BorderFactory.createCompoundBorder
                              ( BorderFactory.createTitledBorder
                                ( JGloss.messages.getString
                                  ( "wordlookup.matchmode")),
                                BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // dictionary selection setup
        dictionaryChoice = new JComboBox();
        dictionaryChoice.setRenderer( new DictionaryCellRenderer( dictionaryChoice.getRenderer()));
        // avoid layout flickering by setting a reasonably sized prototype value
        dictionaryChoice.setPrototypeDisplayValue( "aaaaaaaaaaaaaaaaaaaaaaaa");
        dictionaryChoice.setActionCommand( DICTIONARY_CHOICE_ACTION_COMMAND);
        dictionaryChoice.addActionListener( this);
        dictionaryChoice.setEditable( false);

        ButtonGroup dictionaries = new ButtonGroup();
        dictionary = new JRadioButton();
        dictionary.setActionCommand( DICTIONARY_ACTION_COMMAND);
        dictionary.addActionListener( this);
        UIUtilities.initButton( dictionary, "wordlookup.choice.dictionary");
        dictionaries.add( dictionary);
        dictionary.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e) {
                    dictionaryChoice.setEnabled( dictionary.isSelected());
                }
            });
        allDictionaries = new JRadioButton();
        allDictionaries.setActionCommand( DICTIONARY_ACTION_COMMAND);
        allDictionaries.addActionListener( this);
        UIUtilities.initButton( allDictionaries, "wordlookup.choice.alldictionaries");
        dictionaries.add( allDictionaries);

        JPanel dictionaryPanel = new JPanel( new GridLayout( 0, 2));
        dictionaryPanel.add( dictionary);
        dictionaryPanel.add( dictionaryChoice);
        dictionaryPanel.add( allDictionaries);

        dictionaryPanel = UIUtilities.createFlexiblePanel( dictionaryPanel, false);
        dictionaryPanel.setBorder( BorderFactory.createCompoundBorder
                                   ( BorderFactory.createTitledBorder
                                     ( JGloss.messages.getString
                                       ( "wordlookup.dictionaryselection")),
                                     BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // filter selection setup
        JPanel filterPanel = new JPanel( new GridLayout( 0, 1));
        LookupResultFilter[] _filters = _model.getFilters();
        filters = new JCheckBox[_filters.length];
        for ( int i=0; i<_filters.length; i++) {
            JCheckBox box = new JCheckBox( _filters[i].getName());
            box.setActionCommand( FILTER_ACTION_COMMAND);
            box.addActionListener( this);
            box.setToolTipText( _filters[i].getDescription());
            filterPanel.add( box);
            filters[i] = box;
        }
        filterPanel = UIUtilities.createFlexiblePanel( filterPanel, false);
        filterPanel.setBorder( BorderFactory.createCompoundBorder
                               ( BorderFactory.createTitledBorder
                                 ( JGloss.messages.getString
                                   ( "wordlookup.filter")),
                                 BorderFactory.createEmptyBorder( 2, 2, 2, 2)));

        // input field setup
        JPanel inputPanel = new JPanel( new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
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

        expression = new AutoSearchComboBox( _model, 50);
        JLabel expressionDescription = 
            new JLabel( JGloss.messages.getString( "wordlookup.enterexpression"));
        expressionDescription.setDisplayedMnemonic
            ( JGloss.messages.getString( "wordlookup.enterexpression.mk").charAt( 0));
        expressionDescription.setLabelFor( expression);
        inputPanel.add( expressionDescription, c3);
        inputPanel.add( expression, c);

        inputPanel.add( Box.createHorizontalStrut( 4), c3);
        distance = new JTextField();
        JLabel distanceDescription = 
            new JLabel( JGloss.messages.getString( "wordlookup.enterdistance"));
        distanceDescription.setDisplayedMnemonic
            ( JGloss.messages.getString( "wordlookup.enterdistance.mk").charAt( 0));
        distanceDescription.setLabelFor( expression);
        inputPanel.add( distanceDescription, c3);
        inputPanel.add( distance, c2);
        
        if (_searchAction != null) {
            inputPanel.add( Box.createHorizontalStrut( 4), c3);
            search = new JButton();
            UIUtilities.initButton( search, "wordlookup.search");
            search.addActionListener( this);
            inputPanel.add( search, c3);
            searchAction = _searchAction;
        }

        // layout the panel
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 2;
        this.add( modesPanel, c);

        c = (GridBagConstraints) c.clone();
        c.gridx = 2;
        c.gridy = GridBagConstraints.RELATIVE;
        c.gridheight = filters.length > 0 ? 1 : 2;
        this.add( dictionaryPanel, c);
        if (filters.length > 0)
            this.add( filterPanel, c);

        c = (GridBagConstraints) c.clone();
        c.gridx = 1;
        c.gridheight = 1;
        this.add( fieldsPanel, c);
        this.add( matchPanel, c);
        
        c = (GridBagConstraints) c.clone();
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add( inputPanel, c);

        // update font if prefs change
        UIManager.getDefaults().addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                public void propertyChange( java.beans.PropertyChangeEvent e) { 
                    if (e.getPropertyName().equals( "TextField.font")) {
                        expression.setFont( (Font) e.getNewValue());
                    }
                }
            });

        setModel( _model);

        enableActionEvents = true;
        enableSelectionEvents = true;
    }

    public JButton getSearchButton() { return search; }

    /**
     * Update the list of dictionaries. This method is called after the dictionary list has
     * been changed in the preferences window.
     */
    protected void updateDictionaryChoice() {
        Dictionary[] d = model.getDictionaries();
        dictionaryChoice.removeAllItems();
        if (d.length == 0) {
            dictionaryChoice.addItem( JGloss.messages.getString( "wordlookup.nodictionary"));
            dictionaryChoice.setEnabled( false);
        }
        else {
            dictionaryChoice.setEnabled( true);
            for ( int i=0; i<d.length; i++) {
                dictionaryChoice.addItem( d[i]);
            }
        }
    }

    public LookupModel getModel() { return model; }

    public void setModel( LookupModel _model) {
        if (model == _model)
            return;

        if (model != null)
            model.removeLookupChangeListener( this);

        model = _model;
        model.setMultiDictionarySelectionMode( false);
        expression.setLookupModel( model);

        // set selected and enabled status from model
        enableActionEvents = false;
        updateSearchModeAvailability();
        updateSearchModeSelection();
        updateSearchFieldAvailability();
        updateSearchFieldSelection();
        updateFilterAvailability();
        updateFilterSelection();
        updateDictionaryChoice();
        updateDictionaryAvailability();
        updateDictionarySelection();
        updateInputAvailability();
        updateInputSelection();
        enableActionEvents = true;

        model.addLookupChangeListener( this);
    }

    protected void updateSearchFieldSelection() {
        SearchFieldSelection sf = model.getSearchFields();
        searchFields[0].setSelected( sf.isSelected( DictionaryEntryField.WORD));
        searchFields[1].setSelected( sf.isSelected( DictionaryEntryField.READING));
        searchFields[2].setSelected( sf.isSelected( DictionaryEntryField.TRANSLATION));

        matchModes[0].setSelected( sf.isSelected( MatchMode.FIELD));
        matchModes[1].setSelected( sf.isSelected( MatchMode.WORD));
    }

    protected void updateSearchFieldAvailability() {
        SearchFieldSelection sf = model.getEnabledSearchFields();
        searchFields[0].setEnabled( sf.isSelected( DictionaryEntryField.WORD));
        searchFields[1].setEnabled( sf.isSelected( DictionaryEntryField.READING));
        searchFields[2].setEnabled( sf.isSelected( DictionaryEntryField.TRANSLATION));

        matchModes[0].setEnabled( sf.isSelected( MatchMode.FIELD));
        matchModes[1].setEnabled( sf.isSelected( MatchMode.WORD));
    }

    protected void updateSearchModeSelection() {
        int selectedIndex = model.getSelectedSearchModeIndex();
        if (selectedIndex != -1)
            searchModes[selectedIndex].setSelected( true);
    }

    protected void updateSearchModeAvailability() {
        for ( int i=0; i<searchModes.length; i++) {
            searchModes[i].setEnabled( model.isSearchModeEnabled( i));
        }
    }

    protected void updateFilterSelection() {
        for ( int i=0; i<filters.length; i++) {
            filters[i].setSelected( model.isFilterSelected( i) && model.isFilterEnabled( i));
        }
    }

    protected void updateFilterAvailability() {
        for ( int i=0; i<filters.length; i++) {
            filters[i].setEnabled( model.isFilterEnabled( i));
        }
    }

    protected void updateDictionarySelection() {
        if (model.isAllDictionariesSelected()) {
            allDictionaries.setSelected( true);
            dictionaryChoice.setEnabled( false);
        }
        else {
            dictionary.setSelected( true);
            dictionaryChoice.setEnabled( true);
        }
        try {
            // since the model is not in multi-selection mode, there should always be
            // exactly one selection
            dictionaryChoice.setSelectedItem( model.getSelectedDictionaries().get( 0));
        } catch (IndexOutOfBoundsException ex) {
            // no dictionary in model, ignore
        }
    }

    protected void updateDictionaryAvailability() {
    }

    protected void updateInputSelection() {
        expression.setSelectedItem( model.getSearchExpression());
        distance.setText( String.valueOf( model.getDistance()));
    }

    protected void updateInputAvailability() {
        expression.setEnabled( model.isSearchExpressionEnabled());
        distance.setEnabled( model.isDistanceEnabled());
    }

    public void stateChanged( LookupChangeEvent event) {
        enableActionEvents = false;

        if (event.hasChanged( LookupChangeEvent.SEARCH_MODE_AVAILABILITY))
            updateSearchModeAvailability();
        if (event.hasChanged( LookupChangeEvent.DICTIONARY_AVAILABILITY))
            updateDictionaryAvailability();
        if (event.hasChanged( LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY))
            updateSearchFieldAvailability();
        if (event.hasChanged( LookupChangeEvent.FILTER_AVAILABILITY)) {
            updateFilterAvailability();
            // the selection state of the JCheckBoxes also depends on the availability
            updateFilterSelection();
        }
        if (event.hasChanged( LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY))
            updateInputAvailability();
        if (event.hasChanged( LookupChangeEvent.DICTIONARY_LIST_CHANGED)) {
            updateDictionaryChoice();
            updateDictionarySelection();
        }

        if (enableSelectionEvents) {
            if (event.hasChanged( LookupChangeEvent.SEARCH_MODE_SELECTION))
                updateSearchModeSelection();
            if (event.hasChanged( LookupChangeEvent.DICTIONARY_SELECTION))
                updateDictionarySelection();
            if (event.hasChanged( LookupChangeEvent.MULTI_DICTIONARY_MODE))
                updateDictionarySelection();
            if (event.hasChanged( LookupChangeEvent.FILTER_SELECTION))
                updateFilterSelection();
            if (event.hasChanged( LookupChangeEvent.SEARCH_FIELDS_SELECTION))
                updateSearchFieldSelection();
            if (event.hasChanged( LookupChangeEvent.SEARCH_PARAMETERS))
                updateInputSelection();
        }

        enableActionEvents = true;
    }

    public JComboBox getSearchExpressionField() {
        return expression;
    }

    public JTextField getDistanceField() {
        return distance;
    }

    public void actionPerformed( ActionEvent action) {
        if (!enableActionEvents)
            // ignore action events during panel setup phase
            return;

        // ignore state changes in the model triggered through
        // the action
        enableSelectionEvents = false;

        if (action.getSource() == search) {
            model.setSearchExpression( expression.getSelectedItem().toString());
            try {
                model.setDistance( Integer.parseInt( distance.getText()));
            } catch (NumberFormatException ex) {}
            searchAction.actionPerformed( action);
            return;
        }
        if (action.getActionCommand().equals( SEARCH_MODE_ACTION_COMMAND)) {
            for ( int i=0; i<searchModes.length; i++) {
                if (searchModes[i] == action.getSource()) {
                    model.selectSearchMode( i);
                    break;
                }
            }
        }
        else if (action.getActionCommand().equals( SEARCH_FIELD_ACTION_COMMAND)) {
            JCheckBox source = (JCheckBox) action.getSource();
            model.selectSearchField( (DictionaryEntryField) source.getClientProperty
                                     ( SEARCH_FIELD_KEY), source.isSelected());
        }
        else if (action.getActionCommand().equals( MATCH_MODE_ACTION_COMMAND)) {
            JRadioButton source = (JRadioButton) action.getSource();
            model.selectMatchMode( (MatchMode) source.getClientProperty
                                   ( MATCH_MODE_KEY), source.isSelected());
        }
        else if (action.getActionCommand().equals( FILTER_ACTION_COMMAND)) {
            for ( int i=0; i<searchModes.length; i++) {
                if (filters[i] == action.getSource()) {
                    model.selectFilter( i, filters[i].isSelected());
                    break;
                }
            }
        }
        else if (action.getActionCommand().equals( DICTIONARY_ACTION_COMMAND)) {
            // either dictionary or allDictionaries radio button selected
            model.selectAllDictionaries( action.getSource() != dictionary);
        }
        else if (action.getActionCommand().equals( DICTIONARY_CHOICE_ACTION_COMMAND)) {
            int choice = dictionaryChoice.getSelectedIndex();
            if (!model.isDictionaryEnabled( choice))
                try {
                    dictionaryChoice.setSelectedItem( model.getSelectedDictionaries().get( 0));
                } catch (IndexOutOfBoundsException ex) {
                    // no dictionary in model, ignore
                }
            else
                model.selectDictionary( dictionaryChoice.getSelectedIndex(), true);
        }

        enableSelectionEvents = true;
    }
} // class LookupConfigPanel
