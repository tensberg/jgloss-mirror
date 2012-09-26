package jgloss.ui;

import java.beans.PropertyChangeListener;

public interface View<T> {
    String MODEL_PROPERTY_NAME = "model";
	
	T getModel();
	
	void addModelChangeListener(PropertyChangeListener modelChangeListener);
}
