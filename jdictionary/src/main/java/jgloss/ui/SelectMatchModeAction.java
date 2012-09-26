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
	
	SelectMatchModeAction(View<LookupModel> view, MatchMode matchMode) {
		super(view);
		this.matchMode = matchMode;
		UIUtilities.initAction(this, "wordlookup.matchmode." + matchMode.toString().toLowerCase(Locale.US));
		updateEnabled(getModel());
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	    getModel().selectMatchMode(matchMode, ((AbstractButton) e.getSource()).isSelected());
	}
	
	@Override
    protected void updateEnabled(LookupModel model) {
	    boolean enabled;
	    if (model != null) {
	        SearchFieldSelection sf = model.getEnabledSearchFields();
	        enabled = sf.isSelected(matchMode);
	    } else {
	        enabled = false;
	    }
		setEnabled(enabled);
	}
}