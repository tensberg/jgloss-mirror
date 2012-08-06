package jgloss.ui;

import java.awt.event.ActionListener;

import javax.swing.Timer;

/**
 * Start a dictionary search when the lookup model changes. The search is started after a short
 * delay so that several consecutive model changes are coalesced into a single search.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
class SearchOnModelChangeListener implements LookupChangeListener {

	private final Timer delayedActionTimer;
	
	/**
	 * Creates a new listener which will call the given action listener when the lookup model changes.
	 * 
	 * @param searchActionListener Action listener to call on model change.
	 */
	SearchOnModelChangeListener(ActionListener searchActionListener) {
		delayedActionTimer = new Timer(500, searchActionListener);
		delayedActionTimer.setRepeats(false);
	}
	
	@Override
	public void stateChanged(LookupChangeEvent event) {
		delayedActionTimer.restart();
	}

}