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
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Panel which allows the user to manage the list of excluded words used for document parsing.
 * There exists
 * a single application-wide instance which can be accessed through the
 * {@link #getComponent() getComponent()} method.
 *
 * @author Michael Koch
 */
public class ExclusionList extends JPanel {
    /**
     * The single application-wide instance.
     */
    private static ExclusionList box;

    /**
     * The set of excluded words. The contents of the set may not be the same as
     * displayed in the <CODE>exclusionList</CODE> if the user has not yet applied
     * the changes.
     */
    private Set exclusions;
    /**
     * The widget which displays the excluded words.
     */
    private JList exclusionList;
    /**
     * Flag if the content of the JList was changed.
     */
    private boolean changed;
    
    /**
     * Returns the single application-wide instance.
     *
     * @return The Dictionaries component.
     */
    public static ExclusionList getComponent() {
        if (box == null)
            box = new ExclusionList();
        return box;
    }

    /**
     * Returns the array of currently active dictionaries in the order in which they are
     * displayed. This can be different from the currently shown list if the user has not
     * yet applied changes.
     *
     * @return Array of currently active dictionaries.
     */
    public static Set getExclusions() {
        return getComponent().exclusions;
    }

    /**
     * Adds a single word to the exclusion list.
     */
    public static void addWord( String word) {
        getComponent().add( word);
    }

    /**
     * Creates a new instance of dictionaries.
     */
    private ExclusionList() {
        setLayout( new GridBagLayout());

        exclusions = new HashSet( 20);
        // construct the dictionaries list editor
        exclusionList = new JList();
        exclusionList.setModel( new DefaultListModel());
        exclusionList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);

        final Action add = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    inputExclusion();
                }
            };
        add.setEnabled( true);
        UIUtilities.initAction( add, "exclusions.button.add");
        final Action remove = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    int i = exclusionList.getSelectedIndex();
                    DefaultListModel m = (DefaultListModel) exclusionList.getModel();
                    m.remove( i);
                    changed = true;
                    if (i < m.getSize())
                        exclusionList.setSelectedIndex( i);
                    else if (m.getSize() > 0)
                        exclusionList.setSelectedIndex( m.getSize()-1);
                    else
                        this.setEnabled( false);
                }
            };
        remove.setEnabled( false);
        UIUtilities.initAction( remove, "exclusions.button.remove");
        final Action export = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    exportList();
                }
            };
        export.setEnabled( true);
        UIUtilities.initAction( export, "exclusions.button.export");
        final Action importA = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    importList();
                }
            };
        importA.setEnabled( true);
        UIUtilities.initAction( importA, "exclusions.button.import");

        exclusionList.addListSelectionListener( new ListSelectionListener() {
                public void valueChanged( ListSelectionEvent e) {
                    if (exclusionList.isSelectionEmpty()) {
                        remove.setEnabled( false);
                    }
                    else {
                        remove.setEnabled( true);
                    }
                }
            });

        JScrollPane scroller = new JScrollPane( exclusionList);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.BOTH;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 2;
        gc.weighty = 2;
        gc.gridwidth = 1;
        gc.gridheight = GridBagConstraints.REMAINDER;
        gc.insets = new Insets( 0, 0, 0, 5);
        add( scroller, gc);

        JPanel p = new JPanel( new GridLayout( 0, 1));
        p.add( new JButton( add));
        p.add( new JButton( remove));
        p.add( new JButton( export));
        p.add( new JButton( importA));
        gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTH;
        gc.gridx = 1;
        gc.gridy = GridBagConstraints.RELATIVE;
        gc.fill= GridBagConstraints.NONE;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        add( p, gc);

        String filename = getExclusionListFile();
        if (new File( filename).exists())
            loadExclusionList( filename);
        changed = false;
    }

    /**
     * Copies the content of the JList into the exclusion set.
     */
    public void savePreferences() {
        if (changed) {
            DefaultListModel m = (DefaultListModel) exclusionList.getModel();
            synchronized (exclusions) {
                exclusions.clear();
                for ( int i=0; i<m.size(); i++)
                    exclusions.add( m.get( i));
            }
            saveExclusionList( getExclusionListFile());
            changed = false;
        }
    }

    /**
     * Initializes the content of the JList with the words from the exclusion set.
     */
    public void loadPreferences() {
        DefaultListModel m = (DefaultListModel) exclusionList.getModel();
        m.removeAllElements();
        // Sort the exclusions by putting them in a tree set.
        for ( Iterator i=new TreeSet( exclusions).iterator(); i.hasNext(); ) {
            m.addElement( i.next());
        }
    }

    public void applyPreferences() {}

    /**
     * Displays a dialog where the user can input a new exclusion word.
     */
    private void inputExclusion() {
        String word = JOptionPane.showInputDialog
            ( SwingUtilities.getRoot( box), JGloss.messages.getString
              ( "exclusions.add"), JGloss.messages.getString
              ( "exclusions.add.title"), JOptionPane.PLAIN_MESSAGE);
        if (word!=null && word.length()>0) {
            DefaultListModel m = (DefaultListModel) exclusionList.getModel();
            if (!m.contains( word)) {
                m.addElement( word);
                changed = true;
            }
        }
    }

    /**
     * Returns the default exclusion list filename. The filename is read from the
     * preference EXCLUSIONS_FILE. If it is not an absolute path, the users home
     * directory is prepended.
     */
    private String getExclusionListFile() {
        String filename = JGloss.prefs.getString( Preferences.EXCLUSIONS_FILE);
        if (!new File( filename).isAbsolute()) {
            filename = System.getProperty( "user.home") + File.separator + filename;
        }
        return filename;
    }

    /**
     * Initializes the exclusion list from a file.
     *
     * @param filename Name of the file.
     */
    private void loadExclusionList( String filename) {
        try {
            BufferedReader r = new BufferedReader( CharacterEncodingDetector.getReader
                ( new FileInputStream( filename)));
            Set newExclusions = new HashSet( 1000);
            String line;
            while ((line=r.readLine()) != null) {
                if (line.length() > 0)
                    newExclusions.add( line);
            }
            exclusions = newExclusions;
            r.close();
        } catch (IOException ex) {
            JOptionPane.showConfirmDialog
                ( SwingUtilities.getRoot( box), JGloss.messages.getString
                  ( "error.exclusions.load", new String[] 
                      { filename, ex.getClass().getName(), ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.exclusions.load.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Saves the exclusion list to a file. The character encoding used is
     * UTF-8.
     *
     * @param filename Name of the file.
     */
    private void saveExclusionList( String filename) {
        try {
            BufferedWriter w = new BufferedWriter( new OutputStreamWriter
                ( new FileOutputStream( filename), "UTF-8"));
            for ( Iterator i=exclusions.iterator(); i.hasNext(); ) {
                String word = i.next().toString();
                w.write( word, 0, word.length());
                w.newLine();
            }
            w.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( SwingUtilities.getRoot( box), JGloss.messages.getString
                  ( "error.exclusions.save", new String[] 
                      { filename, ex.getClass().getName(), ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.exclusions.save.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Displays a file chooser and writes the contents of the JList to the selected file.
     */
    private void exportList() {
        JFileChooser f = new JFileChooser( JGloss.getCurrentDir());
        f.setDialogTitle( JGloss.messages.getString( "exclusions.export.title"));
        f.setFileHidingEnabled( true);
        f.setFileView( CustomFileView.getFileView());

        // setup the encoding chooser
        JPanel p = new JPanel();
        p.setLayout( new GridLayout( 1, 1));
        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalStrut( 3));
        b.add( new JLabel( JGloss.messages.getString( "export.encodings")));
        b.add( Box.createHorizontalStrut( 3));
        Vector v = new Vector( 5);
        JComboBox encodings = new JComboBox( JGloss.prefs.getList( Preferences.ENCODINGS, ','));
        encodings.setSelectedItem( JGloss.prefs.getString( Preferences.EXPORT_ENCODING));
        encodings.setEditable( true);
        b.add( encodings);
        b.add( Box.createHorizontalStrut( 3));
        p.add( UIUtilities.createSpaceEater( b, false));
        f.setAccessory( p);

        int r = f.showSaveDialog( SwingUtilities.getRoot( box));
        if (r == JFileChooser.APPROVE_OPTION) {
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            BufferedWriter out = null;
            try {
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()),
                      (String) encodings.getSelectedItem()));
                JGloss.prefs.set( Preferences.EXPORT_ENCODING, (String) encodings.getSelectedItem());
                DefaultListModel m = (DefaultListModel) exclusionList.getModel();
                for ( int i=0; i<m.size(); i++) {
                    String word = m.get( i).toString();
                    out.write( word, 0, word.length());
                    out.newLine();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( SwingUtilities.getRoot( box), JGloss.messages.getString
                      ( "error.export.exception", new Object[] 
                          { f.getSelectedFile(), ex.getClass().getName(),
                            ex.getLocalizedMessage() }),
                      JGloss.messages.getString( "error.export.title"),
                      JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            } finally {
                if (out != null) try {
                    out.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Displays a file chooser and reads the contents of the JList from the selected file.
     */
    private void importList() {
        JFileChooser f = new JFileChooser( JGloss.getCurrentDir());
        f.setDialogTitle( JGloss.messages.getString( "exclusions.import.title"));
        f.setFileHidingEnabled( true);
        f.setFileView( CustomFileView.getFileView());
        int r = f.showOpenDialog( SwingUtilities.getRoot( box));
        if (r == JFileChooser.APPROVE_OPTION) try {
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            DefaultListModel m = new DefaultListModel();
            BufferedReader reader = new BufferedReader
                ( CharacterEncodingDetector.getReader
                  ( new FileInputStream( f.getSelectedFile().getAbsolutePath())));
            String line;
            while ((line=reader.readLine()) != null) {
                if (line.length() > 0)
                    m.addElement( line);
            }
            reader.close();

            exclusionList.setModel( m);
            changed = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( SwingUtilities.getRoot( box), JGloss.messages.getString
                  ( "error.import.exception", new Object[] 
                      { f.getSelectedFile(), ex.getClass().getName(),
                        ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.import.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Adds a single word to both the exclusion list and the JList. The list of exclusions will be
     * written to disk.
     */
    private void add( String word) {
        synchronized (exclusions) {
            if (!exclusions.contains( word)) {
                exclusions.add( word);
                // append the new word to the exclusions file
                String filename = getExclusionListFile();
                try {
                    BufferedWriter w = new BufferedWriter( new OutputStreamWriter
                        ( new FileOutputStream( filename, true), "UTF-8"));
                    w.write( word, 0, word.length());
                    w.newLine();
                    w.close();
                } catch (Exception ex) {
                    JOptionPane.showConfirmDialog
                        ( SwingUtilities.getRoot( box), JGloss.messages.getString
                          ( "error.exclusions.save", new String[] 
                              { filename, ex.getClass().getName(), ex.getLocalizedMessage() }),
                          JGloss.messages.getString( "error.exclusions.save.title"),
                          JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
                ((DefaultListModel) exclusionList.getModel()).addElement( word);
            }
        }
    }
} // class Dictionaries
