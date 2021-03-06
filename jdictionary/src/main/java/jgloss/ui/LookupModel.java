/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.ui;

import static jgloss.dictionary.DictionaryUtils.unwrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntryField;
import jgloss.dictionary.MatchMode;
import jgloss.dictionary.SearchFieldSelection;
import jgloss.dictionary.SearchMode;
import jgloss.dictionary.SearchParameter;
import jgloss.dictionary.StandardSearchParameter;
import jgloss.dictionary.attribute.SearchReference;

/**
 * Model for user configuration of dictionary lookups.
 *
 * @author Michael Koch
 */
public class LookupModel implements Cloneable {
    private List<StateWrapper<SearchMode>> searchModes;

    private List<DictionaryStateWrapper> dictionaries;
    private boolean allDictionariesSelected = false;
    private boolean multiDictionaryMode = false;
    private boolean multiDictionarySelection = false;

    private List<StateWrapper<LookupResultFilter>> filters;

    private SearchFieldSelection searchFields = 
        new SearchFieldSelection( true, true, true, true, false);
    private SearchFieldSelection searchFieldsEnabled = new SearchFieldSelection();

    private String searchExpression = "";
    private boolean searchExpressionEnabled = false;
    private int distance = 1;
    private boolean distanceEnabled = false;
    
    private boolean eventChangeComplete;

    private final List<LookupChangeListener> listeners = new CopyOnWriteArrayList<LookupChangeListener>();

    public LookupModel( List<SearchMode> _searchModes, List<Dictionary> _dictionaries, List<LookupResultFilter> _filters) {
        searchModes = new ArrayList<StateWrapper<SearchMode>>( _searchModes.size());
        for (SearchMode searchMode : _searchModes) {
            searchModes.add( new StateWrapper<SearchMode>(searchMode));
        }

        dictionaries = new ArrayList<DictionaryStateWrapper>( _dictionaries.size());
        for (Dictionary dictionary : _dictionaries) {
            dictionaries.add( new DictionaryStateWrapper(dictionary));
        }

        filters = new ArrayList<StateWrapper<LookupResultFilter>>( _filters.size());
        for (LookupResultFilter filter : _filters) {
            filters.add( new StateWrapper<LookupResultFilter>(filter));
        }
        
        selectAllDictionaries(true);
        selectFirstAvailableSearchMode();
    }

