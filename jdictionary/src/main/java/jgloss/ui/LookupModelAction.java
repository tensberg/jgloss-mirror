package jgloss.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;

abstract class LookupModelAction extends AbstractAction {

	private static final long serialVersionUID = 1L;
	
	private final View<LookupModel> view;

	private final LookupChangeListener updateEnabledListener = new LookupChangeListener() {

		@Override
        public void stateChanged(LookupChangeEvent event) {
	        updateEnabled(getModel());
        }
		
	};
	
	public LookupModelAction(View<LookupModel> view) {
		this.view = view;
		addModelChangeListener();
    }

	public LookupModelAction(View<LookupModel> view, String name) {
		super(name);
		this.view = view;
		addModelChangeListener();
	}
	
	/**
	 * Update the enabled state of the action according to the current state of the lookup model.
	 * 
	 * @param model The current lookup model. May be <code>null</code>.
	 */
	protected abstract void updateEnabled(LookupModel model);

	/**
	 * @return Model of the current view. May be <code>null</code>.
	 */
	protected final LookupModel getModel() {
		return view.getModel();
	}
	
	private void addModelChangeListener() {
		view.addModelChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				modelChanged((LookupModel) event.getOldValue(), (LookupModel) event.getNewValue());
			}
			
		});
		modelChanged(null, view.getModel());
	}
	
	private void modelChanged(LookupModel oldModel, LookupModel newModel) {
	    if (oldModel != null) {
	        oldModel.removeLookupChangeListener(updateEnabledListener);
	    }
	    
	    if (newModel != null) {
	        newModel.addLookupChangeListener(updateEnabledListener);
	    }
	    updateEnabled(newModel);
	}
}
