package jgloss.ui;

import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.AbstractButton;

import jgloss.dictionary.MatchMode;
import jgloss.dictionary.SearchFieldSelection;
import jgloss.ui.util.UIUtilities;

class SelectMatchModeAction extends LookupModelAction {
	private static final long serialVersionUID = 1L;
	
	private final MatchMode matchMode;
	
	SelectMatchModeAction(LookupModel model, MatchMode matchMode) {
		super(model);
		this.matchMode = matchMode;
		UIUtilities.initAction(this, "wordlookup.matchmode." + matchMode.toString().toLowerCase(Locale.US));
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
	    model.selectMatchMode(matchMode, ((AbstractButton) e.getSource()).isSelected());
	}
	
	private void checkSetEnabled() {
        SearchFieldSelection sf = model.getEnabledSearchFields();
        setEnabled(sf.isSelected(matchMode));
	}
}