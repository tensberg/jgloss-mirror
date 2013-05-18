/*
 * Copyright (C) 2001-2013 Michael Koch (tensberg@gmx.net)
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

package jgloss;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

import jgloss.util.UTF8ResourceBundleControl;

/**
 * Management of the application-wide preferences. The preference storage mechanism varies
 * with the Version of the running VM. Under Java 1.3, {@link PropertiesPreferences PropertiesPreferences}
 * is used, which stores the preferences in a properties file. Under Java 1.4,
 * {@link JavaPreferences JavaPreferences} is used, which uses the <CODE>java.util.prefs</CODE> preferences.
 * <p>
 * A set of default preferences is stored with the application jar file and accessed using the
 * <CODE>ResourceBundle</CODE> mechanism from the resource "/preferences".
 * If a user preference is not yet set, the value from the default preferences is used.
 * The default value passed to the methods <CODE>getInt</CODE>,... is only used if the default
 * preference setting (a string) cannot be converted to the requested data type.
 * </p><p>
 * Property change listeners are used to inform interested objects of preference value changes.
 * The field <CODE>oldValue</CODE> of any property change event may not be initialized depending
 * on the implementation.
 * </p>
 *
 * @author Michael Koch
 */
public abstract class Preferences {
    public static final String DICTIONARIES = "dictionaries";
    public static final String DICTIONARIES_DIR = "dictionaries.dir";

    public static final String FRAME_X = "frame.x";
    public static final String FRAME_Y = "frame.y";
    public static final String FRAME_WIDTH = "frame.width";
    public static final String FRAME_HEIGHT = "frame.height";

    public static final String READING_BRACKET_CHARS = "reading.bracket.chars";

    public static final String ENCODINGS = "encodings";
    public static final String FONTSIZES_WORDLOOKUP = "fontsizes.wordlookup";
    public static final String FONTSIZES_KANJI = "fontsizes.kanji";
    public static final String FONTSIZES_READING = "fontsizes.reading";
    public static final String FONTSIZES_TRANSLATION = "fontsizes.translation";

    public static final String FONT_GENERAL_USEDEFAULT = "font.general.usedefault";
    public static final String FONT_GENERAL = "font.general";
    public static final String FONT_GENERAL_SIZEDIFF = "font.general.sizediff";

    public static final String FONT_WORDLOOKUP = "font.wordlookup";
    public static final String FONT_WORDLOOKUP_SIZE = "font.wordlookup.size";

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

    public static final String FONT_DEFAULTFONTS = "font.defaultfonts";
    public static final String FONT_AUTODETECTED = "font.autodetected";

    public static final String ANNOTATION_HIGHLIGHT_COLOR = "annotation.highlight.color";

    public static final String DTD_DEFAULT = "dtd.default";

    public static final String VIEW_COMPACTVIEW = "view.compactview";
    public static final String VIEW_SHOWREADING = "view.showreading";
    public static final String VIEW_SHOWTRANSLATION = "view.showtranslation";
    public static final String VIEW_SHOWANNOTATION = "view.showannotation";
    public static final String VIEW_ANNOTATIONEDITORHIDDEN = "view.annotationeditorhidden";
    public static final String VIEW_DIVIDERLOCATION = "view.dividerlocation";
    public static final String VIEW_MAXTRANSLATIONLENGTH = "view.maxtranslationlength";

    public static final String EDITOR_ENABLEEDITINGCHECKBOX = "editor.enableeditingcheckbox";
    public static final String EDITOR_ENABLEEDITING = "editor.enableediting";

    public static final String IMPORT_PARSER = "import.parser";
    public static final String IMPORT_FIRSTOCCURRENCE = "import.firstoccurrence";
    public static final String IMPORT_DETECTPARAGRAPHS = "import.detectparagraphs";
    public static final String IMPORT_READINGBRACKETS = "import.readingbrackets";

    public static final String IMPORTCLIPBOARD_PARSER = "importclipboard.parser";
    public static final String IMPORTCLIPBOARD_FIRSTOCCURRENCE = "importclipboard.firstoccurrence";
    public static final String IMPORTCLIPBOARD_DETECTPARAGRAPHS = "importclipboard.detectparagraphs";
    public static final String IMPORTCLIPBOARD_READINGBRACKETS = "importclipboard.readingbrackets";

    public static final String EXPORT_PLAINTEXT_ENCODING = "export.plaintext.encoding";
    public static final String EXPORT_PLAINTEXT_WRITEREADING = "export.plaintext.writereading";
    public static final String EXPORT_PLAINTEXT_WRITETRANSLATIONS =
        "export.plaintext.writetranslations";
    public static final String EXPORT_PLAINTEXT_WRITEHIDDEN =
        "export.plaintext.writehidden";

    public static final String EXPORT_HTML_ENCODING = "export.html.encoding";
    public static final String EXPORT_HTML_WRITEREADING = "export.html.writereading";
    public static final String EXPORT_HTML_WRITETRANSLATIONS =
        "export.html.writetranslations";
    public static final String EXPORT_HTML_BACKWARDSCOMPATIBLE =
        "export.html.backwardscompatible";
    public static final String EXPORT_HTML_WRITEHIDDEN =
        "export.html.writehidden";

    public static final String EXPORT_LATEX_WRITEHIDDEN =
        "export.latex.writehidden";
    public static final String EXPORT_LATEX_TEMPLATE = "export.latex.template";
    public static final String EXPORT_LATEX_USERTEMPLATES = "export.latex.usertemplates";
    public static final String EXPORT_LATEX_FONTSIZES = "export.latex.fontsizes";
    public static final String EXPORT_LATEX_FONTSIZE = "export.latex.fontsize";

