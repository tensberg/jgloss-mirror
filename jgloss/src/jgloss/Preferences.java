/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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
import java.beans.*;

/**
 * Management of the application-wide preferences. The preferences are a mapping from string
 * keys to string values. The class provides convenience methods for the storage and
 * retrieval of additional data types. If a user preference for a specific key, the value
 * from the default preferences in resources/preferences.properties (or a localized variant)
 * will be used. The preferences of the user will be stored in the file ".jgloss" in the users
 * home directory.
 *
 * @author Michael Koch
 */
public class Preferences {
    public static final String DICTIONARIES = "dictionaries";
    public static final String DICTIONARIES_DIR = "dictionaries.dir";

    public static final String FRAME_X = "frame.x";
    public static final String FRAME_Y = "frame.y";
    public static final String FRAME_WIDTH = "frame.width";
    public static final String FRAME_HEIGHT = "frame.height";

    public static final String READING_BRACKET_CHARS = "reading.bracket.chars";

    public static final String ENCODINGS = "encodings";
    public static final String FONTSIZES_KANJI = "fontsizes.kanji";
    public static final String FONTSIZES_READING = "fontsizes.reading";
    public static final String FONTSIZES_TRANSLATION = "fontsizes.translation";

    public static final String FONT_TEXT = "font.text";
    public static final String FONT_TEXT_SIZE = "font.text.size";
    public static final String FONT_TEXT_BGCOLOR = "font.text.bgcolor";
    public static final String FONT_TEXT_USECOLOR = "font.text.usecolor";

    public static final String FONT_READING = "font.reading";
    public static final String FONT_READING_SIZE = "font.reading.size";
    public static final String FONT_READING_BGCOLOR = "font.reading.bgcolor";
    public static final String FONT_READING_USECOLOR = "font.reading.usecolor";

    public static final String FONT_TRANSLATION = "font.translation";
    public static final String FONT_TRANSLATION_SIZE = "font.translation.size";
    public static final String FONT_TRANSLATION_BGCOLOR = "font.translation.bgcolor";
    public static final String FONT_TRANSLATION_USECOLOR = "font.translation.usecolor";

    public static final String ANNOTATION_HIGHLIGHT_COLOR = "annotation.highlight.color";

    public static final String DTD_DEFAULT = "dtd.default";

    public static final String VIEW_COMPACTVIEW = "view.compactview";
    public static final String VIEW_SHOWREADING = "view.showreading";
    public static final String VIEW_SHOWTRANSLATION = "view.showtranslation";
    public static final String VIEW_SHOWANNOTATION = "view.showannotation";
    public static final String VIEW_EDITORFOLLOWSMOUSE = "view.editorfollowsmouse";
    public static final String VIEW_ANNOTATIONEDITORHIDDEN = "view.annotationeditorhidden";

    public static final String EDITOR_ENABLEEDITINGCHECKBOX = "editor.enableeditingcheckbox";
    public static final String EDITOR_ENABLEEDITING = "editor.enableediting";

    public static final String IMPORT_PARSER = "import.parser";
    public static final String IMPORT_FIRSTOCCURRENCE = "import.firstoccurrence";
    public static final String IMPORT_READINGBRACKETS = "import.readingbrackets";

    public static final String IMPORTCLIPBOARD_PARSER = "importclipboard.parser";
    public static final String IMPORTCLIPBOARD_FIRSTOCCURRENCE = "importclipboard.firstoccurrence";
    public static final String IMPORTCLIPBOARD_READINGBRACKETS = "importclipboard.readingbrackets";

    public static final String EXPORT_ENCODING = "export.encoding";
    public static final String EXPORT_PLAINTEXT_WRITEREADING = "export.plaintext.writereading";
    public static final String EXPORT_PLAINTEXT_WRITETRANSLATIONS = 
        "export.plaintext.writetranslations";
    public static final String EXPORT_PLAINTEXT_WRITEHIDDEN = 
        "export.plaintext.writehidden";    
    public static final String EXPORT_HTML_WRITEREADING = "export.html.writereading";
    public static final String EXPORT_HTML_WRITETRANSLATIONS = 
        "export.html.writetranslations";
    public static final String EXPORT_HTML_BACKWARDSCOMPATIBLE = 
        "export.html.backwardscompatible";
    public static final String EXPORT_HTML_WRITEHIDDEN = 
        "export.html.writehidden";

    public static final String EXPORT_LATEX_PREAMBLE = "export.latex.preamble";
    public static final String EXPORT_LATEX_DOCUMENTCLASS = "export.latex.documentclass";
    public static final String EXPORT_LATEX_DOCUMENTCLASS_OPTIONS =
        "export.latex.documentclass.options";
    public static final String EXPORT_LATEX_RUBY_OPTIONS =
        "export.latex.ruby.options";
    public static final String EXPORT_LATEX_WRITEREADING = "export.latex.writereading";
    public static final String EXPORT_LATEX_WRITETRANSLATIONS = 
        "export.latex.writetranslations";
    public static final String EXPORT_LATEX_TRANSLATIONSONPAGE = "export.latex.translationsonpage";
    public static final String EXPORT_LATEX_WRITEHIDDEN = 
        "export.latex.writehidden";    

