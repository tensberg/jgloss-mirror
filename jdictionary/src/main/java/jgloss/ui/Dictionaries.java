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
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryFactory;
import jgloss.dictionary.IndexException;
import jgloss.dictionary.IndexedDictionary;

/**
 * Panel which allows the user to add and manipulate dictionaries used for document parsing.
 * The object can be queried for a list of currently selected dictionaries. There exists
 * a single application-wide instance which can be accessed through the
 * {@link #getInstance()} method.
 *
 * @author Michael Koch
 */
public class Dictionaries extends JComponent implements PreferencesPanel {
	private static final Logger LOGGER = Logger.getLogger(Dictionaries.class.getPackage().getName());
	
	private static final long serialVersionUID = 1L;

	/**
     * The single application-wide instance.
     */
    private static Dictionaries instance;

    /**
     * The widget which displays the current selection of dictionaries.
     */
    private final JList dictionaries;
    /**
     * List of {@link Dictionaries.DictionaryWrapper DictionaryWrapper } instances with
     * dictionaries currently used in the application. This is the list of dictionaries
     * returned by {@link #getDictionaries() getDictionaries}.
     * If the user has edited
     * the dictionary list in the preference dialog, but not yet applied the changes,
     * this list is different from the dictionary list displayed.
     */
    private List<DictionaryWrapper> activeDictionaries = new ArrayList<DictionaryWrapper>( 10);

    /**
     * Interface implemented by objects interested in notifications of changes in the
     * active dictionary list.
     */
    public interface DictionaryListChangeListener {
        void dictionaryListChanged();
    } // interface DictionaryListChangeListener

    /**
     * Wrapper for a dictionary and its descriptor. Used as elements in the list model.
     */
    private static class DictionaryWrapper {
        /**
         * Descriptor used to create the dictionary. Usually the path to the dictionary file.
         *
         * @see jgloss.dictionary.DictionaryFactory
         */
        public String descriptor;
        public Dictionary dictionary;

        public DictionaryWrapper( String descriptor, Dictionary dictionary) {
            this.descriptor = descriptor;
            this.dictionary = dictionary;
        }

        /**
         * Returns the name of the dictionary.
         */
        @Override
		public String toString() {
            return dictionary.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
            result = prime * result + ((dictionary == null) ? 0 : dictionary.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DictionaryWrapper other = (DictionaryWrapper) obj;
            if (descriptor == null) {
                if (other.descriptor != null)
                    return false;
            } else if (!descriptor.equals(other.descriptor))
                return false;
            if (dictionary == null) {
                if (other.dictionary != null)
                    return false;
            } else if (!dictionary.equals(other.dictionary))
                return false;
            return true;
        }
    }

    /**
     * Returns the single application-wide instance.
     *
     * @return The Dictionaries component.
     */
    public static Dictionaries getInstance() {
        assert EventQueue.isDispatchThread();

        if (instance == null) {
            instance = new Dictionaries();
        }

        return instance;
    }

    /**
     * Returns the array of currently active dictionaries in the order in which they are
     * displayed. This can be different from the currently shown list if the user has not
     * yet applied changes.
     *
     * @return Array of currently active dictionaries.
     */
    public Dictionary[] getDictionaries() {
        assert EventQueue.isDispatchThread();

        Dictionary[] out = new Dictionary[activeDictionaries.size()];
        int index = 0;
        for ( DictionaryWrapper wrapper : activeDictionaries) {
            out[index++] = wrapper.dictionary;
        }

        return out;
    }

    private final List<DictionaryListChangeListener> dictionaryListChangeListeners = new CopyOnWriteArrayList<DictionaryListChangeListener>();

    public void addDictionaryListChangeListener( DictionaryListChangeListener listener) {
    	dictionaryListChangeListeners.add( listener);
    }

    public void removeDictionaryListChangeListener( DictionaryListChangeListener listener) {
    	dictionaryListChangeListeners.remove( listener);
    }

    private void fireDictionaryListChanged() {
        for (DictionaryListChangeListener listener : dictionaryListChangeListeners) {
            listener.dictionaryListChanged();
        }
    }

    /**
     * Thread used to load dictionaries asynchronously when the user has selected "add dictionaries".
     */
    private class DictionaryLoader implements Runnable {
        private boolean dictionariesLoaded = false;
        private JDialog messageDialog;
        private JLabel message;
        private Cursor currentCursor;
        private File[] dictionaries;

        public DictionaryLoader() {}

