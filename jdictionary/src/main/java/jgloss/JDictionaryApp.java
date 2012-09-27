/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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
import java.lang.reflect.InvocationTargetException;

import jgloss.ui.Dictionaries;
import jgloss.ui.LookupFrame;
import jgloss.ui.PreferencesPanel;
import jgloss.ui.StyleDialog;

/**
 * Base class for launching the JDictionary application, which only includes the word lookup part.
 *
 * @author Michael Koch
 */
public class JDictionaryApp extends JGloss {
    public static void main( final String[] args) throws InterruptedException, InvocationTargetException {
    	EventQueue.invokeAndWait(new Runnable() {
	        @Override
            public void run() {
	        	new JDictionaryApp().init( args);
	        }
        });
    }

    @Override
	protected String getApplicationName() { return "jdictionary"; }

    @Override
	protected void showMainWindow( String[] args) throws Exception {
        new LookupFrame( createLookupModel()).setVisible(true);
    }

    @Override
	protected PreferencesPanel[] getPreferencesPanels() {
        return new PreferencesPanel[] { StyleDialog.getStyleDialog(),
                                        Dictionaries.getInstance() };
    }

    @Override
	protected boolean doExit() {
        return true;
    }
} // class JDictionary
