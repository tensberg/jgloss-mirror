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
import jgloss.dictionary.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Panel which allows the user to add and manipulate dictionaries used for document parsing.
 * The object can be queried for a list of currently selected dictionaries. There exists
 * a single application-wide instance which can be accessed through the
 * {@link #getComponent() getComponent()} method.
 *
 * @author Michael Koch
 */
public class Dictionaries extends Box {
    /**
     * The single application-wide instance.
     */
    private static Dictionaries box;

    /**
     * The widget which displays the current selection of dictionaries.
     */
    private JList dictionaries;
    /**
     * The selection of dictionaries currently stored in the preferences. This can be different
     * from the list displayed to the user if he has not yet applied changes.
     */
    private DefaultListModel dictionariesOrig;

    /**
     * Returns the single application-wide instance.
     *
     * @return The Dictionaries component.
     */
    public static Dictionaries getComponent() {
        if (box == null)
            box = new Dictionaries();
        return box;
    }

    /**
     * Returns the array of currently active dictionaries in the order in which they are
     * displayed. This can be different from the currently shown list if the user has not
     * yet applied changes.
     *
     * @return Array of currently active dictionaries.
     */
    public static Dictionary[] getDictionaries() {
        ListModel m = getComponent().dictionariesOrig;
        Dictionary[] out = new Dictionary[m.getSize()];
        for ( int i=0; i<m.getSize(); i++) {
            out[i] = (Dictionary) m.getElementAt( i);
        }

        return out;
    }

    /**
     * Creates a new instance of dictionaries.
     */
    private Dictionaries() {
        super( BoxLayout.X_AXIS);
        // construct the dictionaries list editor
        dictionaries = new JList();
        dictionaries.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);

        final Action up = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    int i = dictionaries.getSelectedIndex();
                    DefaultListModel m = (DefaultListModel) dictionaries.getModel();
                    Object o = m.remove( i);
                    m.insertElementAt( o, i-1);
                    dictionaries.setSelectedIndex( i-1);
                }
            };
        up.setEnabled( false);
        JGlossFrame.initAction( up, "prefs.button.up");
        final Action down = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    int i = dictionaries.getSelectedIndex();
                    DefaultListModel m = (DefaultListModel) dictionaries.getModel();
                    Object o = m.remove( i);
                    m.insertElementAt( o, i+1);
                    dictionaries.setSelectedIndex( i+1);
                }
            };
        down.setEnabled( false);
        JGlossFrame.initAction( down, "prefs.button.down");
        final Action add = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    addDictionary();
                }
            };
        add.setEnabled( true);
        JGlossFrame.initAction( add, "prefs.button.add");
        final Action remove = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    int i = dictionaries.getSelectedIndex();
                    DefaultListModel m = (DefaultListModel) dictionaries.getModel();
                    m.remove( i);
                    if (i < m.getSize())
                        dictionaries.setSelectedIndex( i);
                    else if (m.getSize() > 0)
                        dictionaries.setSelectedIndex( m.getSize()-1);
                    else
                        this.setEnabled( false);
                }
            };
        remove.setEnabled( false);
        JGlossFrame.initAction( remove, "prefs.button.remove");
        JPanel p = new JPanel( new GridLayout( 0, 1));
        p.add( new JButton( add));
        p.add( new JButton( remove));
        p.add( new JButton( up));
        p.add( new JButton( down));
        p.add( Box.createVerticalGlue());

        dictionaries.addListSelectionListener( new ListSelectionListener() {
                public void valueChanged( ListSelectionEvent e) {
                    if (dictionaries.isSelectionEmpty()) {
                        up.setEnabled( false);
                        down.setEnabled( false);
                        remove.setEnabled( false);
                    }
                    else {
                        if (dictionaries.getSelectedIndex() > 0)
                            up.setEnabled( true);
                        else
                            up.setEnabled( false);
                        if (dictionaries.getSelectedIndex() <
                            dictionaries.getModel().getSize()-1)
                            down.setEnabled( true);
                        else
                            down.setEnabled( false);
                        remove.setEnabled( true);
                    }
                }
            });

        JScrollPane scroller = new JScrollPane( dictionaries);
        add( scroller);
        add( Box.createHorizontalStrut( 5));
        add( p);
    }

    /**
     * Runs the dialog to add a new dictionary to the list.
     */
    private void addDictionary() {
        String dir = JGloss.prefs.getString( Preferences.DICTIONARIES_DIR);
        if (dir==null || dir.trim().length()==0) {
            dir = System.getProperty( "user.home");
        }

        JFileChooser chooser = new JFileChooser( dir);
        chooser.setFileHidingEnabled( true);
        chooser.setMultiSelectionEnabled( true);
        chooser.setDialogTitle( JGloss.messages.getString( "dictionaries.chooser.title"));
        int result = chooser.showDialog( SwingUtilities.getRoot( box), JGloss.messages.getString
                                         ( "dictionaries.chooser.button.add"));
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] fs = chooser.getSelectedFiles();
            for ( int i=0; i<fs.length; i++) {
                Dictionary d = loadDictionary( fs[i].getAbsolutePath());
                if (d != null)
                    ((DefaultListModel) dictionaries.getModel()).addElement( d);
            }
            JGloss.prefs.set( Preferences.DICTIONARIES_DIR, chooser.getCurrentDirectory()
                              .getAbsolutePath());
        }
    }

    /**
     * Creates a new dictionary by loading it from a file. Currently, only
     * <CODE>EDict</CODE> dictionaries are supported. If loading the dictionary fails,
     * an error dialog will be displayed.
     *
     * @param file The path to the dictionary file. The path to the index will be
     *             derived by adding the ending ".xjdx".
     * @return The newly created dictionary.
     * @see jgloss.dictionary.EDict
     */
    private Dictionary loadDictionary( String file) {
        try {
            return new EDict( file, file + ".xjdx");
        } catch (IOException ex) {
            String msgid;
            if (ex instanceof FileNotFoundException)
                msgid = "error.dictionary.filenotfound";
            else
                msgid = "error.dictionary.ioerror";
            
            JOptionPane.showConfirmDialog
                ( SwingUtilities.getRoot( box), JGloss.messages.getString
                  ( msgid, new Object[] { new File( file).getName() }),
                  JGloss.messages.getString( "error.dictionary.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * Makes the list of currently displayed dictionaries the active dictionaries. The list
     * will be stored in the preferences.
     */
    public void savePreferences() {
        Preferences p = JGloss.prefs;
        synchronized (p) {
            ListModel m = dictionaries.getModel();
            String paths = null;

            // construct a string which consists of the paths to the dictionary files
            if (m.getSize() > 0) {
                paths = ((EDict) m.getElementAt( 0)).getDictionaryFile();
                for ( int i=1; i<m.getSize(); i++) {
                    paths += File.pathSeparatorChar +
                        ((EDict) m.getElementAt( i)).getDictionaryFile();
                }
            }
            p.set( Preferences.DICTIONARIES, paths);

            // keep the original list of dictionaries in case the user cancels the dialog
            dictionariesOrig = new DefaultListModel();
            for ( int i=0; i<m.getSize(); i++)
                dictionariesOrig.addElement( m.getElementAt( i));
        }
    }

    /**
     * Initialized the list of displayed dictionaries from the preferences setting.
     */
    public void loadPreferences() {
        Preferences p = JGloss.prefs;
        synchronized (p) {
            // since creating the dictionaries from a file takes a long time, they are
            // stored in a backup list for fast access if the user cancels the dialog.
            if (dictionariesOrig == null) {
                dictionariesOrig = new DefaultListModel();
                String[] fs = p.getPaths( Preferences.DICTIONARIES);
                for ( int i=0; i<fs.length; i++) {
                    Dictionary d = loadDictionary( fs[i]);
                    if (d != null)
                        dictionariesOrig.addElement( d);
                }
            }
            
            DefaultListModel m = new DefaultListModel();
            for ( int i=0; i<dictionariesOrig.getSize(); i++)
                m.addElement( dictionariesOrig.getElementAt( i));
            dictionaries.setModel( m);
        }
    }

    /**
     * Apply the new preference setting to already opened document. Does nothing since
     * no changed need to be applied.
     */
    public void applyPreferences() {}
} // class Dictionaries
