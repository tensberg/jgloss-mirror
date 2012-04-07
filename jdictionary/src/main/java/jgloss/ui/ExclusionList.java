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

package jgloss.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.util.CharacterEncodingDetector;

/**
 * Panel which allows the user to manage the list of excluded words used for document parsing.
 * There exists
 * a single application-wide instance which can be accessed through the
 * {@link #getInstance() getInstance()} method.
 *
 * @author Michael Koch
 */
public class ExclusionList extends JPanel implements PreferencesPanel {
	private static final Logger LOGGER = Logger.getLogger(ExclusionList.class.getPackage().getName());
	
	private static final long serialVersionUID = 1L;

	/**
     * The single application-wide instance.
     */
    private static ExclusionList box;

    /**
     * The set of excluded words. The contents of the set may not be the same as
     * displayed in the <CODE>exclusionList</CODE> if the user has not yet applied
     * the changes.
     */
    private Set<String> exclusions;
    /**
     * The widget which displays the excluded words.
     */
    private final JList exclusionList;
    /**
     * Flag if the content of the JList was changed.
     */
    private boolean changed;
    
    /**
     * Returns the single application-wide instance.
     *
     * @return The Dictionaries component.
     */
    public static ExclusionList getInstance() {
        if (box == null) {
	        box = new ExclusionList();
        }
        return box;
    }

    /**
     * Returns the array of currently active dictionaries in the order in which they are
     * displayed. This can be different from the currently shown list if the user has not
     * yet applied changes.
     *
     * @return Array of currently active dictionaries.
     */
    public static Set<String> getExclusions() {
        return getInstance().exclusions;
    }

    /**
     * Adds a single word to the exclusion list.
     */
    public static void addWord( String word) {
        getInstance().add( word);
    }