    private void selectFirstAvailableSearchMode() {
        SearchMode selectedMode = null;
        for (StateWrapper<SearchMode> wrapper : searchModes) {
            if (wrapper.isEnabled()) {
                wrapper.setSelected( true);
                selectedMode = wrapper.getObject();
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

    public SearchMode getSearchMode( int index) {
        return searchModes.get( index).getObject();
    }

    public SearchMode[] getSearchModes() {
        SearchMode[] out = new SearchMode[searchModes.size()];
        for ( ListIterator<StateWrapper<SearchMode>> i=searchModes.listIterator(); i.hasNext(); ) {
            SearchMode mode = i.next().getObject();
            out[i.previousIndex()] = mode;
        }
        return out;
    }

    public SearchMode getSelectedSearchMode() {
        int selectedIndex = getSelectedSearchModeIndex();
        if (selectedIndex != -1) {
	        return getSearchMode( selectedIndex);
        } else {
	        return null;
        }
    }

    public int getSelectedSearchModeIndex() {
        boolean selectedIsEnabled = true;

        for ( ListIterator<StateWrapper<SearchMode>> i=searchModes.listIterator(); i.hasNext(); ) {
            StateWrapper<SearchMode> wrapper =  i.next();
            if (wrapper.isSelected()) {
                if (wrapper.isEnabled()) {
                    return i.previousIndex();
                }
                else {
                    selectedIsEnabled = false;
                    break;
                }
            }
        }

        if (!selectedIsEnabled) {
            for ( ListIterator<StateWrapper<SearchMode>> i=searchModes.listIterator(); i.hasNext(); ) {
                StateWrapper<SearchMode> wrapper = i.next();
                if (wrapper.isEnabled()) {
                    return i.previousIndex();
                }
            }
        }
        
        return -1;
    }

    public boolean isSearchModeSelected( int index) {
        return searchModes.get( index).isSelected();
    }

    public boolean isSearchModeEnabled( int index) {
        return searchModes.get( index).isEnabled();
    }
    
    public boolean isSearchModeEnabled(SearchMode mode) {
    	for (StateWrapper<SearchMode> modeWrapper : searchModes) {
    		if (modeWrapper.getObject().equals(mode)) {
    			return modeWrapper.isEnabled();
    		}
    	}
    	
    	throw new IllegalArgumentException("unsupported search mode " + mode);
    }

    public boolean selectSearchMode( SearchMode mode) {
        for (StateWrapper<SearchMode> modeWrapper : searchModes) {
            if (modeWrapper.getObject().equals(mode)) {
                selectSearchMode( modeWrapper);
                return true;
            }
        }
        return false;
    }

    public void selectSearchMode( int index) {
        StateWrapper<SearchMode> newModeWrapper = searchModes.get( index);
        selectSearchMode(newModeWrapper);
    }

	private void selectSearchMode(StateWrapper<SearchMode> newModeWrapper) {
	    if (newModeWrapper.isSelected()) {
            return; // nothing to do
        }

        // unselect the old selected search mode
        for (StateWrapper<SearchMode> wrapper : searchModes) {
            if (wrapper.isSelected()) {
                wrapper.setSelected( false);
                break;
            }
        }

        newModeWrapper.setSelected( true);

        SearchMode newMode = getSelectedSearchMode();

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
                            LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0), eventChangeComplete));
    }

