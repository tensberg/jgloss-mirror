/*
 * Copyright (C) 2001,2002 Michael Koch (tensberg@gmx.net)
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

import jgloss.parser.Chasen;
import jgloss.parser.ChasenParser;
import jgloss.parser.KanjiParser;
import jgloss.ui.PreferencesPanel;
import jgloss.ui.GeneralDialog;
import jgloss.ui.DocumentStyleDialog;
import jgloss.ui.ParserSelector;
import jgloss.ui.LookupFrame;
import jgloss.ui.Dictionaries;
import jgloss.ui.ExclusionList;
import jgloss.ui.JGlossFrame;
import jgloss.ui.JGlossLookupFrame;
import jgloss.ui.export.ExportMenu;

import java.io.File;

/**
 * Base class for launching the full application, including the word lookup and document parser
 * part.
 *
 * @author Michael Koch
 */
public class JGlossApp extends JGloss {
    public static final int CURRENT_VERSION = 1100;

    private static LookupFrame lookupFrame;

    /**
     * Starts JGloss.
     *
     * @param args Arguments to the application.
     */
    public static void main( String args[]) {
        new JGlossApp().init( args);
    }

    public static synchronized LookupFrame getLookupFrame() {
        if (lookupFrame == null)
            lookupFrame = new JGlossLookupFrame( application.createLookupModel());

        return lookupFrame;
    }

    protected String getApplicationName() { return "jgloss"; }

    protected void registerDictionaries() {
        super.registerDictionaries();

        // register text parsers
        ParserSelector.registerParser( KanjiParser.class, new KanjiParser( null, null, true).getName());
        ParserSelector.registerParser( ChasenParser.class, 
                                       new ChasenParser( null, false).getName());

        // set default location of the chasen executable if this is the first start of JGloss
        String chasen = prefs.getString( Preferences.CHASEN_LOCATION);
        if (chasen==null || chasen.length()==0)
            prefs.set( Preferences.CHASEN_LOCATION, messages.getString
                       ( File.separatorChar=='\\' ? 
                         "chasen.location.windows" :
                         "chasen.location.unix"));
        Chasen.setDefaultExecutable( JGloss.prefs.getString( Preferences.CHASEN_LOCATION));
    }

    protected PreferencesPanel[] getPreferencesPanels() {
        return new PreferencesPanel[] { GeneralDialog.getInstance(),
                                        DocumentStyleDialog.getDocumentStyleDialog(),
                                        Dictionaries.getInstance(),
                                        ExclusionList.getInstance() };
    }

    protected void showMainWindow( String[] args) throws Exception {
        ExportMenu.registerStandardExporters();

        if (args.length == 0) {
            if (prefs.getBoolean( Preferences.STARTUP_WORDLOOKUP, false))
                getLookupFrame().show();
            else
                new JGlossFrame();
        }
        else {
            for ( int i=0; i<args.length; i++) {
                JGlossFrame f = new JGlossFrame();
                f.loadDocument( new File( args[i]));
            }
        }   
    }

    protected void backgroundCreateDialogs() {
        getLookupFrame();
        super.backgroundCreateDialogs();
    }

    /**
     * Exits JGloss. Before the application quits, the preferences will be saved.
     * This method will be called when the last JGloss frame is closed. The method may return
     * before the application quits to allow event processing to take place if it triggers an
     * error dialog.
     *
     * @return <CODE>false</CODE>, if the application will not quit.
     */
    protected boolean doExit() {
        if (JGlossFrame.getFrameCount()>0 || getLookupFrame().isVisible())
            return false;

        /*
        // debug memory:
        // clear all soft references to get a clean heap dump
        System.err.println( "clearing heap");
        java.util.List l = new java.util.LinkedList();
        try {
            while (true) {
                l.add( new byte[100000]);
            }
        } catch (OutOfMemoryError er) {
            l = null;
        }
        System.gc();

        // debug memory:
        // Flush events. Sometimes, not all pending events have been processed yet, and
        // these events keep references to UI objects. Remove these events to clear the memory.
        System.err.println( "flushing all events");
        while (Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent() != null) try {
            System.err.println( Toolkit.getDefaultToolkit().getSystemEventQueue().getNextEvent());
        } catch (InterruptedException ex) {}
        System.gc();
        */

        System.exit( 0);

        return true;
    }
} // class JGlossApp