    /**
     * Creates a new instance of dictionaries.
     */
    private ExclusionList() {
        setLayout( new GridBagLayout());

        exclusions = new HashSet<String>( 101);
        // construct the dictionaries list editor
        exclusionList = new JList();
        exclusionList.setModel( new DefaultListModel());
        exclusionList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
        // update display if user changed font
        UIManager.getDefaults().addPropertyChangeListener( new java.beans.PropertyChangeListener() {
                @Override
				public void propertyChange( java.beans.PropertyChangeEvent e) { 
                    if (e.getPropertyName().equals( "List.font")) {
                        exclusionList.setFont( (Font) e.getNewValue());
                    }
                }
            });

        final Action add = new AbstractAction() {
            private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    inputExclusion();
                }
            };
        add.setEnabled( true);
        UIUtilities.initAction( add, "exclusions.button.add");
        final Action remove = new AbstractAction() {
            private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    int i = exclusionList.getSelectedIndex();
                    DefaultListModel m = (DefaultListModel) exclusionList.getModel();
                    m.remove( i);
                    changed = true;
                    if (i < m.getSize()) {
	                    exclusionList.setSelectedIndex( i);
                    } else if (m.getSize() > 0) {
	                    exclusionList.setSelectedIndex( m.getSize()-1);
                    } else {
	                    this.setEnabled( false);
                    }
                }
            };
        remove.setEnabled( false);
        UIUtilities.initAction( remove, "exclusions.button.remove");
        final Action export = new AbstractAction() {
            private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    exportList();
                }
            };
        export.setEnabled( true);
        UIUtilities.initAction( export, "exclusions.button.export");
        final Action importA = new AbstractAction() {
            private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    importList();
                }
            };
        importA.setEnabled( true);
        UIUtilities.initAction( importA, "exclusions.button.import");

        exclusionList.addListSelectionListener( new ListSelectionListener() {
                @Override
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
        gc.insets = new Insets( 2, 2, 0, 2);
        add( p, gc);

        String filename = getExclusionListFile();
        if (new File( filename).exists()) {
	        loadExclusionList( filename);
        }
        changed = false;
    }

    @Override
	public String getTitle() { return JGloss.MESSAGES.getString( "exclusions.title"); }
    @Override
	public Component getComponent() { return this; }

    /**
     * Copies the content of the JList into the exclusion set.
     */
    @Override
	public void savePreferences() {
        if (changed) {
            DefaultListModel m = (DefaultListModel) exclusionList.getModel();
            synchronized (exclusions) {
                exclusions.clear();
                for ( int i=0; i<m.size(); i++) {
	                exclusions.add( (String) m.get( i));
                }
            }
            saveExclusionList( getExclusionListFile());
            changed = false;
        }
    }

    /**
     * Initializes the content of the JList with the words from the exclusion set.
     */
    @Override
	public void loadPreferences() {
        DefaultListModel m = (DefaultListModel) exclusionList.getModel();
        m.removeAllElements();
        String[] exclusionArray = new String[exclusions.size()];
        exclusions.toArray(exclusionArray);
        Arrays.sort(exclusionArray);
        for (String exclusion : exclusionArray) {
            m.addElement(exclusion);
        }
    }

    @Override
	public void applyPreferences() {}

    /**
     * Displays a dialog where the user can input a new exclusion word.
     */
    private void inputExclusion() {
        String word = JOptionPane.showInputDialog
            ( SwingUtilities.getRoot( box), JGloss.MESSAGES.getString
              ( "exclusions.add"), JGloss.MESSAGES.getString
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
        String filename = JGloss.PREFS.getString( Preferences.EXCLUSIONS_FILE);
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
            BufferedReader r = new BufferedReader( new InputStreamReader
                ( new FileInputStream( filename), 
                  JGloss.PREFS.getString( Preferences.EXCLUSIONS_ENCODING)));
            Set<String> newExclusions = new HashSet<String>( 1001);
            String line;
            while ((line=r.readLine()) != null) {
                if (line.length() > 0) {
	                newExclusions.add( line);
                }
            }
            exclusions = newExclusions;
            r.close();
        } catch (IOException ex) {
            JOptionPane.showConfirmDialog
                ( SwingUtilities.getRoot( box), JGloss.MESSAGES.getString
                  ( "error.exclusions.load", new String[] 
                      { filename, ex.getClass().getName(), ex.getLocalizedMessage() }),
                  JGloss.MESSAGES.getString( "error.exclusions.load.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Saves the exclusion list to a file.
     *
     * @param filename Name of the file.
     */
    private void saveExclusionList( String filename) {
        try {
            BufferedWriter w = new BufferedWriter( new OutputStreamWriter
                ( new FileOutputStream( filename), 
                  JGloss.PREFS.getString( Preferences.EXCLUSIONS_ENCODING)));
            for (String exclusion : exclusions) {
                w.write( exclusion, 0, exclusion.length());
                w.newLine();
            }
            w.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            JOptionPane.showConfirmDialog
                ( SwingUtilities.getRoot( box), JGloss.MESSAGES.getString
                  ( "error.exclusions.save", new String[] 
                      { filename, ex.getClass().getName(), ex.getLocalizedMessage() }),
                  JGloss.MESSAGES.getString( "error.exclusions.save.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Displays a file chooser and writes the contents of the JList to the selected file.
     */
    private void exportList() {
        /*ExportFileChooser f = new ExportFileChooser( JGloss.getCurrentDir(), "exclusions.export.title");
        f.addElement( ExportFileChooser.ENCODING_CHOOSER, Preferences.EXPORT_EXCLUSIONS_ENCODING);

        int r = f.showSaveDialog( SwingUtilities.getRoot( box));
        if (r == JFileChooser.APPROVE_OPTION) {
            BufferedWriter out = null;
            try {
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()),
                      f.getEncoding()));
                DefaultListModel m = (DefaultListModel) exclusionList.getModel();
                for ( int i=0; i<m.size(); i++) {
                    String word = m.get( i).toString();
                    out.write( word, 0, word.length());
                    out.newLine();
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
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
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            }*/
    }

    /**
     * Displays a file chooser and reads the contents of the JList from the selected file.
     */
    private void importList() {
        JFileChooser f = new JFileChooser( JGloss.getApplication().getCurrentDir());
        f.setDialogTitle( JGloss.MESSAGES.getString( "exclusions.import.title"));
        f.setFileHidingEnabled( true);
        f.setFileView( CustomFileView.getFileView());
        int r = f.showOpenDialog( SwingUtilities.getRoot( box));
        if (r == JFileChooser.APPROVE_OPTION) {
	        try {
	            JGloss.getApplication().setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
	            DefaultListModel m = new DefaultListModel();
	            BufferedReader reader = new BufferedReader
	                ( CharacterEncodingDetector.getReader
	                  ( new FileInputStream( f.getSelectedFile().getAbsolutePath())));
	            String line;
	            while ((line=reader.readLine()) != null) {
	                if (line.length() > 0) {
	                    m.addElement( line);
	                }
	            }
	            reader.close();

	            exclusionList.setModel( m);
	            changed = true;
	        } catch (Exception ex) {
	            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
	            JOptionPane.showConfirmDialog
	                ( SwingUtilities.getRoot( box), JGloss.MESSAGES.getString
	                  ( "error.import.exception", new Object[] 
	                      { f.getSelectedFile(), ex.getClass().getName(),
	                        ex.getLocalizedMessage() }),
	                  JGloss.MESSAGES.getString( "error.import.title"),
	                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
	        }
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
                        ( new FileOutputStream( filename, true), 
                          JGloss.PREFS.getString( Preferences.EXCLUSIONS_ENCODING)));
                    w.write( word, 0, word.length());
                    w.newLine();
                    w.close();
                } catch (Exception ex) {
                    JOptionPane.showConfirmDialog
                        ( SwingUtilities.getRoot( box), JGloss.MESSAGES.getString
                          ( "error.exclusions.save", new String[] 
                              { filename, ex.getClass().getName(), ex.getLocalizedMessage() }),
                          JGloss.MESSAGES.getString( "error.exclusions.save.title"),
                          JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
                ((DefaultListModel) exclusionList.getModel()).addElement( word);
            }
        }
    }
} // class Dictionaries
