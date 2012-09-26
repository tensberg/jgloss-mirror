package jgloss.ui;

import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.AbstractButton;

import jgloss.dictionary.DictionaryEntryField;
import jgloss.dictionary.SearchFieldSelection;
import jgloss.ui.util.UIUtilities;

class SelectSearchFieldAction extends LookupModelAction {
	private static final long serialVersionUID = 1L;

	private final DictionaryEntryField searchField;
	
	SelectSearchFieldAction(View<LookupModel> view, DictionaryEntryField searchField) {
		super(view);
		this.searchField = searchField;
		UIUtilities.initAction(this, "wordlookup.searchfield." + searchField.toString().toLowerCase(Locale.US));
		updateEnabled(getModel());
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	    getModel().selectSearchField(searchField, ((AbstractButton) e.getSource()).isSelected());
	}
	
	@Override
    protected void updateEnabled(LookupModel model) {
	    boolean enabled;
	    if (model != null) {
	        SearchFieldSelection sf = getModel().getEnabledSearchFields();
	        enabled = sf.isSelected(searchField);
	    } else {
	        enabled = false;
	    }
	    setEnabled(enabled);
	}
}