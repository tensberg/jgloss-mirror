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
import jgloss.ui.doc.*;
import jgloss.ui.annotation.*;
import jgloss.ui.export.*;
import jgloss.dictionary.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * Frame which contains everything needed to edit a single JGloss document.
 * It consists of a {@link JGlossEditor JGlossEditor} and an {@link AnnotationEditor AnnotationEditor},
 * and a menu bar which allows access to all document-related actions.
 *
 * @author Michael Koch
 */
public class JGlossFrame extends JFrame implements ActionListener {
    /**
     * Collection of publically available actions.
     */
    public static class Actions {
        /**
         * Imports a document into an empty JGlossFrame.
         */
        public final Action importDocument;
        /**
         * Imports the clipboard content into an empty JGlossFrame.
         */
        public final Action importClipboard;
        /**
         * Menu listener which will update the state of the import clipboard
         * action when the menu is selected.
         */
        public final MenuListener importClipboardListener;
        /**
         * Opens a document created by JGloss in an empty JGlossFrame.
         */
        public final Action open;
        /**
         * Listens to open recent selections. Use with 
         * {@link OpenRecentMenu#createDocumentMenu(File,OpenRecentMenu.FileSelectedListener) 
         *  OpenRecentMenu.createDocumentMenu}.
         */
        public final OpenRecentMenu.FileSelectedListener openRecentListener;

        /**
         * Creates a new instance of the actions which will invoke the methods
         * on the specified target. If the target is <CODE>null</CODE>, a new JGlossFrame
         * will be created on each invocation.
         */
        private Actions( final JGlossFrame target) {
            importDocument = new AbstractAction() {
                    public void actionPerformed( ActionEvent e) {
                        new Thread( "JGloss import") {
                                public void run() {
                                    ImportDialog d = new ImportDialog( target);
                                    if (d.doDialog()) {
                                        JGlossFrame which;
                                        if (target==null || target.documentLoaded)
                                            which = new JGlossFrame();
                                        else
                                            which = target;
                                        
                                        if (d.selectionIsFilename())
                                            which.importDocument
                                                ( d.getSelection(),
                                                  d.createParser( Dictionaries.getDictionaries( true),
                                                                  ExclusionList.getExclusions()),
                                                  d.createReadingAnnotationFilter(),
                                                  d.getEncoding());
                                        else
                                            which.importString
                                                ( d.getSelection(), 
                                                  d.createParser( Dictionaries.getDictionaries( true),
                                                                  ExclusionList.getExclusions()),
                                                  d.createReadingAnnotationFilter(),
                                                  JGloss.messages.getString( "import.textarea"),
                                                  JGloss.messages.getString( "import.textarea"),
                                                  false);
                                        which.documentChanged = true;
                                    }
                                }
                            }.start();
                    }
                };
            importDocument.setEnabled( true);
            UIUtilities.initAction( importDocument, "main.menu.import"); 
            importClipboard = new AbstractAction() {
                    public void actionPerformed( ActionEvent e) {
                        new Thread( "JGloss import") {
                                public void run() {
                                    if (target == null)
                                        new JGlossFrame().doImportClipboard();
                                    else
                                        target.doImportClipboard();
                                }
                            }.start();
                    }
                };
            importClipboard.setEnabled( false);
            UIUtilities.initAction( importClipboard, "main.menu.importclipboard"); 
            open = new AbstractAction() {
                    public void actionPerformed( ActionEvent e) {
                        new Thread( "JGloss open") {
                                public void run() {
                                    JFileChooser f = new JFileChooser( JGloss.getCurrentDir());
                                    f.addChoosableFileFilter( jglossFileFilter);
                                    f.setFileHidingEnabled( true);
                                    f.setFileView( CustomFileView.getFileView());
                                    int r = f.showOpenDialog( target);
                                    if (r == JFileChooser.APPROVE_OPTION) {
                                        JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
                                        // test if the file is already open
                                        String path = f.getSelectedFile().getAbsolutePath();
                                        for ( Iterator i=jglossFrames.iterator(); i.hasNext(); ) {
                                            JGlossFrame next = (JGlossFrame) i.next();
                                            if (path.equals( next.documentPath)) {
                                                next.show();
                                                return;
                                            }
                                        }
                                        
                                        // load the file
                                        JGlossFrame which = target==null ||
                                            target.documentLoaded ? new JGlossFrame() : target;
                                        which.loadDocument( f.getSelectedFile());
                                    }
                                }
                            }.start();
                    }
                };
            open.setEnabled( true);
            UIUtilities.initAction( open, "main.menu.open");
            
            openRecentListener = new OpenRecentMenu.FileSelectedListener() {
                    public void fileSelected( final File file) {
                        // test if the file is already open
                        String path = file.getAbsolutePath();
                        for ( Iterator i=jglossFrames.iterator(); i.hasNext(); ) {
                            JGlossFrame next = (JGlossFrame) i.next();
                            if (path.equals( next.documentPath)) {
                                next.show();
                                return;
                            }
                        }
                        
                        // load the file asynchronously
                        new Thread() {
                                public void run() {
                                    JGlossFrame which = target==null ||
                                        target.documentLoaded ? new JGlossFrame() : target;
                                    which.loadDocument( file);
                                }
                            }.start();
                    }
                };

            importClipboardListener = new MenuListener() {
                    public void menuSelected( MenuEvent e) {
                        // enable the import clipboard menu item if the clipboard contains some text
                        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard()
                            .getContents( this);
                        boolean enabled = false;
                        if (t != null) {
                            if (t.isDataFlavorSupported( DataFlavor.stringFlavor))
                                enabled = true;
                            else if (t.getClass().getName().equals( "sun.awt.motif.X11Selection")) try {
                                // With the X11 implementation of Java, getting the transfer data
                                // succeeds even if isDataFlavorSupported returns false.
                                t.getTransferData( DataFlavor.stringFlavor);
                                enabled = true;
                            } catch (UnsupportedFlavorException ex) {
                            } catch (IOException ex) {}
                        }
                        importClipboard.setEnabled( enabled);
                    }
                    public void menuDeselected( MenuEvent e) {}
                    public void menuCanceled( MenuEvent e) {}
                };
        }
    }

    /**
     * Static instance of the actions which can be used by other classes. If an action
     * is invoked, a new <CODE>JGlossFrame</CODE> will be created as the target of the
     * action.
     */
    public final static Actions actions = new Actions( null);

    /**
     * Open recent menu used for JGloss documents. The instance is shared between instances of
     * <CODE>JGlossFrame</CODE> and <CODE>WordLookup</CODE>.
     */
    public final static OpenRecentMenu OPEN_RECENT = new OpenRecentMenu( 8);

    /**
     * Scrollpane which wraps the document editor.
     */
    private JScrollPane docpaneScroller;
    /**
     * The document editor.
     */
    private JGlossEditor docpane;
    /**
     * Editor kit used in the creation of the document.
     */
    private JGlossEditorKit kit;
    /**
     * The JGloss annotated document this frame edits.
     */
    private JGlossDocument doc;
    /**
     * Scrollpane which wraps the annotation editor.
     */
    private JScrollPane annotationEditorScroller;
    /**
     * The editor used to edit annotations.
     */
    private AnnotationEditor annotationEditor;
    /**
     * The split pane containing the document and annotation editor.
     */
    private JSplitPane split;

