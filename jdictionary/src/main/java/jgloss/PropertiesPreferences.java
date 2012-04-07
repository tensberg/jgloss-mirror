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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the application preferences using a <CODE>Properties</CODE> object
 * which is stored in a file. 
 *
 * @author Michael Koch
 */
class PropertiesPreferences extends Preferences {
	private static final Logger LOGGER = Logger.getLogger(PropertiesPreferences.class.getPackage().getName());
	
	/**
     * Path to the user preferences file.
     */
    public final static String PREFS_FILE = 
        System.getProperty( "user.home") + File.separator + ".jgloss";

    private final Properties prefs;
    private boolean changed = false;

    /**
     * Loads the user's preferences from the preference file.
     */
    public PropertiesPreferences() {
        prefs = new Properties( defaults);
        try {
            load();
        } catch (IOException ex) {
            LOGGER.severe( JGloss.MESSAGES.getString( "error.loadPreferences"));
        }
        // store the preferences at shutdown
        JGloss.getApplication().addExitListener(new ExitListener() {
                @Override
				public void onExit() {
                    try {
                        store();
                    } catch (IOException ex) {
                        LOGGER.severe( JGloss.MESSAGES.getString( "error.storePreferences"));
                    }
                }
            });
    }

    @Override
	public synchronized String getString( String key, String d) {
        String out = prefs.getProperty( key);
        if (out == null) {
	        out = d;
        }

        return out;
    }

    /**
     * Sets a new preference value.
     *
     * @param key Key to the preference.
     * @param value The new value. May be <CODE>null</CODE> to reset to the default preference.
     */
    @Override
	public synchronized void set( String key, String value) {
        // test if the value really changed
        String orig = getString( key);
        if (value!=null && !value.equals( orig) || orig==null) {
            changed = true;
            prefs.setProperty( key, value);
            firePropertyChanged( key, orig, value);
        }
    }

    /**
     * Returns the preference for the given key as an int. If the preference is not a
     * valid integer number, the value given to the method will be used.
     *
     * @param key Key to the preference.
     * @param d Default value, if the preference is not parseable as int.
     * @return The corresponding preference value.
     */
    @Override
	public synchronized int getInt( String key, int d) {
        int out = d;
        try {
            out = Integer.parseInt( getString( key));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return out;
    }

    /**
     * Sets the new preference value to an int.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
    @Override
	public synchronized void set( String key, int value) {
        set( key, Integer.toString( value));
    }

    /**
     * Returns the preference for the given key as a double. If the preference is not a
     * valid double number, the value given to the method will be used.
     *
     * @param key Key to the preference.
     * @param d Default value, if the preference is not parseable as double.
     * @return The corresponding preference value.
     */
    @Override
	public synchronized double getDouble( String key, double d) {
        double out = d;
        try {
            out = Double.parseDouble( getString( key));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return out;
    }

    /**
     * Sets the new preference value to a double.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
    @Override
	public synchronized void set( String key, double value) {
        set( key, Double.toString( value));
    }

    /**
     * Returns the preference for the given key as a boolean. If the preference is not a
     * valid boolean, false will be returned.
     *
     * @param key Key to the preference.
     * @return The corresponding preference value.
     */
    @Override
	public synchronized boolean getBoolean( String key, boolean d) {
        String value = getString( key);
        if ("true".equalsIgnoreCase( value)) {
	        return true;
        } else if ("false".equalsIgnoreCase( value)) {
	        return false;
        } else {
	        return d;
        }
    }

    /**
     * Sets the new preference value to a boolean.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
    @Override
	public synchronized void set( String key, boolean value) {
        set( key, value ? "true" : "false");
    }

    /**
     * Loads the user preferences.
     *
     * @exception java.io.IOException if the preferences file cannot be loaded. If the preferences
     *            file does not exist, no error will be signalled.
     */
    private synchronized void load() throws IOException {
        File f = new File( PREFS_FILE);
        if (!f.exists()) {
	        return;
        }

        FileInputStream in = new FileInputStream( PREFS_FILE);
        prefs.load( in);
        in.close();
        changed = false;
    }

    /**
     * Saves the current preferences settings.
     *
     * @param header Message put in the file header.
     * @exception java.io.IOException if the user preference file cannot be written.
     */
    public synchronized void store( String header) throws IOException {
        FileOutputStream out = new FileOutputStream( PREFS_FILE);
        prefs.store( out, header);
        out.close();
        changed = false;
    }

    /**
     * Saves the current preferences settings. The preferences setting are only written if they
     * have been changed. The standard preferences file header is used.
     *
     * @exception java.io.IOException if the user preference file cannot be written.
     */
    private synchronized void store() throws IOException {
        if (changed) {
            store( JGloss.MESSAGES.getString( "preferences.header"));
        }
    }

    /**
     * Copy all preference values set in this preferences (but not in the default setting) to the
     * given {@link Preferences Preferences} object.
     */
    public void copyPreferences( Preferences newPrefs) {
        // iterate over all entries in the preference Hashtable (but not the default prefs)
        for (Map.Entry<Object, Object> entry : prefs.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            try {
                newPrefs.set( key, Integer.parseInt( value));
            } catch (NumberFormatException ex) {
                try {
                    newPrefs.set( key, Double.parseDouble( value));
                } catch (NumberFormatException ex2) {
                    if ("true".equalsIgnoreCase( value)) {
	                    newPrefs.set( key, true);
                    } else if ("false".equalsIgnoreCase( value)) {
	                    newPrefs.set( key, false);
                    } else {
	                    newPrefs.set( key, value);
                    }
                }
            }
        }
    }
} // class PropertiesPreferences
