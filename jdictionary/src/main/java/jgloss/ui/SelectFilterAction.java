package jgloss.ui;

import static jgloss.ui.LookupChangeEvent.FILTER_AVAILABILITY;

import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;

class SelectFilterAction extends LookupModelAction {
	
	private static final long serialVersionUID = 1L;
	
	private final LookupResultFilter filter;
	
	SelectFilterAction(LookupModel model, LookupResultFilter filter) {
		super(model, filter.getName());
		this.filter = filter;
		putValue(Action.SHORT_DESCRIPTION, filter.getDescription());
		checkSetEnabled();
		model.addLookupChangeListener(new LookupChangeListener() {
			
			@Override
			public void stateChanged(LookupChangeEvent event) {
				if (event.hasChanged(FILTER_AVAILABILITY)) {
					checkSetEnabled();
				}
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    model.selectFilter(filter, ((AbstractButton) e.getSource()).isSelected());
	}
	
	private void checkSetEnabled() {
		setEnabled(model.isFilterEnabled(filter));
	}
}