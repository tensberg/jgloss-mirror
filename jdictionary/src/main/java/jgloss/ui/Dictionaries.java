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
 * {@link #getComponent() getComponent()} method.
 *
 * @author Michael Koch
 */
public class Dictionaries extends JComponent implements PreferencesPanel {
	private static final long serialVersionUID = 1L;

	/**
     * The single application-wide instance.
     */
    private static Dictionaries box;

    /**
     * The widget which displays the current selection of dictionaries.
     */
    private JList dictionaries;
    /**
     * List of {@link Dictionaries.DictionaryWrapper DictionaryWrapper } instances with
     * dictionaries currently used in the application. This is the list of dictionaries
     * returned by {@link #getDictionaries(boolean) getDictionaries}.
     * If the user has edited
     * the dictionary list in the preference dialog, but not yet applied the changes,
     * this list is different from the dictionary list displayed.
     */
    private static List<DictionaryWrapper> activeDictionaries = new ArrayList<DictionaryWrapper>( 10);
    /**
     * An EDICT editable by the user.
     */
    //private static UserDictionary userDictionary;
    /**
     * Implementation used to create the user dictionary.
     */
    //private static UserDictionary.Implementation userDictImplementation;

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
    private class DictionaryWrapper {
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
		public boolean equals( Object o) {
            try {
                DictionaryWrapper d = (DictionaryWrapper) o;
                return (d.descriptor.equals( descriptor) && d.dictionary.equals( dictionary));
            } catch (ClassCastException ex) {
                return false;
            }
        }
    }

    /**
     * Returns the single application-wide instance.
     *
     * @return The Dictionaries component.
     */
    public static Dictionaries getInstance() {
        synchronized (componentLock) {
            if (box == null)
                box = new Dictionaries();
        }

        return box;
    }

    /**
     * Lock synchronized on in {@link #getComponent() getComponent}.
     */
    private static final Object componentLock = "component lock";

    /**
     * Returns the array of currently active dictionaries in the order in which they are
     * displayed. This can be different from the currently shown list if the user has not
     * yet applied changes.
     *
     *
     * @param waitForLoad If <CODE>true</CODE>, blocks until the dictionaries are initialized from
     *                    the preferences.
     * @return Array of currently active dictionaries.
     */
    public static Dictionary[] getDictionaries( boolean waitForLoad) {
        if (waitForLoad) {
            Dictionaries component = getInstance();
            // Guarantee that all dictionaries are loaded from the preferences before returning.
            // By synchronizing on component, guarantee mutual exclusivity with loadPreferences
            // and savePreferences
            synchronized (component) {
                if (activeDictionaries.size() == 0)
                    component.loadDictionariesFromPreferences();
            }
        }

        synchronized (activeDictionaries) {
            Dictionary[] out = new Dictionary[activeDictionaries.size()];
            int index = 0;
            for ( DictionaryWrapper wrapper : activeDictionaries)
                out[index++] = wrapper.dictionary;

            return out;
        }
    }

    /**
     * Returns the user dictionary. If the user dictionary creation failed <CODE>null</CODE>
     * will be returned.
     */
    /*public static UserDictionary getUserDictionary() {
        return userDictionary;
        }*/

    private final static List<DictionaryListChangeListener> dictionaryListChangeListeners = new CopyOnWriteArrayList<DictionaryListChangeListener>();

    public static void addDictionaryListChangeListener( DictionaryListChangeListener listener) {
    	dictionaryListChangeListeners.add( listener);
    }

    public static void removeDictionaryListChangeListener( DictionaryListChangeListener listener) {
    	dictionaryListChangeListeners.remove( listener);
    }

    protected static void fireDictionaryListChanged() {
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
            if (message == null)
                message = new JLabel( "", SwingConstants.CENTER);
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
                    messageDialog.setTitle( JGloss.messages.getString( "dictionaries.loading.title"));
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
            List errors = new ArrayList( dictionaries.length*2);
            for ( int i=0; i<dictionaries.length; i++) {
                final String descriptor = dictionaries[i].getAbsolutePath();
                // check if the dictionary is already added
                boolean alreadyAdded = false;
                for ( Enumeration e=model.elements(); e.hasMoreElements(); ) {
                    if (((DictionaryWrapper) e.nextElement()).descriptor.equals( descriptor)) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) {
                    EventQueue.invokeLater( new Runnable() {
                            @Override
							public void run() {
                                message.setText( JGloss.messages.getString
                                                 ( "dictionaries.loading",
                                                   new String[] { new File( descriptor)
                                                       .getName() }));
                            }
                        });
                    try {
                        final Dictionary d = DictionaryFactory
                            .createDictionary( descriptor);
                        if (d instanceof IndexedDictionary) {
                            if (!((IndexedDictionary) d).loadIndex())
                                ((IndexedDictionary) d).buildIndex();
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
            for ( Iterator i=errors.iterator(); i.hasNext(); )
                showDictionaryError( (Exception) i.next(), (String) i.next());
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
                    DictionaryWrapper d = (DictionaryWrapper) m.remove( i);
                    if (i < m.getSize())
                        dictionaries.setSelectedIndex( i);
                    else if (m.getSize() > 0)
                        dictionaries.setSelectedIndex( m.getSize()-1);
                    else
                        this.setEnabled( false);
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
                        if (dictionaries.getSelectedIndex() > 0)
                            up.setEnabled( true);
                        else
                            up.setEnabled( false);
                        if (dictionaries.getSelectedIndex() <
                            dictionaries.getModel().getSize()-1)
                            down.setEnabled( true);
                        else
                            down.setEnabled( false);
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
	public String getTitle() { return JGloss.messages.getString( "dictionaries.title"); }
    @Override
	public Component getComponent() { return this; }

    /**
     * Runs the dialog to add a new dictionary to the list.
     */
    private void addDictionary() {
        String dir = JGloss.prefs.getString( Preferences.DICTIONARIES_DIR);
        if (dir==null || dir.trim().length()==0) {
            dir = System.getProperty( "user.home");
        }

        final JFileChooser chooser = new JFileChooser( dir);
        chooser.setFileHidingEnabled( true);
        chooser.setMultiSelectionEnabled( true);
        chooser.setDialogTitle( JGloss.messages.getString( "dictionaries.chooser.title"));
        chooser.setFileView( CustomFileView.getFileView());
        int result = chooser.showDialog( SwingUtilities.getRoot( box), JGloss.messages.getString
                                         ( "dictionaries.chooser.button.add"));
        if (result == JFileChooser.APPROVE_OPTION) {
            new DictionaryLoader().loadDictionaries( chooser.getSelectedFiles());
            JGloss.prefs.set( Preferences.DICTIONARIES_DIR, chooser.getCurrentDirectory()
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
                JGloss.messages.getString("error.dictionary.format", new String[] { path }),
                JGloss.messages.getString("error.dictionary.title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null,
                new String[] { JGloss.messages.getString("button.ok"), JGloss.messages.getString("button.why") }, null);     
 
            if (choice == JOptionPane.NO_OPTION) { // this is really the Why? option
                JTextArea text = new JTextArea(JGloss.messages.getString( "error.dictionary.reason", new String[] { path, ex.getMessage() }), 25, 55);
                text.setEditable(false);
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
            
                JOptionPane.showConfirmDialog
                    ( SwingUtilities.getRoot( this), new JScrollPane(text),
                      JGloss.messages.getString( "error.dictionary.title"),
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
    
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( SwingUtilities.getRoot( this), JGloss.messages.getString
                  ( msgid, objects),
                  JGloss.messages.getString( "error.dictionary.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Makes the list of currently displayed dictionaries the active dictionaries. The list
     * will be stored in the preferences.
     */
    @Override
	public synchronized void savePreferences() {
        ListModel model = dictionaries.getModel();
        StringBuffer paths = new StringBuffer( model.getSize()*32);
        List newDictionaries = new ArrayList( model.getSize());

        synchronized (model) {
            for ( int i=0; i<model.getSize(); i++) {
                newDictionaries.add( model.getElementAt( i));
                if (paths.length() > 0)
                    paths.append( File.pathSeparatorChar);
                paths.append( ((DictionaryWrapper) model.getElementAt( i)).descriptor);
            }
        }
        synchronized (activeDictionaries) {
            // dispose any dictionaries which are no longer active
            for ( Iterator i=activeDictionaries.iterator(); i.hasNext(); ) {
                DictionaryWrapper d = (DictionaryWrapper) i.next();
                if (!newDictionaries.contains( d))
                    d.dictionary.dispose();
            }
            activeDictionaries.clear();
            activeDictionaries.addAll( newDictionaries);
        }

        JGloss.prefs.set( Preferences.DICTIONARIES, paths.toString());
        fireDictionaryListChanged();
    }

    /**
     * Initializes the list of active dictionaries from the dictionaries stored in the
     * preferences. If the list in the preferences does not contain the user dictionary,
     * it is inserted at the first position.
     */
    private synchronized void loadDictionariesFromPreferences() {
        String[] fs = JGloss.prefs.getPaths( Preferences.DICTIONARIES);
        // exceptions occurring during dictionary loading
        final List exceptions = new ArrayList( 5);

        for ( int i=0; i<fs.length; i++) {
            try {
                Dictionary d = DictionaryFactory.createDictionary( fs[i]);
                if (d instanceof IndexedDictionary) {
                    if (!((IndexedDictionary) d).loadIndex()) {
                        System.err.println( "building index for dictionary " + d.getName());
                        ((IndexedDictionary) d).buildIndex();
                    }
                }
                synchronized (activeDictionaries) {
                    activeDictionaries.add( new DictionaryWrapper( fs[i], d));
                }
                fireDictionaryListChanged(); // fire for every loaded dictionary
                /*if (d instanceof UserDictionary)
                  userDictionary = (UserDictionary) d;*/
            } catch (Exception ex) {
                exceptions.add( ex);
                exceptions.add( fs[i]);
            }
        }
        // insert the user dictionary if not already in the dictionary list
        /*if (userDictionary == null) try {
            userDictionary = (UserDictionary)
                DictionaryFactory.createDictionary( userDictImplementation.getDescriptor());
            synchronized (activeDictionaries) {
                activeDictionaries.add( 0, new DictionaryWrapper
                    ( userDictImplementation.getDescriptor(), userDictionary));
            }
        } catch (DictionaryFactory.Exception ex) {
            exceptions.add( ex);
            exceptions.add( userDictImplementation.getDescriptor());
            }*/
        
        EventQueue.invokeLater( new Runnable() {
                @Override
				public void run() {
                    // show dialogs for all errors which occurred while the dictionaries were opened
                    for ( Iterator i=exceptions.iterator(); i.hasNext(); ) {
                        showDictionaryError( (Exception) i.next(), (String) i.next());
                    }
                }
            });
    }

    /**
     * Initializes the list of displayed dictionaries from the preferences setting.
     */
    @Override
	public synchronized void loadPreferences() {
        // activeDictionaries is only initialized from the preferences if it does not
        // already contain dictionaries.
        boolean prefsLoaded = false;
        if (activeDictionaries.size() == 0) {
            loadDictionariesFromPreferences();
            prefsLoaded = true;
        }
        
        // display the list of loaded dictionaries
        final DefaultListModel model = new DefaultListModel();
        synchronized (activeDictionaries) {
            // discard any dictionaries loaded in the component but not in the active list
            ListModel oldModel = dictionaries.getModel();
            synchronized (oldModel) {
                for ( int i=0; i<oldModel.getSize(); i++) {
                    DictionaryWrapper d = (DictionaryWrapper) oldModel.getElementAt( i);
                    if (!activeDictionaries.contains( d))
                        d.dictionary.dispose();
                }
            }
            
            for ( Iterator i=activeDictionaries.iterator(); i.hasNext(); )
                model.addElement( i.next());
        }
        Runnable worker = new Runnable() {
                @Override
				public void run() {
                    dictionaries.setModel( model);
                }
            };
        if (!EventQueue.isDispatchThread())
            EventQueue.invokeLater( worker);
        else
            worker.run();

        if (!prefsLoaded)
            fireDictionaryListChanged();
        // otherwise loadDictionariesFromPreferences has already fired the event
    }

    /**
     * Apply the new preference setting to already opened document. Does nothing since
     * there are no changes which will apply instantly.
     */
    @Override
	public void applyPreferences() {}
} // class Dictionaries
