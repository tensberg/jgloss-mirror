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

import jgloss.dictionary.*;
import jgloss.ui.*;

import java.io.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;
import java.awt.*;
import java.awt.event.*;

import javax.swing.UIManager;

/**
 * Entry point for the JGloss application. Does the initialisation and manages access to
 * localisation resources and preferences.
 *
 * @author Michael Koch
 */
public class JGloss {
    /**
     * Path to the file with message strings.
     */
    private static final String MESSAGES = "resources/messages";

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
            messages = ResourceBundle.getBundle( MESSAGES);
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
    public static final Preferences prefs = new Preferences();
    /**
     * The application-wide messages. Use this to retrieve localizable string messages.
     */
    public static final Messages messages = new Messages( MESSAGES);

    /**
     * Starts JGloss.
     *
     * @param args Arguments to the application. Not used.
     */
    public static void main( String args[]) {
        try {
            if (args.length > 0) {
                if (args[0].equals( "-h") || args[0].equals( "--help") ||
                    args[0].equals( "/?")) {
                    System.err.println( messages.getString( "main.usage"));
                    System.exit( 0);
                }
                else if (args[0].equals( "-i") || args[0].equals( "--createindex")) {
                    for ( int i=1; i<args.length; i++) {
                        EDict e = new EDict( args[i], false);
                        e.buildIndex();
                        try {
                            // write index to local directory
                            e.saveJJDX( new File( e.getName() + EDict.JJDX_EXTENSION));
                        } catch (IOException ex) {
                            System.err.println( messages.getString
                                                ( "edict.error.writejjdx",
                                                  new String[] { ex.getClass().getName(),
                                                                 ex.getLocalizedMessage() }));
                            System.exit( 1);
                        }
                    }
                    System.exit( 0);
                }
                else if (args[0].startsWith( "-") || args[0].startsWith( "/")) {
                    System.err.println( messages.getString( "main.unknownoption",
                                                            new String[] { args[0] }));
                    System.err.println( messages.getString( "main.usage"));
                    System.exit( 1);
                }
            }

            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
            
            SplashScreen splash = new SplashScreen();
            splash.setInfo( messages.getString( "splashscreen.loadingPreferences"));
            try {
                prefs.load();
            } catch (IOException ex) {
                displayError( messages.getString( "error.loadPreferences"), ex, false);
            }
            
            splash.setInfo( messages.getString( "splashscreen.initPreferences"));
            // Initialize the preferences at startup. This includes loading the dictionaries.
            PreferencesFrame.getFrame();
            splash.setInfo( messages.getString( "splashscreen.initMain"));
            if (args.length == 0)
                new JGlossFrame();
            else
                for ( int i=0; i<args.length; i++) {
                    JGlossFrame f = new JGlossFrame();
                    f.loadDocument( args[i]);
                }
            splash.close();
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

    /**
     * Exits JGloss. Before the application quits, the preferences will be saved.
     * This method will be called when the last JGloss frame is closed. The method may return
     * before the application quits to allow event processing to take place if it triggers an
     * error dialog.
     */
    public static void exit() {
        // Instantiate a new Thread because the exit() method may have been called from
        // an event dispatch thread (for example a window close event). The displayError method which might
        // be called needs the event dispatch thread to work, so the exit() method has to return.
        new Thread() {
                public void run() {
                    try {
                        prefs.store();
                    } catch (IOException ex) {
                        displayError( messages.getString( "error.storePreferences"), ex, false);
                    }
                    System.exit( 0);
                }
            }.start();
    }

    /**
     * Shows an error message to the user.
     *
     * @param message A user-understandable error message.
     * @param t The exception which signalled the error.
     * @param fatal <code>true</code> if the application cannot be started because of the error.
     */
    private static void displayError( String message, Throwable t, boolean fatal) {
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
                public void actionPerformed( ActionEvent e) {
                    synchronized (f) {
                        f.hide();
                        f.dispose();
                        f.notify();
                    }
                }
            });
        f.add( ok, BorderLayout.SOUTH);
        f.pack();
        
        Dimension d = f.getToolkit().getScreenSize();
        f.setLocation( (d.width-f.getWidth())/2, (d.height-f.getHeight())/2);
        
        f.show();
        synchronized (f) {
            try {
                f.wait();
            } catch (InterruptedException ex) {}
        }
    }

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
} // class JGloss