    /**
     * The name of the document. For files this is the file name.
     */
    private String documentName = null;
    /**
     * The full path name to the document. This will be <CODE>null</CODE> if the 
     * document is imported and not yet saved.
     */
    private String documentPath = null;
    /**
     * <CODE>true</CODE> if a document is loaded in this frame either by opening or
     * importing a document.
     */
    private boolean documentLoaded = false;
    /**
     * <CODE>true</CODE> if the document was modified since the last save.
     */
    private boolean documentChanged = false;
    /**
     * Defer a window closing event until the frame object is in a safe state. This is used while
     * the <CODE>loadDocument</CODE> method is executing.
     */
    private boolean deferWindowClosing = false;

    /**
     * Saves the document.
     */
    private Action saveAction;
    /**
     * Saves the document after asking the user for a filename.
     */
    private Action saveAsAction;
    /**
     * Prints the document.
     */
    private Action printAction;
    /**
     * Closes this JGlossFrame.
     */
    private Action closeAction;
    /**
     * Open recent menu for this instance of <CODE>JGlossFrame</CODE>.
     */
    private JMenu openRecentMenu;
    /**
     * Submenu containing the export actions.
     */
    private JMenu exportMenu;
    /**
     * Exports the document as plain text.
     */
    private Action exportPlainTextAction;
    /**
     * Export the document in LaTeX format.
     */
    private Action exportLaTeXAction;
    /**
     * Exports the document as HTML.
     */
    private Action exportHTMLAction;
    /**
     * Exports the annotations to a file.
     */
    private Action exportAnnotationListAction;
    /**
     * Menu item which holds the show preferences action.
     */
    private JMenuItem preferencesItem;
    /**
     * Menu item which holds the show about box action.
     */
    private JMenuItem aboutItem;

    /**
     * Toggles compact view.
     */
    private JCheckBoxMenuItem compactViewItem;
    /**
     * Toggles the display of reading annotations.
     */
    private JCheckBoxMenuItem showReadingItem;
    /**
     * Toggles the display of translation annotations.
     */
    private JCheckBoxMenuItem showTranslationItem;
    /**
     * Toggles the automatic display of annotation tooltips if the mouse
     * hovers over an annotation.
     */
    private JCheckBoxMenuItem showAnnotationItem;
    /**
     * Listens to changes of the properties.
     */
    private PropertyChangeListener prefsListener;

    /**
     * List of open JGloss documents.
     */
    private static LinkedList jglossFrames = new LinkedList();

    /**
     * Returns the number of currently open JGlossFrames.
     */
    public static int getFrameCount() { return jglossFrames.size(); }

    /**
     * A file filter which will accept JGloss documents.
     */
    public static final javax.swing.filechooser.FileFilter jglossFileFilter = 
        new ExtensionFileFilter( "jgloss", 
                                 JGloss.messages.getString( "filefilter.description.jgloss"));

