package jgloss.ui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import jgloss.ui.util.UIUtilities;

/**
 * Keeps the lookup history of a {@link LookupFrame} and provides the actions to move
 * backwards and forwards in the history.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
class LookupHistory {
    private static final int DEFAULT_MAX_HISTORY_SIZE = 20;
	
	private final List<HistoryItem> history;
    
    private final Action historyBackAction;
    
    private final Action historyForwardAction;

	private final LookupFrame lookupFrame;

	private final int maxHistorySize;
	
	private int historyPosition = -1;
	
	LookupHistory(LookupFrame lookupFrame) {
		this(lookupFrame, DEFAULT_MAX_HISTORY_SIZE);
	}
	
	LookupHistory(LookupFrame lookupFrame, int maxHistorySize) {
		this.lookupFrame = lookupFrame;
		this.maxHistorySize = maxHistorySize;
		this.history = new ArrayList<HistoryItem>(maxHistorySize);

		historyBackAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( ActionEvent e) {
				historyBack();
			}
		};
		UIUtilities.initAction( historyBackAction, "wordlookup.history.back");
		historyBackAction.setEnabled( false);
		historyForwardAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( ActionEvent e) {
				historyForward();
			}
		};
		UIUtilities.initAction( historyForwardAction, "wordlookup.history.forward");
		historyForwardAction.setEnabled( false);
	}
	
	public Action getHistoryBackAction() {
	    return historyBackAction;
    }
	
	public Action getHistoryForwardAction() {
	    return historyForwardAction;
    }

    private void historyBack() {
    	if (historyPosition == 0) {
    		throw new IllegalStateException("history position is 0");
    	}
    	
        historyPosition--;
        HistoryItem hi = history.get(historyPosition);

        if (historyPosition == 0) {
	        historyBackAction.setEnabled(false);
        }
        historyForwardAction.setEnabled(true);
        lookupFrame.showHistoryItem(hi);
    }

    private void historyForward() {
    	if (historyPosition >= history.size() - 1) {
    		throw new IllegalStateException("history position is " + historyPosition);
    	}
    	
    	historyPosition++;
        HistoryItem hi = history.get( historyPosition);

        historyForwardAction.setEnabled( historyPosition < history.size() - 1);
        historyBackAction.setEnabled(true);
        lookupFrame.showHistoryItem(hi);
    }

    /**
     * Adds the current state of the lookup frame to the history list. This is done after a search
     * triggered by the user completes. Navigating back will move
     * to the state before the current state (if there is any). Navigating forwards is not
     * possible afterwards because the current state becomes the new final history item.
     * 
     * @param hi The current lookup frame state represented as {@link HistoryItem}.
     */
    void addCurrentState( HistoryItem hi) {
    	history.subList(historyPosition + 1, history.size()).clear();
    	if (history.size() == maxHistorySize) {
    		history.remove( 0);
    	}
        history.add(hi);
        historyPosition = history.size() - 1;
        
        historyBackAction.setEnabled(historyPosition > 0);
        historyForwardAction.setEnabled(false);
    }
}
