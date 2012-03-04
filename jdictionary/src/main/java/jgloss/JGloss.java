/*
 * Copyright (C) 2001-2004 Michael Koch (tensberg@gmx.net)
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

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.UIManager;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryFactory;
import jgloss.dictionary.DistanceSearchModes;
import jgloss.dictionary.EDict;
import jgloss.dictionary.ExpressionSearchModes;
import jgloss.dictionary.IndexedDictionary;
import jgloss.dictionary.KanjiDic;
import jgloss.dictionary.SearchMode;
import jgloss.dictionary.WadokuJT;
import jgloss.dictionary.attribute.Attributes;
import jgloss.ui.AboutFrame;
import jgloss.ui.AttributeResultFilter;
import jgloss.ui.Dictionaries;
import jgloss.ui.LookupModel;
import jgloss.ui.LookupResultFilter;
import jgloss.ui.PreferencesFrame;
import jgloss.ui.PreferencesPanel;
import jgloss.ui.SplashScreen;
import jgloss.ui.StyleDialog;
import jgloss.util.UTF8ResourceBundleControl;

/**
 * Framework for the initialization of the two applications {@link JGlossApp JGlossApp} and
 * {@link JDictionaryApp JDictionaryApp}. Provides the basic functionality for initialization
 * and destruction of the application instance and hooks for extending the process. Also provides
 * static access to resources used by the application UI classes.
 *
 * @author Michael Koch
 */
public abstract class JGloss {
    /**
     * Path to the file with message strings.
     */
    private static final String MESSAGES = "messages";

    /**
     * Path to the directory last used.
     */
    private static String currentDir;

    /**
     * Ties together ResourceBundles and MessageFormat to access localizable,
     * customizable messages.
     */
    public static class Messages {
        private ResourceBundle messages;

        /**
         * Creates a new Messages object which accesses the given resource bundle.
         * The system default locale will be used.
         *
         * @param bundle Base name of the bundle.
         */
        public Messages( String bundle) {
            messages = ResourceBundle.getBundle( MESSAGES, new UTF8ResourceBundleControl());
        }

        /**
         * Returns the string for the given key.
         *
         * @param key Key for a string in the resource bundle.
         * @return Message for this key.
         */
        public String getString( String key) {
            return messages.getString( key);
        }

        /**
         * Returns the string for the given key, filled in with the given values.
         * MessageFormat is used to parse the string stored in the resource bundle
         * and fill in the Objects. See the documentation for MessageFormat for the
         * used format.
         *
         * @param key Key for a string in the resource bundle.
         * @param data Data to insert in the message.
         * @return Message for this key and data.
         * @see java.text.MessageFormat
         */
        public String getString( String key, Object[] data) {
            return MessageFormat.format( messages.getString( key), data);
        }
    } // class messages

    /**
     * The application-wide preferences. Use this to store and retrieve preferences.
     */
    public static final Preferences prefs = initPreferences();

    /**
     * The application-wide messages. Use this to retrieve localizable string messages.
     */
    public static final Messages messages = new Messages( MESSAGES);

    protected static JGloss application;

    protected LookupModel mainLookupModel;

    public static boolean exit() {
        return application.doExit();
    }

    /**
     * Empty constructor.
     */
    JGloss() {}

