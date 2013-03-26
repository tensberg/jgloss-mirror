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
 *
 */

package jgloss.ui.gloss;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.JobAttributes;
import java.awt.PageAttributes;
import java.awt.PrintJob;
import java.awt.Rectangle;
import java.awt.Shape;
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jgloss.JGloss;
import jgloss.JGlossApp;
import jgloss.Preferences;
import jgloss.dictionary.DictionaryEntry;
import jgloss.ui.AboutFrame;
import jgloss.ui.CustomFileView;
import jgloss.ui.ExtensionFileFilter;
import jgloss.ui.FirstEntryCache;
import jgloss.ui.JGlossLogo;
import jgloss.ui.KeystrokeForwarder;
import jgloss.ui.LookupFrame;
import jgloss.ui.LookupResultHyperlinker;
import jgloss.ui.OpenRecentMenu;
import jgloss.ui.PreferencesFrame;
import jgloss.ui.SaveFileChooser;
import jgloss.ui.SimpleLookup;
import jgloss.ui.SplitPaneManager;
import jgloss.ui.annotation.Annotation;
import jgloss.ui.annotation.AnnotationListModel;
import jgloss.ui.export.ExportMenu;
import jgloss.ui.html.AnnotationListSynchronizer;
import jgloss.ui.html.AnnotationTags;
import jgloss.ui.html.JGlossEditor;
import jgloss.ui.html.JGlossEditorKit;
import jgloss.ui.html.JGlossHTMLDoc;
import jgloss.ui.html.SelectedAnnotationHighlighter;
import jgloss.ui.util.UIUtilities;
import jgloss.ui.util.XCVManager;

/**
 * Frame which contains everything needed to edit a single JGloss document.
 *
 * @author Michael Koch
 */