    public void setMultiDictionarySelectionMode( boolean mode) {
        if (mode == multiDictionaryMode) {
	        return;
        }

        boolean dictionarySelectionChanged = false;
        if (multiDictionarySelection) {
            boolean firstDictionary = true;
            for (StateWrapper<Dictionary> wrapper : dictionaries) {
                if (wrapper.isSelected()) {
                    if (firstDictionary) {
	                    firstDictionary = false;
                    } else {
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
                            (searchModeChanged ? LookupChangeEvent.SEARCH_MODE_AVAILABILITY|
                             LookupChangeEvent.SEARCH_MODE_SELECTION : 0) | 
                            (filterChanged ? LookupChangeEvent.FILTER_AVAILABILITY : 0) |
                            (parameterFieldsChanged ? 
                            LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY : 0) |
                            (searchFieldsChanged ? 
                            LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0), eventChangeComplete));
    }

    public void selectAllDictionaries( boolean select) {
        if (allDictionariesSelected == select) {
	        return;
        }

        allDictionariesSelected = select;

        SearchMode searchmode = getSelectedSearchMode();
        boolean searchModeChanged = updateSearchModeAvailability();
        boolean parameterFieldsChanged = updateSearchParametersAvailability( searchmode);
        boolean searchFieldsChanged = updateSearchFieldsAvailability( searchmode);
        boolean filterChanged = updateFilterAvailability();
        fireLookupChange( new LookupChangeEvent
                          ( this, LookupChangeEvent.DICTIONARY_SELECTION |
                            (searchModeChanged ? LookupChangeEvent.SEARCH_MODE_AVAILABILITY|
                             LookupChangeEvent.SEARCH_MODE_SELECTION : 0) | 
                            (filterChanged ? LookupChangeEvent.FILTER_AVAILABILITY : 0) |
                            (parameterFieldsChanged ? 
                            LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY : 0) |
                            (searchFieldsChanged ? 
                            LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0), eventChangeComplete));
    }

    public boolean isAllDictionariesSelected() { return allDictionariesSelected; }

    public void setDictionaries( List<Dictionary> _newDictionaries) {
        List<Dictionary> newDictionaries = new ArrayList<Dictionary>( _newDictionaries);
        Map<Dictionary, DictionaryStateWrapper> oldDictionaries = new HashMap<Dictionary, DictionaryStateWrapper>( (int) (dictionaries.size()*1.5));
        for (DictionaryStateWrapper wrapper : dictionaries) {
            oldDictionaries.put( wrapper.getObject(), wrapper);
        }

        boolean seenSelectedDictionary = false;
        multiDictionarySelection = false;

        List<DictionaryStateWrapper> newDictionaryWrappers = new ArrayList<DictionaryStateWrapper>(newDictionaries.size());
        for (Dictionary d : newDictionaries) {
            DictionaryStateWrapper wrapper = oldDictionaries.get( d);
            if (wrapper == null) {
	            wrapper = new DictionaryStateWrapper( d, false, false);
            } else {
                if (wrapper.isSelected() && wrapper.isEnabled()) {
                    if (seenSelectedDictionary) {
                        multiDictionarySelection = true;
                    }
                    else {
                        seenSelectedDictionary = true;
                    }
                }
            }
            newDictionaryWrappers.add(wrapper);
        }

        boolean selectNewDictionary = dictionaries.isEmpty() && !newDictionaries.isEmpty() && 
            !multiDictionarySelection;
        dictionaries = newDictionaryWrappers;

        if (selectNewDictionary) {
        	dictionaries.get( 0).setEnabled( true);
        	dictionaries.get( 0).setSelected( true);
        }

        boolean searchModeChanged = updateSearchModeAvailability();
        SearchMode searchmode = getSelectedSearchMode();
        boolean dictionaryChanged = updateDictionaryAvailability( searchmode);
        boolean parameterFieldsChanged = updateSearchParametersAvailability( searchmode);
        boolean searchFieldsChanged = updateSearchFieldsAvailability( searchmode);
        boolean filterChanged = updateFilterAvailability();
        fireLookupChange( new LookupChangeEvent
                          ( this, LookupChangeEvent.DICTIONARY_LIST_CHANGED |
                            (searchModeChanged ? LookupChangeEvent.SEARCH_MODE_AVAILABILITY|
                             LookupChangeEvent.SEARCH_MODE_SELECTION : 0) | 
                            (dictionaryChanged ? LookupChangeEvent.DICTIONARY_AVAILABILITY : 0) | 
                            (filterChanged ? LookupChangeEvent.FILTER_AVAILABILITY : 0) |
                            (parameterFieldsChanged ? 
                             LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY : 0) |
                            (searchFieldsChanged ? 
                             LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0), eventChangeComplete));
    }

    public void selectDictionary(Dictionary dictionary) {
    	for (DictionaryStateWrapper wrapper : dictionaries) {
    		Dictionary wrappedDictionary = unwrap(wrapper);
			if (wrappedDictionary.equals(dictionary)) {
    			selectDictionary(true, wrapper);
    			return;
    		}
    	}
    	
    	throw new IllegalArgumentException("unsupported dictionary " + dictionary);
    }
    
    public void selectDictionary( int index, boolean select) {
        StateWrapper<Dictionary> newDictionaryWrapper = dictionaries.get( index);
        selectDictionary(select, newDictionaryWrapper);
    }

	private void selectDictionary(boolean select, StateWrapper<Dictionary> dictionaryWrapper) {
	    if (dictionaryWrapper.isSelected() == select) {
	        return; // nothing to do
        }
        if (select && !dictionaryWrapper.isEnabled()) {
	        throw new IllegalArgumentException( "selected dictionary not enabled");
        }

        if (!multiDictionaryMode && select) {
            // clear old selection
            for (StateWrapper<Dictionary> wrapper : dictionaries) {
                if (wrapper.isSelected()) {
                    wrapper.setSelected( false);
                    break;
                }
            }
        }

        dictionaryWrapper.setSelected( select);
        
        if (multiDictionaryMode) {
            // check if more than one dictionary is selected
            boolean seenDictionary = false;
            multiDictionarySelection = false;
            for (StateWrapper<Dictionary> wrapper : dictionaries) {
                if (wrapper.isSelected()) {
                    if (seenDictionary) {
                        multiDictionarySelection = true;
                        break;
                    } else {
                        seenDictionary = true;
                    }
                }
            }
        }

        SearchMode searchmode = getSearchMode( getSelectedSearchModeIndex());
        boolean searchModeChanged = updateSearchModeAvailability();
        boolean parameterFieldsChanged = updateSearchParametersAvailability( searchmode);
        boolean searchFieldsChanged = updateSearchFieldsAvailability( searchmode);
        boolean filterChanged = updateFilterAvailability();
        fireLookupChange( new LookupChangeEvent
                          ( this, LookupChangeEvent.DICTIONARY_SELECTION |
                            (searchModeChanged ? LookupChangeEvent.SEARCH_MODE_AVAILABILITY|
                             LookupChangeEvent.SEARCH_MODE_SELECTION : 0) | 
                            (filterChanged ? LookupChangeEvent.FILTER_AVAILABILITY : 0) |
                            (parameterFieldsChanged ? 
                            LookupChangeEvent.SEARCH_PARAMETERS_AVAILABILITY : 0) |
                            (searchFieldsChanged ? 
                            LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY : 0), eventChangeComplete));
    }

    public boolean isDictionarySelected( int index) {
        return allDictionariesSelected || dictionaries.get( index).isSelected();
    }

    public boolean isDictionaryEnabled( int index) {
        return dictionaries.get( index).isEnabled();
    }

    public int getDictionaryCount() {
        return dictionaries.size();
    }

    public Dictionary[] getDictionaries() {
        Dictionary[] out = new Dictionary[dictionaries.size()];
        for ( ListIterator<DictionaryStateWrapper> i=dictionaries.listIterator(); i.hasNext(); ) {
            Dictionary dictionary = i.next().getObject();
            out[i.previousIndex()] = dictionary;
        }
        return out;
    }

    public List<Dictionary> getSelectedDictionaries() {
        List<Dictionary> out = new ArrayList<Dictionary>( dictionaries.size());
        for (StateWrapper<Dictionary> wrapper : dictionaries) {
            if ((allDictionariesSelected || wrapper.isSelected()) && wrapper.isEnabled()) {
	            out.add( wrapper.getObject());
            }
        }
        return out;
    }

    public void selectSearchField( DictionaryEntryField field, boolean selected) {
        if (searchFields.isSelected( field) == selected) {
	        return;
        }
        if (!searchFieldsEnabled.isSelected( field)) {
	        throw new IllegalStateException( "selected field is not enabled");
        }
        searchFields.select( field, selected);
        
        fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.SEARCH_FIELDS_SELECTION, eventChangeComplete));
    }