    public static final String EXCLUSIONS_FILE = "exclusions.file";
    public static final String USERDICTIONARY_FILE = "userdictionary.file";

    public static final String STARTUP_WORDLOOKUP = "startup.wordlookup";
    public static final String LEFTCLICK_TOOLTIP = "leftclick.tooltip";

    public static final String WORDLOOKUP_WIDTH = "wordlookup.width";
    public static final String WORDLOOKUP_HEIGHT = "wordlookup.height";
    public static final String WORDLOOKUP_SEARCHTYPE = "wordlookup.searchtype";
    public static final String WORDLOOKUP_DEINFLECTION = "wordlookup.deinflection";
    public static final String WORDLOOKUP_ALLDICTIONARIES = "wordlookup.alldictionaries";
    public static final String WORDLOOKUP_RESULTLIMIT = "wordlookup.resultlimit";

    public static final String CHASEN_LOCATION = "chasen.location";

    public static final String OPENRECENT_FILES = "openrecent.files";
    
    /**
     * Path to the user preferences file.
     */
    private final String PREFS_FILE = 
        System.getProperty( "user.home") + File.separator + ".jgloss";

    private Properties prefs;
    private boolean changed = false;
    private List propertyChangeListeners;

    /**
     * Initializes the preferences with the user's settings.
     */
    public Preferences() {
        // load default settings from properties file
        Properties defaults = new Properties();
        ResourceBundle defrb = ResourceBundle.getBundle( "resources/preferences");
        for ( Enumeration e=defrb.getKeys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            defaults.setProperty( key, (String) defrb.getString( key));
        }

        prefs = new Properties( defaults);
        propertyChangeListeners = new LinkedList();
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
        // see if the value really changed
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
     * Returns the preference for the given key as a boolean. If the preference is not a
     * valid boolean, false will be returned.
     *
     * @param key Key to the preference.
     * @return The corresponding preference value.
     */
    public synchronized boolean getBoolean( String key) {
        return Boolean.valueOf( getString( key)).booleanValue();
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
     * Returns the preference for the given key as an array of strings with pathnames.
     * This is a convenience method which calls getList( key, separator) with the
     * path separator char of this platform.
     *
     * @param key Key to the preference.
     * @return The corresponding preference value.
     */
    public synchronized String[] getPaths( String key) {
        return getList( key, File.pathSeparatorChar);
    }

    /**
     * Returns the preferences for the given key as an array of strings.
     *
     * @param key Key to the preference.
     * @param separator Character which separates the strings in the preference value.
     * @return The corresponding preference value.
     */
    public synchronized String[] getList( String key, char separator) {
        List paths = new ArrayList( 10);
        String s = getString( key);
        if (s != null) {
            s = s.trim();
            int i = 0;
            int j = s.indexOf( separator);
            while (j != -1) {
                paths.add( s.substring( i, j));
                i = j + 1;
                j = s.indexOf( separator, i);
            }
            if (i < s.length()) {
                paths.add( s.substring( i));
            }
        }

        String p[] = new String[paths.size()];
        return (String[]) paths.toArray( p);
    }

    /**
     * Loads the user preferences.
     *
     * @exception java.io.IOException if the preferences file cannot be loaded. If the preferences
     *            file does not exist, no error will be signalled.
     */
    public synchronized void load() throws IOException {
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
    public synchronized void store() throws IOException {
        if (changed) {
            FileOutputStream out = new FileOutputStream( PREFS_FILE);
            prefs.store( out, JGloss.messages.getString( "preferences.header"));
            out.close();
            changed = false;
        }
    }

    /**
     * Adds a listener which will be notified of changes to the preferences.
     *
     * @param cl The listener.
     */
    public void addPropertyChangeListener( PropertyChangeListener cl) {
        synchronized (propertyChangeListeners) {
            if (!propertyChangeListeners.contains( cl))
                propertyChangeListeners.add( cl);
        }
    }

    /**
     * Removes a listener.
     *
     * @param cl The listener.
     */
    public void removePropertyChangeListener( PropertyChangeListener cl) {
        synchronized (propertyChangeListeners) {
            propertyChangeListeners.remove( cl);
        }
    }

    /**
     * Notifies the listeners of a change to the preferences.
     *
     * @param name The key of the changed preference.
     * @param oldValue The previous setting of the preference.
     * @param newValue The new setting of the preference.
     */
    private void firePropertyChanged( String name, Object oldValue, Object newValue) {
        PropertyChangeEvent e = new PropertyChangeEvent( this, name, oldValue, newValue);

        synchronized (propertyChangeListeners) {
            for ( Iterator i=propertyChangeListeners.iterator(); i.hasNext(); ) {
                ((PropertyChangeListener) i.next()).propertyChange( e);
            }
        }
    }
} // class Preferences
