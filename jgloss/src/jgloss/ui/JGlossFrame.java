/*
 * Copyright (C) 2001-2003 Michael Koch (tensberg@gmx.net)
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

import jgloss.JGloss;
import jgloss.JGlossApp;
import jgloss.Preferences;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;
import jgloss.dictionary.DictionaryEntry;
import jgloss.ui.annotation.Annotation;
import jgloss.ui.annotation.AnnotationListModel;
import jgloss.ui.export.ExportMenu;
import jgloss.ui.html.AnnotationTags;
import jgloss.ui.html.JGlossEditor;
import jgloss.ui.html.JGlossEditorKit;
import jgloss.ui.html.JGlossHTMLDoc;
import jgloss.ui.html.AnnotationListSynchronizer;
import jgloss.ui.html.SelectedAnnotationHighlighter;
import jgloss.ui.xml.JGlossDocument;
import jgloss.ui.xml.JGlossDocumentBuilder;
import jgloss.util.CharacterEncodingDetector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.JobAttributes;
import java.awt.PageAttributes;
import java.awt.PrintJob;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.ProgressMonitor;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.Position;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Frame which contains everything needed to edit a single JGloss document.
 *
 * @author Michael Koch
 */
public class JGlossFrame extends JPanel implements ActionListener, ListSelectionListener,
                                                   HyperlinkListener, CaretListener {
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
        public final ImportClipboardListener importClipboardListener;
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
                                    ImportDialog d = new ImportDialog( target != null ? 
                                                                       target.frame :
                                                                       null);
                                    if (d.doDialog()) {
                                        JGlossFrame which;
                                        if (target==null || target.model.isEmpty())
                                            which = new JGlossFrame();
                                        else
                                            which = target;
                                        
                                        if (d.selectionIsFilename())
                                            which.importDocument
                                                ( d.getSelection(), d.isDetectParagraphs(),
                                                  d.createParser( Dictionaries.getDictionaries( true),
                                                                  ExclusionList.getExclusions()),
                                                  d.createReadingAnnotationFilter(),
                                                  d.getEncoding());
                                        else
                                            which.importString
                                                ( d.getSelection(), d.isDetectParagraphs(), 
                                                  JGloss.messages.getString( "import.textarea"),
                                                  JGloss.messages.getString( "import.textarea"),
                                                  d.createParser( Dictionaries.getDictionaries( true),
                                                                  ExclusionList.getExclusions()),
                                                  d.createReadingAnnotationFilter(),
                                                  false);
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
                                            if (path.equals( next.model.getDocumentPath())) {
                                                next.frame.show();
                                                return;
                                            }
                                        }
                                        
                                        // load the file
                                        JGlossFrame which = target==null ||
                                            target.model.isEmpty() ? new JGlossFrame() : target;
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
                            if (path.equals( next.model.getDocumentPath())) {
                                next.frame.show();
                                return;
                            }
                        }
                        
                        // load the file asynchronously
                        new Thread() {
                                public void run() {
                                    JGlossFrame which = target==null ||
                                        target.model.isEmpty() ? new JGlossFrame() : target;
                                    which.loadDocument( file);
                                }
                            }.start();
                    }
                };

            importClipboardListener = new ImportClipboardListener( importClipboard);
        }
    } // class Actions

    /**
     * Static instance of the actions which can be used by other classes. If an action
     * is invoked, a new <CODE>JGlossFrame</CODE> will be created as the target of the
     * action.
     */
    public final static Actions actions = new Actions( null);

    /**
     * Updates the status of the import clipboard action corresponding to certain events.
     * The import clipboard action should only be enabled if the system clipboard contains a
     * string. This listener checks the status of the clipboard and updates the action state if
     * the window the listener is attached to is brought to the foreground and/or if the menu the
     * listener is attached to is expanded.
     */
    private static class ImportClipboardListener extends WindowAdapter implements MenuListener {
        private Action importClipboard;

        public ImportClipboardListener( Action _importClipboard) {
            this.importClipboard = _importClipboard;
        }

        private void checkUpdate() {
            // enable the import clipboard menu item if the clipboard contains some text
            importClipboard.setEnabled( UIUtilities.clipboardContainsString());
        }

        public void windowActivated( WindowEvent e) {
            checkUpdate();
        }
        public void menuSelected( MenuEvent e) {
            checkUpdate();
        }
        public void menuDeselected( MenuEvent e) {}
        public void menuCanceled( MenuEvent e) {}
    } // class ImportClipboardListener

    /**
     * Open recent menu used for JGloss documents. The instance is shared between instances of
     * <CODE>JGlossFrame</CODE> and {@link LookupFrame LookupFrame}.
     */
    public final static OpenRecentMenu OPEN_RECENT = new OpenRecentMenu( 8);

    /**
     * Data model of this frame.
     */
    private JGlossFrameModel model;

    /**
     * JGloss document frame object. The frame keeps the <code>JGlossFrame</code> as sole component
     * in its content pane.
     */
    private JFrame frame;
    /**
     * Reacts to the document closed events.
     */
    private WindowListener windowListener;
    /**
     * Saves changes in window size to the preferences.
     */
    private ComponentListener componentListener;
    /**
     * The document editor.
     */
    private JGlossEditor docpane;
    /**
     * Scrollpane which contains the document editor.
     */
    private JScrollPane docpaneScroller;
    /**
     * Editor kit used in the creation of the document.
     */
    private JGlossEditorKit kit;
    private AnnotationList annotationList;
    private AnnotationEditorPanel annotationEditor;
    private SimpleLookup lookupPanel;
    private JSplitPane[] splitPanes;
    private LookupResultList.Hyperlinker hyperlinker;
    /**
     * Remembers the first dictionary entry shown in the result list of the lookup panel.
     * This is used when a new annotation is created to automatically set the reading and
     * translation.
     */
    private FirstEntryCache firstEntryCache;

    private Position lastSelectionStart;
    private Position lastSelectionEnd;
    
    /**
     * Defer a window closing event until the frame object is in a safe state. This is used while
     * the <CODE>loadDocument</CODE> method is executing.
     */
    private boolean deferWindowClosing = false;

    private Transformer jglossWriterTransformer;

    /**
     * Manager for the cut/copy/past actions;
     */
    private XCVManager xcvManager;

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
     * Action which annotates the current selection.
     */
    private Action addAnnotationAction;
    /**
     * Displays the document title and lets the user change it.
     */
    private Action documentTitleAction;
    /**
     * Brings the word lookup frame to the foreground and searches the current selection.
     */
    private Action wordLookupAction;

    /**
     * Open recent menu for this instance of <CODE>JGlossFrame</CODE>.
     */
    private JMenu openRecentMenu;
    /**
     * Submenu containing the export actions.
     */
    private ExportMenu exportMenu;
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
        jglossFrames.add( this);

        model = new JGlossFrameModel();

        // set up the frame
        /* The JGlossFrame does not directly inherit from
         * JFrame, because memory profiling has shown that the JFrame objects are not correctly garbage
         * collected as expected (at least in JDK1.3). To keep the state associated with the JFrame objects
         * to a minimum, they are kept separate from the JGlossFrame state.
         * See also JGlossFrame.dispose().
         */
        frame = new JFrame( JGloss.messages.getString( "main.title"));
        frame.getContentPane().setBackground( Color.white);
        frame.getContentPane().setLayout( new GridLayout( 1, 1));
        frame.setLocation( JGloss.prefs.getInt( Preferences.FRAME_X, 0),
                           JGloss.prefs.getInt( Preferences.FRAME_Y, 0));
        frame.setSize( JGloss.prefs.getInt( Preferences.FRAME_WIDTH, frame.getPreferredSize().width),
                       JGloss.prefs.getInt( Preferences.FRAME_HEIGHT, frame.getPreferredSize().height));
        
        componentListener = new ComponentAdapter() {
                public void componentMoved( ComponentEvent e) {
                    JGloss.prefs.set( Preferences.FRAME_X, frame.getX());
                    JGloss.prefs.set( Preferences.FRAME_Y, frame.getY());
                }
                public void componentResized( ComponentEvent e) {
                    JGloss.prefs.set( Preferences.FRAME_WIDTH, frame.getWidth());
                    JGloss.prefs.set( Preferences.FRAME_HEIGHT, frame.getHeight());
                }
            };
        frame.addComponentListener( componentListener);
        windowListener = new WindowAdapter() {
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
            };
        frame.addWindowListener( windowListener);
        frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE);

        // set up actions and menu
        xcvManager = new XCVManager();
        
        Actions action = initActions();
        frame.addWindowListener( actions.importClipboardListener);

        // annotation list must be created before initMenuBar is called
        annotationList = new AnnotationList();
        annotationList.addListSelectionListener( this);

        frame.setJMenuBar( initMenuBar( actions));

        // set up the content of this component
        setBackground( Color.white);
        setLayout( new GridLayout( 1, 1));

        annotationEditor = new AnnotationEditorPanel();
        docpane = new JGlossEditor( annotationList);
        docpane.addCaretListener( this);
        new SelectedAnnotationHighlighter(annotationList, docpane);
        xcvManager.addManagedComponent( docpane);
        hyperlinker = new LookupResultList.Hyperlinker
            ( true, true, true, true, true);
        lookupPanel = new SimpleLookup( new Component[] { new JButton( addAnnotationAction) },
                                        hyperlinker);
        lookupPanel.addHyperlinkListener( this);
        firstEntryCache = new FirstEntryCache();
        lookupPanel.addLookupResultHandler(firstEntryCache);

        JScrollPane annotationEditorScroller = 
            new JScrollPane( annotationEditor,
                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane annotationListScroller = 
            new JScrollPane( annotationList,
                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JLabel rendering = new JLabel( JGloss.messages.getString( "main.renderingdocument"),
                                       JLabel.CENTER);
        rendering.setBackground( Color.white);
        rendering.setOpaque( true);
        rendering.setFont( rendering.getFont().deriveFont( 18.0f));
        docpaneScroller = new JScrollPane( rendering,
                                           JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                           JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        KeystrokeForwarder forwarder = new KeystrokeForwarder();
        docpane.addKeyListener(forwarder);
        forwarder.addTarget(annotationList);
        forwarder.addTarget(lookupPanel.getLookupResultList().getFancyResultPane());
        // Disable focusability (and thereby tab traversal) for annotation list and
        // result list. They receive their keyboard events via forwarding.
        annotationList.setFocusable(false);
        lookupPanel.getLookupResultList().getFancyResultPane().setFocusable(false);
        lookupPanel.getLookupResultList().getPlainResultPane().setFocusable(false);
        // Disable focusability of some more components
        annotationEditorScroller.getHorizontalScrollBar().setFocusable(false);
        annotationListScroller.getHorizontalScrollBar().setFocusable(false);
        docpaneScroller.getHorizontalScrollBar().setFocusable(false);
        annotationEditorScroller.getVerticalScrollBar().setFocusable(false);
        annotationListScroller.getVerticalScrollBar().setFocusable(false);
        docpaneScroller.getVerticalScrollBar().setFocusable(false);

        JSplitPane split1 = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
                                            docpaneScroller,
                                            annotationListScroller);
        split1.setOneTouchExpandable( true);

        JSplitPane split2 = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
                                            annotationEditorScroller,
                                            lookupPanel);
        split2.setOneTouchExpandable( true);

        JSplitPane split3 = new JSplitPane( JSplitPane.VERTICAL_SPLIT,
                                            split1, split2);
        split3.setOneTouchExpandable( true);
        this.add( split3);

        splitPanes = new JSplitPane[] { split1, split2, split3 };

        // show the created frame
        frame.show();
    }

    private Actions initActions() {
        saveAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    if (model.getDocumentPath() == null)
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

        addAnnotationAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    annotateDocumentSelection();
                }
            };
        addAnnotationAction.setEnabled( false);
        UIUtilities.initAction( addAnnotationAction, "main.menu.addannotation");

        documentTitleAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    String title = model.getHTMLDocument().getTitle();
                    if (title == null)
                        title = "";
                    Object result = JOptionPane.showInputDialog
                        ( frame, 
                          JGloss.messages.getString( "main.dialog.doctitle"),
                          JGloss.messages.getString( "main.dialog.doctitle.title"),
                          JOptionPane.PLAIN_MESSAGE, null, null, 
                          title);
                    if (result != null) {
                        model.getHTMLDocument().setTitle( result.toString());
                    }
                }
            };
        documentTitleAction.setEnabled( false);
        UIUtilities.initAction( documentTitleAction, "main.menu.doctitle");

        wordLookupAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    JGlossApp.getLookupFrame().show();
                    String selection = docpane.getSelectedText();
                    if (selection == null || selection.length() == 0) {
                        Annotation anno = (Annotation) annotationList.getSelectedValue();
                        if (anno != null)
                            selection = anno.getDictionaryForm();
                    }
                    if (selection!=null && selection.length()>0)
                        JGlossApp.getLookupFrame().search( selection);
                }
            };
        UIUtilities.initAction( wordLookupAction, "main.menu.wordlookup");
        
        return new Actions( this);
    }

    private JMenuBar initMenuBar( Actions actions) {
        // set up the menu bar
        JMenuBar bar = new JMenuBar();

        JMenu menu = new JMenu( JGloss.messages.getString( "main.menu.file"));
        menu.add( UIUtilities.createMenuItem( actions.importDocument));
        menu.add( UIUtilities.createMenuItem( actions.importClipboard));
        menu.addMenuListener( actions.importClipboardListener);
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( actions.open));
        openRecentMenu = OPEN_RECENT.createMenu( actions.openRecentListener);
        menu.add( openRecentMenu);
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( saveAction));
        menu.add( UIUtilities.createMenuItem( saveAsAction));
        exportMenu = new ExportMenu();
        exportMenu.setMnemonic( JGloss.messages.getString( "main.menu.export.mk").charAt( 0));
        menu.add( exportMenu);
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( printAction));
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( closeAction));
        bar.add( menu);

        menu = new JMenu( JGloss.messages.getString( "main.menu.edit"));
        menu.add( UIUtilities.createMenuItem( xcvManager.getCutAction()));
        menu.add( UIUtilities.createMenuItem( xcvManager.getCopyAction()));
        menu.add( UIUtilities.createMenuItem( xcvManager.getPasteAction()));
        menu.addMenuListener( xcvManager.getEditMenuListener());

        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( wordLookupAction));
        menu.add( UIUtilities.createMenuItem( addAnnotationAction));
        menu.add( UIUtilities.createMenuItem( documentTitleAction));
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

        bar.add( annotationList.getMenu());
        
        menu = new JMenu( JGloss.messages.getString( "main.menu.help"));
        aboutItem = UIUtilities.createMenuItem( AboutFrame.showAction);
        menu.add( aboutItem);
        bar.add( menu);

        return bar;
    }

    public JGlossFrameModel getModel() { return model; }

    /**
     * Checks if it is OK to close the document. If the user has changed the document,
     * a dialog will ask the user if the changes should be saved and the appropriate
     * actions will be taken. If after this the document can be closed, the
     * method will return <CODE>true</CODE>.
     *
     * @return <CODE>true</CODE>, if the document can be closed.
     */
    private boolean askCloseDocument() {
        if (!model.isEmpty() && model.isDocumentChanged()) {
            int r = JOptionPane.showOptionDialog
                ( this, JGloss.messages.getString( "main.dialog.close.message",
                                                   new Object[] { model.getDocumentName() }),
                  JGloss.messages.getString( "main.dialog.close.title"),
                  JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                  new Object[] { JGloss.messages.getString( "button.save"),
                                 JGloss.messages.getString( "button.discard"),
                                 JGloss.messages.getString( "button.cancel") },
                  JGloss.messages.getString( "button.save"));
            switch (r) {
            case 0: // save
                if (model.getDocumentPath() == null)
                    saveDocumentAs(); // does not clear documentChanged if cancelled
                else
                    saveDocument();
                break;

            case 1: // discard
                model.setDocumentChanged( false);
                break;

            case 2: // cancel
            default: // probably CLOSED_OPTION
                // leave documentChanged untouched to prevent closing the window
                break;
            }                                   
        }

        return !(!model.isEmpty() && model.isDocumentChanged());
    }

    /**
     * Close the document window and clean up associated resources.
     */
    private void closeDocument() {
        // save the currently selected node in the preferences
        if (model.getDocumentPath() != null) {
            int index = annotationList.getSelectedIndex();
            if (index != -1) {
                StringBuffer history = new StringBuffer();
                String[] oldHistory = JGloss.prefs.getList( Preferences.HISTORY_SELECTION, 
                                                            File.pathSeparatorChar);
                // Copy from the old history all files which are not the current file.
                // Limit the size of the copied history to HISTORY_SIZE-1 by leaving out the
                // last entry to ensure that there is room for the new entry.
                int maxsize = JGloss.prefs.getInt( Preferences.HISTORY_SIZE, 20);
                for ( int i=0; i<oldHistory.length && i<(maxsize-1)*2; i+=2) try {
                    if (!oldHistory[i].equals( model.getDocumentPath())) {
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
                history.insert( 0, model.getDocumentPath());
                JGloss.prefs.set( Preferences.HISTORY_SELECTION, history.toString());
            }
        }

        frame.hide();
        this.dispose(); // this.dispose() calls frame.dispose()

        if (jglossFrames.size() == 0) { // this was the last open document
            JGloss.exit();
        }
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

                JGlossFrame which = this;
                if (!model.isEmpty())
                    which = new JGlossFrame();
                
                which.importFromReader
                    ( in, JGloss.prefs.getBoolean
                      ( Preferences.IMPORTCLIPBOARD_DETECTPARAGRAPHS, true),
                      JGloss.messages.getString( "import.clipboard"),
                      JGloss.messages.getString( "import.clipboard"),
                      GeneralDialog.getInstance().createReadingAnnotationFilter(),
                      GeneralDialog.getInstance().createImportClipboardParser
                      ( Dictionaries.getDictionaries( true), ExclusionList.getExclusions()),
                      len);
                which.model.setDocumentChanged( true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( this, JGloss.messages.getString
                      ( "error.import.exception", new Object[] 
                          { JGloss.messages.getString( "import.clipboard"), ex.getClass().getName(),
                            ex.getLocalizedMessage() }),
                      JGloss.messages.getString( "error.import.title"),
                      JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Sets up everything neccessary to import a file and loads it. If <CODE>filename</CODE> is
     * a URL, it will create a reader which reads from the location the document points to. If it
     * is a path to a local file, it will create a reader which reads from it. 
     * The method will then call <CODE>loadDocument</CODE> with the newly 
     * created reader.
     *
     * @param path URL or path of the file to import.
     * @param detectParagraphs Flag if paragraph detection should be done.
     * @param parser Parser used to annotate the text.
     * @param filter Filter for fetching the reading annotations from a parsed document.
     * @param encoding Character encoding of the file. May be either <CODE>null</CODE> or the
     *                 value of the "encodings.default" resource to use autodetection.
     */
    private void importDocument( String path, boolean detectParagraphs, Parser parser, 
                                 ReadingAnnotationFilter filter, String encoding) {
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
                InputStream is = new BufferedInputStream( c.getInputStream());
                // a user-selected value for encoding overrides enc
                if (encoding != null) // use user-selected encoding
                    in = new InputStreamReader( is, encoding);
                else { // auto-detect, works even if enc==null
                    in = CharacterEncodingDetector.getReader( is, enc);
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
                InputStream is = new BufferedInputStream( new FileInputStream( path));
                if (encoding != null) // use user-selected encoding
                    in = new InputStreamReader( is, encoding);
                else { // auto-detect
                    in = CharacterEncodingDetector.getReader( is);
                    encoding = ((InputStreamReader) in).getEncoding();
                }
            }

            importFromReader( in, detectParagraphs, path, title, filter, parser,
                              CharacterEncodingDetector.guessLength( contentlength, encoding));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( this, JGloss.messages.getString
                  ( "error.import.exception", new Object[] 
                      { path, ex.getClass().getName(),
                        ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.import.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            if (model.getDocumentName() == null)
                // error before document was opened, close window
                this.dispose();
        }
    }

    /**
     * Creates a new annotated document by reading the original text from a string.
     * The method can only be applied on a <CODE>JGlossFrame</CODE> with no open document.
     *
     * @param text The text which will be imported.
     * @param detectParagraphs Flag if paragraph detection should be done.
     * @param title Title of the newly created document.
     * @param path Path to the document.
     * @param setPath If <CODE>true</CODE>, the document path will
     *        be set to the <CODE>path</CODE> parameter. Use this if path denotes a the file to
     *        which the newly created document should be written. If <CODE>false</CODE>,
     *        <CODE>path</CODE> will only be used in informational messages to the user during import.
     */
    public void importString( String text, boolean detectParagraphs, String path, String title,
                              Parser parser, ReadingAnnotationFilter filter, 
                              boolean setPath) {
        try {
            importFromReader( new StringReader( text), detectParagraphs, path, title, filter, parser,
                              text.length());
            model.setDocumentChanged( true);
            if (setPath)
                this.model.setDocumentPath( path);
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
            model.setDocumentPath( f.getAbsolutePath());
            loadDocument( new FileInputStream( f), f.getName());

            synchronized (this) { // prevent closing of document while post-processing
                if (model.isEmpty()) {
                    // document window was closed by the user while document was loading
                    return;
                }

                OPEN_RECENT.addDocument( f);

                // re-select the selection at the time the document was closed
                String[] history = JGloss.prefs.getList( Preferences.HISTORY_SELECTION, 
                                                         File.pathSeparatorChar);
                for ( int i=0; i<history.length; i+=2) try {
                    if (history[i].equals( model.getDocumentPath())) {
                        final int index = Integer.parseInt( history[i+1]);
                        if (index >= 0 && index < annotationList.getModel().getSize()) {
                            Runnable worker = new Runnable() {
                                    public void run() {
                                        annotationList.setSelectedIndex( index);
                                    }
                                };
                            if (EventQueue.isDispatchThread())
                                worker.run();
                            else
                                EventQueue.invokeLater( worker);
                            break;
                        }
                    }
                } catch (NumberFormatException ex) {
                } catch (NullPointerException ex) {
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( this, JGloss.messages.getString
                  ( "error.load.exception", new Object[] 
                      { model.getDocumentPath(), ex.getClass().getName(), ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.load.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDocument( InputStream in, String title) throws IOException, SAXException {
        // Prevent the windowClosing event from directly closing the window.
        // If a windowClosing event is registered while the loadDocument method is executing,
        // the deferWindowClosing flag will be cleared.
        deferWindowClosing = true;

        model.setDocument( new JGlossDocument( new InputSource( in)));
        setupFrame( title);
    }

    private void importFromReader( Reader in, boolean detectParagraphs,
                                   String path, String title, 
                                   ReadingAnnotationFilter filter, Parser parser, int length) 
        throws IOException {
        // Prevent the windowClosing event from directly closing the window.
        // If a windowClosing event is registered while the loadDocument method is executing,
        // the deferWindowClosing flag will be cleared.
        deferWindowClosing = true;
        
        final StopableReader stin = new StopableReader( in);

        final ProgressMonitor pm = new ProgressMonitor
            ( this, JGloss.messages.getString( "load.progress", 
                                               new Object[] { path }),
              null, 0, length);
        final Thread currentThread = Thread.currentThread(); // needed to interrupt parsing if user cancels
        javax.swing.Timer progressUpdater = new javax.swing.Timer( 1000, new ActionListener() {
                public void actionPerformed( ActionEvent e) {
                    // this handler is called from the event dispatch thread
                    pm.setProgress( stin.getCharCount());
                    if (pm.isCanceled() || // cancel button of progress bar pressed
                        !deferWindowClosing) { // close button of document frame pressed
                        stin.stop();
                        currentThread.interrupt();
                    }
                }
            });
        progressUpdater.start();

        try {
            model.setDocument( new JGlossDocumentBuilder().build
                               ( stin, detectParagraphs, filter,
                                 parser, Dictionaries.getDictionaries( true)));
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

        if (model.isEmpty()) {
            deferWindowClosing = false;
        }
        else {
            setupFrame( title);
        }
    }

    private void setupFrame( String title) throws IOException {
        if (!deferWindowClosing) {
            // close button of document frame pressed while document was loading
            closeDocument();
            return;
        }

        kit = new JGlossEditorKit( compactViewItem.isSelected(),
                                   showReadingItem.isSelected(),
                                   showTranslationItem.isSelected());
        final JGlossHTMLDoc htmlDoc = (JGlossHTMLDoc) kit.createDefaultDocument();
        model.setHTMLDocument( htmlDoc);
        DocumentStyleDialog.getDocumentStyleDialog()
            .addStyleSheet( htmlDoc.getStyleSheet());
        htmlDoc.setJGlossDocument( model.getDocument());

        model.setDocumentName( title);

        Runnable worker = new Runnable() {
                public void run() {
                    final Cursor currentCursor = getCursor();
                    updateTitle();
                    setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR));

                    docpane.setEditorKit( kit);
                    xcvManager.updateActions( docpane);
                    docpane.setStyledDocument( htmlDoc);
                    AnnotationListModel annoModel = 
                        new AnnotationListModel( htmlDoc.getAnnotationElements());
                    new AnnotationListSynchronizer(htmlDoc, annoModel);
                    model.setAnnotationListModel
                        ( annoModel);
                    annotationList.setAnnotationListModel( annoModel);
                    annoModel.addAnnotationListener( annotationEditor);

                    frame.getContentPane().removeAll();
                    frame.getContentPane().add( JGlossFrame.this);
                    frame.validate();
                
                    // must wait after frame validation for split pane setup,
                    // because otherwise the split pane divider location gets screwed up
                    SplitPaneManager splitManager = new SplitPaneManager( "view.");
                    splitManager.add( splitPanes[0], 1.0);
                    splitManager.add( splitPanes[1], 0.3);
                    splitManager.add( splitPanes[2], 0.65);
    
                    // Layout document view in the background while still allowing user interaction
                    // in other windows. Since Swing is single-threaded, this is somewhat complicated:
                    // 1. A renderer thread is created, which prepares the docpane asynchronously to
                    //    the event dispatch thread. This is the part that uses the most time.
                    // 2. After the preparation is done, the
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
                                JScrollPane ds = docpaneScroller;
                                final JViewport port = new JViewport();
                                if (dp!=null && ds!=null) {
                                    synchronized (dp) {
                                        // the docpane is not visible, and we are synched on it,
                                        // so it is safe to set the size even though this is not 
                                        // the event dispatch thread
                                        dp.setSize( ds.getViewport().getExtentSize().width,
                                                    dp.getPreferredSize().height);
                                        // now the docpane is set to the correct width, call method again
                                        // to set to correct height for the applied width
                                        dp.setSize( ds.getViewport().getExtentSize().width,
                                                    dp.getPreferredSize().height);
                                        port.setView( dp);
                                    }
                                }
                                // installing the docpane in the scroller, which is already visible,
                                // has to be done in the event dispatch thread
                                Runnable installer = new Runnable() {
                                        public void run() {
                                            synchronized (frame) {
                                                // dispose might already have been called, test if
                                                // member variables still exist
                                                if (docpaneScroller!=null && 
                                                    docpane!=null && 
                                                    annotationEditor!=null) {
                                                    docpaneScroller.setViewport( port);
                                                    frame.validate();
                                                    docpane.followMouse( showAnnotationItem.isSelected());
                                                    // scroll to selected annotation
                                                    Annotation current = 
                                                        (Annotation) annotationList
                                                        .getSelectedValue();
                                                    if (current != null) {
                                                        docpane.makeVisible( current.getStartOffset(),
                                                                             current.getEndOffset());
                                                    }
                                                    setCursor( currentCursor);
                                                    docpane.requestFocusInWindow();
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
                    htmlDoc.setStrictParsing( false);

                    exportMenu.setContext( model);
                    printAction.setEnabled( true);
                    if (model.getDocumentPath() == null) 
                        // this means that the document is imported, save will behave like save as
                        saveAction.setEnabled( true);
                    saveAsAction.setEnabled( true);

                    // get notified of title changes
                    htmlDoc.addPropertyChangeListener( new PropertyChangeListener() {
                            public void propertyChange( PropertyChangeEvent e) {
                                markChanged();
                            }
                        });
                    // mark document as changed if some editing occurs
                    htmlDoc.addDocumentListener( new DocumentListener() {
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
            } catch (InvocationTargetException ex2) {
                if (ex2.getCause() instanceof IOException)
                    throw (IOException) ex2.getCause();
                else if (ex2.getCause() instanceof RuntimeException)
                    throw (RuntimeException) ex2.getCause();
                else // should not happen
                    ex2.printStackTrace();
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
                model.getHTMLDocument().getStyleSheet().addRule
                    ( AnnotationTags.ANNOTATION.getId() + " {}");
            }
            else if (e.getSource()==showReadingItem) {
                JGloss.prefs.set( Preferences.VIEW_SHOWREADING, showReadingItem.isSelected());
                kit.showReading( showReadingItem.isSelected());
                // force docpane to be re-layouted.
                model.getHTMLDocument().getStyleSheet().addRule( AnnotationTags.READING.getId() + " {}");
            }
            else if (e.getSource()==showTranslationItem) {
                JGloss.prefs.set( Preferences.VIEW_SHOWTRANSLATION, showTranslationItem.isSelected());
                kit.showTranslation( showTranslationItem.isSelected());
                // force docpane to be re-layouted.
                model.getHTMLDocument().getStyleSheet().addRule
                    ( AnnotationTags.TRANSLATION.getId() + " {}");
            }
            else if (e.getSource()==showAnnotationItem) {
                JGloss.prefs.set( Preferences.VIEW_SHOWANNOTATION, showAnnotationItem.isSelected());
                docpane.followMouse( showAnnotationItem.isSelected());
            }
        }
    }

    public void hyperlinkUpdate( HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            int colon = e.getDescription().indexOf( ':');
            String protocol = e.getDescription().substring( 0, colon);
            String refKey = e.getDescription().substring( colon+1);
            handleHyperlink( protocol, refKey, e.getSourceElement());
        }
    }

    private void handleHyperlink( String protocol, String refKey, Element e) {
        Annotation anno = (Annotation) annotationList.getSelectedValue();
        if (anno == null)
            return;

        String text = "";
        try {
            text = e.getDocument().getText( e.getStartOffset(), 
                                            e.getEndOffset()-e.getStartOffset());
        } catch (BadLocationException ex) {}
        
        if (protocol.equals( LookupResultList.Hyperlinker.WORD_PROTOCOL))
            anno.setDictionaryForm( text);
        else if (protocol.equals( LookupResultList.Hyperlinker.READING_PROTOCOL)) {
            anno.setDictionaryFormReading( text);
            anno.setReading( text);
        }
        else if (protocol.equals( LookupResultList.Hyperlinker.TRANSLATION_PROTOCOL))
            anno.setTranslation( text);
    }

    /**
     * React to changes to the selection of the annotation list by selecting the annotation
     * in the annotation editor and lookup panel.
     */
    public void valueChanged( ListSelectionEvent e) {
        if (e.getFirstIndex() >= 0) {
            Annotation anno = (Annotation) annotationList.getSelectedValue();
            if (anno != null) {
                annotationEditor.setAnnotation( anno);
                lookupPanel.search( anno.getDictionaryForm());
            }
            else {
                annotationEditor.setAnnotation( null);
            }
        }
        // else: content of currently selected annotation changed; ignore
    }

    /**
     * Reacts to text selection in the annotated document by searching the selected text in
     * the lookup panel.
     */
    public void caretUpdate( CaretEvent e) {
        if (e.getDot() == e.getMark()) {
            // no selection
            addAnnotationAction.setEnabled(false);
        }
        else {
            addAnnotationAction.setEnabled(true);

            int from;
            int to;
            if (e.getDot() < e.getMark()) {
                from = e.getDot();
                to = e.getMark();
            }
            else {
                from = e.getMark();
                to = e.getDot();
            }

            // don't search again if the selection hasn't changed.
            if (lastSelectionStart!=null && lastSelectionStart.getOffset()==from &&
                lastSelectionEnd!=null && lastSelectionEnd.getOffset()==to)
                return;

            String selection = model.getHTMLDocument()
                .getUnannotatedText(from,to);
            if (selection.length() > 0)
                lookupPanel.search( selection);
            try {
                lastSelectionStart = model.getHTMLDocument().createPosition(from);
                lastSelectionEnd = model.getHTMLDocument().createPosition(to);
            } catch (BadLocationException ex) {}
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
                ( frame, model.getDocumentName(), ja, pa);

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
     * Saves the document in JGloss XML format.
     *
     * @return <CODE>true</CODE> if the document was successfully saved.
     */
    private boolean saveDocument() {
        try {
            OutputStream out = new BufferedOutputStream( new FileOutputStream( model.getDocumentPath()));
            if (jglossWriterTransformer == null)
                jglossWriterTransformer = TransformerFactory.newInstance().
                    newTransformer();
            jglossWriterTransformer.transform( new DOMSource( model.getDocument().getDOMDocument()),
                                               new StreamResult( out));
            out.close();
            model.setDocumentChanged( false);
            saveAction.setEnabled( false);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( this, JGloss.messages.getString
                  ( "error.save.exception", new Object[] 
                      { model.getDocumentPath(), ex.getClass().getName(), ex.getLocalizedMessage() }),
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
        if (model.getDocumentPath() == null)
            path = JGloss.getCurrentDir();
        else
            path = new File( model.getDocumentPath()).getPath();
        JFileChooser f = new SaveFileChooser( path);
        f.setFileHidingEnabled( true);
        f.addChoosableFileFilter( jglossFileFilter);
        f.setFileView( CustomFileView.getFileView());
        int r = f.showSaveDialog( this);
        if (r == JFileChooser.APPROVE_OPTION) {
            model.setDocumentPath(  f.getSelectedFile().getAbsolutePath());
            model.setDocumentName( f.getSelectedFile().getName());
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            updateTitle();
            if (saveDocument())
                OPEN_RECENT.addDocument( f.getSelectedFile());
        }
    }

    /**
     * Marks the document as changed and updates the save action accordingly.
     */
    protected void markChanged() {
        if (!model.isDocumentChanged()) {
            model.setDocumentChanged( true);
            if (model.getDocumentPath() != null)
                saveAction.setEnabled( true);
        }
    }

    /**
     * Update the document window title.
     */
    protected void updateTitle() {
        frame.setTitle( model.getDocumentName() + ":" + JGloss.messages.getString( "main.title"));
    }

    protected void annotateDocumentSelection() {
        int selectionStart = docpane.getSelectionStart();
        int selectionEnd = docpane.getSelectionEnd();
        if (selectionStart == selectionEnd)
            return;

        annotationList.clearSelection();
        model.getHTMLDocument().addAnnotation(selectionStart, selectionEnd, kit);
        annotationList.setSelectedIndex
            (model.getAnnotationListModel().findAnnotationIndex
             (selectionStart, AnnotationListModel.BIAS_NONE));
        // Set the new annotation reading and translation to the reading and translation
        // of the first dictionary item displayed in the lookup result list.
        DictionaryEntry entry = firstEntryCache.getEntry();
        if (entry != null) {
            Annotation anno = (Annotation) annotationList.getSelectedValue();
            anno.setReading(entry.getReading(0));
            anno.setTranslation(entry.getTranslation(0, 0, 0));
        }
    }

    /**
     * Dispose resources associated with the JGloss document.
     */
    public synchronized void dispose() {
        jglossFrames.remove( this);

        JGloss.prefs.removePropertyChangeListener( prefsListener);
        if (model.getDocument() != null)
            DocumentStyleDialog.getDocumentStyleDialog().removeStyleSheet
                ( model.getHTMLDocument().getStyleSheet());
        docpane.dispose();
        OPEN_RECENT.removeMenu( openRecentMenu);

        // remove references from static action to menu item
        preferencesItem.setAction( null);
        aboutItem.setAction( null);

        // The JRE1.3 AWT/Swing implementation keeps static references to created JFrames, thus
        // they are never garbage collected. Remove all references from the JFrame to the JGlossFrame
        // to ensure that the JGlossFrame can be garbage collected.
        UIUtilities.dismantleHierarchy( frame.getJMenuBar());
        frame.setJMenuBar( null);
        frame.removeComponentListener( componentListener);
        frame.removeWindowListener( windowListener);
        frame.setContentPane( new JPanel());
        frame.getContentPane().requestFocusInWindow();
        frame.dispose();

        // Sometimes, due to the fact that swing keeps internal references, the JGlossFrame is
        // not garbage collected until a new frame is created. To ensure that the objects referenced
        // from the JGlossFrame are freed, set references to null
        model = null;
        splitPanes = null;
        exportMenu.setContext( null);
        exportMenu = null;
        docpane.removeCaretListener( this);
        docpane = null;
        docpaneScroller = null;
        kit = null;
        annotationList.removeListSelectionListener( this);
        annotationList = null;
        annotationEditor = null;
        lookupPanel.removeHyperlinkListener( this);
        lookupPanel = null;
        lastSelectionStart = null;
        lastSelectionEnd = null;
    }
} // class JGlossFrame