    protected void init( String[] args) {
        application = this;
        try {
            registerDictionaries();

            handleCommandLine( args);

            initUI();

            SplashScreen splash = new SplashScreen( getApplicationName());

            splash.setInfo( messages.getString( "splashscreen.initMain"));

            Runtime.getRuntime().addShutdownHook
                ( new Thread() {
                        @Override
						public void run() {
                            shutdownHook();
                        }
                    });

            createDialogs();

            showMainWindow( args);

            splash.close();

            new Thread() {
                @Override
				public void run() {
                    try {
                        setPriority( Thread.MIN_PRIORITY);
                    } catch (IllegalArgumentException ex) {}

                    backgroundCreateDialogs();
                }
            }.start();
        } catch (NoClassDefFoundError ex) {
            displayError( messages.getString( "error.noclassdef"), ex, true);
            System.exit( 1);
        } catch (NoSuchMethodError ex) {
            displayError( messages.getString( "error.noclassdef"), ex, true);
            System.exit( 1);
        } catch (ClassNotFoundException ex) {
            displayError( messages.getString( "error.noclassdef"), ex, true);
            System.exit( 1);
        } catch (Exception ex) {
            displayError( messages.getString( "error.initialization.generic"), ex, true);
            System.exit( 1);
        }
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
    public static String getCurrentDir() {
        if (currentDir == null)
            currentDir = System.getProperty( "user.home");

        return currentDir;
    }

    /**
     * Sets the current directory. This method should be called after the user has
     * selected a file in a file chooser.
     *
     * @param dir The new current directory.
     */
    public static void setCurrentDir( String dir) {
        currentDir = dir;
    }

    protected void registerDictionaries() {
        DictionaryFactory.registerImplementation(EDict.implementationEUC);
        DictionaryFactory.registerImplementation(EDict.implementationUTF8);
        DictionaryFactory.registerImplementation(WadokuJT.implementation);
        DictionaryFactory.registerImplementation(KanjiDic.IMPLEMENTATION);
    }

    protected void initUI() throws Exception {
        UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
            
        // automatically set the fonts the first time JGloss is run
        if (!JGloss.prefs.getBoolean( Preferences.FONT_AUTODETECTED, false)) {
            StyleDialog.autodetectFonts();
            JGloss.prefs.set( Preferences.FONT_AUTODETECTED, true);
        }
        
        // make sure the UI font is initialized before any UI elements are created
        StyleDialog.applyUIFont();
    }

    protected void handleCommandLine( String[] args) throws Exception {
        // parse command line options
        if (args.length > 0) {
            if (args[0].equals( "-h") || args[0].equals( "--help") ||
                args[0].equals( "/?")) {
                System.err.println( messages.getString( "main.usage", 
                                                        new String[] { getApplicationName() }));
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
                            System.err.println( messages.getString
                                                ( "main.createindex",
                                                  new String[] { d.getName() }));
                            ((IndexedDictionary) d).buildIndex();
                        }
                        else {
                            System.err.println( messages.getString
                                                ( "main.createindex.noindex", 
                                                  new String[] { d.getName() }));
                        }
                        d.dispose();
                    } catch (DictionaryFactory.NotSupportedException ex) {
                        System.err.println( messages.getString
                                            ( "main.format.unrecognized",
                                              new String[] { args[i] }));
                    } catch (Exception ex) {
                        if (ex instanceof DictionaryFactory.InstantiationException)
                            ex = (Exception) ex.getCause();
                        System.err.println( messages.getString
                                            ( "main.createindex.exception",
                                              new String[] { args[i],
                                                             ex.getClass().getName(),
                                                             ex.getLocalizedMessage() }));
                    }
                }
                System.exit( 0);
            }
            else if (args[0].equals( "-f") || args[0].equals( "--format")) {
                for ( int i=1; i<args.length; i++) {
                    try {
                        DictionaryFactory.Implementation<?> imp =
                            DictionaryFactory.getImplementation( args[i]);
                        System.out.println( messages.getString
                                            ( "main.format",
                                              new String[] { args[i], imp.getName() }));
                    } catch (DictionaryFactory.NotSupportedException ex) {
                        System.out.println( messages.getString
                                            ( "error.dictionary.reason",
                                              new String[] { args[i], ex.getMessage() }));
                    }
                }
                System.exit( 0);
            }
            else if (args[0].startsWith( "-") || 
                     File.separatorChar != '/' && args[0].startsWith( "/")) {
                System.err.println( messages.getString( "main.unknownoption",
                                                        new String[] { args[0] }));
                System.err.println( messages.getString( "main.usage"));
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
        System.err.println( message);
        t.printStackTrace();

        // Display error message in a frame. Note that this needs at least Java 1.1
        final Frame f = new Frame( messages.getString( "error.initialization.title"));
        f.setLayout( new BorderLayout());
        
        String msg = messages.getString( "error.initialization", new String[] {
            fatal ? messages.getString( "error.initialization.fatal") : "",
            message, t.getClass().getName(), t.getLocalizedMessage() });
        
        int rows = 1;
        int maxcols = 0;
        int cols = 0;
        for ( int i=0; i<msg.length(); i++) {
            if (msg.charAt( i) == '\n') {
                rows++;
                if (maxcols < cols)
                    maxcols = cols;
                cols = 0;
            }
            else
                cols++;
        }
        if (maxcols < cols)
            maxcols = cols;
        
        TextArea a = new TextArea
            ( msg, rows, maxcols,
              TextArea.SCROLLBARS_NONE);
        a.setFont( new Font( "Dialog", Font.PLAIN, 12));
        a.setEditable( false);
        f.add( a, BorderLayout.CENTER);
        
        Button ok = new Button( messages.getString( "button.ok"));
        ok.addActionListener( new ActionListener() {
                @Override
				public void actionPerformed( ActionEvent e) {
                    synchronized (f) {
                        f.setVisible(false);
                        f.dispose();
                        f.notify();
                    }
                }
            });
        f.add( ok, BorderLayout.SOUTH);
        f.pack();
        
        Dimension d = f.getToolkit().getScreenSize();
        f.setLocation( (d.width-f.getWidth())/2, (d.height-f.getHeight())/2);
        f.setSize( f.getPreferredSize());
        
        f.setVisible(true);
        synchronized (f) {
            try {
                f.wait();
            } catch (InterruptedException ex) {}
        }
    }

