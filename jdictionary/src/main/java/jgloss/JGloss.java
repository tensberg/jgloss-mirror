/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryFactory;
import jgloss.dictionary.DictionaryImplementation;
import jgloss.dictionary.DictionaryInstantiationException;
import jgloss.dictionary.ExpressionSearchModes;
import jgloss.dictionary.IndexedDictionary;
import jgloss.dictionary.KanjiDic;
import jgloss.dictionary.SearchMode;
import jgloss.dictionary.UnsupportedDescriptorException;
import jgloss.dictionary.attribute.Attributes;
import jgloss.dictionary.filebased.EDict;
import jgloss.dictionary.filebased.WadokuJT;
import jgloss.ui.AboutFrame;
import jgloss.ui.AttributeResultFilter;
import jgloss.ui.Dictionaries;
import jgloss.ui.DictionaryListChangeListener;
import jgloss.ui.LookupModel;
import jgloss.ui.LookupResultFilter;
import jgloss.ui.PreferencesFrame;
import jgloss.ui.PreferencesPanel;
import jgloss.ui.SplashScreen;
import jgloss.ui.StyleDialog;

/**
 * Framework for the initialization of the two applications {@link JGlossApp JGlossApp} and
 * {@link JDictionaryApp JDictionaryApp}. Provides the basic functionality for initialization
 * and destruction of the application instance and hooks for extending the process. Also provides
 * static access to resources used by the application UI classes.
 *
 * @author Michael Koch
 */
public abstract class JGloss implements ExitListener {
	private static final Logger LOGGER = Logger.getLogger(JGloss.class.getPackage().getName());

	/**
     * Path to the file with message strings.
     */
    private static final String MESSAGES_BUNDLE = "messages";

    /**
     * The application-wide preferences. Use this to store and retrieve preferences.
     */
    public static final Preferences PREFS = initPreferences();

    /**
     * The application-wide messages. Use this to retrieve localizable string messages.
     */
    public static final Messages MESSAGES = new Messages( MESSAGES_BUNDLE);

    private static JGloss application;

    private final List<ExitListener> exitListeners = new CopyOnWriteArrayList<ExitListener>();

    /**
     * Path to the directory last used.
     */
    private String currentDir;

    protected LookupModel mainLookupModel;

    /**
     *
     * @return Singleton instance of the JGloss application.
     */
    public static JGloss getApplication() {
        assert EventQueue.isDispatchThread();
        assert application != null;
        return application;
    }

    /**
     * Empty constructor.
     */
    JGloss() {}

