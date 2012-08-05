package jgloss.ui;

import java.awt.event.ActionEvent;

import jgloss.ui.util.UIUtilities;

class SelectAllDictionariesAction extends LookupModelAction {
	private static final long serialVersionUID = 1L;

	private final boolean selectAllDictionaries;

	SelectAllDictionariesAction(LookupModel model, boolean selectAllDictionaries) {
		super(model);
		this.selectAllDictionaries = selectAllDictionaries;
		UIUtilities.initAction(this, selectAllDictionaries ? "wordlookup.choice.alldictionaries" : "wordlookup.choice.dictionary");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	    model.selectAllDictionaries(selectAllDictionaries);
	}
}