    protected LookupModel createLookupModel() {
        mainLookupModel = new LookupModel
            ( Arrays.asList
              ( new SearchMode[] { ExpressionSearchModes.EXACT,
                               ExpressionSearchModes.ANY,
                               ExpressionSearchModes.PREFIX,
                               ExpressionSearchModes.SUFFIX,
                               DistanceSearchModes.NEAR,
                               DistanceSearchModes.RADIUS }),
              Arrays.asList( Dictionaries.getDictionaries( false)),
              Arrays.asList( new LookupResultFilter[] 
                  { 
                      new AttributeResultFilter( messages.getString( "filter.mainentry.name"),
                                                 messages.getString( "filter.mainentry.desc"),
                                                 WadokuJT.MAIN_ENTRY, true), 
                      new AttributeResultFilter( messages.getString( "filter.example.name"),
                                                 messages.getString( "filter.example.desc"),
                                                 Attributes.EXAMPLE, true),
                      new AttributeResultFilter( messages.getString( "filter.priority.name"),
                                                 messages.getString( "filter.priority.desc"),
                                                 Attributes.PRIORITY, true)
                  }));
        Dictionaries.addDictionaryListChangeListener
            ( new Dictionaries.DictionaryListChangeListener() {
                    @Override
					public void dictionaryListChanged() {
                        mainLookupModel.setDictionaries
                            ( Arrays.asList( Dictionaries.getDictionaries( false)));
                    }
                });
        mainLookupModel.loadFromPreferences( prefs, "wordlookup");

        return mainLookupModel;
    }

    protected void shutdownHook() {
        Dictionary[] dicts = Dictionaries.getDictionaries( true);
        for ( int i=0; i<dicts.length; i++)
            dicts[i].dispose();
        if (mainLookupModel != null) {
            mainLookupModel.saveToPreferences( prefs, "wordlookup");
        }
    }

    /**
     * Initialize dialogs before the main window is shown.
     */
    protected void createDialogs() {
        AboutFrame.createShowAction( getApplicationName());
    }

    /**
     * Prepare dialogs in a background thread. This method is called from a low-priority thread
     * after the main window is shown. It should be used to create all dialogs which are not
     * needed immediately. Creating the dialogs here decreases the response time when the user
     * selects a dialog for the first time.
     */
    protected void backgroundCreateDialogs() {
        PreferencesFrame.createFrame
            ( getPreferencesPanels());
        AboutFrame.createFrame( getApplicationName());
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
                prop.store( ResourceBundle.getBundle( MESSAGES)
                            .getString( "preferences.header.obsolete"));
            } catch (IOException ex) {}
            prefs.set( Preferences.PREFERENCES_MIGRATED, true);
        }

        // migrate export.encoding, which was used in JGloss 1.0.3 and earlier
        String encoding = prefs.getString( "export.encoding");
        if (encoding == null) // encoding wasn't set, use old default value
            encoding = "SHIFT_JIS";
        if (prefs.getString( Preferences.EXPORT_PLAINTEXT_ENCODING).length() == 0)
            prefs.set( Preferences.EXPORT_PLAINTEXT_ENCODING, encoding);
        if (prefs.getString( Preferences.EXPORT_HTML_ENCODING).length() == 0)
            prefs.set( Preferences.EXPORT_HTML_ENCODING, encoding);
        if (prefs.getString( Preferences.EXPORT_ANNOTATIONLIST_ENCODING).length() == 0)
            prefs.set( Preferences.EXPORT_ANNOTATIONLIST_ENCODING, encoding);
        if (prefs.getString( Preferences.EXPORT_EXCLUSIONS_ENCODING).length() == 0)
            prefs.set( Preferences.EXPORT_EXCLUSIONS_ENCODING, encoding);

        return prefs;
    }
} // class JGloss