public class JGlossFrame extends JPanel implements ActionListener, ListSelectionListener,
                                                   HyperlinkListener, CaretListener {
	/**
	 * Mark the JGloss document changed if the content changes.
     *
     * @author Michael Koch <tensberg@gmx.net>
     */
    private class MarkChangedListener implements PropertyChangeListener, DocumentListener {
        @Override
        public void propertyChange( PropertyChangeEvent e) {
            markChanged();
            if (e.getPropertyName() == Document.TitleProperty) {
            	updateTitle();
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            markChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            markChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            // triggered by style changes, don't react to this
        }
    }

    private static final Logger LOGGER = Logger.getLogger(JGlossFrame.class.getPackage().getName());

	private static final long serialVersionUID = 1L;

    /**
     * Open recent menu used for JGloss documents. The instance is shared between instances of
     * <CODE>JGlossFrame</CODE> and {@link LookupFrame LookupFrame}.
     */
    public static final OpenRecentMenu OPEN_RECENT = new OpenRecentMenu( 8);

    /**
     * Data model of this frame.
     */
    private JGlossFrameModel model;

    /**
     * JGloss document frame object. The frame keeps the <code>JGlossFrame</code> as sole component
     * in its content pane.
     */
    final JFrame frame;
    /**
     * Reacts to the document closed events.
     */
    private final WindowListener windowListener;
    /**
     * Saves changes in window size to the preferences.
     */
    private final ComponentListener componentListener;
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
    private final LookupResultHyperlinker hyperlinker;
    /**
     * Remembers the first dictionary entry shown in the result list of the lookup panel.
     * This is used when a new annotation is created to automatically set the reading and
     * translation.
     */
    private final FirstEntryCache firstEntryCache = new FirstEntryCache();

    private Position lastSelectionStart;
    private Position lastSelectionEnd;

    private Transformer jglossWriterTransformer;

    /**
     * Manager for the cut/copy/past actions;
     */
    private final XCVManager xcvManager;

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
    static final LinkedList<JGlossFrame> JGLOSS_FRAMES = new LinkedList<JGlossFrame>();

    /**
     * Returns the number of currently open JGlossFrames.
     */
    public static int getFrameCount() { return JGLOSS_FRAMES.size(); }

    /**
     * A file filter which will accept JGloss documents.
     */
    public static final FileFilter JGLOSS_FILE_FILTER =
        new ExtensionFileFilter( "jgloss",
                                 JGloss.MESSAGES.getString( "filefilter.description.jgloss"));

    /**
     * Creates a new JGlossFrame which does not contain a document. The user can add a document
     * by using import or open actions.
     */
    public JGlossFrame() {
        JGLOSS_FRAMES.add( this);

        model = new JGlossFrameModel();

        // set up the frame
        /* The JGlossFrame does not directly inherit from
         * JFrame, because memory profiling has shown that the JFrame objects are not correctly garbage
         * collected as expected (at least in JDK1.3). To keep the state associated with the JFrame objects
         * to a minimum, they are kept separate from the JGlossFrame state.
         * See also JGlossFrame.dispose().
         */
        frame = new JFrame( JGloss.MESSAGES.getString( "main.title"));
        frame.setIconImages(JGlossLogo.ALL_LOGO_SIZES);
        frame.getContentPane().setBackground( Color.white);
        frame.getContentPane().setLayout( new GridLayout(1, 1));
        frame.setLocation( JGloss.PREFS.getInt( Preferences.FRAME_X, 0),
                           JGloss.PREFS.getInt( Preferences.FRAME_Y, 0));
        frame.setSize( JGloss.PREFS.getInt( Preferences.FRAME_WIDTH, frame.getPreferredSize().width),
                       JGloss.PREFS.getInt( Preferences.FRAME_HEIGHT, frame.getPreferredSize().height));

        componentListener = new ComponentAdapter() {
                @Override
				public void componentMoved( ComponentEvent e) {
                    JGloss.PREFS.set( Preferences.FRAME_X, frame.getX());
                    JGloss.PREFS.set( Preferences.FRAME_Y, frame.getY());
                }
                @Override
				public void componentResized( ComponentEvent e) {
                    JGloss.PREFS.set( Preferences.FRAME_WIDTH, frame.getWidth());
                    JGloss.PREFS.set( Preferences.FRAME_HEIGHT, frame.getHeight());
                }
            };
        frame.addComponentListener( componentListener);
        windowListener = new WindowAdapter() {
                @Override
				public void windowClosing( WindowEvent e) {
                    if (askCloseDocument()) {
                        closeDocument();
                    }
                }
            };
        frame.addWindowListener( windowListener);
        frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE);

        // set up actions and menu
        xcvManager = new XCVManager();

        initActions();

        DocumentActions documentActions = new DocumentActions(this);
        frame.getContentPane().add(new EmptyDocumentActionsPanel(documentActions));

        frame.addWindowListener( documentActions.importClipboardListener);

        // annotation list must be created before initMenuBar is called
        annotationList = new AnnotationList();
        annotationList.addListSelectionListener( this);

        frame.setJMenuBar( createMenuBar( documentActions));

        // set up the content of this component
        setBackground( Color.white);
        setLayout( new GridLayout( 1, 1));

        annotationEditor = new AnnotationEditorPanel();
        docpane = new JGlossEditor( annotationList);
        docpane.addCaretListener( this);
        new SelectedAnnotationHighlighter(annotationList, docpane);
        xcvManager.addManagedComponent( docpane);
        hyperlinker = new LookupResultHyperlinker
            ( true, true, true, false, false);
        lookupPanel = new SimpleLookup( new Component[] { new JButton( addAnnotationAction) },
        				hyperlinker);
        lookupPanel.addHyperlinkListener( this);
        lookupPanel.addLookupResultHandler(firstEntryCache);

        JScrollPane annotationEditorScroller =
            new JScrollPane( annotationEditor,
                             ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane annotationListScroller =
            new JScrollPane( annotationList,
                             ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JLabel rendering = new JLabel( JGloss.MESSAGES.getString( "main.renderingdocument"),
                                       SwingConstants.CENTER);
        rendering.setBackground( Color.white);
        rendering.setOpaque( true);
        rendering.setFont( rendering.getFont().deriveFont( 18.0f));
        docpaneScroller = new JScrollPane( rendering,
                                           ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                           ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        KeystrokeForwarder forwarder = new KeystrokeForwarder();
        docpane.addKeyListener(forwarder);
        forwarder.addTarget(annotationList);
        forwarder.addTarget(lookupPanel.getLookupResultList().getResultPane());
        // Disable focusability (and thereby tab traversal) for annotation list and
        // result list. They receive their keyboard events via forwarding.
        annotationList.setFocusable(false);
        lookupPanel.getLookupResultList().getResultPane().setFocusable(false);
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
        frame.setVisible(true);
    }

    public JFrame getFrame() {
        return frame;
    }

    private DocumentActions initActions() {
        saveAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    if (model.getDocumentPath() == null) {
	                    saveDocumentAs();
                    } else {
	                    saveDocument();
                    }
                }
            };
        saveAction.setEnabled( false);
        UIUtilities.initAction( saveAction, "main.menu.save");

        saveAsAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    saveDocumentAs();
                }
            };
        saveAsAction.setEnabled( false);
        UIUtilities.initAction( saveAsAction, "main.menu.saveAs");

        printAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    doPrint();
                }
            };
        printAction.setEnabled( false);
        UIUtilities.initAction( printAction, "main.menu.print");

        closeAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    if (askCloseDocument()) {
                        closeDocument();
                    }
                }
            };
        UIUtilities.initAction( closeAction, "main.menu.close");

        addAnnotationAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    annotateDocumentSelection();
                }
            };
        addAnnotationAction.setEnabled( false);
        UIUtilities.initAction( addAnnotationAction, "main.menu.addannotation");

        documentTitleAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    String title = model.getHTMLDocument().getTitle();
                    if (title == null) {
	                    title = "";
                    }
                    Object result = JOptionPane.showInputDialog
                        ( frame,
                          JGloss.MESSAGES.getString( "main.dialog.doctitle"),
                          JGloss.MESSAGES.getString( "main.dialog.doctitle.title"),
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
                private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    JGlossApp.getLookupFrame().setVisible(true);
                    String selection = docpane.getSelectedText();
                    if (selection == null || selection.length() == 0) {
                    Annotation anno = annotationList.getSelectedValue();
                        if (anno != null) {
	                        selection = anno.getDictionaryForm();
                        }
                    }
                    if (selection!=null && selection.length()>0) {
	                    JGlossApp.getLookupFrame().search( selection);
                    }
                }
            };
        UIUtilities.initAction( wordLookupAction, "main.menu.wordlookup");

        return new DocumentActions( this);
    }

    private JMenuBar createMenuBar( DocumentActions actions) {
        // set up the menu bar
        JMenuBar bar = new JMenuBar();

        bar.add(createFileMenu(actions));
        bar.add(createEditMenu());
        bar.add(createViewMenu());
        bar.add(annotationList.getMenu());
        bar.add(createHelpMenu(actions));

        return bar;
    }

    private JMenu createHelpMenu(DocumentActions actions) {
        JMenu menu = new JMenu( JGloss.MESSAGES.getString( "main.menu.help"));

        menu.add(UIUtilities.createMenuItem(actions.showWelcomeDialog));
        menu.addSeparator();

        aboutItem = UIUtilities.createMenuItem( AboutFrame.getShowAction());
        menu.add( aboutItem);
        return menu;
    }

    private JMenu createViewMenu() {
        showReadingItem = new JCheckBoxMenuItem( JGloss.MESSAGES.getString( "main.menu.showreading"));
        showReadingItem.setSelected( JGloss.PREFS.getBoolean( Preferences.VIEW_SHOWREADING, true));
        showReadingItem.setToolTipText( JGloss.MESSAGES.getString( "main.menu.showreading.tt"));
        showReadingItem.addActionListener( this);
        showTranslationItem = new JCheckBoxMenuItem( JGloss.MESSAGES.getString
                                                     ( "main.menu.showtranslation"));
        showTranslationItem.setSelected( JGloss.PREFS.getBoolean
                                         ( Preferences.VIEW_SHOWTRANSLATION, true));
        showTranslationItem.setToolTipText( JGloss.MESSAGES.getString( "main.menu.showtranslation.tt"));
        showTranslationItem.addActionListener( this);
        showAnnotationItem = new JCheckBoxMenuItem( JGloss.MESSAGES.getString
                                                    ( "main.menu.showannotation"));
        showAnnotationItem.setSelected( JGloss.PREFS.getBoolean
                                        ( Preferences.VIEW_SHOWANNOTATION, false));
        showAnnotationItem.setToolTipText( JGloss.MESSAGES.getString( "main.menu.showannotation.tt"));
        showAnnotationItem.addActionListener( this);

        JMenu menu = new JMenu(JGloss.MESSAGES.getString("main.menu.view"));
        menu.add( showReadingItem);
        menu.add( showTranslationItem);
        menu.add( showAnnotationItem);

        return menu;
    }

    private JMenu createEditMenu() {
        JMenu menu;
        menu = new JMenu( JGloss.MESSAGES.getString( "main.menu.edit"));
        menu.add( UIUtilities.createMenuItem( xcvManager.getCutAction()));
        menu.add( UIUtilities.createMenuItem( xcvManager.getCopyAction()));
        menu.add( UIUtilities.createMenuItem( xcvManager.getPasteAction()));
        menu.addMenuListener( xcvManager.getEditMenuListener());

        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( wordLookupAction));
        menu.add( UIUtilities.createMenuItem( addAnnotationAction));
        menu.add( UIUtilities.createMenuItem( documentTitleAction));
        menu.addSeparator();
        preferencesItem = UIUtilities.createMenuItem( PreferencesFrame.SHOW_ACTION);
        menu.add( preferencesItem);
        return menu;
    }

    private JMenu createFileMenu(DocumentActions actions) {
        JMenu menu = new JMenu( JGloss.MESSAGES.getString( "main.menu.file"));
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
        exportMenu.setMnemonic( JGloss.MESSAGES.getString( "main.menu.export.mk").charAt( 0));
        menu.add( exportMenu);
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( printAction));
        menu.addSeparator();
        menu.add( UIUtilities.createMenuItem( closeAction));
        return menu;
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
                ( this, JGloss.MESSAGES.getString( "main.dialog.close.message",
                                                   new Object[] { model.getDocumentName() }),
                  JGloss.MESSAGES.getString( "main.dialog.close.title"),
                  JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                  new Object[] { JGloss.MESSAGES.getString( "button.save"),
                                 JGloss.MESSAGES.getString( "button.discard"),
                                 JGloss.MESSAGES.getString( "button.cancel") },
                  JGloss.MESSAGES.getString( "button.save"));
            switch (r) {
            case 0: // save
                if (model.getDocumentPath() == null) {
	                saveDocumentAs(); // does not clear documentChanged if cancelled
                } else {
	                saveDocument();
                }
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
                StringBuilder history = new StringBuilder();
                String[] oldHistory = JGloss.PREFS.getList( Preferences.HISTORY_SELECTION,
                                                            File.pathSeparatorChar);
                // Copy from the old history all files which are not the current file.
                // Limit the size of the copied history to HISTORY_SIZE-1 by leaving out the
                // last entry to ensure that there is room for the new entry.
                int maxsize = JGloss.PREFS.getInt( Preferences.HISTORY_SIZE, 20);
                for ( int i=0; i<oldHistory.length && i<(maxsize-1)*2; i+=2) {
	                try {
	                    if (!oldHistory[i].equals( model.getDocumentPath())) {
	                        if (history.length() > 0) {
	                            history.append( File.pathSeparatorChar);
	                        }
	                        history.append( oldHistory[i]);
	                        history.append( File.pathSeparatorChar);
	                        history.append( oldHistory[i+1]);
	                    }
	                } catch (ArrayIndexOutOfBoundsException ex) {}
                }

                // create the new history entry
                if (history.length() > 0) {
	                history.insert( 0, File.pathSeparatorChar);
                }
                history.insert( 0, index);
                history.insert( 0, File.pathSeparatorChar);
                history.insert( 0, model.getDocumentPath());
                JGloss.PREFS.set( Preferences.HISTORY_SELECTION, history.toString());
            }
        }

        frame.setVisible(false);
        this.dispose(); // this.dispose() calls frame.dispose()

        if (JGLOSS_FRAMES.size() == 0) { // this was the last open document
            JGloss.getApplication().exit();
        }
    }

    /** Re-select the selection at the time the document was closed. */
    private void restoreSelection() {
        if (model.getDocumentPath() == null) {
            return;
        }

        String[] history = JGloss.PREFS.getList( Preferences.HISTORY_SELECTION,
                                                 File.pathSeparatorChar);
        for ( int i=0; i<history.length; i+=2) {
            try {
                if (history[i].equals( model.getDocumentPath())) {
                    final int index = Integer.parseInt( history[i+1]);
                    if (index >= 0 && index < annotationList.getModel().getSize()) {
                        annotationList.setSelectedIndex(index);
                        break;
                    }
                }
            } catch (Exception ex) {
                LOGGER.info("failed to restore annotation selection");
                LOGGER.log(Level.FINER, "failed to restore annotation selection", ex);
            }
        }
    }

    void setModel(final JGlossFrameModel model) {
        assert EventQueue.isDispatchThread();

        this.model = model;
        kit = new JGlossEditorKit(showReadingItem.isSelected(), showTranslationItem.isSelected());
        JGlossHTMLDoc htmlDoc = createHtmlDoc(model);

        DocumentStyleDialog.getDocumentStyleDialog().addStyleSheet( htmlDoc.getStyleSheet());

        updateTitle();

        docpane.setEditorKit( kit);
        xcvManager.updateActions( docpane);
        docpane.setStyledDocument( htmlDoc);
        AnnotationListModel annoModel =
                new AnnotationListModel( htmlDoc.getAnnotationElements());
        new AnnotationListSynchronizer(htmlDoc, annoModel);
        model.setAnnotationListModel(annoModel);
        annotationList.setAnnotationListModel( annoModel);
        annoModel.addAnnotationListener( annotationEditor);
        restoreSelection();

        frame.getContentPane().removeAll();
        frame.getContentPane().add(this);
        frame.validate();

        // must wait after frame validation for split pane setup,
        // because otherwise the split pane divider location gets screwed up
        SplitPaneManager splitManager = new SplitPaneManager( "view.");
        splitManager.add( splitPanes[0], 1.0);
        splitManager.add( splitPanes[1], 0.3);
        splitManager.add( splitPanes[2], 0.65);

        exportMenu.setContext( model);
        printAction.setEnabled( true);
        if (model.getDocumentPath() == null) {
            // this means that the document is imported, save will behave like save as
            saveAction.setEnabled( true);
        }
        saveAsAction.setEnabled( true);
        documentTitleAction.setEnabled(true);

        // first paint the frame, then install the docpane in a separate event since showing the docpane
        // takes a long time for larger documents
        repaint();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                showDocpane();
            }
        });
    }

    private JGlossHTMLDoc createHtmlDoc(JGlossFrameModel model) {
        JGlossHTMLDoc htmlDoc = (JGlossHTMLDoc) kit.createDefaultDocument();

        // Parser must be set to non-strict mode for editing to work.
        htmlDoc.setStrictParsing( false);

        htmlDoc.setJGlossDocument( model.getDocument());
        model.setHTMLDocument( htmlDoc);

        // get notified of title changes
        MarkChangedListener markChangedListener = new MarkChangedListener();
        htmlDoc.addPropertyChangeListener(markChangedListener);

        // mark document as changed if some editing occurs
        htmlDoc.addDocumentListener(markChangedListener);

        return htmlDoc;
    }

    private void showDocpane() {
        // dispose might already have been called, test if
        // member variables still exist
        if (docpaneScroller != null && docpane != null && annotationEditor != null) {
            docpaneScroller.setViewportView(docpane);
            docpane.followMouse( showAnnotationItem.isSelected());
            // scroll to selected annotation
            Annotation current = annotationList.getSelectedValue();
            if (current != null) {
                docpane.makeVisible( current.getStartOffset(),
                        current.getEndOffset());
            }
            docpane.requestFocusInWindow();
        }
    }

    /**
     * Executes the appropriate action for a selection in the view menu.
     *
     * @param e The action event.
     */
    @Override
	public void actionPerformed( ActionEvent e) {
        if (kit != null) {
            if (e.getSource() == showReadingItem) {
                JGloss.PREFS.set( Preferences.VIEW_SHOWREADING, showReadingItem.isSelected());
                kit.showReading( showReadingItem.isSelected());
                // force docpane to be re-layouted.
                model.getHTMLDocument().getStyleSheet().addRule( AnnotationTags.READING.getId() + " {}");
            }
            else if (e.getSource()==showTranslationItem) {
                JGloss.PREFS.set( Preferences.VIEW_SHOWTRANSLATION, showTranslationItem.isSelected());
                kit.showTranslation( showTranslationItem.isSelected());
                // force docpane to be re-layouted.
                model.getHTMLDocument().getStyleSheet().addRule
                    ( AnnotationTags.TRANSLATION.getId() + " {}");
            }
            else if (e.getSource()==showAnnotationItem) {
                JGloss.PREFS.set( Preferences.VIEW_SHOWANNOTATION, showAnnotationItem.isSelected());
                docpane.followMouse( showAnnotationItem.isSelected());
            }
        }
    }

    @Override
	public void hyperlinkUpdate( HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            int colon = e.getDescription().indexOf( ':');
            String protocol = e.getDescription().substring( 0, colon);
            String refKey = e.getDescription().substring( colon+1);
            handleHyperlink( protocol, refKey, e.getSourceElement());
        }
    }

    private void handleHyperlink( String protocol, String refKey, Element e) {
        Annotation anno = annotationList.getSelectedValue();
        if (anno == null) {
	        return;
        }

        String text = "";
        try {
            text = e.getDocument().getText( e.getStartOffset(),
                                            e.getEndOffset()-e.getStartOffset());
        } catch (BadLocationException ex) {}

        if (protocol.equals( LookupResultHyperlinker.WORD_PROTOCOL)) {
	        anno.setDictionaryForm( text);
        } else if (protocol.equals( LookupResultHyperlinker.READING_PROTOCOL)) {
            anno.setDictionaryFormReading( text);
            anno.setReading( text);
        }
        else if (protocol.equals( LookupResultHyperlinker.TRANSLATION_PROTOCOL)) {
	        anno.setTranslation( text);
        }
    }

    /**
     * React to changes to the selection of the annotation list by selecting the annotation
     * in the annotation editor and lookup panel.
     */
    @Override
	public void valueChanged( ListSelectionEvent e) {
        if (e.getFirstIndex() >= 0) {
            Annotation anno = annotationList.getSelectedValue();
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
    @Override
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
                lastSelectionEnd!=null && lastSelectionEnd.getOffset()==to) {
	            return;
            }

            String selection = model.getHTMLDocument()
                .getUnannotatedText(from,to);
            if (selection.length() > 0) {
	            lookupPanel.search( selection);
            }
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
                        while (i < v.getViewCount()) {
                            View cv = v.getView( i);
                            Shape s = v.getChildAllocation( i, r);
                            Rectangle cr;
                            if (s == null) { // invisible view? It does happen.
                                i++;
                                continue;
                            }
                            else if (s instanceof Rectangle) {
	                            cr = (Rectangle) s;
                            } else {
	                            cr = s.getBounds();
                            }
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
                            } else {
	                            break;
                            }
                            i++;
                        }

                        if (nh == 0) {
                            // this page either is empty (contains no views), or all views on this page
                            // don't fit
                            nh = page.height;
                        }
                        pagebounds.height = nh;

                        pagecount++;
                        if (pagecount > lastpage) {
	                        break;
                        }

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
            if (jglossWriterTransformer == null) {
	            jglossWriterTransformer = TransformerFactory.newInstance().
                    newTransformer();
            }
            jglossWriterTransformer.transform( new DOMSource( model.getDocument().getDOMDocument()),
                                               new StreamResult( out));
            out.close();
            model.setDocumentChanged( false);
            saveAction.setEnabled( false);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            JOptionPane.showConfirmDialog
                ( this, JGloss.MESSAGES.getString
                  ( "error.save.exception", new Object[]
                      { model.getDocumentPath(), ex.getClass().getName(), ex.getLocalizedMessage() }),
                  JGloss.MESSAGES.getString( "error.save.title"),
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
        if (model.getDocumentPath() == null) {
	        path = JGloss.getApplication().getCurrentDir();
        } else {
	        path = new File( model.getDocumentPath()).getPath();
        }
        JFileChooser f = new SaveFileChooser( path);
        f.setFileHidingEnabled( true);
        f.addChoosableFileFilter( JGLOSS_FILE_FILTER);
        f.setFileView( CustomFileView.getFileView());
        int r = f.showSaveDialog( this);
        if (r == JFileChooser.APPROVE_OPTION) {
            model.setDocumentPath(  f.getSelectedFile().getAbsolutePath());
            model.setDocumentName( f.getSelectedFile().getName());
            JGloss.getApplication().setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            updateTitle();
            if (saveDocument()) {
	            OPEN_RECENT.addDocument( f.getSelectedFile());
            }
        }
    }

    /**
     * Marks the document as changed and updates the save action accordingly.
     */
    protected void markChanged() {
        if (!model.isDocumentChanged()) {
            model.setDocumentChanged( true);
            if (model.getDocumentPath() != null) {
	            saveAction.setEnabled( true);
            }
            updateTitle();
        }
    }

    /**
     * Update the document window title.
     */
    protected void updateTitle() {
    	String documentTitle = model.getHTMLDocument().getTitle();

    	if (documentTitle == null || documentTitle.isEmpty()) {
    		documentTitle = model.getDocumentName();
    	}

    	if (model.isDocumentChanged()) {
    		documentTitle = "*" + documentTitle;
    	}

        frame.setTitle( documentTitle + ":" + JGloss.MESSAGES.getString( "main.title"));
    }

    private void annotateDocumentSelection() {
        int selectionStart = docpane.getSelectionStart();
        int selectionEnd = docpane.getSelectionEnd();
        if (selectionStart == selectionEnd) {
	        return;
        }

        annotationList.clearSelection();
        model.getHTMLDocument().addAnnotation(selectionStart, selectionEnd, kit);
        annotationList.setSelectedIndex
            (model.getAnnotationListModel().findAnnotationIndex
             (selectionStart, AnnotationListModel.Bias.NONE));
        // Set the new annotation reading and translation to the reading and translation
        // of the first dictionary item displayed in the lookup result list.
        DictionaryEntry entry = firstEntryCache.getEntry();
        if (entry != null) {
            Annotation anno = annotationList.getSelectedValue();
            anno.setReading(entry.getReading(0));
            anno.setTranslation(entry.getTranslation(0, 0, 0));
        }
    }

    /**
     * Dispose resources associated with the JGloss document.
     */
    public void dispose() {
        JGLOSS_FRAMES.remove( this);

        JGloss.PREFS.removePropertyChangeListener( prefsListener);
        if (model.getDocument() != null) {
	        DocumentStyleDialog.getDocumentStyleDialog().removeStyleSheet
                ( model.getHTMLDocument().getStyleSheet());
        }
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