    protected void init(final String[] args) {
        assert application == null;

        application = this;

        addExitListener(this);

        registerDictionaries();

        try {
            handleCommandLine(args);

            initUI();
        } catch (Exception ex) {
            displayError(MESSAGES.getString("error.initialization.generic"), ex, true);
            System.exit(1);
        }

        final SplashScreen splash = new SplashScreen(getApplicationName());

        splash.setInfo(MESSAGES.getString("splashscreen.initMain"));

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    createDialogs();
                    showMainWindow(args);
                } catch (NoClassDefFoundError | NoSuchMethodError | ClassNotFoundException ex) {
                    displayError(MESSAGES.getString("error.noclassdef"), ex, true);
                    System.exit(1);
                } catch (Exception ex) {
                    displayError(MESSAGES.getString("error.initialization.generic"), ex, true);
                    System.exit(1);
                }
                splash.close();
            }
        });
    }

    public boolean exit() {
        boolean exit = doExit();
        if (exit) {
            LOGGER.log(FINE, "shutting down JGloss");
            fireOnExit();

            LOGGER.log(INFO, "JGloss shut down");
            System.exit(0);
        }
        return exit;
    }

    protected abstract String getApplicationName();

    protected abstract void showMainWindow( String[] args) throws Exception;

    protected abstract PreferencesPanel[] getPreferencesPanels();

    protected abstract boolean doExit();

    /**
     * Returns the current directory. This usually is the directory which was last
     * used in a file chooser. When called for the first time, the user's home directory
     * is used.
     *
     * @return Path to the current directory.
     */
    public String getCurrentDir() {
        assert EventQueue.isDispatchThread();
        if (currentDir == null) {
	        currentDir = System.getProperty( "user.home");
        }

        return currentDir;
    }

    /**
     * Sets the current directory. This method should be called after the user has
     * selected a file in a file chooser.
     *
     * @param dir The new current directory.
     */
    public void setCurrentDir( String dir) {
        assert EventQueue.isDispatchThread();
        currentDir = dir;
    }

    public void addExitListener(ExitListener listener) {
        exitListeners.add(listener);
    }

    public void removeExitListener(ExitListener listener) {
        exitListeners.remove(listener);
    }

    private void fireOnExit() {
        for (ExitListener listener : exitListeners) {
            try {
                listener.onExit();
            } catch (RuntimeException ex) {
                LOGGER.log(WARNING, "exit listener failed", ex);
            }
        }
    }

    protected void registerDictionaries() {
        DictionaryFactory.registerImplementation(EDict.IMPLEMENTATION_EUC);
        DictionaryFactory.registerImplementation(EDict.IMPLEMENTATION_UTF8);
        DictionaryFactory.registerImplementation(WadokuJT.IMPLEMENTATION);
        DictionaryFactory.registerImplementation(KanjiDic.IMPLEMENTATION);
    }

    protected void initUI() throws Exception {
        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());

        // automatically set the fonts the first time JGloss is run
        if (!JGloss.PREFS.getBoolean( Preferences.FONT_AUTODETECTED, false)) {
            StyleDialog.autodetectFonts();
            JGloss.PREFS.set( Preferences.FONT_AUTODETECTED, true);
        }

        // make sure the UI font is initialized before any UI elements are created
        StyleDialog.applyUIFont();
    }

    protected void handleCommandLine( String[] args) throws Exception {
        // parse command line options
        if (args.length > 0) {
            if (args[0].equals( "-h") || args[0].equals( "--help") ||
                args[0].equals( "/?")) {
                System.out.println( MESSAGES.getString( "main.usage", getApplicationName()));
                System.exit( 0);
            }
            else if (args[0].equals( "-i") || args[0].equals( "--createindex")) {
                for ( int i=1; i<args.length; i++) {
                    // build an index for the given file if it is in a known dictionary format,
                    // which is of class IndexedDictionary
                    try {
                        Dictionary d = DictionaryFactory.createDictionary( args[i]);
                        if (d instanceof IndexedDictionary &&
                            !((IndexedDictionary) d).loadIndex()) {
                            LOGGER.severe( MESSAGES.getString
                                                ( "main.createindex", d.getName()));
                            ((IndexedDictionary) d).buildIndex();
                        }
                        else {
                            LOGGER.severe( MESSAGES.getString
                                                ( "main.createindex.noindex", d.getName()));
                        }
                        d.dispose();
                    } catch (UnsupportedDescriptorException ex) {
                        LOGGER.severe( MESSAGES.getString
                                            ( "main.format.unrecognized", args[i] ));
                    } catch (Exception ex) {
                        if (ex instanceof DictionaryInstantiationException) {
	                        ex = (Exception) ex.getCause();
                        }
                        LOGGER.severe( MESSAGES.getString
                                            ( "main.createindex.exception", args[i],
                                                             ex.getClass().getName(),
                                                             ex.getLocalizedMessage()));
                    }
                }
                System.exit( 0);
            }
            else if (args[0].equals( "-f") || args[0].equals( "--format")) {
                for ( int i=1; i<args.length; i++) {
                    try {
                        DictionaryImplementation<?> imp =
                            DictionaryFactory.getImplementation( args[i]);
                        LOGGER.severe( MESSAGES.getString
                                            ( "main.format", args[i], imp.getName()));
                    } catch (UnsupportedDescriptorException ex) {
                        LOGGER.severe( MESSAGES.getString
                                            ( "error.dictionary.reason", args[i], ex.getMessage()));
                    }
                }
                System.exit( 0);
            }
            else if (args[0].startsWith( "-") ||
                     File.separatorChar != '/' && args[0].startsWith( "/")) {
                LOGGER.severe( MESSAGES.getString( "main.unknownoption", args[0]));
                LOGGER.severe( MESSAGES.getString( "main.usage"));
                System.exit( 1);
            }
        }
    }

    /**
     * Shows an error message to the user.
     *
     * @param message A user-understandable error message.
     * @param t The exception which signalled the error.
     * @param fatal <code>true</code> if the application cannot be started because of the error.
     */
    protected static void displayError( String message, Throwable t, boolean fatal) {
        LOGGER.log(Level.SEVERE, message, t);

        // Display error message in a frame. Note that this needs at least Java 1.1
        final Frame f = new Frame();
        f.setLayout( new BorderLayout());

        String msg = MESSAGES.getString( "error.initialization",
            fatal ? MESSAGES.getString( "error.initialization.fatal") : "",  message);

        JOptionPane.showMessageDialog(null, msg, MESSAGES.getString("error.initialization.title"), JOptionPane.ERROR_MESSAGE);
    }

    protected LookupModel createLookupModel() {
        mainLookupModel = new LookupModel
            ( Arrays.asList
              ( new SearchMode[] { ExpressionSearchModes.EXACT,
                               ExpressionSearchModes.ANY,
                               ExpressionSearchModes.PREFIX,
                               ExpressionSearchModes.SUFFIX,
                               // distance search modes are currently not supported by any dictionary implementation
//                               DistanceSearchModes.NEAR,
//                               DistanceSearchModes.RADIUS
                               }),
              Arrays.asList( Dictionaries.getInstance().getDictionaries()),
              Arrays.asList( new LookupResultFilter[]
                  {
                      new AttributeResultFilter( MESSAGES.getString( "filter.mainentry.name"),
                                                 MESSAGES.getString( "filter.mainentry.desc"),
                                                 WadokuJT.MAIN_ENTRY, true),
                      new AttributeResultFilter( MESSAGES.getString( "filter.example.name"),
                                                 MESSAGES.getString( "filter.example.desc"),
                                                 Attributes.EXAMPLE, true),
                      new AttributeResultFilter( MESSAGES.getString( "filter.priority.name"),
                                                 MESSAGES.getString( "filter.priority.desc"),
                                                 Attributes.PRIORITY, true)
                  }));
        Dictionaries.getInstance().addDictionaryListChangeListener
            ( new DictionaryListChangeListener() {
                    @Override
					public void dictionaryListChanged() {
                        mainLookupModel.setDictionaries
                            ( Arrays.asList( Dictionaries.getInstance().getDictionaries()));
                    }
                });

        return mainLookupModel;
    }

    @Override
    public void onExit() {
        Dictionary[] dicts = Dictionaries.getInstance().getDictionaries();
        for (Dictionary dict : dicts) {
	        dict.dispose();
        }
    }

    /**
     * Initialize dialogs before the main window is shown.
     */
    protected void createDialogs() {
        AboutFrame.createShowAction( getApplicationName());
        PreferencesFrame.createFrame( getPreferencesPanels());
    }

    /**
     * Return a Preference implementation appropriate for the current Java VM.
     *
     * @see PropertiesPreferences
     * @see JavaPreferences
     */
    private static Preferences initPreferences() {
        Preferences prefs = null;
        prefs = new JavaPreferences();
        // copy old settings if needed
        if (!prefs.getBoolean( Preferences.PREFERENCES_MIGRATED, false) &&
            new File( PropertiesPreferences.PREFS_FILE).exists()) {
            PropertiesPreferences prop = new PropertiesPreferences();
            prop.copyPreferences( prefs);
            // write note about migration in old prefs file
            try {
                prop.store( ResourceBundle.getBundle( MESSAGES_BUNDLE)
                            .getString( "preferences.header.obsolete"));
            } catch (IOException ex) {}
            prefs.set( Preferences.PREFERENCES_MIGRATED, true);
        }

        // migrate export.encoding, which was used in JGloss 1.0.3 and earlier
        String encoding = prefs.getString( "export.encoding");
        if (encoding == null) {
	        encoding = "SHIFT_JIS";
        }
        if (prefs.getString( Preferences.EXPORT_PLAINTEXT_ENCODING).length() == 0) {
	        prefs.set( Preferences.EXPORT_PLAINTEXT_ENCODING, encoding);
        }
        if (prefs.getString( Preferences.EXPORT_HTML_ENCODING).length() == 0) {
	        prefs.set( Preferences.EXPORT_HTML_ENCODING, encoding);
        }
        if (prefs.getString( Preferences.EXPORT_ANNOTATIONLIST_ENCODING).length() == 0) {
	        prefs.set( Preferences.EXPORT_ANNOTATIONLIST_ENCODING, encoding);
        }
        if (prefs.getString( Preferences.EXPORT_EXCLUSIONS_ENCODING).length() == 0) {
	        prefs.set( Preferences.EXPORT_EXCLUSIONS_ENCODING, encoding);
        }

        return prefs;
    }
} // class JGloss
