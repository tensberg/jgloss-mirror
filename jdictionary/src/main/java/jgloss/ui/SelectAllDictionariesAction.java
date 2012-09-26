package jgloss.ui;

import java.awt.event.ActionEvent;

import jgloss.ui.util.UIUtilities;

class SelectAllDictionariesAction extends LookupModelAction {
	private static final long serialVersionUID = 1L;

	private final boolean selectAllDictionaries;

	SelectAllDictionariesAction(View<LookupModel> view, boolean selectAllDictionaries) {
		super(view);
		this.selectAllDictionaries = selectAllDictionaries;
		UIUtilities.initAction(this, selectAllDictionaries ? "wordlookup.choice.alldictionaries" : "wordlookup.choice.dictionary");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	    getModel().selectAllDictionaries(selectAllDictionaries);
	}

    @Override
    protected void updateEnabled(LookupModel model) {
        setEnabled(model != null);
    }
}