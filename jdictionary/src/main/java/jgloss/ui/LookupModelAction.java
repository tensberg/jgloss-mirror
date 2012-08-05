package jgloss.ui;

import javax.swing.AbstractAction;

abstract class LookupModelAction extends AbstractAction {

	private static final long serialVersionUID = 1L;
	
	protected final LookupModel model;
	
	public LookupModelAction(LookupModel model) {
		this.model = model;
    }

	public LookupModelAction(LookupModel model, String name) {
		super(name);
		this.model = model;
	}

}
