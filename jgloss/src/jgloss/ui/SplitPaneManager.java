package jgloss.ui;

import jgloss.JGloss;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.JSplitPane;

class SplitPaneManager implements PropertyChangeListener {
    private static final String PREFS_KEY = "splitpane.";
    private static final String PREFS_KEY_LOCATION = ".location";

    private static final String PROPERTY_KEY_LOCATION = "location prefs key property";

    private String prefsPrefix;
    private int splitPaneCount;

    SplitPaneManager( String _prefsPrefix) {
        prefsPrefix = _prefsPrefix;
        if (prefsPrefix.length() > 0 && prefsPrefix.charAt( prefsPrefix.length()-1)!='.')
            prefsPrefix += '.';
    }
    
    public void add( JSplitPane splitPane, double defaultLocation) {
        splitPaneCount++;
        String prefix = prefsPrefix + PREFS_KEY + splitPaneCount;
        String locationKey = prefix + PREFS_KEY_LOCATION;

        splitPane.putClientProperty( PROPERTY_KEY_LOCATION,
                                     locationKey);

        double location = JGloss.prefs.getDouble( locationKey, defaultLocation);
        splitPane.setDividerLocation( location);
        splitPane.setResizeWeight( location);

        splitPane.addPropertyChangeListener( this);
    }

    public void propertyChange( PropertyChangeEvent e) {
        JSplitPane splitPane = (JSplitPane) e.getSource();
        if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals( e.getPropertyName())) {
            String prefsKey = (String) splitPane.getClientProperty( PROPERTY_KEY_LOCATION);
            int newValue = ((Integer) e.getNewValue()).intValue();

            if (newValue <= splitPane.getMinimumDividerLocation())
                newValue = 0;
            else if (newValue >= splitPane.getMaximumDividerLocation())
                newValue = splitPane.getWidth();

            JGloss.prefs.set( prefsKey,
                              ((double) newValue)/
                              splitPane.getWidth());
        }
    }
} // class SplitPaneManager
