/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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
 * $Id$
 *
 */

package jgloss;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

/**
 * Implementation of the application preferences which uses the Java 1.4 prefs API.
 *
 * @author Michael Koch
 */
class JavaPreferences extends Preferences implements PreferenceChangeListener {
    /**
     * Underlying Preferences object which holds the user preferences.
     */
    private java.util.prefs.Preferences prefs;
    
    /**
     * Initialize the preferences by getting a <CODE>Preferences<CODE> object for the user node
     * of class jgloss.JGloss.
     */
    public JavaPreferences() {
        prefs = java.util.prefs.Preferences.userNodeForPackage( JGloss.class);
        prefs.addPreferenceChangeListener( this);
    }
    
    /**
     * Return the preference for the given key.
     *
     * @param key Key to a preference.
     * @return The corresponding preference string.
     */
    public String getString( String key, String d) {
        String defaultPref = defaults.getProperty( key);
        if (defaultPref == null)
            defaultPref = d;
        return prefs.get( key, defaultPref);
    }
    
    /**
     * Sets a new preference value.
     *
     * @param key Key to the preference.
     * @param value The new value. May be <CODE>null</CODE> to reset to the default preference.
     */
    public void set( String key, String value) {
        if (value != null)
            prefs.put( key, value);
        else
            prefs.remove( key);
    }

    /**
     * Returns the preference for the given key as an int. If the preference is not a
     * valid integer number, the value given to the method will be used.
     *
     * @param key Key to the preference.
     * @param d Default value, if the preference is not parseable as int.
     * @return The corresponding preference value.
     */
    public int getInt( String key, int d) {
        int def = d;
        try {
            def = Integer.parseInt( defaults.getProperty( key));
        } catch (NumberFormatException ex) {
        } catch (NullPointerException ex) {}
        
        return prefs.getInt( key, def);
    }

    /**
     * Sets the new preference value to an int.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
    public void set( String key, int value) {
        prefs.putInt( key, value);
    }

    /**
     * Returns the preference for the given key as a double. If the preference is not a
     * valid double number, the value given to the method will be used.
     *
     * @param key Key to the preference.
     * @param d Default value, if the preference is not parseable as double.
     * @return The corresponding preference value.
     */
    public double getDouble( String key, double d) {
        double def = d;
        try {
            def = Double.parseDouble( defaults.getProperty( key));
        } catch (NumberFormatException ex) {
        } catch (NullPointerException ex) {}
        
        return prefs.getDouble( key, def);
    }

    /**
     * Sets the new preference value to a double.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
    public void set( String key, double value) {
        prefs.putDouble( key, value);
    }

    /**
     * Returns the preference for the given key as a boolean. If the preference is not a
     * valid boolean, false will be returned.
     *
     * @param key Key to the preference.
     * @return The corresponding preference value.
     */
    public boolean getBoolean( String key, boolean d) {
        boolean def;
        String value = defaults.getProperty( key);
        if ("true".equalsIgnoreCase( value))
            def = true;
        else if ("false".equalsIgnoreCase( value))
            def = false;
        else // unparseable as boolean; use default
            def = d;
        return prefs.getBoolean( key, def);
    }

    /**
     * Sets the new preference value to a boolean.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
    public void set( String key, boolean value) {
        prefs.putBoolean( key, value);
    }

    /**
     * Fire a <CODE>PropertyChangedEvent</CODE> with the data from the preference change event.
     * The field <CODE>oldValue</CODE> of the property change event is not supported and
     * set to <CODE>null</CODE>.
     */
    public void preferenceChange( PreferenceChangeEvent e) {
        firePropertyChanged( e.getKey(), null, e.getNewValue());
    }
} // class JavaPreferences
