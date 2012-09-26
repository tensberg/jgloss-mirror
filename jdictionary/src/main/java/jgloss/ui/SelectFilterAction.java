package jgloss.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;

class SelectFilterAction extends LookupModelAction {
	
	private static final long serialVersionUID = 1L;
	
	private final LookupResultFilter filter;
	
	SelectFilterAction(View<LookupModel> view, LookupResultFilter filter) {
		super(view, filter.getName());
		this.filter = filter;
		putValue(Action.SHORT_DESCRIPTION, filter.getDescription());
		updateEnabled(getModel());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    getModel().selectFilter(filter, ((AbstractButton) e.getSource()).isSelected());
	}
	
	@Override
    protected void updateEnabled(LookupModel model) {
		setEnabled(model != null && model.isFilterEnabled(filter));
	}
}