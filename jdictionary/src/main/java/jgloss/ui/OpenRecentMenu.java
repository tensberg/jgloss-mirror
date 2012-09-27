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
 * $Id$
 *
 */

package jgloss.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jgloss.JGloss;
import jgloss.Preferences;

/**
 * Manager for a menu with recently opened documents.
 *
 * @author Michael Koch
 */
public class OpenRecentMenu {
    /**
     * Notification of the selection of an item from the {@link OpenRecentMenu}.
     *
     * @author Michael Koch <tensberg@gmx.net>
     */
    public interface FileSelectedListener {
        /**
         * Invoked when the user selects a file from the open recent menu
         */
        void fileSelected( File file);
    } // class FileSelectedAction

    private static class MenuWithListener {
    	private final JMenu menu;
    	
    	private final FileSelectedListener listener;
    	
    	public MenuWithListener(JMenu menu, FileSelectedListener listener) {
	        this.menu = menu;
	        this.listener = listener;
        }

		public JMenu getMenu() {
	        return menu;
        }
    	
    	public FileSelectedListener getListener() {
	        return listener;
        }
    }
    
    /**
     * List of Files with documents shown in the menu.
     */
    private final List<File> documents;
    /**
     * Maximum number of entries in the document menu.
     */
    private final int size;
    /**
     * List of managed open recent menus with associated FileSelectedListener.
     */
    private final List<MenuWithListener> menus;

    /**
     * Create a new open recent menu.
     *
     * @param listener The listener is notified if the user selects an entry from the open recent menu.
     * @param size Maximum number of entries the menu will show.
     */
    public OpenRecentMenu( int size) {
        this.size = size;
        documents = new ArrayList<File>( size);
        String[] files = JGloss.PREFS.getList( Preferences.OPENRECENT_FILES, File.pathSeparatorChar);
        for ( int i=0; i<files.length&&documents.size()<size; i++) {
            final File doc = new File( files[i]);
            if (doc.canRead()) { // only add documents which exist
                documents.add( doc);
            }
        }
        menus = new ArrayList<MenuWithListener>( 10);
    }

    /**
     * Creates a new open recent menu which takes the specified action if an entry is selected.
     * Changes to the list of documents done in {@link #addDocument(File) addDocument} will be
     * reflected in the menu.
     *
     * @param listener Will be notified when an entry from the menu is selected.
     * @return A newly created popup menu with the recent documents.
     */
    public JMenu createMenu( FileSelectedListener listener) { 
        JMenu menu = new JMenu( JGloss.MESSAGES.getString( "main.menu.openrecent"));
        for (File document : documents) {
            menu.add( createDocumentMenu(document, listener));
        }
        if (menu.getItemCount() == 0) {
	        menu.setEnabled( false);
        }
        menus.add(new MenuWithListener(menu, listener));
        return menu; 
    }

    /**
     * Removes a menu generated by {@link #createMenu(OpenRecentMenu.FileSelectedListener)
     * createMenu} from the list of managed menus.
     *
     * @param menu The menu to removed.
     */
    public void removeMenu( JMenu menu) {
    	for (Iterator<MenuWithListener> menuWithListenerIterator=menus.iterator(); menuWithListenerIterator.hasNext(); ) {
    		MenuWithListener menuWithListener = menuWithListenerIterator.next();
    		if (menu.equals(menuWithListener.getMenu())) {
    			menuWithListenerIterator.remove();
    			break;
    		}
    	}
    }

    /**
     * Adds a document to all open recent menus managed by this instance.
     * It will be inserted at first position.
     * If the document is already in the list, the other occurrence will be removed.
     *
     * @param doc Document to add.
     */
    public void addDocument( File doc) {
        if (!doc.canRead()) {
	        return;
        }
        
        int index = documents.indexOf( doc);
        if (index == 0) {
	        // already at first position
            return;
        }
        if (index != -1) {
            // move entry to first position
            documents.remove( index);
            documents.add( 0, doc);
            for (MenuWithListener menuWithListener : menus) {
                JMenu menu = menuWithListener.getMenu();
				JMenuItem item = menu.getItem( index);
                menu.remove( index);
                menu.insert( item, 0);
            }
        }
        else {
            // create new entry
            if (documents.size() == size) {
                // remove oldest entry
                documents.remove( size-1);
                for (MenuWithListener menuWithListener : menus) {
                    menuWithListener.getMenu().remove( size-1);
                }
            }
            documents.add( 0, doc);
            for (MenuWithListener menuWithListener : menus) {
                JMenu menu = menuWithListener.getMenu();
                menu.add( createDocumentMenu( doc, menuWithListener.getListener()), 0);
                menu.setEnabled( true); // was disabled if this is first entry
            }
        }
        // save current entries in preferences
        StringBuilder docs = new StringBuilder();
        boolean firstDoc = true;
        for (File document : documents) {
        	if (firstDoc) {
        		firstDoc = false;
        	} else {
        		docs.append( File.pathSeparatorChar);
        	}
            docs.append( document.getAbsolutePath());
        }
        JGloss.PREFS.set( Preferences.OPENRECENT_FILES, docs.toString());
    }
    
    private JMenuItem createDocumentMenu( final File doc, final FileSelectedListener listener) {
        return new JMenuItem( new AbstractAction( doc.getName()) {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( java.awt.event.ActionEvent e) {
                    listener.fileSelected( doc);
                }
            });
    }
} // class OpenRecentMenu