    /**
     * Creates a new JGlossFrame which does not contain a document. The user can add a document
     * by using import or open actions.
     */
    public JGlossFrame() {
        super( JGloss.messages.getString( "main.title"));
        jglossFrames.add( this);
        
        // set up the frame
        annotationEditor = new AnnotationEditor();
        annotationEditorScroller = new JScrollPane( annotationEditor,
                                                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        docpane = new JGlossEditor( annotationEditor);
        docpane.setEditable( JGloss.prefs.getBoolean( Preferences.EDITOR_ENABLEEDITING, false));
        JLabel rendering = new JLabel( JGloss.messages.getString( "main.renderingdocument"),
                                       JLabel.CENTER);
        rendering.setBackground( Color.white);
        rendering.setOpaque( true);
        rendering.setFont( rendering.getFont().deriveFont( 18.0f));
        docpaneScroller = new JScrollPane( rendering,
                                           JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                           JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        docpane.setNextFocusableComponent( annotationEditor);
        annotationEditor.setNextFocusableComponent( docpane);
        split = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
                                docpaneScroller,
                                annotationEditorScroller);
        double position = JGloss.prefs.getDouble
            ( Preferences.VIEW_DIVIDERLOCATION, 0.7d);
        split.setDividerLocation( position);
        split.setResizeWeight( position);
        split.setOneTouchExpandable( true);

        getContentPane().setBackground( Color.white);

        setLocation( JGloss.prefs.getInt( Preferences.FRAME_X, 0),
                     JGloss.prefs.getInt( Preferences.FRAME_Y, 0));
        setSize( JGloss.prefs.getInt( Preferences.FRAME_WIDTH, getPreferredSize().width),
                 JGloss.prefs.getInt( Preferences.FRAME_HEIGHT, getPreferredSize().height));
        addComponentListener( new ComponentAdapter() {
                public void componentMoved( ComponentEvent e) {
                    JGloss.prefs.set( Preferences.FRAME_X, getX());
                    JGloss.prefs.set( Preferences.FRAME_Y, getY());
                }
                public void componentResized( ComponentEvent e) {
                    JGloss.prefs.set( Preferences.FRAME_WIDTH, getWidth());
                    JGloss.prefs.set( Preferences.FRAME_HEIGHT, getHeight());
                }
            });
        addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent e) {
                    synchronized (this) {
                        if (deferWindowClosing) {
                            // Another thread is currently executing loadDocument.
                            // Defer the closing of the window until the frame object is in a safe state.
                            // The loadDocument method is responsible for closing the window.
                            deferWindowClosing = false;
                        }
                        else {
                            if (askCloseDocument())
                                closeDocument();
                        }
                    }
                }

                public void windowClosed( WindowEvent e) {
                    if (jglossFrames.size() == 0) { // no more open documents
                        JGloss.exit();
                    }
                }
            });
        setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE);

        prefsListener = new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent e) {
                    if (e.getPropertyName().equals( Preferences.EDITOR_ENABLEEDITING))
                        docpane.setEditable( "true".equals( e.getNewValue()));
                };
            };
        JGloss.prefs.addPropertyChangeListener( prefsListener);
        
        saveAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    if (documentPath == null)
                        saveDocumentAs();
                    else
                        saveDocument();
                }
            };
        saveAction.setEnabled( false);
        UIUtilities.initAction( saveAction, "main.menu.save"); 
        saveAsAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    saveDocumentAs();
                }
            };
        saveAsAction.setEnabled( false);
        UIUtilities.initAction( saveAsAction, "main.menu.saveAs");
        exportPlainTextAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doExportPlainText();
                }
            };
        exportPlainTextAction.setEnabled( false);
        UIUtilities.initAction( exportPlainTextAction, "main.menu.export.plaintext"); 
        exportLaTeXAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doExportLaTeX();
                } 
            };
        exportLaTeXAction.setEnabled( false);
        UIUtilities.initAction( exportLaTeXAction, "main.menu.export.latex"); 
        exportHTMLAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doExportHTML();
                }
            };
        exportHTMLAction.setEnabled( false);
        UIUtilities.initAction( exportHTMLAction, "main.menu.export.html"); 
        exportAnnotationListAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doExportAnnotationList();
                }
            };
        exportAnnotationListAction.setEnabled( false);
        UIUtilities.initAction( exportAnnotationListAction, "main.menu.export.annotationlist"); 
        printAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doPrint();
                }
            };
        printAction.setEnabled( false);
        UIUtilities.initAction( printAction, "main.menu.print"); 
        closeAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    if (askCloseDocument()) {
                        closeDocument();
                    }
                }
            };
        UIUtilities.initAction( closeAction, "main.menu.close");
        
        Actions actions = new Actions( this);

        // set up the menu bar
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu( JGloss.messages.getString( "main.menu.file"));
        menu.add( UIUtilities.createMenuItem( actions.importDocument));
        menu.add( UIUtilities.createMenuItem( actions.importClipboard));
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( actions.open));
        openRecentMenu = OPEN_RECENT.createMenu( actions.openRecentListener);
        menu.add( openRecentMenu);
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( saveAction));
        menu.add( UIUtilities.createMenuItem( saveAsAction));
        exportMenu = new JMenu( JGloss.messages.getString( "main.menu.export"));
        exportMenu.setMnemonic( JGloss.messages.getString( "main.menu.export.mk").charAt( 0));
        exportMenu.setEnabled( false);
        exportMenu.add( UIUtilities.createMenuItem( exportHTMLAction));
        exportMenu.add( UIUtilities.createMenuItem( exportPlainTextAction));
        exportMenu.add( UIUtilities.createMenuItem( exportLaTeXAction));
        exportMenu.add( UIUtilities.createMenuItem( exportAnnotationListAction));
        menu.add( exportMenu);
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( printAction));
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( closeAction));
        bar.add( menu);

        menu.addMenuListener( actions.importClipboardListener);

        menu = docpane.getEditMenu();
        menu.addSeparator();
        Action wordLookupAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    WordLookup.showAction.actionPerformed( e);
                    String selection = docpane.getSelectedText();
                    if (selection != null && selection.length() > 0)
                        WordLookup.getFrame().search( selection);
                }
            };
        UIUtilities.initAction( wordLookupAction, "main.menu.wordlookup");
        menu.add( UIUtilities.createMenuItem( wordLookupAction));
        menu.addSeparator();
        preferencesItem = UIUtilities.createMenuItem( PreferencesFrame.showAction);
        menu.add( preferencesItem);
        bar.add( menu);

        compactViewItem = new JCheckBoxMenuItem( JGloss.messages.getString( "main.menu.compactview"));
        compactViewItem.setSelected( JGloss.prefs.getBoolean( Preferences.VIEW_COMPACTVIEW, false));
        compactViewItem.setToolTipText( JGloss.messages.getString( "main.menu.compactview.tt"));
        compactViewItem.addActionListener( this);
        showReadingItem = new JCheckBoxMenuItem( JGloss.messages.getString( "main.menu.showreading"));
        showReadingItem.setSelected( JGloss.prefs.getBoolean( Preferences.VIEW_SHOWREADING, true));
        showReadingItem.setToolTipText( JGloss.messages.getString( "main.menu.showreading.tt"));
        showReadingItem.addActionListener( this);
        showTranslationItem = new JCheckBoxMenuItem( JGloss.messages.getString
                                                     ( "main.menu.showtranslation"));
        showTranslationItem.setSelected( JGloss.prefs.getBoolean
                                         ( Preferences.VIEW_SHOWTRANSLATION, true));
        showTranslationItem.setToolTipText( JGloss.messages.getString( "main.menu.showtranslation.tt"));
        showTranslationItem.addActionListener( this);
        showAnnotationItem = new JCheckBoxMenuItem( JGloss.messages.getString
                                                    ( "main.menu.showannotation"));
        showAnnotationItem.setSelected( JGloss.prefs.getBoolean
                                        ( Preferences.VIEW_SHOWANNOTATION, false));
        showAnnotationItem.setToolTipText( JGloss.messages.getString( "main.menu.showannotation.tt"));
        showAnnotationItem.addActionListener( this);

        menu = new JMenu( JGloss.messages.getString( "main.menu.view"));
        menu.add( compactViewItem);
        menu.add( showReadingItem);
        menu.add( showTranslationItem);
        menu.add( showAnnotationItem);
        bar.add( menu);

        bar.add( annotationEditor.getMenu());
        
        menu = new JMenu( JGloss.messages.getString( "main.menu.help"));
        aboutItem = UIUtilities.createMenuItem( AboutFrame.showAction);
        menu.add( aboutItem);
        bar.add( menu);
        
        setJMenuBar( bar);

        show();
    }

    /**
     * Checks if it is OK to close the document. If the user has changed the document,
     * a dialog will ask the user if the changes should be saved and the appropriate
     * actions will be taken. If after this the document can be closed, the
     * method will return <CODE>true</CODE>.
     *
     * @return <CODE>true</CODE>, if the document can be closed.
     */
    private boolean askCloseDocument() {
        if (documentChanged) {
            int r = JOptionPane.showOptionDialog
                ( this, JGloss.messages.getString( "main.dialog.close.message",
                                                   new Object[] { documentName }),
                  JGloss.messages.getString( "main.dialog.close.title"),
                  JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                  new Object[] { JGloss.messages.getString( "button.save"),
                                 JGloss.messages.getString( "button.discard"),
                                 JGloss.messages.getString( "button.cancel") },
                  JGloss.messages.getString( "button.save"));
            switch (r) {
            case 0: // save
                if (documentPath == null)
                    saveDocumentAs(); // does not clear documentChanged if cancelled
                else
                    saveDocument();
                break;

            case 1: // discard
                documentChanged = false;
                break;

            case 2: // cancel
            default: // probably CLOSED_OPTION
                // leave documentChanged untouched to prevent closing the window
                break;
            }                                   
        }

        return !documentChanged;
    }

    /**
     * Close the document window and clean up associated resources.
     */
    private void closeDocument() {
        // save the currently selected node in the preferences
        if (documentPath != null) {
            int index = ((javax.swing.tree.DefaultTreeModel) annotationEditor.getModel()).getIndexOfChild
                ( annotationEditor.getModel().getRoot(), annotationEditor.getLastSelectedAnnotation());
            if (index != -1) {
                StringBuffer history = new StringBuffer();
                String[] oldHistory = JGloss.prefs.getList( Preferences.HISTORY_SELECTION, 
                                                            File.pathSeparatorChar);
                // Copy from the old history all files which are not the current file.
                // Limit the size of the copied history to HISTORY_SIZE-1 by leaving out the
                // last entry to ensure that there is room for the new entry.
                int maxsize = JGloss.prefs.getInt( Preferences.HISTORY_SIZE, 20);
                for ( int i=0; i<oldHistory.length && i<(maxsize-1)*2; i+=2) try {
                    if (!oldHistory[i].equals( documentPath)) {
                        if (history.length() > 0)
                            history.append( File.pathSeparatorChar);
                        history.append( oldHistory[i]);
                        history.append( File.pathSeparatorChar);
                        history.append( oldHistory[i+1]);
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {}
                
                // create the new history entry
                if (history.length() > 0)
                    history.insert( 0, File.pathSeparatorChar);
                history.insert( 0, index);
                history.insert( 0, File.pathSeparatorChar);
                history.insert( 0, documentPath);
                JGloss.prefs.set( Preferences.HISTORY_SELECTION, history.toString());
            }
        }

        // close the window
        hide();
        dispose();
    }

    /**
     * Imports the content of the clipboard, if it contains plain text.
     */
    private void doImportClipboard() {
        Transferable t = getToolkit().getSystemClipboard().getContents( this);

        if (t != null) {
            try {
                Reader in = null;
                int len = 0;
                String data = (String) t.getTransferData( DataFlavor.stringFlavor);
                len = data.length();

                // try to autodetect the character encoding if the transfer didn't honor the 
                // charset correctly.
                boolean autodetect = true;
                for ( int i=0; i<data.length(); i++) {
                    if (data.charAt( i) > 255) {
                        // The string contains a character outside the ISO-8859-1 range,
                        // so presumably the transfer went OK.
                        autodetect = false;
                        break;
                    }
                }
                if (autodetect) {
                    byte[] bytes = data.getBytes( "ISO-8859-1");
                    String enc = CharacterEncodingDetector.guessEncodingName( bytes);
                    if (!enc.equals( CharacterEncodingDetector.ENC_UTF_8)) // don't trust UTF-8 detection
                        data = new String( bytes, enc);
                }

                in = new StringReader( data);

                if (in != null) {
                    JGlossFrame which = this;
                    if (documentLoaded)
                        which = new JGlossFrame();
                    
                    which.loadDocument
                        ( new HTMLifyReader( in),
                          JGloss.messages.getString( "import.clipboard"),
                          JGloss.messages.getString( "import.clipboard"),
                          GeneralDialog.getComponent().createImportClipboardParser
                          ( Dictionaries.getDictionaries( true), ExclusionList.getExclusions()),
                          GeneralDialog.getComponent().createReadingAnnotationFilter(),
                          true, len);
                    which.documentChanged = true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( this, JGloss.messages.getString
                      ( "error.import.exception", new Object[] 
                          { "import.clipboard", ex.getClass().getName(),
                            ex.getLocalizedMessage() }),
                      JGloss.messages.getString( "error.import.title"),
                      JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Sets up everything neccessary to import a file and loads it. If <CODE>filename</CODE> is
     * a URL, it will create a reader which reads from the location the document points to. If it
     * is a path to a local file, it will create a reader which reads from it. In both cases, 
     * if the document is not already in HTML format, it will be 
     * converted to HTML. The method will then call <CODE>loadDocument</CODE> with the newly created reader.
     *
     * @param path URL or path of the file to import.
     * @param parser Parser used to annotate the text.
     * @param filter Filter for fetching the reading annotations from a parsed document.
     * @param encoding Character encoding of the file. May be either <CODE>null</CODE> or the
     *                 value of the "encodings.default" resource to use autodetection.
     */
    private void importDocument( String path, Parser parser, ReadingAnnotationFilter filter, 
                                 String encoding) {
        try {
            Reader in = null;
            String contenttype = "text/plain";
            int contentlength = 0;
            if (JGloss.messages.getString( "encodings.default").equals( encoding))
                encoding = null; // autodetect the encoding
            String title = "";

            try {
                URL url = new URL( path);
                URLConnection c = url.openConnection();
                contentlength = c.getContentLength();
                contenttype = c.getContentType();
                String enc = c.getContentEncoding();
                InputStream is = c.getInputStream();
                // a user-selected value for encoding overrides enc
                if (encoding != null) // use user-selected encoding
                    in = new InputStreamReader( c.getInputStream(), encoding);
                else { // auto-detect, works even if enc==null
                    in = CharacterEncodingDetector.getReader( c.getInputStream(), enc);
                    encoding = ((InputStreamReader) in).getEncoding();
                }
                title = url.getFile();
                if (title==null || title.length()==0)
                    title = path;
            } catch (MalformedURLException ex) {
                // probably a local file
                File f = new File( path);
                contentlength = (int) f.length();
                title = f.getName();
                if (title.toLowerCase().endsWith( "htm") || title.toLowerCase().endsWith( "html"))
                    contenttype = "text/html";
                InputStream is = new FileInputStream( path);
                if (encoding != null) // use user-selected encoding
                    in = new InputStreamReader( is, encoding);
                else { // auto-detect
                    in = CharacterEncodingDetector.getReader( is);
                    encoding = ((InputStreamReader) in).getEncoding();
                }
            }

            if (!contenttype.startsWith( "text/html"))
                in = new HTMLifyReader( in);

            loadDocument( in, path, title, parser, filter, true,
                          CharacterEncodingDetector.guessLength( contentlength, encoding));
            if (contenttype.startsWith( "text/html")) {
                JOptionPane.showMessageDialog( this, JGloss.messages.getString( "error.import.htmlwarning"),
                                               JGloss.messages.getString( "error.import.htmlwarning.title"),
                                               JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( this, JGloss.messages.getString
                  ( "error.import.exception", new Object[] 
                      { path, ex.getClass().getName(),
                        ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.import.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            if (documentName == null)
                // error before document was opened, close window
                this.dispose();
        }
    }

    /**
     * Creates a new annotated document by reading the original text from a string.
     * The method can only be applied on a <CODE>JGlossFrame</CODE> with no open document.
     *
     * @param text The text which will be imported.
     * @param title Title of the newly created document.
     * @param path Path to the document.
     * @param setPath If <CODE>true</CODE>, the {@link #documentPath documentPath} variable will
     *        be set to the <CODE>path</CODE> parameter. Use this if path denotes a the file to
     *        which the newly created document should be written. If <CODE>false</CODE>,
     *        <CODE>path</CODE> will only be used in informational messages to the user during import.
     */
    public void importString( String text, Parser parser, ReadingAnnotationFilter filter, String title,
                              String path, boolean setPath) {
        try {
            loadDocument
                ( new HTMLifyReader( new StringReader( text)), path, title,
                  parser, filter, true, text.length());
            documentChanged = true;
            if (setPath)
                this.documentPath = path;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( this, JGloss.messages.getString
                  ( "error.import.exception", new Object[] 
                      { path, ex.getClass().getName(),
                        ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.import.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads a JGloss document from a local file.
     *
     * @param f File to load.
     */
    public void loadDocument( File f) {
        try {
            Reader in = new InputStreamReader( new FileInputStream( f), "UTF-8");
            documentPath = f.getAbsolutePath();
            loadDocument( in, documentPath, f.getName(), 
                          null, null, false,
                          CharacterEncodingDetector.guessLength( (int) f.length(), "UTF-8"));

            synchronized (this) { // prevent closing of document while post-processing
                if (doc == null) {
                    // document window was closed by the user while document was loading
                    return;
                }

                // test the file version of the loaded document
                String formatWarning = null;
                if (doc.getJGlossVersion() != -1) {
                    if (doc.getFileFormatMajorVersion() > JGlossWriter.FILE_FORMAT_MAJOR_VERSION)
                        formatWarning = JGloss.messages.getString
                            ( "warning.load.version.larger.major",
                              new Object[] { new Integer( doc.getJGlossVersion()/100),
                                             new Integer( doc.getJGlossVersion()%100) });
                    else if (doc.getFileFormatMajorVersion() == JGlossWriter.FILE_FORMAT_MAJOR_VERSION &&
                             doc.getFileFormatMinorVersion() > JGlossWriter.FILE_FORMAT_MINOR_VERSION)
                        formatWarning = JGloss.messages.getString
                            ( "warning.load.version.larger.minor",
                              new Object[] { new Integer( doc.getJGlossVersion()/100),
                                             new Integer( doc.getJGlossVersion()%100) });
                    else if (doc.getFileFormatMajorVersion() < JGlossWriter.FILE_FORMAT_MAJOR_VERSION ||
                             doc.getFileFormatMajorVersion() == JGlossWriter.FILE_FORMAT_MAJOR_VERSION &&
                             doc.getFileFormatMinorVersion() < JGlossWriter.FILE_FORMAT_MINOR_VERSION)
                        formatWarning = JGloss.messages.getString
                            ( "warning.load.version.smaller");
                    // else format == current format
                }
                else
                    formatWarning = JGloss.messages.getString( "warning.load.notjgloss");
                
                if (formatWarning != null) {
                    JOptionPane.showConfirmDialog
                        ( this, formatWarning,
                          JGloss.messages.getString( "warning.load.title"),
                          JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
                    // force a save as since the document format has changed
                    documentPath = null;
                    documentChanged = true;
                    saveAction.setEnabled( true); // will behave like save as
                }
                OPEN_RECENT.addDocument( f);

                // re-select the selection at the time the document was closed
                String[] history = JGloss.prefs.getList( Preferences.HISTORY_SELECTION, 
                                                         File.pathSeparatorChar);
                for ( int i=0; i<history.length; i+=2) try {
                    if (history[i].equals( documentPath)) {
                        int index = Integer.parseInt( history[i+1]);
                        final AnnotationNode annotation = (AnnotationNode) annotationEditor.getModel()
                            .getChild( annotationEditor.getModel().getRoot(), index);
                        Runnable worker = new Runnable() {
                                public void run() {
                                    annotationEditor.makeVisible( annotation);
                                    annotationEditor.selectNode( annotation);
                                }
                            };
                        if (EventQueue.isDispatchThread())
                            worker.run();
                        else
                            EventQueue.invokeLater( worker);
                        break;
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                } catch (NumberFormatException ex2) {
                } catch (NullPointerException ex3) {}
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( this, JGloss.messages.getString
                  ( "error.load.exception", new Object[] 
                      { documentPath, ex.getClass().getName(), ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.load.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads a document into this JGlossFrame. This will completely set up the frame for editing.
     * This can be used to either open a JGloss document or to import some Japanese text, depending
     * of the setting of <CODE>addAnnotations</CODE>. The document must be in JGloss or HTML format.
     *
     * @param in The reader from which to load the document.
     * @param path The location of the document which was used to create the reader.
     * @param title Title of the file. This will normally be the filename component of the path.
     * @param parser The parser to use to annotate Japanese text.
     * @param filter Filter for fetching the reading annotations from a parsed document.
     * @param addAnnotations <CODE>true</CODE> if annotations should be added while loading the
     *                       document.
     * @param length Approximate length in characters of the document to load. This will only be used
     *               for the progress bar.
     */
    private void loadDocument( Reader in, String path, String title, Parser parser, 
                               ReadingAnnotationFilter filter,
                               boolean addAnnotations, int length) 
        throws IOException {
        // Prevent the windowClosing event from directly closing the window.
        // If a windowClosing event is registered while the loadDocument method is executing,
        // the deferWindowClosing flag will be cleared.
        deferWindowClosing = true;
        documentLoaded = true;

        kit = new JGlossEditorKit( parser, filter, addAnnotations, compactViewItem.isSelected(),
                                   showReadingItem.isSelected(),
                                   showTranslationItem.isSelected());
        doc = (JGlossDocument) kit.createDefaultDocument();
        StyleDialog.getComponent().addStyleSheet( doc.getStyleSheet(), Collections.EMPTY_MAP);

        documentName = title;

        final StopableReader stin = new StopableReader( in);

        final ProgressMonitor pm = new ProgressMonitor
            ( this, JGloss.messages.getString( "load.progress", 
                                               new Object[] { path }),
              null, 0, length);
        final Thread currentThread = Thread.currentThread(); // needed to interrupt parsing if user cancels
        javax.swing.Timer progressUpdater = new javax.swing.Timer( 1000, new ActionListener() {
                public void actionPerformed( ActionEvent e) {
                    // this handler called from the event dispatching thread
                    pm.setProgress( ((JGlossDocument) doc).getParsePosition());
                    if (pm.isCanceled() || // cancel button of progress bar pressed
                        !deferWindowClosing) { // close button of document frame pressed
                        stin.stop();
                        currentThread.interrupt();
                    }
                }
            });
        progressUpdater.start();

        try {
            kit.read( stin, doc, 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( JGlossFrame.this, JGloss.messages.getString
                  ( "error.import.exception", new Object[] 
                      { path, ex.getClass().getName(), ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.import.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }

        progressUpdater.stop();
        in.close();
        pm.close();

        if (!deferWindowClosing) {
            // close button of document frame pressed while document was loading
            closeDocument();
            return;
        }

        updateTitle();
        // get notified of title changes
        doc.addPropertyChangeListener( new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent e) {
                    markChanged();
                }
            });

        Runnable worker = new Runnable() {
                public void run() {
                    final Cursor currentCursor = getCursor();
                    setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR));

                    docpane.setEditorKit( kit);
                    docpane.setStyledDocument( doc);
                    annotationEditor.setDocument( doc.getDefaultRootElement(), docpane);
                    
                    getContentPane().removeAll();
                    getContentPane().add( split);
                    validate();
                    if (JGloss.prefs.getBoolean( Preferences.VIEW_ANNOTATIONEDITORHIDDEN, false))
                        split.setDividerLocation( 1.0f);
                    else {
                        double position = JGloss.prefs.getDouble
                            ( Preferences.VIEW_DIVIDERLOCATION, 0.7d);
                        split.setDividerLocation( position);
                        split.setResizeWeight( position);
                    }
                    
                    // Layout document view in the background while still allowing user interaction
                    // in other windows. Since Swing is single-threaded, this is somewhat complicated:
                    // 1. A renderer thread is created, which prepares the docpane asynchronously to
                    //    the event dispatch thread.
                    // 2. After the preparation is done (this is the part that uses the most time), the
                    //    docpane is installed in the docpaneScroller. This is done in the event dispatch
                    //    thread
                    // 3. The renderer thread is only started after the document window is drawn with
                    //    the "Creating Document View" notice in the document window. To do this,
                    //    renderer.start is wrapped in its own runnable and invoked later in the
                    //    event thread
                    final Thread renderer = new Thread() {
                            public void run() {
                                try {
                                    setPriority( Math.max( getPriority()-3, Thread.MIN_PRIORITY));
                                } catch (IllegalArgumentException ex) {}
                                // the user may close the document before or during rendering,
                                // so make sure that docpane is not set to null
                                JGlossEditor dp = docpane;
                                final JViewport port = new JViewport();
                                if (dp != null) {
                                    synchronized (dp) {
                                        // the docpane is not visible, and we are synched on it,
                                        // so it is safe to set the size even though this is not 
                                        // the event dispatch thread
                                        dp.setSize( docpaneScroller.getViewport().getExtentSize().width,
                                                    docpane.getPreferredSize().height);
                                        // now the docpane is set to the correct width, call method again
                                        // to set to correct height for the applied width
                                        dp.setSize( docpaneScroller.getViewport().getExtentSize().width,
                                                    docpane.getPreferredSize().height);
                                        port.setView( dp);
                                    }
                                }
                                // installing the docpane in the scroller, which is already visible,
                                // has to be done in the event dispatch thread
                                Runnable installer = new Runnable() {
                                        public void run() {
                                            synchronized (JGlossFrame.this) {
                                                // dispose might already have been called, test if
                                                // member variables still exist
                                                if (docpaneScroller!=null && 
                                                    docpane!=null && 
                                                    annotationEditor!=null) {
                                                    docpaneScroller.setViewport( port);
                                                    validate();
                                                    docpane.followMouse( showAnnotationItem.isSelected(),
                                                                         false);
                                                    // scroll to selected annotation
                                                    AnnotationNode current = annotationEditor
                                                        .getSelectedAnnotation();
                                                    if (current != null) {
                                                        docpane.makeVisible( current.getAnnotationElement().
                                                                             getStartOffset(),
                                                                             current.getAnnotationElement().
                                                                             getEndOffset());
                                                    }
                                                    setCursor( currentCursor);
                                                }
                                            }
                                        }
                                    };
                                EventQueue.invokeLater( installer);
                            }
                        };
                    // defer rendering the document until after the window is painted
                    Runnable rendererStart = new Runnable() {
                            public void run() {
                                renderer.start();
                            }
                        };
                    EventQueue.invokeLater( rendererStart);

                    // Parser must be set to non-strict mode for editing to work.
                    doc.setStrictParsing( false);

                    doc.setAddAnnotations( false);
                    exportMenu.setEnabled( true);
                    exportPlainTextAction.setEnabled( true);
                    exportLaTeXAction.setEnabled( true);
                    exportHTMLAction.setEnabled( true);
                    exportAnnotationListAction.setEnabled( true);
                    printAction.setEnabled( true);
                    if (documentPath == null) 
                        // this means that the document is imported, save will behave like save as
                        saveAction.setEnabled( true);
                    saveAsAction.setEnabled( true);

                    // mark document as changed if some editing occurs
                    doc.addDocumentListener( new DocumentListener() {
                            public void insertUpdate(DocumentEvent e) {
                                markChanged();
                            }
                            public void removeUpdate(DocumentEvent e) {
                                markChanged();
                            }
                            public void changedUpdate(DocumentEvent e) {
                                // triggered by style changes, don't react to this
                            }
                        });

                    // save splitpane location settings in the preferences
                    split.addPropertyChangeListener( new PropertyChangeListener() {
                            public void propertyChange( PropertyChangeEvent e) {
                                if (split.DIVIDER_LOCATION_PROPERTY.equals( e.getPropertyName())) {
                                    int newPosition = ((Integer) e.getNewValue()).intValue();
                                    if (newPosition >= split.getMaximumDividerLocation())
                                        JGloss.prefs.set( Preferences.VIEW_ANNOTATIONEDITORHIDDEN,
                                                          true);
                                    else {
                                        JGloss.prefs.set( Preferences.VIEW_ANNOTATIONEDITORHIDDEN,
                                                          false);
                                        JGloss.prefs.set( Preferences.VIEW_DIVIDERLOCATION,
                                                          ((double) newPosition)/
                                                          split.getWidth());
                                    }
                                }
                            }
                        });
                }
            };
        if (EventQueue.isDispatchThread()) {
            worker.run();
        }
        else {
            try {
                EventQueue.invokeAndWait( worker);
            } catch (InterruptedException ex) {
                // What? Should not happen.
                ex.printStackTrace();
            } catch (java.lang.reflect.InvocationTargetException ex) {
                if (ex.getTargetException() instanceof IOException)
                    throw (IOException) ex.getTargetException();
                else if (ex.getTargetException() instanceof RuntimeException)
                    throw (RuntimeException) ex.getTargetException();
                else // should not happen
                    ex.printStackTrace();
            }
        }

        if (!deferWindowClosing) {
            // close button of document frame pressed while document was loading
            closeDocument();
        }
        deferWindowClosing = false;
    }

    /**
     * Executes the appropriate action for a selection in the view menu.
     *
     * @param e The action event.
     */
    public void actionPerformed( ActionEvent e) {
        if (kit != null) {
            if (e.getSource() == compactViewItem) {
                JGloss.prefs.set( Preferences.VIEW_COMPACTVIEW, compactViewItem.isSelected());
                kit.setCompactView( compactViewItem.isSelected());
                // force docpane to be re-layouted.
                doc.getStyleSheet().addRule( AnnotationTags.ANNOTATION.getId() + " {}");
            }
            else if (e.getSource()==showReadingItem) {
                JGloss.prefs.set( Preferences.VIEW_SHOWREADING, showReadingItem.isSelected());
                kit.showReading( showReadingItem.isSelected());
                // force docpane to be re-layouted.
                doc.getStyleSheet().addRule( AnnotationTags.READING.getId() + " {}");
            }
            else if (e.getSource()==showTranslationItem) {
                JGloss.prefs.set( Preferences.VIEW_SHOWTRANSLATION, showTranslationItem.isSelected());
                kit.showTranslation( showTranslationItem.isSelected());
                // force docpane to be re-layouted.
                doc.getStyleSheet().addRule( AnnotationTags.TRANSLATION.getId() + " {}");
            }
            else if (e.getSource()==showAnnotationItem) {
                JGloss.prefs.set( Preferences.VIEW_SHOWANNOTATION, showAnnotationItem.isSelected());
                docpane.followMouse( showAnnotationItem.isSelected(), false);
            }
        }
    }

    /**
     * Runs the print dialog and prints the document.
     */
    private void doPrint() {
        if (kit != null) {
            // run the print dialog
            JobAttributes ja = new JobAttributes();
            PageAttributes pa = new PageAttributes();
            pa.setOrigin( PageAttributes.OriginType.PRINTABLE);
            PrintJob job = getToolkit().getPrintJob
                ( this, documentName, ja, pa);

            if (job != null) {
                // do the printing

                Dimension page = job.getPageDimension();
                Rectangle pagebounds = new Rectangle( 0, 0, page.width, page.height);
                docpaneScroller.getViewport().remove( docpane);
                docpane.setLocation( 0, 0);
                // set the width of the page, ignore the height for now
                docpane.setSize( page.width, docpane.getPreferredSize().height);
                // keep width, set preferred height which as I hope is now calculated
                // according to the previously set width
                docpane.setSize( page.width, docpane.getPreferredSize().height);
                Rectangle docbounds = docpane.getBounds();
                View root = docpane.getUI().getRootView( docpane);
                root.setSize( page.width, page.height); // make sure that the layout is correct
                int pagecount = 0;
                int firstpage = 0;
                int lastpage = Integer.MAX_VALUE;
                if (ja.getDefaultSelection().equals( JobAttributes.DefaultSelectionType.RANGE)) {
                    firstpage = ja.getFromPage();
                    lastpage = ja.getToPage();
                }
                for ( int copies=0; copies<ja.getCopies(); copies++) {
                    pagebounds.y = 0;
                    while (pagebounds.y<docbounds.height) {
                        // calculate the y position of the last fully visible line on this page
                        int nh = 0;
                        View v = root;
                        Rectangle r = docbounds;
                        int i = 0;
                        int c = 0;
                        while (i < v.getViewCount()) {
                            View cv = v.getView( i);
                            Shape s = v.getChildAllocation( i, r);
                            Rectangle cr;
                            if (s == null) { // invisible view? It does happen.
                                i++;
                                continue;
                            }
                            else if (s instanceof Rectangle)
                                cr = (Rectangle) s;
                            else
                                cr = s.getBounds();
                            cr.y += 3; // correct a weird layout problem
                            
                            if (cr.y < pagebounds.y+page.height) {
                                // part of the view lies in the page bounds
                                if (cr.y+cr.height-1 < pagebounds.y) {
                                    // view lies above current page: continue
                                }
                                else if (cr.y+cr.height < pagebounds.y+page.height) {
                                    // view lies in the page bounds
                                    nh = cr.y-pagebounds.y+cr.height; // bottom of this view
                                }
                                else if (cv.getElement() instanceof JGlossEditorKit.AnnotationView) {
                                    // don't break it apart
                                    break;
                                }
                                else { // part of the view lies below the page bounds
                                    // check if child views lie in the page bounds
                                    // note that the loop will terminate with the first leaf view
                                    // which is only partially visible, and not test all leafs.
                                    i = -1; // will be incremented at the end of the loop
                                    v = cv;
                                    r = cr;
                                    r.y -= 3; // correct a weird layout problem
                                }
                            }
                            else // all following child views are below page
                                break;
                            i++;
                        }
                    
                        if (nh == 0) {
                            // this page either is empty (contains no views), or all views on this page
                            // don't fit
                            nh = page.height;
                        }
                        pagebounds.height = nh;
                        
                        pagecount++;
                        if (pagecount > lastpage)
                            break;
                        
                        if (pagecount >= firstpage) {
                            // render the current page
                            Graphics g = job.getGraphics();
                            if (g != null) {
                                g.setClip( 0, 0, pagebounds.width, pagebounds.height);
                                g.translate( 0, -pagebounds.y);
                                docpane.printAll( g);
                                /* Debug page end calculation
                                   c = 0;
                                   for ( Iterator it=coords.iterator(); it.hasNext(); ) {
                                   Rectangle r2 = (Rectangle) it.next();
                                   g.setColor( Color.red);
                                   c++;
                                   g.drawLine( r2.x, r2.y+1, r2.x+r2.width+-1, r2.y+1);
                                   g.drawString( Integer.toString( c), r2.x+r2.width-15, r2.y+15);
                                   g.setColor( Color.green);
                                   g.drawLine( r2.x, r2.y+r2.height, r2.x+r2.width-1, r2.y+r2.height);
                                   g.drawString( Integer.toString( c), r2.x+5, r2.y+r2.height-10);
                                   }
                                */
                                g.dispose();
                            }
                        }

                        pagebounds.y += pagebounds.height;
                    }
                }
                job.end();
                docpaneScroller.getViewport().setView( docpane);
            }
        }
        
    }

    /**
     * Saves the document in JGloss format. This is basically HTML using a UTF-8 character
     * encoding.
     *
     * @return <CODE>true</CODE> if the document was successfully saved.
     */
    private boolean saveDocument() {
        try {
            Writer out = new OutputStreamWriter( new FileOutputStream( documentPath), "UTF-8");
            JGlossWriter w = new JGlossWriter( out, "UTF-8", doc);
            w.write();
            out.close();
            documentChanged = false;
            saveAction.setEnabled( false);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( this, JGloss.messages.getString
                  ( "error.save.exception", new Object[] 
                      { documentPath, ex.getClass().getName(), ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.save.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Runs a file chooser dialog, and if the user accepts saves the document under the new
     * filename.
     */
    private void saveDocumentAs() {
        String path;
        if (documentPath == null)
            path = JGloss.getCurrentDir();
        else
            path = new File( documentPath).getPath();
        JFileChooser f = new SaveFileChooser( path);
        f.setFileHidingEnabled( true);
        f.addChoosableFileFilter( jglossFileFilter);
        f.setFileView( CustomFileView.getFileView());
        int r = f.showSaveDialog( this);
        if (r == JFileChooser.APPROVE_OPTION) {
            documentPath =  f.getSelectedFile().getAbsolutePath();
            documentName = f.getSelectedFile().getName();
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            setTitle( documentName + 
                      ":" + JGloss.messages.getString( "main.title"));
            if (saveDocument())
                OPEN_RECENT.addDocument( f.getSelectedFile());
        }
    }

    /**
     * Marks the document as changed and updates the save action accordingly.
     */
    private void markChanged() {
        if (!documentChanged) {
            documentChanged = true;
            if (documentPath != null)
                saveAction.setEnabled( true);
        }
    }

    /**
     * Exports the document as plain text to a user-specified file.
     */
    private void doExportPlainText() {
        ExportFileChooser f = new ExportFileChooser( JGloss.getCurrentDir(),
                                                     JGloss.messages.getString( "export.plaintext.title"));

        f.addElement( ExportFileChooser.ENCODING_CHOOSER, Preferences.EXPORT_PLAINTEXT_ENCODING);
        f.addElement( ExportFileChooser.WRITE_READING, Preferences.EXPORT_PLAINTEXT_WRITEREADING);
        f.addElement( ExportFileChooser.WRITE_TRANSLATIONS, Preferences.EXPORT_PLAINTEXT_WRITETRANSLATIONS);
        f.addElement( ExportFileChooser.WRITE_HIDDEN, Preferences.EXPORT_PLAINTEXT_WRITEHIDDEN);

        int r = f.showSaveDialog( this);
        if (r == JFileChooser.APPROVE_OPTION) {
            Writer out = null;
            try {
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()),
                      f.getEncoding()));
                PlainTextExporter.export( doc, (AnnotationModel) annotationEditor.getModel(),
                                          out, f.getWriteReading(), f.getWriteTranslations(),
                                          f.getWriteHidden());
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( this, JGloss.messages.getString
                      ( "error.export.exception", new Object[] 
                          { documentPath, ex.getClass().getName(),
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
     * Exports the document as plain text to a user-specified file.
     */
    private void doExportLaTeX() {
        LaTeXExportFileChooser f = new LaTeXExportFileChooser( JGloss.getCurrentDir());

        f.addElement( ExportFileChooser.WRITE_HIDDEN, Preferences.EXPORT_LATEX_WRITEHIDDEN);

        int r = f.showSaveDialog( this);
        if (r == JFileChooser.APPROVE_OPTION) {
            InputStreamReader template = null;
            Writer out = null;
            try {
                template = f.getTemplate();
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()), template.getEncoding()));
                new LaTeXExporter().export
                    ( f.getTemplate(), doc, documentName, f.getFontSize(), 
                      (AnnotationModel) annotationEditor.getModel(),
                      out, template.getEncoding(), f.getWriteHidden());
            } catch (Exception ex) {
                if (ex instanceof TemplateException) {
                    JOptionPane.showConfirmDialog
                        ( this, ex.getMessage(),
                          JGloss.messages.getString( "error.export.title"),
                          JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
                else {
                    ex.printStackTrace();
                    JOptionPane.showConfirmDialog
                        ( this, JGloss.messages.getString
                          ( "error.export.exception", new Object[] 
                              { documentPath, ex.getClass().getName(),
                                ex.getLocalizedMessage() }),
                          JGloss.messages.getString( "error.export.title"),
                          JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                }
            } finally {
                if (template != null) try {
                    template.close();
                } catch (IOException ex) { ex.printStackTrace(); }
                if (out != null) try {
                    out.close();
                } catch (IOException ex) { ex.printStackTrace(); }
            }
        }        
    }

    /**
     * Exports the document as HTML to a user-specified file.
     */
    private void doExportHTML() {
        ExportFileChooser f = new ExportFileChooser( JGloss.getCurrentDir(),
                                                     JGloss.messages.getString( "export.html.title"));

        f.addElement( ExportFileChooser.ENCODING_CHOOSER, Preferences.EXPORT_HTML_ENCODING);
        f.addElement( ExportFileChooser.WRITE_READING, Preferences.EXPORT_HTML_WRITEREADING);
        f.addElement( ExportFileChooser.WRITE_TRANSLATIONS, 
                      Preferences.EXPORT_HTML_WRITETRANSLATIONS);
        JCheckBox backwardsCompatible = 
            new JCheckBox( JGloss.messages.getString( "export.html.backwardscompatible"));        
        backwardsCompatible.setSelected( JGloss.prefs.getBoolean
                                         ( Preferences.EXPORT_HTML_BACKWARDSCOMPATIBLE, true));
        f.addCustomElement( backwardsCompatible);
        f.addElement( ExportFileChooser.WRITE_HIDDEN, Preferences.EXPORT_HTML_WRITEHIDDEN);

        f.setFileFilter( new ExtensionFileFilter
            ( "html", JGloss.messages.getString( "filefilter.description.html")));

        int r = f.showSaveDialog( this);
        if (r == JFileChooser.APPROVE_OPTION) {
            Writer out = null;

            // The document is modified during export. Save the original changed state.
            boolean originalDocumentChanged = documentChanged;
            try {
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()),
                      (String) f.getEncoding()));
                JGloss.prefs.set( Preferences.EXPORT_HTML_BACKWARDSCOMPATIBLE,
                                  backwardsCompatible.isSelected());
                new HTMLExporter( out, f.getEncoding(), doc,
                                  f.getWriteReading(), f.getWriteTranslations(),
                                  backwardsCompatible.isSelected(),
                                  f.getWriteHidden()).write();
                // restore document changed state
                if (originalDocumentChanged == false) {
                    documentChanged = false;
                    saveAction.setEnabled( false);
                }   
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( this, JGloss.messages.getString
                      ( "error.export.exception", new Object[] 
                          { documentPath, ex.getClass().getName(),
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
     * Exports the annotation list to a user-specified file.
     */
    private void doExportAnnotationList() {
        ExportFileChooser f = new ExportFileChooser( JGloss.getCurrentDir(),
                                                     JGloss.messages.getString
                                                     ( "export.annotationlist.title"));

        f.addElement( ExportFileChooser.ENCODING_CHOOSER, Preferences.EXPORT_ANNOTATIONLIST_ENCODING);

        int r = f.showSaveDialog( this);
        if (r == JFileChooser.APPROVE_OPTION) {
            Writer out = null;
            try {
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()),
                      f.getEncoding()));
                out.write( JGloss.messages.getString( "export.annotationlist.header",
                                                      new Object[] 
                    { documentName, f.getEncoding() }));
                // do not output duplicate annotations:
                Set seenAnnotations = new TreeSet();
                for ( Iterator i=((AnnotationModel) annotationEditor.getModel()).getAnnotationNodes();
                      i.hasNext(); ) {
                    AnnotationNode an = (AnnotationNode) i.next();
                    if (!an.isHidden()) {
                        String word = an.getDictionaryFormNode().getWord();
                        String reading = an.getDictionaryFormNode().getReading();
                        String translation = an.getTranslationNode().getText();

                        String line = word;
                        if (reading!=null && reading.length()>0)
                            line += " [" + reading + "]";
                        if (translation!=null && translation.length()>0)
                            line += " /" + translation + "/";
                        
                        if (!seenAnnotations.contains( line)) {
                            out.write( line);
                            out.write( '\n');
                            seenAnnotations.add( line);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( this, JGloss.messages.getString
                      ( "error.export.exception", new Object[] 
                          { documentPath, ex.getClass().getName(),
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
     * Update the document window title.
     */
    protected void updateTitle() {
        setTitle( documentName + ":" + JGloss.messages.getString( "main.title"));
    }

    /**
     * Dispose resources associated with the JGloss document.
     */
    public synchronized void dispose() {
        // dump as many references as possible to ensure that the objects are garbage collected
        jglossFrames.remove( this);   
        JGloss.prefs.removePropertyChangeListener( prefsListener);
        if (doc != null)
            StyleDialog.getComponent().removeStyleSheet( doc.getStyleSheet());
        docpane.dispose();
        preferencesItem.setAction( null);
        aboutItem.setAction( null);
        docpane = null;
        doc = null;
        kit = null;
        annotationEditor = null;
        split = null;
        docpaneScroller = null;
        annotationEditorScroller = null;
        getContentPane().removeAll();
        OPEN_RECENT.removeMenu( openRecentMenu);
        openRecentMenu = null;
        super.dispose();
    }
} // class JGlossFrame