    public void selectMatchMode( MatchMode mode, boolean selected) {
        if (searchFields.isSelected( mode) == selected) {
	        return;
        }
        if (!searchFieldsEnabled.isSelected( mode)) {
	        throw new IllegalStateException( "selected field is not enabled");
        }
        searchFields.select( mode, selected);
        searchFields.select( mode==MatchMode.FIELD ? MatchMode.WORD : MatchMode.FIELD, !selected);
        
        fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.SEARCH_FIELDS_SELECTION, eventChangeComplete));
    }
    
    public void setSearchFieldSelection(SearchFieldSelection searchFields) {
    	this.searchFields = searchFields;
    	fireLookupChange(new LookupChangeEvent(this, LookupChangeEvent.SEARCH_FIELDS_SELECTION, eventChangeComplete));
    }

    public SearchFieldSelection getSearchFields() { return searchFields; }
    public SearchFieldSelection getEnabledSearchFields() { return searchFieldsEnabled; }

	public boolean selectFilter(LookupResultFilter filter, boolean select) {
	    for (StateWrapper<LookupResultFilter> filterWrapper : filters) {
	    	if (filterWrapper.getObject().equals(filter)) {
	    		selectFilter(filterWrapper, select);
	    		return true;
	    	}
	    }
	    
	    return false;
    }
    
    public void selectFilter( int index, boolean select) {
        StateWrapper<LookupResultFilter> filterWrapper = filters.get( index);
        selectFilter(filterWrapper, select);
    }

	private void selectFilter(StateWrapper<LookupResultFilter> filterWrapper, boolean select) {
	    if (filterWrapper.isSelected() == select)
		 {
	        return; // nothing to do
        }
        if (select && !filterWrapper.isEnabled()) {
	        throw new IllegalArgumentException( "selected filter not enabled");
        }

        filterWrapper.setSelected( select);
        fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.FILTER_SELECTION, eventChangeComplete));
    }

    public boolean isFilterSelected( int index) {
        return filters.get( index).isSelected();
    }

    public boolean isFilterEnabled( int index) {
        return filters.get( index).isEnabled();
    }
    
    public boolean isFilterEnabled(LookupResultFilter filter) {
    	for (StateWrapper<LookupResultFilter> filterWrapper : filters) {
    		if (filterWrapper.getObject().equals(filter)) {
    			return filterWrapper.isEnabled();
    		}
    	}
    	
    	throw new IllegalArgumentException("unsupported filter " + filter);
    }

    public LookupResultFilter[] getFilters() {
        LookupResultFilter[] out = new LookupResultFilter[filters.size()];
        for ( ListIterator<StateWrapper<LookupResultFilter>> i=filters.listIterator(); i.hasNext(); ) {
            LookupResultFilter filter = i.next().getObject();
            out[i.previousIndex()] = filter;
        }
        return out;
    }

    public List<LookupResultFilter> getSelectedFilters() {
        List<LookupResultFilter> out = new ArrayList<LookupResultFilter>( filters.size());
        for (StateWrapper<LookupResultFilter> filterWrapper : filters) {
            if (filterWrapper.isSelected() && filterWrapper.isEnabled()) {
	            out.add( filterWrapper.getObject());
            }
        }

        return out;
    }

    public String getSearchExpression() { return searchExpression; }

    public void setSearchExpression( String _searchExpression) {
        if (!searchExpression.equals( _searchExpression)) {
            searchExpression = _searchExpression;
            fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.SEARCH_PARAMETERS, eventChangeComplete));
        }
    }

    public boolean isSearchExpressionEnabled() { return searchExpressionEnabled; }

    public int getDistance() { return distance; }

    public void setDistance( int _distance) {
        if (distance != _distance) {
            distance = _distance;
            fireLookupChange( new LookupChangeEvent( this, LookupChangeEvent.SEARCH_PARAMETERS, eventChangeComplete));
        }
    }

    public boolean isDistanceEnabled() { return distanceEnabled; }
    
    private boolean updateSearchModeAvailability() {
        boolean changed = false;

        modes: 
        for (StateWrapper<SearchMode> searchModeWrapper : searchModes) {
            SearchMode mode = searchModeWrapper.getObject();
            for (StateWrapper<Dictionary> dictionaryWrapper : dictionaries) {
                Dictionary dic = dictionaryWrapper.getObject();
                if (dictionaryWrapper.isEnabled() && (allDictionariesSelected || dictionaryWrapper.isSelected()) &&
                    !dic.supports( mode, false)) {
                	if (searchModeWrapper.isEnabled()) {
                        searchModeWrapper.setEnabled( false);
                        changed = true;
                    }
                    continue modes;
                }
                // if we get here, this search mode is supported by all selected dictionaries
                if (!searchModeWrapper.isEnabled()) {
                    searchModeWrapper.setEnabled( true);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private boolean updateDictionaryAvailability( SearchMode searchmode) {
        boolean changed = false;

        for (StateWrapper<Dictionary> wrapper : dictionaries) {
            Dictionary dic = wrapper.getObject();
            boolean newState = searchmode==null ||
                dic.supports( searchmode, !(allDictionariesSelected || multiDictionarySelection));
            if (newState != wrapper.isEnabled()) {
                changed = true;
                wrapper.setEnabled( newState);
            }
        }

        return changed;
    }

    private boolean updateFilterAvailability() {
        boolean changed = false;
        
        filters: 
        for (StateWrapper<LookupResultFilter> filterWrapper : filters) {
            LookupResultFilter filter = filterWrapper.getObject();
            for (StateWrapper<Dictionary> dictionaryWrapper : dictionaries) {
                Dictionary dic = dictionaryWrapper.getObject();
                if (dictionaryWrapper.isEnabled() && (allDictionariesSelected || dictionaryWrapper.isSelected()) &&
                    filter.enableFor( dic)) {
                    if (!filterWrapper.isEnabled()) {
                        filterWrapper.setEnabled( true);
                        changed = true;
                    }
                    continue filters;
                }
            }
            // no fitting dictionary, filter should be disabled
            if (filterWrapper.isEnabled()) {
                filterWrapper.setEnabled( false);
                changed = true;
            }
        }

        return changed;
    }

    private boolean updateSearchParametersAvailability( SearchMode searchmode) {
        boolean changed = false;

        if (searchmode != null) {
            List<SearchParameter> params = searchmode.getParameters();
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
        }
        else {
            changed = searchExpressionEnabled | distanceEnabled;
            searchExpressionEnabled = false;
            distanceEnabled = false;
        }

        return changed;
    }

    private boolean updateSearchFieldsAvailability( SearchMode searchmode) {
        SearchFieldSelection newEnabled = new SearchFieldSelection( false, false, false,
                                                                    false, false);

        if (searchmode != null &&
            searchmode.getParameters().contains( StandardSearchParameter.SEARCH_FIELDS)) {
            for (StateWrapper<Dictionary> wrapper : dictionaries) {
                Dictionary dic = wrapper.getObject();
                if (wrapper.isSelected() && wrapper.isEnabled() &&
                    dic.supports( searchmode, 
                                  !(allDictionariesSelected || multiDictionarySelection))) {
                    newEnabled = newEnabled.or( dic.getSupportedFields( searchmode));
                }
            }
        }

        if (newEnabled.equals( searchFieldsEnabled)) {
	        return false;
        } else {
            searchFieldsEnabled = newEnabled;
            return true;
        }
    }

    public void addLookupChangeListener( LookupChangeListener listener) {
    	assert !listeners.contains(listener);
    	
        listeners.add( listener);
    }

    public void removeLookupChangeListener( LookupChangeListener listener) {
        listeners.remove( listener);
    }

    private void fireLookupChange( LookupChangeEvent event) {
    	for (LookupChangeListener listener : listeners) {
            listener.stateChanged( event);
        }
    }

    @Override
	public LookupModel clone() {
        try {
            LookupModel out = (LookupModel) super.clone();
            // clone fields
            out.searchModes = cloneStateList( searchModes);
            out.dictionaries = cloneStateList( dictionaries);
            out.filters = cloneStateList( filters);
            out.searchFields = (SearchFieldSelection) searchFields.clone();
            out.searchFieldsEnabled = (SearchFieldSelection) searchFieldsEnabled.clone();

            return out;
        } catch (CloneNotSupportedException ex) { 
        	throw new AssertionError(ex);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <S, T extends StateWrapper<S>> List<T> cloneStateList( List<T> in) {
        List<T> out = new ArrayList<T>( in.size());
        for (T wrapper : in) {
            out.add((T) wrapper.clone());
        }
        return out;
    }
    
    public void followReference(SearchReference reference) {
    	selectAllDictionaries(false);
    	selectDictionary(reference.getDictionary());
    	setSearchExpression(reference.getReference());
    	selectSearchMode(reference.getSearchMode());
    	eventChangeComplete = true;
    	setSearchFieldSelection(reference.getSearchFieldSelection());
    	eventChangeComplete = false;
    }
} // class LookupModel
