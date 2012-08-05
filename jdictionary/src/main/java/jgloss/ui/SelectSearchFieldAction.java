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
	
	SelectSearchFieldAction(LookupModel model, DictionaryEntryField searchField) {
		super(model);
		this.searchField = searchField;
		UIUtilities.initAction(this, "wordlookup.searchfield." + searchField.toString().toLowerCase(Locale.US));
		checkSetEnabled();
		model.addLookupChangeListener(new LookupChangeListener() {
			
			@Override
			public void stateChanged(LookupChangeEvent event) {
				if (event.hasChanged(LookupChangeEvent.SEARCH_FIELDS_AVAILABILITY)) {
					checkSetEnabled();
				}
			}
		});
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	    model.selectSearchField(searchField, ((AbstractButton) e.getSource()).isSelected());
	}
	
	private void checkSetEnabled() {
		 SearchFieldSelection sf = model.getEnabledSearchFields();
		 setEnabled(sf.isSelected(searchField));
	}
}