    public static final String EXPORT_ANNOTATIONLIST_ENCODING = "export.annotationlist.encoding";

    public static final String EXPORT_EXCLUSIONS_ENCODING = "export.exclusions.encoding";

    public static final String EXCLUSIONS_FILE = "exclusions.file";
    public static final String EXCLUSIONS_ENCODING = "exclusions.encoding";
    public static final String USERDICTIONARY_FILE = "userdictionary.file";

    public static final String STARTUP_WORDLOOKUP = "startup.wordlookup";
    public static final String LEFTCLICK_TOOLTIP = "leftclick.tooltip";

    public static final String WORDLOOKUP_WIDTH = "wordlookup.width";
    public static final String WORDLOOKUP_HEIGHT = "wordlookup.height";
    public static final String WORDLOOKUP_SEARCHTYPE = "wordlookup.searchtype";
    public static final String WORDLOOKUP_DEINFLECTION = "wordlookup.deinflection";
    public static final String WORDLOOKUP_ALLDICTIONARIES = "wordlookup.alldictionaries";
    public static final String WORDLOOKUP_RESULTLIMIT = "wordlookup.resultlimit";
    public static final String WORDLOOKUP_SEARCHCLIPBOARD = "wordlookup.searchclipboard";

    public static final String CHASEN_LOCATION = "chasen.location";

    public static final String OPENRECENT_FILES = "openrecent.files";

    public static final String HISTORY_SELECTION = "history.selection";
    public static final String HISTORY_SIZE = "history.size";

    public static final String SHOW_WELCOME_DIALOG = "welcomedialog.show";

    public static final String PREFERENCES_MIGRATED = "preferences.migrated";

    /**
     * Default application preferences. The default preferences are loaded from the
     * resource "/preferences" and stored in a property file and initialized in the
     * <CODE>Preferences</CODE> constructor.
     */
    protected Properties defaults;
    /**
     * List of objects being notified of preference changes.
     */
    protected final List<PropertyChangeListener> propertyChangeListeners = new CopyOnWriteArrayList<PropertyChangeListener>();

    /**
     * Initializes the preferences with the user's settings.
     */
    public Preferences() {
        // load default settings from properties file
        defaults = new Properties();
        ResourceBundle defrb = ResourceBundle.getBundle( "preferences", new UTF8ResourceBundleControl());
        for (String key : defrb.keySet()) {
            defaults.setProperty( key, defrb.getString( key));
        }
    }

    /**
     * Return the preference for the given key.
     *
     * @param key Key to a preference.
     * @return The corresponding preference string.
     */
    public String getString( String key) {
        return getString( key, "");
    }

    /**
     * Return the preference for the given key.
     *
     * @param key Key to a preference.
     * @param d Default value. Used if neither the preference value nor the default preference
     *          value is set.
     * @return The corresponding preference string.
     */
    public abstract String getString( String key, String d);

    /**
     * Sets a new preference value.
     *
     * @param key Key to the preference.
     * @param value The new value. May be <CODE>null</CODE> to reset to the default preference.
     */
    public abstract void set( String key, String value);

    /**
     * Returns the preference for the given key as an int. If the preference is not a
     * valid integer number, the value given to the method will be used.
     *
     * @param key Key to the preference.
     * @param d Default value, if the preference is not parseable as int.
     * @return The corresponding preference value.
     */
    public abstract int getInt( String key, int d);

    /**
     * Sets the new preference value to an int.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
    public abstract void set( String key, int value);

    /**
     * Returns the preference for the given key as a double. If the preference is not a
     * valid double number, the value given to the method will be used.
     *
     * @param key Key to the preference.
     * @param d Default value, if the preference is not parseable as double.
     * @return The corresponding preference value.
     */
    public abstract double getDouble( String key, double d);

    /**
     * Sets the new preference value to a double.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
    public abstract void set( String key, double value);

    /**
     * Returns the preference for the given key as a boolean. If the preference is not a
     * valid boolean, false will be returned.
     *
     * @param key Key to the preference.
     * @param d Default value, if the preference is not parseable as boolean.
     * @return The corresponding preference value.
     */
    public abstract boolean getBoolean( String key, boolean d);

    /**
     * Sets the new preference value to a boolean.
     *
     * @param key Key to the preference.
     * @param value The new value.
     */
    public abstract void set( String key, boolean value);

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
     * @return The corresponding preference value as string array. If the preference is not
     *         set, an empty array will be returned.
     */
    public synchronized String[] getList( String key, char separator) {
        List<String> paths = new ArrayList<String>( 10);
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

        return paths.toArray(new String[paths.size()]);
    }

    /**
     * Adds a listener which will be notified of changes to the preferences.
     *
     * @param cl The listener.
     */
    public void addPropertyChangeListener( PropertyChangeListener cl) {
        synchronized (propertyChangeListeners) {
            if (!propertyChangeListeners.contains( cl)) {
	            propertyChangeListeners.add( cl);
            }
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
    protected void firePropertyChanged( String name, Object oldValue, Object newValue) {
        PropertyChangeEvent e = new PropertyChangeEvent( this, name, oldValue, newValue);

        for (PropertyChangeListener listener : propertyChangeListeners) {
        	listener.propertyChange( e);
        }
    }
} // class Preferences
