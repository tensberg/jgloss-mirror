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

import jgloss.dictionary.*;
import jgloss.ui.*;

/**
 * Base class for launching the JDictionary application, which only includes the word lookup part.
 *
 * @author Michael Koch
 */
public class JDictionaryApp extends JGloss {
    public static void main( String args[]) {
        application = new JDictionaryApp();

        try {
            registerDictionaries();

            handleCommandLine( args, "jdictionary");

            initUI();

            SplashScreen splash = new SplashScreen( "jdictionary");
            
            splash.setInfo( messages.getString( "splashscreen.initMain"));
            
            registerShutdownHook();

            createLookupFrame().show();

            // Initialize the preferences at startup. This includes loading the dictionaries.
            // Do this in its own thread to decrease perceived initialization time.
            new Thread() {
                    public void run() {
                        try {
                            setPriority( Thread.MIN_PRIORITY);
                        } catch (IllegalArgumentException ex) {}

                        PreferencesFrame.createFrame
                            ( new PreferencesPanel[] { StyleDialog.getStyleDialog(),
                                                       Dictionaries.getInstance() });
                    }
                }.start();

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

    protected boolean doExit() {
        System.exit( 0);

        return true;
    }
} // class JDictionary
