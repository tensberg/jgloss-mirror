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

package jgloss.ui;

import jgloss.*;

import java.util.*;
import java.io.*;

import javax.swing.*;

/**
 * Manager for a menu with recently opened documents.
 *
 * @author Michael Koch
 */
public class OpenRecentMenu {
    public static interface FileSelectedListener {
        /**
         * Invoked when the user selects a file from the open recent menu
         */
        void fileSelected( File file);
    } // class FileSelectedAction

    /**
     * List of Files with documents shown in the menu.
     */
    private List documents;
    /**
     * Maximum number of entries in the document menu.
     */
    private int size;
    /**
     * List of managed open recent menus with associated FileSelectedListener.
     */
    private List menus;

    /**
     * Create a new open recent menu.
     *
     * @param listener The listener is notified if the user selects an entry from the open recent menu.
     * @param size Maximum number of entries the menu will show.
     */
    public OpenRecentMenu( int size) {
        this.size = size;
        documents = new ArrayList( size);
        String[] files = JGloss.prefs.getList( Preferences.OPENRECENT_FILES, File.pathSeparatorChar);
        for ( int i=0; i<files.length&&documents.size()<size; i++) {
            final File doc = new File( files[i]);
            if (doc.canRead()) { // only add documents which exist
                documents.add( doc);
            }
        }
        menus = new ArrayList( 10);
    }

    /**
     * Creates a new open recent menu which takes the specified action if an entry is selected.
     * Changes to the list of documents done in {@link #addDocument(File) addDocument} will be
     * reflected in the menu.
     *
     * @param listener Will be notified when an entry from the menu is selected.
     * @return A newly created popup menu with the recent documents.
     */
    public synchronized JMenu createMenu( FileSelectedListener listener) { 
        JMenu menu = new JMenu( JGloss.messages.getString( "main.menu.openrecent"));
        for ( Iterator i=documents.iterator(); i.hasNext(); ) {
            menu.add( createDocumentMenu( (File) i.next(), listener));
        }
        if (menu.getItemCount() == 0)
            menu.setEnabled( false);
        menus.add( menu);
        menus.add( listener);
        return menu; 
    }

    /**
     * Removes a menu generated by {@link #createMenu(OpenRecentMenu.FileSelectedListener)
     * createMenu} from the list of managed menus.
     *
     * @param menu The menu to removed.
     */
    public synchronized void removeMenu( JMenu menu) {
        int i = menus.indexOf( menu);
        if (i != -1) {
            menus.remove( i); // JMenu
            menus.remove( i); // FileSelectedListener
        }
    }

    /**
     * Adds a document to all open recent menus managed by this instance.
     * It will be inserted at first position.
     * If the document is already in the list, the other occurrence will be removed.
     *
     * @param doc Document to add.
     */
    public synchronized void addDocument( File doc) {
        if (!doc.canRead()) // skip invalid entries
            return;
        
        int index = documents.indexOf( doc);
        if (index == 0)
            // already at first position
            return;
        if (index != -1) {
            // move entry to first position
            documents.remove( index);
            documents.add( 0, doc);
            for ( Iterator i=menus.iterator(); i.hasNext(); ) {
                JMenu menu = (JMenu) i.next();
                JMenuItem item = menu.getItem( index);
                menu.remove( index);
                menu.insert( item, 0);
                i.next(); // skip FileSelectedListener
            }
        }
        else {
            // create new entry
            if (documents.size() == size) {
                // remove oldest entry
                documents.remove( size-1);
                for ( Iterator i=menus.iterator(); i.hasNext(); ) {
                    ((JMenu) i.next()).remove( size-1);
                    i.next(); // skip FileSelectedListener
                }
            }
            documents.add( 0, doc);
            for ( Iterator i=menus.iterator(); i.hasNext(); ) {
                JMenu menu = (JMenu) i.next();
                menu.add( createDocumentMenu( doc, (FileSelectedListener) i.next()), 0);
                menu.setEnabled( true); // was disabled if this is first entry
            }
        }
        // save current entries in preferences
        StringBuffer docs = new StringBuffer();
        for ( Iterator i=documents.iterator(); i.hasNext(); ) {
            docs.append( ((File) i.next()).getAbsolutePath());
            if (i.hasNext())
                docs.append( File.pathSeparatorChar);
        }
        JGloss.prefs.set( Preferences.OPENRECENT_FILES, docs.toString());
    }
    
    private JMenuItem createDocumentMenu( final File doc, final FileSelectedListener listener) {
        return new JMenuItem( new AbstractAction( doc.getName()) {
                public void actionPerformed( java.awt.event.ActionEvent e) {
                    listener.fileSelected( doc);
                }
            });
    }
} // class OpenRecentMenu
