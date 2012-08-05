package jgloss.ui;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import jgloss.dictionary.SearchMode;

class SelectSearchModeAction extends LookupModelAction {
	
    private static final long serialVersionUID = 1L;
    
	private final SearchMode searchMode;

    SelectSearchModeAction(LookupModel model, SearchMode searchMode) {
    	super(model, searchMode.getName());
    	putValue(Action.SHORT_DESCRIPTION, searchMode.getDescription());
		this.searchMode = searchMode;	
		checkSetEnabled();
		model.addLookupChangeListener(new LookupChangeListener() {
			
			@Override
			public void stateChanged(LookupChangeEvent event) {
				if (event.hasChanged(LookupChangeEvent.SEARCH_MODE_AVAILABILITY)) {
					checkSetEnabled();
				}
			}
		});
    }
    
	@Override
    public void actionPerformed(ActionEvent e) {
    	model.selectSearchMode(searchMode);
    }
	
	private void checkSetEnabled() {
		setEnabled(model.isSearchModeEnabled(searchMode));
	}
}