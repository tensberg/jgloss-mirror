package jgloss.ui;

/**
 * Stores one lookup result item in the {@link LookupHistory}. Keeps a copy of the lookup model
 * as well as the lookup result list state.
 *  
 * @author Michael Koch <tensberg@gmx.net>
 */
class HistoryItem {
    private final LookupModel lookupModel;
    
    private final LookupResultList.ViewState resultState;

    HistoryItem( LookupModel _lookupModel,
                         LookupResultList.ViewState _resultState) {
        lookupModel = _lookupModel;
        resultState = _resultState;
    }
    
    public LookupModel getLookupModel() {
	    return lookupModel;
    }
    
    public LookupResultList.ViewState getResultState() {
	    return resultState;
    }
}