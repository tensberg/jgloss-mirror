package jgloss.ui;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import jgloss.dictionary.SearchMode;

class SelectSearchModeAction extends LookupModelAction {
	
    private static final long serialVersionUID = 1L;
    
	private final SearchMode searchMode;

    SelectSearchModeAction(View<LookupModel> view, SearchMode searchMode) {
    	super(view, searchMode.getName());
    	putValue(Action.SHORT_DESCRIPTION, searchMode.getDescription());
		this.searchMode = searchMode;	
		updateEnabled(getModel());
    }
    
	@Override
    public void actionPerformed(ActionEvent e) {
    	getModel().selectSearchMode(searchMode);
    }
	
	@Override
    protected void updateEnabled(LookupModel model) {
		setEnabled(model != null && model.isSearchModeEnabled(searchMode));
	}
}