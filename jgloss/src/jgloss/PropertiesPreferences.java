/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

import java.io.*;
import java.util.*;

/**
 * Implementation of the application preferences using a <CODE>Properties</CODE> object
 * which is stored in a file. 
 *
 * @author Michael Koch
 */
class PropertiesPreferences extends Preferences {
    /**
     * Path to the user preferences file.
     */
    private final String PREFS_FILE = 
        System.getProperty( "user.home") + File.separator + ".jgloss";

    private Properties prefs;
    private boolean changed = false;

    /**
     * Loads the user's preferences from the preference file.
     */
    public PropertiesPreferences() {
        prefs = new Properties( defaults);
        try {
            load();
        } catch (IOException ex) {
            System.err.println( JGloss.messages.getString( "error.loadPreferences"));
        }
        // store the preferences at shutdown
        Runtime.getRuntime().addShutdownHook( new Thread() {
                public void run() {
                    try {
                        store();
                    } catch (IOException ex) {
                        System.err.println( JGloss.messages.getString( "error.storePreferences"));
                    }
                }
            });
    }

    /**
     * Return the preference for the given key.
     *
     * @param key Key to a preference.
     * @return The corresponding preference string.
     */
    public synchronized String getString( String key) {
        return prefs.getProperty( key);
    }

    /**
     * Sets a new preference value.
     *
     * @param key Key to the preference.
     * @param value The new value. May be <CODE>null</CODE> to reset to the default preference.
     */
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
    public synchronized int getInt( String key, int d) {
        int out = d;
        try {
            out = Integer.parseInt( getString( key));
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }

        return out;
    }

    /**
     * Sets the new preference value to an int.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
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
    public synchronized double getDouble( String key, double d) {
        double out = d;
        try {
            out = Double.parseDouble( getString( key));
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }

        return out;
    }

    /**
     * Sets the new preference value to a double.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
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
    public synchronized boolean getBoolean( String key, boolean d) {
        String value = getString( key);
        if ("true".equalsIgnoreCase( value))
            return true;
        else if ("false".equalsIgnoreCase( value))
            return false;
        else // unparseable as boolean; return default
            return d;
    }

    /**
     * Sets the new preference value to a boolean.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
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
        if (!f.exists()) // no preferences
            return;

        FileInputStream in = new FileInputStream( PREFS_FILE);
        prefs.load( in);
        in.close();
        changed = false;
    }

    /**
     * Saves the current preferences settings.
     *
     * @exception java.io.IOException if the user preference file cannot be written.
     */
    private synchronized void store() throws IOException {
        if (changed) {
            FileOutputStream out = new FileOutputStream( PREFS_FILE);
            prefs.store( out, JGloss.messages.getString( "preferences.header"));
            out.close();
            changed = false;
        }
    }

    /**
     * Copy all preference values set in this preferences (but not in the default setting) to the
     * given {@link Preferences Preferences} object.
     */
    public void copyPreferences( Preferences newPrefs) {
        // iterate over all entries in the preference Hashtable (but not the default prefs)
        for ( Iterator entries=prefs.entrySet().iterator(); entries.hasNext(); ) {
            Map.Entry entry = (Map.Entry) entries.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            try {
                newPrefs.set( key, Integer.parseInt( value));
            } catch (NumberFormatException ex) {
                try {
                    newPrefs.set( key, Double.parseDouble( value));
                } catch (NumberFormatException ex2) {
                    if ("true".equalsIgnoreCase( value))
                        newPrefs.set( key, true);
                    else if ("false".equalsIgnoreCase( value))
                        newPrefs.set( key, false);
                    else // set prefs value as string
                        newPrefs.set( key, value);
                }
            }
        }
    }
} // class PropertiesPreferences