        /**
         * Load the dictionaries from the list of files and add them to the current list of
         * dictionaries. The dictionaries are loaded in their own thread. If the thread does
         * not terminate after one second, this method will pop up a model information dialog and
         * return. The thread will dispose the dialog after it has loaded all dictionaries and display
         * any error messages for errors in dictionary loading.
         *
         * @param dictionaries List of dictionary files to load. If a dictionary file is already
         *        loaded, it will be ignored.
         */
        public void loadDictionaries( File[] dictionaries) {
            this.dictionaries = dictionaries;
            dictionariesLoaded = false;
            if (message == null) {
	            message = new JLabel( "", SwingConstants.CENTER);
            }
            currentCursor = getCursor();

            Thread worker = new Thread( this);
            worker.start();

            try {
                worker.join( 1000);
            } catch (InterruptedException ex) {}

            if (!dictionariesLoaded) {
                setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR));
                if (messageDialog == null) {
                    Frame parent = (Frame) SwingUtilities.getRoot( Dictionaries.this);
                    messageDialog = new JDialog( parent, true);
                    messageDialog.setTitle( JGloss.MESSAGES.getString( "dictionaries.loading.title"));
                    messageDialog.getContentPane().add( message);
                    messageDialog.setSize( 450, 50);
                    messageDialog.setLocation( Math.max( (int) (parent.getLocation().getX() + 
                                                                parent.getSize().getWidth()/2 - 225), 0),
                                               (int) (parent.getLocation().getY() + 
                                                      parent.getSize().getHeight()/2 - 25));
                    messageDialog.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE);
                }
                messageDialog.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR));
                messageDialog.setVisible(true);
            }
        }

        @Override
		public void run() {
            final DefaultListModel model = (DefaultListModel) Dictionaries.this.dictionaries.getModel();
            List<Object> errors = new ArrayList<Object>( dictionaries.length*2);
            for (File dictionarie : dictionaries) {
                final String descriptor = dictionarie.getAbsolutePath();
                // check if the dictionary is already added
                boolean alreadyAdded = false;
                for ( Enumeration<?> e=model.elements(); e.hasMoreElements(); ) {
                    if (((DictionaryWrapper) e.nextElement()).descriptor.equals( descriptor)) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) {
                    EventQueue.invokeLater( new Runnable() {
                            @Override
							public void run() {
                                message.setText( JGloss.MESSAGES.getString
                                                 ( "dictionaries.loading",
                                                   new String[] { new File( descriptor)
                                                       .getName() }));
                            }
                        });
                    try {
                        final Dictionary d = DictionaryFactory
                            .createDictionary( descriptor);
                        if (d instanceof IndexedDictionary) {
                            if (!((IndexedDictionary) d).loadIndex()) {
	                            ((IndexedDictionary) d).buildIndex();
                            }
                        }
                        if (d != null) {
                            EventQueue.invokeLater( new Runnable() {
                                    @Override
									public void run() {
                                        model.addElement( new DictionaryWrapper( descriptor, d));
                                    }
                                });
                        }
                    } catch (Exception ex) {
                        // stack the errors and show them after all other dictionaries are
                        // loaded
                        errors.add( ex);
                        errors.add( descriptor);
                    }
                }
            }

            dictionariesLoaded = true;
            setCursor( currentCursor);
            if (messageDialog != null) {
                messageDialog.setVisible(false);
                messageDialog.dispose(); 
            }               

            // show error messages for dictionary load failures
            for ( Iterator<Object> i=errors.iterator(); i.hasNext(); ) {
	            showDictionaryError( (Exception) i.next(), (String) i.next());
            }
        }
    }

    /**
     * Creates a new instance of dictionaries.
     */
    private Dictionaries() {
        setLayout( new GridBagLayout());

        // construct the dictionaries list editor
        dictionaries = new JList();
        dictionaries.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);

        final Action up = new AbstractAction() {
			private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    int i = dictionaries.getSelectedIndex();
                    DefaultListModel m = (DefaultListModel) dictionaries.getModel();
                    Object o = m.remove( i);
                    m.insertElementAt( o, i-1);
                    dictionaries.setSelectedIndex( i-1);
                }
            };
        up.setEnabled( false);
        UIUtilities.initAction( up, "dictionaries.button.up");
        final Action down = new AbstractAction() {
			private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    int i = dictionaries.getSelectedIndex();
                    DefaultListModel m = (DefaultListModel) dictionaries.getModel();
                    Object o = m.remove( i);
                    m.insertElementAt( o, i+1);
                    dictionaries.setSelectedIndex( i+1);
                }
            };
        down.setEnabled( false);
        UIUtilities.initAction( down, "dictionaries.button.down");
        final Action add = new AbstractAction() {
			private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    addDictionary();
                }
            };
        add.setEnabled( true);
        UIUtilities.initAction( add, "dictionaries.button.add");
        final Action remove = new AbstractAction() {
			private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    int i = dictionaries.getSelectedIndex();
                    DefaultListModel m = (DefaultListModel) dictionaries.getModel();
                    m.remove( i);
                    if (i < m.getSize()) {
	                    dictionaries.setSelectedIndex( i);
                    } else if (m.getSize() > 0) {
	                    dictionaries.setSelectedIndex( m.getSize()-1);
                    } else {
	                    this.setEnabled( false);
                    }
                }
            };
        remove.setEnabled( false);
        UIUtilities.initAction( remove, "dictionaries.button.remove");

        dictionaries.addListSelectionListener( new ListSelectionListener() {
                @Override
				public void valueChanged( ListSelectionEvent e) {
                    if (dictionaries.isSelectionEmpty()) {
                        up.setEnabled( false);
                        down.setEnabled( false);
                        remove.setEnabled( false);
                    }
                    else {
                        if (dictionaries.getSelectedIndex() > 0) {
	                        up.setEnabled( true);
                        } else {
	                        up.setEnabled( false);
                        }
                        if (dictionaries.getSelectedIndex() <
                            dictionaries.getModel().getSize()-1) {
	                        down.setEnabled( true);
                        } else {
	                        down.setEnabled( false);
                        }
                        //if (((DictionaryWrapper) dictionaries.getSelectedValue()).dictionary !=
                        //    userDictionary)
                            remove.setEnabled( true);
                            //else
                            //remove.setEnabled( false);
                    }
                }
            });

        JScrollPane scroller = new JScrollPane( dictionaries);
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
        p.add( new JButton( up));
        p.add( new JButton( down));
        gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTH;
        gc.gridx = 1;
        gc.gridy = GridBagConstraints.RELATIVE;
        gc.fill= GridBagConstraints.NONE;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.insets = new Insets( 2, 2, 0, 2);
        add( p, gc);

        /*if (userDictImplementation == null) {
            userDictImplementation = new UserDictionary.Implementation
                ( System.getProperty( "user.home") + File.separator +
                  JGloss.prefs.getString( Preferences.USERDICTIONARY_FILE));
            DictionaryFactory.registerImplementation( userDictImplementation);
            }*/
    }

    @Override
	public String getTitle() { return JGloss.MESSAGES.getString( "dictionaries.title"); }
    @Override
	public Component getComponent() { return this; }

    /**
     * Runs the dialog to add a new dictionary to the list.
     */
    private void addDictionary() {
        String dir = JGloss.PREFS.getString( Preferences.DICTIONARIES_DIR);
        if (dir==null || dir.trim().length()==0) {
            dir = System.getProperty( "user.home");
        }

        final JFileChooser chooser = new JFileChooser( dir);
        chooser.setFileHidingEnabled( true);
        chooser.setMultiSelectionEnabled( true);
        chooser.setDialogTitle( JGloss.MESSAGES.getString( "dictionaries.chooser.title"));
        chooser.setFileView( CustomFileView.getFileView());
        int result = chooser.showDialog( SwingUtilities.getRoot( instance), JGloss.MESSAGES.getString
                                         ( "dictionaries.chooser.button.add"));
        if (result == JFileChooser.APPROVE_OPTION) {
            new DictionaryLoader().loadDictionaries( chooser.getSelectedFiles());
            JGloss.PREFS.set( Preferences.DICTIONARIES_DIR, chooser.getCurrentDirectory()
                              .getAbsolutePath());
        }
    }

    /**
     * Show an error dialog for the dictionary exception.
     */
    private void showDictionaryError( Exception ex, String file) {
        if (ex instanceof DictionaryFactory.InstantiationException) {
            ex = (Exception) ((DictionaryFactory.InstantiationException) ex).getCause();
        }

        File f = new File( file);
        String path = f.getAbsolutePath();
        if (ex instanceof DictionaryFactory.NotSupportedException) {
            int choice = JOptionPane.showOptionDialog(
                SwingUtilities.getRoot(this),
                JGloss.MESSAGES.getString("error.dictionary.format", new String[] { path }),
                JGloss.MESSAGES.getString("error.dictionary.title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null,
                new String[] { JGloss.MESSAGES.getString("button.ok"), JGloss.MESSAGES.getString("button.why") }, null);     
 
            if (choice == JOptionPane.NO_OPTION) { // this is really the Why? option
                JTextArea text = new JTextArea(JGloss.MESSAGES.getString( "error.dictionary.reason", new String[] { path, ex.getMessage() }), 25, 55);
                text.setEditable(false);
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
            
                JOptionPane.showConfirmDialog
                    ( SwingUtilities.getRoot( this), new JScrollPane(text),
                      JGloss.MESSAGES.getString( "error.dictionary.title"),
                      JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
            }
        }
        else {
            String msgid;
            String [] objects;

            if (ex instanceof FileNotFoundException) {
                msgid = "error.dictionary.filenotfound";
                objects = new String[] { f.getName(), path };
            }
            else if (ex instanceof IndexException) {
                msgid = "error.dictionary.indexnotavailable";
                objects = new String[] { path, f.getParent() };
            }
            else if (ex instanceof IOException) {
                msgid = "error.dictionary.ioerror";
                objects = new String[] { path };
            }
            else {
                msgid = "error.dictionary.exception";
                objects = new String[] { path, ex.getLocalizedMessage(), 
                                         ex.getClass().getName() };
            }
    
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            JOptionPane.showConfirmDialog
                ( SwingUtilities.getRoot( this), JGloss.MESSAGES.getString
                  ( msgid, objects),
                  JGloss.MESSAGES.getString( "error.dictionary.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Makes the list of currently displayed dictionaries the active dictionaries. The list
     * will be stored in the preferences.
     */
    @Override
	public void savePreferences() {
        assert EventQueue.isDispatchThread();
        
        ListModel model = dictionaries.getModel();
        StringBuilder paths = new StringBuilder( model.getSize()*32);
        List<DictionaryWrapper> newDictionaries = new ArrayList<DictionaryWrapper>( model.getSize());

        for ( int i=0; i<model.getSize(); i++) {
            DictionaryWrapper dictionaryWrapper = (DictionaryWrapper) model.getElementAt( i);
            newDictionaries.add( dictionaryWrapper);
            if (paths.length() > 0) {
                paths.append( File.pathSeparatorChar);
            }
            paths.append( dictionaryWrapper.descriptor);
        }

        // dispose any dictionaries which are no longer active
        for (DictionaryWrapper d : activeDictionaries) {
            if (!newDictionaries.contains( d)) {
                d.dictionary.dispose();
            }
        }
        activeDictionaries.clear();
        activeDictionaries.addAll( newDictionaries);

        JGloss.PREFS.set( Preferences.DICTIONARIES, paths.toString());
        fireDictionaryListChanged();
    }

    /**
     * Initializes the list of active dictionaries from the dictionaries stored in the
     * preferences.
     */
    private void loadDictionariesFromPreferences() {
        String[] fs = JGloss.PREFS.getPaths( Preferences.DICTIONARIES);
        // exceptions occurring during dictionary loading
        final List<Object> exceptions = new ArrayList<Object>( 5);

        for (String element : fs) {
            try {
                Dictionary d = DictionaryFactory.createDictionary( element);
                if (d instanceof IndexedDictionary) {
                    if (!((IndexedDictionary) d).loadIndex()) {
                        LOGGER.info( "building index for dictionary " + d.getName());
                        // TODO: add progress feedback
                        ((IndexedDictionary) d).buildIndex();
                    }
                }

                activeDictionaries.add( new DictionaryWrapper( element, d));
            } catch (Exception ex) {
                exceptions.add( ex);
                exceptions.add( element);
            }
        }
        fireDictionaryListChanged();
        
        EventQueue.invokeLater( new Runnable() {
                @Override
				public void run() {
                    // show dialogs for all errors which occurred while the dictionaries were opened
                    for ( Iterator<Object> i=exceptions.iterator(); i.hasNext(); ) {
                        showDictionaryError( (Exception) i.next(), (String) i.next());
                    }
                }
            });
    }

    /**
     * Initializes the list of displayed dictionaries from the preferences setting.
     */
    @Override
	public void loadPreferences() {
        assert EventQueue.isDispatchThread();
        
        // activeDictionaries is only initialized from the preferences if it does not
        // already contain dictionaries.
        boolean prefsLoaded = false;
        if (activeDictionaries.isEmpty()) {
            loadDictionariesFromPreferences();
            prefsLoaded = true;
        }
        
        // display the list of loaded dictionaries
        final DefaultListModel model = new DefaultListModel();

        // discard any dictionaries loaded in the component but not in the active list
        ListModel oldModel = dictionaries.getModel();

        for ( int i=0; i<oldModel.getSize(); i++) {
            DictionaryWrapper d = (DictionaryWrapper) oldModel.getElementAt( i);
            if (!activeDictionaries.contains( d)) {
                d.dictionary.dispose();
            }
        }

        for (DictionaryWrapper dictionaryWrapper : activeDictionaries) {
            model.addElement(dictionaryWrapper);
        }

        dictionaries.setModel( model);

        if (!prefsLoaded) {
	        fireDictionaryListChanged();
	        // otherwise loadDictionariesFromPreferences has already fired the event
        }
    }

    /**
     * Apply the new preference setting to already opened document. Does nothing since
     * there are no changes which will apply instantly.
     */
    @Override
	public void applyPreferences() {}
} // class Dictionaries
