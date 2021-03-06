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

import java.awt.EventQueue;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import jgloss.parser.Chasen;
import jgloss.parser.ChasenParser;
import jgloss.parser.KanjiParser;
import jgloss.parser.NullParser;
import jgloss.ui.Dictionaries;
import jgloss.ui.ExclusionList;
import jgloss.ui.GeneralDialog;
import jgloss.ui.LookupFrame;
import jgloss.ui.ParserSelector;
import jgloss.ui.PreferencesPanel;
import jgloss.ui.export.ExportMenu;
import jgloss.ui.gloss.DocumentStyleDialog;
import jgloss.ui.gloss.JGlossFrame;
import jgloss.ui.gloss.JGlossLookupFrame;
import jgloss.ui.gloss.OpenDocumentWorker;
import jgloss.ui.html.JGlossEditorKit;
import jgloss.ui.welcome.WelcomeDialog;

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
     * @throws InvocationTargetException
     * @throws InterruptedException
     */
    public static void main( final String args[]) throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                new JGlossApp().init( args);
            }
        });
    }

    public static LookupFrame getLookupFrame() {
        assert EventQueue.isDispatchThread();

        if (lookupFrame == null) {
	        lookupFrame = new JGlossLookupFrame( getApplication().createLookupModel());
        }

        return lookupFrame;
    }

    @Override
	protected String getApplicationName() { return "jgloss"; }

    @Override
	protected void registerDictionaries() {
        super.registerDictionaries();

        // register text parsers
        ParserSelector.registerParser( KanjiParser.class, new KanjiParser( null, null, true).getName());
        ParserSelector.registerParser( ChasenParser.class,
                                       new ChasenParser( null, false).getName());
        ParserSelector.registerParser( NullParser.class, new NullParser().getName());

        // set default location of the chasen executable if this is the first start of JGloss
        String chasen = PREFS.getString( Preferences.CHASEN_LOCATION);
        if (chasen==null || chasen.length()==0) {
            String defaultChasenLocationKey;
            String osName = System.getProperty("os.name");
            if (osName.startsWith("Windows")) {
                String osArch = System.getProperty("os.arch");
                if ("x86".equals(osArch)) {
                    defaultChasenLocationKey = "windows";
                } else {
                    defaultChasenLocationKey = "windows64";
                }
            } else {
                defaultChasenLocationKey = "unix";
            }
            PREFS.set(Preferences.CHASEN_LOCATION, MESSAGES.getString("chasen.location." + defaultChasenLocationKey));
        }
        Chasen.setDefaultExecutable( JGloss.PREFS.getString( Preferences.CHASEN_LOCATION));
    }

    @Override
	protected PreferencesPanel[] getPreferencesPanels() {
        return new PreferencesPanel[] { GeneralDialog.getInstance(),
                                        DocumentStyleDialog.getDocumentStyleDialog(),
                                        Dictionaries.getInstance(),
                                        ExclusionList.getInstance() };
    }

    @Override
	protected void showMainWindow( String[] args) throws Exception {
        ExportMenu.registerStandardExporters();

        // The DTD must be initialized before the first HTML document is shown.
        // (HTML text fields of the Welcome Page also count).
        // Otherwise a HTML DTD-internal cache will not be initialized to the
        // correct size, causing errors when adding annotations later.
        JGlossEditorKit.getDTD();

        if (args.length == 0) {
            JGlossFrame targetDocument;
            Window parentFrame;
            if (PREFS.getBoolean( Preferences.STARTUP_WORDLOOKUP, false)) {
                targetDocument = null;
                parentFrame = getLookupFrame();
                parentFrame.setVisible(true);
            } else {
                targetDocument = new JGlossFrame();
                parentFrame = targetDocument.getFrame();
            }

            if (PREFS.getBoolean(Preferences.SHOW_WELCOME_DIALOG, true)) {
                WelcomeDialog.showWelcomeDialog(targetDocument, parentFrame);
            }
        }
        else {
            for (String arg : args) {
                OpenDocumentWorker.openDocument(new JGlossFrame(), new File(arg));
            }
        }
    }

    /**
     * Exits JGloss. Before the application quits, the preferences will be saved.
     * This method will be called when the last JGloss frame is closed. The method may return
     * before the application quits to allow event processing to take place if it triggers an
     * error dialog.
     *
     * @return <CODE>false</CODE>, if the application will not quit.
     */
    @Override
	protected boolean doExit() {
        if (JGlossFrame.getFrameCount()>0 || getLookupFrame().isVisible()) {
	        return false;
        }

        /*
        // debug memory:
        // clear all soft references to get a clean heap dump
        LOGGER.severe( "clearing heap");
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
        LOGGER.severe( "flushing all events");
        while (Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent() != null) try {
            LOGGER.severe( Toolkit.getDefaultToolkit().getSystemEventQueue().getNextEvent());
        } catch (InterruptedException ex) {}
        System.gc();
        */

        return true;
    }
} // class JGlossApp
