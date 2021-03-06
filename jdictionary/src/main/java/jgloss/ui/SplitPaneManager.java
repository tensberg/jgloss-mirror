/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
 *
 * This file is part of JGloss.
 *
 * JGloss is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JGloss is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGloss; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 */

package jgloss.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSplitPane;

import jgloss.JGloss;

/**
 * Makes the state of split panes persistent by storing their divider locations in the preferences.
 *
 * @author Michael Koch
 */
public class SplitPaneManager implements PropertyChangeListener {
	private static final String PREFS_KEY = "splitpane.";
    private static final String PREFS_KEY_LOCATION = ".location";

    private static final String PROPERTY_KEY_LOCATION = "location prefs key property";

    private String prefsPrefix;
    private int splitPaneCount;

    public SplitPaneManager( String _prefsPrefix) {
        prefsPrefix = _prefsPrefix;
        if (prefsPrefix.length() > 0 && prefsPrefix.charAt( prefsPrefix.length()-1)!='.') {
	        prefsPrefix += '.';
        }
    }
    
    public void add( JSplitPane splitPane, double defaultLocation) {
        splitPaneCount++;
        String prefix = prefsPrefix + PREFS_KEY + splitPaneCount;
        String locationKey = prefix + PREFS_KEY_LOCATION;

        splitPane.putClientProperty( PROPERTY_KEY_LOCATION,
                                     locationKey);

        double location = JGloss.PREFS.getDouble( locationKey, defaultLocation);
        splitPane.setDividerLocation( location);
        splitPane.setResizeWeight( location);

        splitPane.addPropertyChangeListener( this);
    }

    @Override
	public void propertyChange( PropertyChangeEvent e) {
        JSplitPane splitPane = (JSplitPane) e.getSource();
        if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals( e.getPropertyName())) {
            String prefsKey = (String) splitPane.getClientProperty( PROPERTY_KEY_LOCATION);
            int newValue = ((Integer) e.getNewValue()).intValue();

            if (newValue <= splitPane.getMinimumDividerLocation()) {
	            newValue = 0;
            } else if (newValue >= splitPane.getMaximumDividerLocation()) {
	            newValue = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ?
                    splitPane.getWidth() : splitPane.getHeight();
            }
            
            double newLocation = ((double) newValue)/
                (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ?
                 splitPane.getWidth() : splitPane.getHeight());
                              
            JGloss.PREFS.set(prefsKey, newLocation);
        }
    }
} // class SplitPaneManager
