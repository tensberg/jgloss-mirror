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

import jgloss.dictionary.*;

import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class LookupModel implements Cloneable {
    protected List searchModes;

    protected List dictionaries;
    protected boolean allDictionariesSelected = false;
    protected boolean multiDictionaryMode = false;
    protected boolean multiDictionarySelection = false;

    protected List filters;

    protected SearchFieldSelection searchFields = 
        new SearchFieldSelection( true, true, true, true, false);
    protected SearchFieldSelection searchFieldsEnabled = new SearchFieldSelection();

    protected String searchExpression = "";
    protected boolean searchExpressionEnabled = false;
    protected int distance = 1;
    protected boolean distanceEnabled = false;

    protected List listeners = new ArrayList( 5);

    public LookupModel( List _searchModes, List _dictionaries, List _filters) {
        searchModes = new ArrayList( _searchModes.size());
        for ( Iterator i=_searchModes.iterator(); i.hasNext(); ) {
            searchModes.add( new StateWrapper( i.next()));
        }

        dictionaries = new ArrayList( _dictionaries.size());
        for ( Iterator i=_dictionaries.iterator(); i.hasNext(); ) {
            dictionaries.add( new StateWrapper( i.next()));
        }

        filters = new ArrayList( _filters.size());
        for ( Iterator i=_filters.iterator(); i.hasNext(); ) {
            filters.add( new StateWrapper( i.next()));
        }
        
        if (dictionaries.size() > 0) {
            ((StateWrapper) dictionaries.get( 0)).setEnabled( true);
            ((StateWrapper) dictionaries.get( 0)).setSelected( true);
            updateSearchModeAvailability();
            SearchMode selectedMode = null;
            for ( Iterator i=searchModes.iterator(); i.hasNext(); ) {
                StateWrapper wrapper = (StateWrapper) i.next();
                if (wrapper.isEnabled()) {
                    wrapper.setSelected( true);
                    selectedMode = (SearchMode) wrapper.getObject();
                    break;
                }
            }
            if (selectedMode != null) {
                updateDictionaryAvailability( selectedMode);
                updateSearchParametersAvailability( selectedMode);
                updateSearchFieldsAvailability( selectedMode);
                updateFilterAvailability();
            }
        }
        else {
            ((StateWrapper) searchModes.get( 0)).setSelected( true);
        }
    }

    public SearchMode getSearchMode( int index) {
        return (SearchMode) ((StateWrapper) searchModes.get( index)).getObject();
    }

    public SearchMode[] getSearchModes() {
        SearchMode[] out = new SearchMode[searchModes.size()];
        for ( ListIterator i=searchModes.listIterator(); i.hasNext(); ) {
            SearchMode mode = (SearchMode) ((StateWrapper) i.next()).getObject();
            out[i.previousIndex()] = mode;
        }
        return out;
    }

    public SearchMode getSelectedSearchMode() {
        return getSearchMode( getSelectedSearchModeIndex());
    }

    public int getSelectedSearchModeIndex() {
        for ( ListIterator i=searchModes.listIterator(); i.hasNext(); ) {
            StateWrapper wrapper = (StateWrapper) i.next();
            if (wrapper.isSelected())
                return i.previousIndex();
        }

        return -1;
    }

    public boolean isSearchModeSelected( int index) {
        return ((StateWrapper) searchModes.get( index)).isSelected();
    }

    public boolean isSearchModeEnabled( int index) {
        return ((StateWrapper) searchModes.get( index)).isEnabled();
    }

    public boolean selectSearchMode( SearchMode mode) {
        for ( ListIterator i=searchModes.listIterator(); i.hasNext(); ) {
            StateWrapper wrapper = (StateWrapper) i.next();
            if (wrapper.getObject() == mode) {
                selectSearchMode( i.previousIndex());
                return true;
            }
        }
        return false;
    }

    public void selectSearchMode( int index) {
        StateWrapper newModeWrapper = (StateWrapper) searchModes.get( index);
        if (newModeWrapper.isSelected())
            return; // nothing to do
        if (!newModeWrapper.isEnabled())
            throw new IllegalArgumentException( "selected search mode not enabled");

        ((StateWrapper) searchModes.get( getSelectedSearchModeIndex())).setSelected( false);
        newModeWrapper.setSelected( true);
        SearchMode newMode = (SearchMode) newModeWrapper.getObject();

        boolean dictionaryChanged = updateDictionaryAvailability( newMode);
        boolean parameterFieldsChanged = updateSearchParametersAvailability( newMode);
        boolean searchFieldsChanged = updateSearchFieldsAvailability( newMode);
        boolean filterChanged = updateFilterAvailability();
        fireLookupChange( new LookupChangeEvent
                          ( this, LookupChangeEvent.SEARCH_MODE_SELECTION |
                            (dictionaryChanged ? LookupChangeEvent.DICTIONARY_AVAILABILITY : 0) | 
                            (filterChanged ? LookupChangeEvent.FILTER_AVAILABILITY : 0) |
                            (parameterFieldsChanged ? 
                            LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY : 0) |
                            (searchFieldsChanged ? 
                            LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0)));
    }

    public void setMultiDictionarySelectionMode( boolean mode) {
        if (mode == multiDictionaryMode)
            return;

        boolean dictionarySelectionChanged = false;
        if (multiDictionarySelection) {
            boolean firstDictionary = true;
            for ( Iterator i=dictionaries.iterator(); i.hasNext(); ) {
                StateWrapper wrapper = (StateWrapper) i.next();
                if (wrapper.isSelected()) {
                    if (firstDictionary)
                        firstDictionary = false;
                    else {
                        wrapper.setSelected( false);
                        dictionarySelectionChanged = true;
                    }
                }
            }
            multiDictionarySelection = false;
        }

        boolean searchModeChanged = false;
        boolean parameterFieldsChanged = false;
        boolean searchFieldsChanged = false;
        boolean filterChanged = false;
        if (dictionarySelectionChanged) {
            SearchMode searchmode = getSearchMode( getSelectedSearchModeIndex());
            searchModeChanged = updateSearchModeAvailability();
            parameterFieldsChanged = updateSearchParametersAvailability( searchmode);
            searchFieldsChanged = updateSearchFieldsAvailability( searchmode);
            filterChanged = updateFilterAvailability();
        }

        multiDictionaryMode = mode;

        fireLookupChange( new LookupChangeEvent
                          ( this, LookupChangeEvent.MULTI_DICTIONARY_MODE |
                            (searchModeChanged ? LookupChangeEvent.SEARCH_MODE_AVAILABILITY : 0) | 
                            (filterChanged ? LookupChangeEvent.FILTER_AVAILABILITY : 0) |
                            (parameterFieldsChanged ? 
                            LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY : 0) |
                            (searchFieldsChanged ? 
                            LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0)));
    }

    public void selectAllDictionaries( boolean select) {
        if (allDictionariesSelected == select)
            return;

        allDictionariesSelected = select;

        SearchMode searchmode = getSelectedSearchMode();
        boolean searchModeChanged = updateSearchModeAvailability();
        boolean parameterFieldsChanged = updateSearchParametersAvailability( searchmode);
        boolean searchFieldsChanged = updateSearchFieldsAvailability( searchmode);
        boolean filterChanged = updateFilterAvailability();
        fireLookupChange( new LookupChangeEvent
                          ( this, LookupChangeEvent.DICTIONARY_SELECTION |
                            (searchModeChanged ? LookupChangeEvent.SEARCH_MODE_AVAILABILITY : 0) | 
                            (filterChanged ? LookupChangeEvent.FILTER_AVAILABILITY : 0) |
                            (parameterFieldsChanged ? 
                            LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY : 0) |
                            (searchFieldsChanged ? 
                            LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0)));
    }

    public boolean isAllDictionariesSelected() { return allDictionariesSelected; }

    public void setDictionaries( List _newDictionaries) {
        List newDictionaries = new ArrayList( _newDictionaries);
        Map oldDictionaries = new HashMap( (int) (dictionaries.size()*1.5));
        for ( Iterator i=dictionaries.iterator(); i.hasNext(); ) {
            StateWrapper wrapper = (StateWrapper) i.next();
            oldDictionaries.put( wrapper.getObject(), wrapper);
        }

        boolean seenSelectedDictionary = false;
        multiDictionarySelection = false;

        for ( ListIterator i=newDictionaries.listIterator(); i.hasNext(); ) {
            Object d = i.next();
            StateWrapper wrapper = (StateWrapper) oldDictionaries.get( d);
            if (wrapper == null)
                wrapper = new StateWrapper( d, false, false);
            else {
                if (wrapper.isSelected() && wrapper.isEnabled()) {
                    if (seenSelectedDictionary) {
                        multiDictionarySelection = true;
                    }
                    else {
                        seenSelectedDictionary = true;
                    }
                }
            }
            i.set( wrapper);
        }

        boolean selectNewDictionary = dictionaries.size()==0 && newDictionaries.size()>0 && 
            !multiDictionarySelection;
        dictionaries = newDictionaries;

        boolean searchModeChanged = false;
        if (selectNewDictionary) {
            ((StateWrapper) newDictionaries.get( 0)).setEnabled( true);
            ((StateWrapper) newDictionaries.get( 0)).setSelected( true);
            searchModeChanged = updateSearchModeAvailability();
            SearchMode selectedMode = null;
            for ( Iterator i=searchModes.iterator(); i.hasNext(); ) {
                StateWrapper wrapper = (StateWrapper) i.next();
                if (wrapper.isEnabled()) {
                    wrapper.setSelected( true);
                    selectedMode = (SearchMode) wrapper.getObject();
                    break;
                }
            }
        }
        else {
            ((StateWrapper) searchModes.get( 0)).setSelected( true);
        }

        searchModeChanged |= updateSearchModeAvailability();
        SearchMode searchmode = getSelectedSearchMode();
        boolean dictionaryChanged = updateDictionaryAvailability( searchmode);
        boolean parameterFieldsChanged = updateSearchParametersAvailability( searchmode);
        boolean searchFieldsChanged = updateSearchFieldsAvailability( searchmode);
        boolean filterChanged = updateFilterAvailability();
        fireLookupChange( new LookupChangeEvent
                          ( this, LookupChangeEvent.DICTIONARY_LIST_CHANGED |
                            (searchModeChanged ? LookupChangeEvent.SEARCH_MODE_AVAILABILITY : 0) | 
                            (dictionaryChanged ? LookupChangeEvent.DICTIONARY_AVAILABILITY : 0) | 
                            (filterChanged ? LookupChangeEvent.FILTER_AVAILABILITY : 0) |
                            (parameterFieldsChanged ? 
                             LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY : 0) |
                            (searchFieldsChanged ? 
                             LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0)));
    }

    public void selectDictionary( int index, boolean select) {
        StateWrapper newDictionaryWrapper = (StateWrapper) dictionaries.get( index);
        if (newDictionaryWrapper.isSelected() == select)
            return; // nothing to do
        if (select && !newDictionaryWrapper.isEnabled())
            throw new IllegalArgumentException( "selected dictionary not enabled");

        if (!multiDictionaryMode && select) {
            // clear old selection
            for ( Iterator i=dictionaries.iterator(); i.hasNext(); ) {
                StateWrapper wrapper = (StateWrapper) i.next();
                if (wrapper.isEnabled()) {
                    wrapper.setSelected( false);
                    break;
                }
            }
        }

        newDictionaryWrapper.setSelected( select);

        // check if more than one dictionary is selected
        boolean seenDictionary = false;
        multiDictionarySelection = false;
        for ( Iterator i=dictionaries.iterator(); i.hasNext() && !multiDictionarySelection; ) {
            StateWrapper wrapper = (StateWrapper) i.next();
            if (wrapper.isSelected()) {
                if (seenDictionary)
                    multiDictionarySelection = true;
                else
                    seenDictionary = true;
            }
        }

        SearchMode searchmode = getSearchMode( getSelectedSearchModeIndex());
        boolean searchModeChanged = updateSearchModeAvailability();
        boolean parameterFieldsChanged = updateSearchParametersAvailability( searchmode);
        boolean searchFieldsChanged = updateSearchFieldsAvailability( searchmode);
        boolean filterChanged = updateFilterAvailability();
        fireLookupChange( new LookupChangeEvent
                          ( this, LookupChangeEvent.DICTIONARY_SELECTION |
                            (searchModeChanged ? LookupChangeEvent.SEARCH_MODE_AVAILABILITY : 0) | 
                            (filterChanged ? LookupChangeEvent.FILTER_AVAILABILITY : 0) |
                            (parameterFieldsChanged ? 
                            LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY : 0) |
                            (searchFieldsChanged ? 
                            LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0)));
    }

    public boolean isDictionarySelected( int index) {
        return allDictionariesSelected || ((StateWrapper) dictionaries.get( index)).isSelected();
    }

    public boolean isDictionaryEnabled( int index) {
        return ((StateWrapper) dictionaries.get( index)).isEnabled();
    }

    public int getDictionaryCount() {
        return dictionaries.size();
    }

    public Dictionary[] getDictionaries() {
        Dictionary[] out = new Dictionary[dictionaries.size()];
        for ( ListIterator i=dictionaries.listIterator(); i.hasNext(); ) {
            Dictionary dictionary = (Dictionary) ((StateWrapper) i.next()).getObject();
            out[i.previousIndex()] = dictionary;
        }
        return out;
    }

    public List getSelectedDictionaries() {
        List out = new ArrayList( dictionaries.size());
        for ( Iterator i=dictionaries.iterator(); i.hasNext(); ) {
            StateWrapper wrapper = (StateWrapper) i.next();
            if ((allDictionariesSelected || wrapper.isSelected()) && wrapper.isEnabled())
                out.add( wrapper.getObject());
        }
        return out;
    }

    public void selectSearchField( DictionaryEntryField field, boolean selected) {
        if (searchFields.isSelected( field) == selected)
            return;
        if (!searchFieldsEnabled.isSelected( field))
            throw new IllegalStateException( "selected field is not enabled");
        searchFields.select( field, selected);
        
        fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.SEARCH_FIELDS_SELECTION));
    }

    public void selectMatchMode( MatchMode mode, boolean selected) {
        if (searchFields.isSelected( mode) == selected)
            return;
        if (!searchFieldsEnabled.isSelected( mode))
            throw new IllegalStateException( "selected field is not enabled");
        searchFields.select( mode, selected);
        searchFields.select( mode==MatchMode.FIELD ? MatchMode.WORD : MatchMode.FIELD, !selected);
        
        fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.SEARCH_FIELDS_SELECTION));
    }

    public SearchFieldSelection getSearchFields() { return searchFields; }
    public SearchFieldSelection getEnabledSearchFields() { return searchFieldsEnabled; }

    public void selectFilter( int index, boolean select) {
        StateWrapper filterWrapper = (StateWrapper) filters.get( index);
        if (filterWrapper.isSelected() == select)
            return; // nothing to do
        if (select && !filterWrapper.isEnabled())
            throw new IllegalArgumentException( "selected filter not enabled");

        filterWrapper.setSelected( select);
        fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.FILTER_SELECTION));
    }

    public boolean isFilterSelected( int index) {
        return ((StateWrapper) filters.get( index)).isSelected();
    }

    public boolean isFilterEnabled( int index) {
        return ((StateWrapper) filters.get( index)).isEnabled();
    }

    public LookupResultFilter[] getFilters() {
        LookupResultFilter[] out = new LookupResultFilter[filters.size()];
        for ( ListIterator i=filters.listIterator(); i.hasNext(); ) {
            LookupResultFilter filter = (LookupResultFilter) ((StateWrapper) i.next()).getObject();
            out[i.previousIndex()] = filter;
        }
        return out;
    }

    public List getSelectedFilters() {
        List out = new ArrayList( filters.size());
        for ( Iterator i=filters.iterator(); i.hasNext(); ) {
            StateWrapper filterWrapper = (StateWrapper) i.next();
            if (filterWrapper.isSelected() && filterWrapper.isEnabled())
                out.add( filterWrapper.getObject());
        }

        return out;
    }

    public String getSearchExpression() { return searchExpression; }

    public void setSearchExpression( String _searchExpression) {
        if (!searchExpression.equals( _searchExpression)) {
            searchExpression = _searchExpression;
            fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.SEARCH_PARAMETERS));
        }
    }

    public boolean isSearchExpressionEnabled() { return searchExpressionEnabled; }

    public int getDistance() { return distance; }

    public void setDistance( int _distance) {
        if (distance != _distance) {
            distance = _distance;
            fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.SEARCH_PARAMETERS));
        }
    }

    public boolean isDistanceEnabled() { return distanceEnabled; }
    
    protected boolean updateSearchModeAvailability() {
        boolean changed = false;

        modes: for ( Iterator i=searchModes.iterator(); i.hasNext(); ) {
            StateWrapper wrapper = (StateWrapper) i.next();
            SearchMode mode = (SearchMode) wrapper.getObject();
            for ( Iterator j=dictionaries.iterator(); j.hasNext(); ) {
                StateWrapper wrapperd = (StateWrapper) j.next();
                Dictionary dic = (Dictionary) wrapperd.getObject();
                if (wrapperd.isEnabled() && (allDictionariesSelected || wrapperd.isSelected()) &&
                    !dic.supports( mode, !(allDictionariesSelected || multiDictionarySelection))) {
                    if (wrapper.isEnabled()) {
                        wrapper.setEnabled( false);
                        changed = true;
                    }
                    continue modes;
                }
                // if we get here, this search mode is supported by all selected dictionaries
                if (!wrapper.isEnabled()) {
                    wrapper.setEnabled( true);
                    changed = true;
                }
            }
        }

        return changed;
    }

    protected boolean updateDictionaryAvailability( SearchMode searchmode) {
        boolean changed = false;

        for ( Iterator i=dictionaries.iterator(); i.hasNext(); ) {
            StateWrapper wrapper = (StateWrapper) i.next();
            Dictionary dic = (Dictionary) wrapper.getObject();
            boolean newState = dic.supports( searchmode, 
                                             !(allDictionariesSelected || multiDictionarySelection));
            if (newState != wrapper.isEnabled()) {
                changed = true;
                wrapper.setEnabled( newState);
            }
        }

        return changed;
    }

    protected boolean updateFilterAvailability() {
        boolean changed = false;
        
        filters: for ( Iterator i=filters.iterator(); i.hasNext(); ) {
            StateWrapper wrapper = (StateWrapper) i.next();
            LookupResultFilter filter = (LookupResultFilter) wrapper.getObject();
            for ( Iterator j=dictionaries.iterator(); j.hasNext(); ) {
                StateWrapper wrapperd = (StateWrapper) j.next();
                Dictionary dic = (Dictionary) wrapperd.getObject();
                if (wrapperd.isEnabled() && (allDictionariesSelected || wrapperd.isSelected()) &&
                    filter.enableFor( dic)) {
                    if (!wrapper.isEnabled()) {
                        wrapper.setEnabled( true);
                        changed = true;
                    }
                    continue filters;
                }
            }
            // no fitting dictionary, filter should be disabled
            if (wrapper.isEnabled()) {
                wrapper.setEnabled( false);
                changed = true;
            }
        }

        return changed;
    }

    protected boolean updateSearchParametersAvailability( SearchMode searchmode) {
        boolean changed = false;

        SearchParameters params = searchmode.getParameters();
        boolean hasExpression = params.contains( StandardSearchParameter.EXPRESSION);
        if (hasExpression != searchExpressionEnabled) {
            searchExpressionEnabled = hasExpression;
            changed = true;
        }

        boolean hasDistance = params.contains( StandardSearchParameter.DISTANCE);
        if (hasDistance != distanceEnabled) {
            distanceEnabled = hasDistance;
            changed = true;
        }

        return changed;
    }

    protected boolean updateSearchFieldsAvailability( SearchMode searchmode) {
        SearchFieldSelection newEnabled = new SearchFieldSelection();

        if (searchmode.getParameters().contains( StandardSearchParameter.SEARCH_FIELDS)) {
            for ( Iterator i=dictionaries.iterator(); i.hasNext(); ) {
                StateWrapper wrapper = (StateWrapper) i.next();
                Dictionary dic = (Dictionary) wrapper.getObject();
                if (wrapper.isSelected() && wrapper.isEnabled() &&
                    dic.supports( searchmode, 
                                  !(allDictionariesSelected || multiDictionarySelection))) {
                    newEnabled = newEnabled.or( dic.getSupportedFields( searchmode));
                }
            }
        }

        if (newEnabled.equals( searchFieldsEnabled))
            return false;
        else {
            searchFieldsEnabled = newEnabled;
            return true;
        }
    }

    public void addLookupChangeListener( LookupChangeListener listener) {
        listeners.add( listener);
    }

    public void removeLooupChangeListener( LookupChangeListener listener) {
        listeners.remove( listener);
    }

    protected void fireLookupChange( LookupChangeEvent event) {
        for ( Iterator i=listeners.iterator(); i.hasNext(); ) {
            ((LookupChangeListener) i.next()).stateChanged( event);
        }
    }

    public Object clone() {
        try {
            LookupModel out = (LookupModel) super.clone();
            // clone fields
            out.searchModes = cloneStateList( searchModes);
            out.dictionaries = cloneStateList( dictionaries);
            out.filters = cloneStateList( filters);
            out.searchFields = (SearchFieldSelection) searchFields.clone();
            out.searchFieldsEnabled = (SearchFieldSelection) searchFieldsEnabled.clone();
            out.listeners = new ArrayList( 5);

            return out;
        } catch (CloneNotSupportedException ex) { return null; }
    }
    
    private List cloneStateList( List in) {
        List out = new ArrayList( in.size());
        for ( Iterator i=in.iterator(); i.hasNext(); ) {
            out.add( ((StateWrapper) i.next()).clone());
        }
        return out;
    }
} // class LookupModel
