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
public class JGlossFrame implements ActionListener {
    /**
     * The frame which contains all user interaction widgets.
     */
    private JFrame frame;
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
     * Imports a document into an empty JGlossFrame.
     */
    private Action importAction;
    /**
     * Imports the clipboard content into an empty JGlossFrame.
     */
    private Action importClipboardAction;
    /**
     * Opens a document created by JGloss in an empty JGlossFrame.
     */
    private Action openAction;
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
     * Displays the preferences dialog.
     */
    private Action preferencesAction;
    /**
     * Displays the about dialog.
     */
    private Action aboutAction;
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
     * Toggles the automatic scrolling of the annotation editor if the mouse hovers
     * over an annotation.
     */
    private JCheckBoxMenuItem editorFollowsMouseItem;

    /**
     * Counts the number of open JGlossFrames. If the count drops to zero, the application
     * will quit.
     */
    private static int framecount;

    /**
     * A file filter which will accept JGloss documents.
     */
    public static final javax.swing.filechooser.FileFilter jglossFileFilter = 
        new javax.swing.filechooser.FileFilter() {
            public boolean accept( File f) {
                return f.isDirectory() ||
                    f.getName().toLowerCase().endsWith( ".jgloss");
            }

            public String getDescription() { 
                return JGloss.messages.getString( "filefilter.jgloss.description");
            }
        };

    /**
     * Creates a new JGlossFrame which does not contain a document. The user can add a document
     * by using import or open actions.
     */
    public JGlossFrame() {
        framecount++;

        // set up the frame
        frame = new JFrame( JGloss.messages.getString( "main.title"));
        annotationEditor = new AnnotationEditor();
        annotationEditorScroller = new JScrollPane( annotationEditor,
                                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        docpane = new JGlossEditor( annotationEditor);
        docpane.setEditable( JGloss.prefs.getBoolean( Preferences.EDITOR_ENABLEEDITING));
        docpaneScroller = new JScrollPane( docpane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                           JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        split = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
                                docpaneScroller,
                                annotationEditorScroller);
        split.setDividerLocation( 0.8d);
        split.setResizeWeight( 0.8d);
        split.setOneTouchExpandable( true);
        split.addPropertyChangeListener( new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent e) {
                    if (split.DIVIDER_LOCATION_PROPERTY.equals( e.getPropertyName())) {
                        JGloss.prefs.set( Preferences.VIEW_ANNOTATIONEDITORHIDDEN,
                                          ((Integer) e.getNewValue()).intValue() >=
                                          split.getMaximumDividerLocation());
                    }
                }
            });

        frame.getContentPane().setBackground( Color.white);

        frame.setLocation( JGloss.prefs.getInt( Preferences.FRAME_X, 0),
                           JGloss.prefs.getInt( Preferences.FRAME_Y, 0));
        frame.setSize( JGloss.prefs.getInt( Preferences.FRAME_WIDTH, frame.getPreferredSize().width),
                       JGloss.prefs.getInt( Preferences.FRAME_HEIGHT, frame.getPreferredSize().height));
        frame.addComponentListener( new ComponentAdapter() {
                public void componentMoved( ComponentEvent e) {
                    JGloss.prefs.set( Preferences.FRAME_X, frame.getX());
                    JGloss.prefs.set( Preferences.FRAME_Y, frame.getY());
                }
                public void componentResized( ComponentEvent e) {
                    JGloss.prefs.set( Preferences.FRAME_WIDTH, frame.getWidth());
                    JGloss.prefs.set( Preferences.FRAME_HEIGHT, frame.getHeight());
                }
            });
        frame.addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent e) {
                    if (askCloseDocument())
                        frame.dispose();
                }

                public void windowClosed( WindowEvent e) {
                    framecount--;
                    if (framecount == 0)
                        JGloss.exit();
                }
            });
        frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE);

        JGloss.prefs.addPropertyChangeListener( new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent e) {
                    if (e.getPropertyName().equals( Preferences.EDITOR_ENABLEEDITING))
                        docpane.setEditable( "true".equals( e.getNewValue()));
                }
            });

        // set up the menu bar
        JMenuBar bar = new JMenuBar();

        importAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    new Thread( "JGloss import") {
                            public void run() {
                                doImport();
                            }
                        }.start();
                }
            };
        importAction.setEnabled( true);
        initAction( importAction, "main.menu.import"); 
        importClipboardAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doImportClipboard();
                }
            };
        importClipboardAction.setEnabled( false);
        initAction( importClipboardAction, "main.menu.importclipboard"); 
        openAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    openDocument();
                }
            };
        openAction.setEnabled( true);
        initAction( openAction, "main.menu.open"); 
        saveAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    if (documentPath == null)
                        saveDocumentAs();
                    else
                        saveDocument();
                }
            };
        saveAction.setEnabled( false);
        initAction( saveAction, "main.menu.save"); 
        saveAsAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    saveDocumentAs();
                }
            };
        saveAsAction.setEnabled( false);
        initAction( saveAsAction, "main.menu.saveAs");
        exportPlainTextAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doExportPlainText();
                }
            };
        exportPlainTextAction.setEnabled( false);
        initAction( exportPlainTextAction, "main.menu.export.plaintext"); 
        exportLaTeXAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doExportLaTeX();
                }
            };
        exportLaTeXAction.setEnabled( false);
        initAction( exportLaTeXAction, "main.menu.export.latex"); 
        exportHTMLAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doExportHTML();
                }
            };
        exportHTMLAction.setEnabled( false);
        initAction( exportHTMLAction, "main.menu.export.html"); 
        exportAnnotationListAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doExportAnnotationList();
                }
            };
        exportAnnotationListAction.setEnabled( false);
        initAction( exportAnnotationListAction, "main.menu.export.annotationlist"); 
        printAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    doPrint();
                }
            };
        printAction.setEnabled( false);
        initAction( printAction, "main.menu.print"); 
        closeAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    if (askCloseDocument()) {
                        frame.hide();
                        frame.dispose();
                    }
                }
            };
        initAction( closeAction, "main.menu.close");

        JMenu menu = new JMenu( JGloss.messages.getString( "main.menu.file"));
        menu.add( createMenuItem( importAction));
        menu.add( createMenuItem( importClipboardAction));
        menu.addSeparator();
        menu.add( createMenuItem( openAction));
        menu.add( createMenuItem( saveAction));
        menu.add( createMenuItem( saveAsAction));
        exportMenu = new JMenu( JGloss.messages.getString( "main.menu.export"));
        exportMenu.setMnemonic( JGloss.messages.getString( "main.menu.export.mk").charAt( 0));
        exportMenu.setEnabled( false);
        exportMenu.add( createMenuItem( exportHTMLAction));
        exportMenu.add( createMenuItem( exportPlainTextAction));
        exportMenu.add( createMenuItem( exportLaTeXAction));
        exportMenu.add( createMenuItem( exportAnnotationListAction));
        menu.add( exportMenu);
        menu.addSeparator();
        menu.add( createMenuItem( printAction));
        menu.addSeparator();
        menu.add( createMenuItem( closeAction));
        bar.add( menu);

        menu.addMenuListener( new MenuListener() {
                public void menuSelected( MenuEvent e) {
                    Transferable t = frame.getToolkit().getSystemClipboard().getContents( this);
                    if (t != null &&
                        (t.isDataFlavorSupported( DataFlavor.getTextPlainUnicodeFlavor()) ||
                        t.isDataFlavorSupported( DataFlavor.stringFlavor))) {
                        importClipboardAction.setEnabled( true);
                    }
                    else
                        importClipboardAction.setEnabled( false);
                }
                public void menuDeselected( MenuEvent e) {}
                public void menuCanceled( MenuEvent e) {}
            });

        preferencesAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    PreferencesFrame.getFrame().show();
                }
            };
        preferencesAction.setEnabled( true);
        initAction( preferencesAction, "main.menu.preferences");

        menu = docpane.getEditMenu();
        menu.addSeparator();
        menu.add( createMenuItem( preferencesAction));
        bar.add( menu);

        compactViewItem = new JCheckBoxMenuItem( JGloss.messages.getString( "main.menu.compactview"));
        compactViewItem.setSelected( JGloss.prefs.getBoolean( Preferences.VIEW_COMPACTVIEW));
        compactViewItem.addActionListener( this);
        showReadingItem = new JCheckBoxMenuItem( JGloss.messages.getString( "main.menu.showreading"));
        showReadingItem.setSelected( JGloss.prefs.getBoolean( Preferences.VIEW_SHOWREADING));
        showReadingItem.addActionListener( this);
        showTranslationItem = new JCheckBoxMenuItem( JGloss.messages.getString
                                                     ( "main.menu.showtranslation"));
        showTranslationItem.setSelected( JGloss.prefs.getBoolean
                                         ( Preferences.VIEW_SHOWTRANSLATION));
        showTranslationItem.addActionListener( this);
        showAnnotationItem = new JCheckBoxMenuItem( JGloss.messages.getString
                                                    ( "main.menu.showannotation"));
        showAnnotationItem.setSelected( JGloss.prefs.getBoolean
                                        ( Preferences.VIEW_SHOWANNOTATION));
        showAnnotationItem.addActionListener( this);
        editorFollowsMouseItem = new JCheckBoxMenuItem( JGloss.messages.getString
                                                    ( "main.menu.editorfollowsmouse"));
        editorFollowsMouseItem.setSelected( JGloss.prefs.getBoolean
                                        ( Preferences.VIEW_EDITORFOLLOWSMOUSE));
        editorFollowsMouseItem.addActionListener( this);

        menu = new JMenu( JGloss.messages.getString( "main.menu.view"));
        menu.add( compactViewItem);
        menu.add( showReadingItem);
        menu.add( showTranslationItem);
        menu.add( showAnnotationItem);
        menu.add( editorFollowsMouseItem);
        bar.add( menu);

        bar.add( annotationEditor.getMenu());

        aboutAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    AboutFrame.getFrame().show();
                }
            };
        initAction( aboutAction, "main.menu.about");
        menu = new JMenu( JGloss.messages.getString( "main.menu.help"));
        menu.add( createMenuItem( aboutAction));
        bar.add( menu);

        frame.setJMenuBar( bar);

        frame.show();
    }

    /**
     * Initializes an action with values taken from the messages resource bundle.
     * The name of the action, keyboard shortcuts and the action tool tip will be
     * initialized if they are available in the resource bundle. The key is taken as key to
     * the name property, the accellerator key property will be accessed by adding ".ak",
     * the mnemonic key property by adding ".mk" and the tooltip by adding ".tt" to the key.
     *
     * @param a The action to initialize.
     * @param key The base key in the messages resource bundle.
     * @see javax.swing.Action
     */
    public static void initAction( Action a, String key) {
        a.putValue( Action.NAME, JGloss.messages.getString( key));

        // accelerator key
        String s = null;
        try {
            s = JGloss.messages.getString( key + ".ak");
        } catch (MissingResourceException ex) {}
        if (s!=null && s.length()>0)
            a.putValue( Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( s));

        // mnemonic key
        try {
            s = JGloss.messages.getString( key + ".mk");
        } catch (MissingResourceException ex) {}
        if (s!=null && s.length()>0) try {
            a.putValue( Action.MNEMONIC_KEY, KeyEvent.class.getField( "VK_" + s.toUpperCase().charAt( 0))
                        .get( null));
        } catch (Exception ex) {
            System.out.println( "Mnemonic Key " + s);
            ex.printStackTrace();
        }
        
        // tooltip
        try {
            s = JGloss.messages.getString( key + ".tt");
        } catch (MissingResourceException ex) {}
        if (s!=null && s.length()>0)
            a.putValue( Action.SHORT_DESCRIPTION, s);
    }

    /**
     * Creates a JMenuItem from an action. All properties from the action, including the
     * accelerator key, will be taken from the action.
     *
     * @param a The action for which to create the menu item.
     * @return The newly created menu item.
     */
    public static JMenuItem createMenuItem( Action a) {
        JMenuItem item = new JMenuItem();
        item.setAction( a);
        KeyStroke stroke = (KeyStroke) a.getValue( Action.ACCELERATOR_KEY);
        if (stroke != null)
            item.setAccelerator( stroke);

        return item;
    }

    /**
     * Creates a container which will expand to fill all additional space in the enclosing
     * container, without expanding the contained component.
     *
     * @param c The component which the space eater should contain.
     * @param horizontal <CODE>true</CODE> if the container should grow horizontal, or
     *                   <CODE>false</CODE> to make it grow vertical.
     * @return The newly created space eater component.
     */
    public static Component createSpaceEater( Component c, boolean horizontal) {
        JPanel se = new JPanel( new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        se.add( c, gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        if (horizontal) {
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 1;
            gbc.weightx = 1;
        }
        else {
            gbc.fill = GridBagConstraints.VERTICAL;
            gbc.gridy = 1;
            gbc.weighty = 1;
        }
        se.add( Box.createGlue(), gbc);

        return se;
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
                ( frame, JGloss.messages.getString( "main.dialog.close.message",
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
            default: // propably CLOSED_OPTION
                // leave documentChanged untouched to prevent closing the window
                break;
            }                                   
        }

        return !documentChanged;
    }

    /**
     * Runs the import dialog and opens the document the user selected.
     *
     * @see ImportDialog
     */
    private void doImport() {
        ImportDialog d = new ImportDialog( frame);
        if (d.doDialog()) {
            JGlossFrame which = this;
            if (documentLoaded)
                which = new JGlossFrame();

            which.importDocument( d.getSelection(), d.getReadingStart(), 
                                  d.getReadingEnd(), d.getEncoding());
            which.documentChanged = true;
        }
    }

    /**
     * Imports the content of the clipboard, if it contains plain text.
     */
    private void doImportClipboard() {
        Transferable t = frame.getToolkit().getSystemClipboard().getContents( this);
        DataFlavor plain = DataFlavor.getTextPlainUnicodeFlavor();

        if (t != null) {
            try {
                Reader in = null;
                int len = 0;
                if (t.isDataFlavorSupported( plain)) {
                    in = plain.getReaderForText( t);
                    // no length information available
                }
                else if (t.isDataFlavorSupported( DataFlavor.stringFlavor)) {
                    String data = (String) t.getTransferData( DataFlavor.stringFlavor);
                    len = data.length();
                    in = new StringReader( data);
                }

                if (in != null) {
                    JGlossFrame which = this;
                    if (documentLoaded)
                        which = new JGlossFrame();
                    
                    which.loadDocument
                        ( new HTMLifyReader( in),
                          JGloss.messages.getString( "import.clipboard"),
                          JGloss.messages.getString( "import.clipboard"),
                          new Parser( Dictionaries.getDictionaries()), true, len);
                    which.documentChanged = true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( frame, JGloss.messages.getString
                      ( "error.import.exception", new Object[] 
                          { "import.clipboard", ex.getLocalizedMessage() }),
                      JGloss.messages.getString( "error.import.title"),
                      JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
            }
        }
        else // method was called with empty clipboard
            System.err.println( "Somebody stole my clipboard content");
    }

    /**
     * Sets up everything neccessary to import a file and loads it. If <CODE>filename</CODE> is
     * a URL, it will create a reader which reads from the location the document points to. If it
     * is a path to a local file, it will create a reader which reads from it. In both cases, 
     * if the document is not already in HTML format, it will be 
     * converted to HTML. The method will then call <CODE>loadDocument</CODE> with the newly created reader.
     *
     * @param path URL or path of the file to import.
     * @param readingStart Character which delimits a reading annotation in the document. May be '\0' if
     *                  it is unused.
     * @param readingEnd Character which delimits a reading annotation in the document. May be '\0' if
     *                it is unused.
     * @param encoding Character encoding of the file. May be either <CODE>null</CODE> or the
     *                 value of the "encodings.default" resource to use autodetection.
     * @see #loadDocument(Reader,String,String,Parser,boolean,int)
     */
    private void importDocument( String path, char readingStart, char readingEnd, String encoding) {
        try {
            Reader in = null;
            int contentlength = 0;
            String contenttype = "text/plain";
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

            if (!contenttype.equals( "text/html"))
                in = new HTMLifyReader( in);

            loadDocument( in, path, title, 
                          new Parser( Dictionaries.getDictionaries(), readingStart, readingEnd), true,
                          CharacterEncodingDetector.guessLength( contentlength, encoding));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( frame, JGloss.messages.getString
                  ( "error.import.exception", new Object[] 
                      { path, ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.import.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads a JGloss document from a local file.
     *
     * @param path The path of the file to load.
     */
    private void loadDocument( String path) {
        try {
            File f = new File( path);
            Reader in = new InputStreamReader( new FileInputStream( f), "UTF-8");
            loadDocument( in, path, f.getName(), 
                          new Parser( Dictionaries.getDictionaries()), false,
                          CharacterEncodingDetector.guessLength( (int) f.length(), "UTF-8"));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( frame, JGloss.messages.getString
                  ( "error.load.exception", new Object[] 
                      { path, ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.load.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads a document into this JGlossFrame. This will completely set up the frame for editing.
     * This can be used to either open a JGloss document or to import some japanese text, depending
     * of the setting of <CODE>addAnnotations</CODE>.
     *
     * @param in The reader from which to load the document.
     * @param path The location of the document which was used to create the reader.
     * @param title Title of the file. This will normally be the filename component of the path.
     * @param parser The parser to use to annotate japanese text.
     * @param addAnnotations <CODE>true</CODE> if annotations should be added while loading the
     *                       document.
     * @param length Approximate length in characters of the document to load. This will only be used
     *               for the progress bar.
     * @exception Exception if something goes wrong while loading the document.
     */
    private void loadDocument( Reader in, String path, String title, Parser parser, 
                               boolean addAnnotations, int length) 
        throws Exception {
        kit = new JGlossEditorKit( parser, addAnnotations, compactViewItem.isSelected(),
                                   showReadingItem.isSelected(),
                                   showTranslationItem.isSelected());
        doc = (JGlossDocument) kit.createDefaultDocument();
        StyleDialog.getComponent().addStyleSheet( doc.getStyleSheet(), getAdditionalStyles());
            
        documentName = title;
        frame.setTitle( title + ":" + JGloss.messages.getString( "main.title"));

        final StopableReader stin = new StopableReader( in);
        // run import in own thread so we can monitor progress
        final EditorKit tkit = kit;
        final Document tdoc = doc;
        final String tpath = path;
        Thread t = new Thread() {
                public void run() {
                    try {
                        tkit.read( stin, tdoc, 0);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showConfirmDialog
                            ( frame, JGloss.messages.getString
                              ( "error.import.exception", new Object[] 
                                  { tpath, ex.getLocalizedMessage() }),
                              JGloss.messages.getString( "error.import.title"),
                              JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
        t.start();

        ProgressMonitor pm = new ProgressMonitor
            ( frame, JGloss.messages.getString( "load.progress", 
                                                new Object[] { path }),
              null, 0, length);
        while (t.isAlive()) {
            try {
                t.join( 1000);
                pm.setProgress( ((JGlossDocument) doc).getParsePosition());
                if (pm.isCanceled()) {
                    t.interrupt();
                    stin.stop();
                    t.join();
                }
            } catch (InterruptedException ex) {}
        }
        in.close();

        docpane.setEditorKit( kit);
        docpane.setStyledDocument( doc);
        doc.setAddAnnotations( false);
        exportMenu.setEnabled( true);
        exportPlainTextAction.setEnabled( true);
        exportLaTeXAction.setEnabled( true);
        exportHTMLAction.setEnabled( true);
        exportAnnotationListAction.setEnabled( true);
        printAction.setEnabled( true);
        if (documentPath == null) // this means that the document is imported, save will behave like
                                  // save as
            saveAction.setEnabled( true);
        saveAsAction.setEnabled( true);

        annotationEditor.setDocument( doc.getDefaultRootElement(), docpane);
        annotationEditor.expandAll();

        docpane.followMouse( showAnnotationItem.isSelected(),
                             editorFollowsMouseItem.isSelected());

        frame.getContentPane().removeAll();
        frame.getContentPane().add( split);
        frame.getContentPane().validate();
        if (JGloss.prefs.getBoolean( Preferences.VIEW_ANNOTATIONEDITORHIDDEN))
            split.setDividerLocation( 1.0f);

        documentLoaded = true;

        doc.addDocumentListener( new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    markChanged();
                }
                public void removeUpdate(DocumentEvent e) {
                    markChanged();
                }
                public void changedUpdate(DocumentEvent e) {
                    markChanged();
                }
            });

        pm.close();
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
            else if (e.getSource()==showAnnotationItem || e.getSource()==editorFollowsMouseItem) {
                JGloss.prefs.set( Preferences.VIEW_SHOWANNOTATION, showAnnotationItem.isSelected());
                JGloss.prefs.set( Preferences.VIEW_EDITORFOLLOWSMOUSE, editorFollowsMouseItem.isSelected());
                docpane.followMouse( showAnnotationItem.isSelected(),
                                     editorFollowsMouseItem.isSelected());
            }
        }
    }

    /**
     * Returns styles to be used in the style sheet specific to this document.
     * This is currenty unused.
     *
     * @return A map from html tag names to CSS styles.
     */
    private java.util.Map getAdditionalStyles() {
        java.util.Map out = new HashMap();

        return out;
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
            PrintJob job = frame.getToolkit().getPrintJob
                ( frame, JGloss.messages.getString( "print.title"), ja, pa);

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
                documentLoaded = true;
            }
        }
        
    }

    /**
     * Saves the document in JGloss format. This is basically HTML using a UTF-8 character
     * encoding.
     */
    private void saveDocument() {
        try {
            Writer out = new OutputStreamWriter( new FileOutputStream( documentPath), "UTF-8");
            JGlossWriter w = new JGlossWriter( out, "UTF-8", doc);
            w.write();
            out.close();
            documentChanged = false;
            saveAction.setEnabled( false);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showConfirmDialog
                ( frame, JGloss.messages.getString
                  ( "error.save.exception", new Object[] 
                      { documentPath, ex.getLocalizedMessage() }),
                  JGloss.messages.getString( "error.save.title"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
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
        JFileChooser f = new JFileChooser( path);
        f.setFileHidingEnabled( true);
        f.addChoosableFileFilter( jglossFileFilter);
        int r = f.showSaveDialog( frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            documentPath =  f.getSelectedFile().getAbsolutePath();
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            frame.setTitle( f.getSelectedFile().getName() + 
                            ":" + JGloss.messages.getString( "main.title"));
            saveDocument();
        }
    }

    /**
     * Runs a file chooser dialog, and if the user accepts opens the selected JGloss document,
     * either in a new JGlossFrame, or in this if it is currently unused.
     */
    private void openDocument() {
        JFileChooser f = new JFileChooser( JGloss.getCurrentDir());
        f.addChoosableFileFilter( jglossFileFilter);
        f.setFileHidingEnabled( true);
        int r = f.showOpenDialog( frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            JGlossFrame which = documentLoaded ? new JGlossFrame() : this;
            which.documentPath = f.getSelectedFile().getAbsolutePath();
            which.loadDocument( f.getSelectedFile().getAbsolutePath());
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
        JFileChooser f = new JFileChooser( JGloss.getCurrentDir());
        f.setDialogTitle( JGloss.messages.getString( "export.plaintext.title"));
        f.setFileHidingEnabled( true);

        // setup the encoding chooser
        JPanel p = new JPanel();
        p.setLayout( new GridLayout( 1, 1));
        Box b2 = Box.createVerticalBox();
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
        b2.add( b);
        b2.add( Box.createVerticalStrut( 3));

        b = Box.createHorizontalBox();
        JCheckBox writeReading = new JCheckBox( JGloss.messages.getString( "export.writereading"));
        writeReading.setSelected( JGloss.prefs.getBoolean( Preferences.EXPORT_PLAINTEXT_WRITEREADING));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( writeReading, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        b = Box.createHorizontalBox();
        JCheckBox writeTranslations = 
            new JCheckBox( JGloss.messages.getString( "export.writetranslations"));        
        writeTranslations.setSelected( JGloss.prefs.getBoolean
                                       ( Preferences.EXPORT_PLAINTEXT_WRITETRANSLATIONS));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( writeTranslations, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        b = Box.createHorizontalBox();
        JCheckBox writeHidden = 
            new JCheckBox( JGloss.messages.getString( "export.writehidden"));        
        writeHidden.setSelected( JGloss.prefs.getBoolean
                                 ( Preferences.EXPORT_PLAINTEXT_WRITEHIDDEN));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( writeHidden, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        p.add( createSpaceEater( b2, false));
        f.setAccessory( p);

        int r = f.showSaveDialog( frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            Writer out = null;
            try {
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()),
                      (String) encodings.getSelectedItem()));
                JGloss.prefs.set( Preferences.EXPORT_ENCODING, (String) encodings.getSelectedItem());
                JGloss.prefs.set( Preferences.EXPORT_PLAINTEXT_WRITEREADING,
                                  writeReading.isSelected());
                JGloss.prefs.set( Preferences.EXPORT_PLAINTEXT_WRITETRANSLATIONS,
                                  writeTranslations.isSelected());
                JGloss.prefs.set( Preferences.EXPORT_PLAINTEXT_WRITEHIDDEN,
                                  writeHidden.isSelected());
                PlainTextExporter.export( doc, (AnnotationModel) annotationEditor.getModel(),
                                          out, writeReading.isSelected(), writeTranslations.isSelected(),
                                          writeHidden.isSelected());
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( frame, JGloss.messages.getString
                      ( "error.export.exception", new Object[] 
                          { documentPath, ex.getLocalizedMessage() }),
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
        JFileChooser f = new JFileChooser( JGloss.getCurrentDir());
        f.setDialogTitle( JGloss.messages.getString( "export.latex.title"));
        f.setFileHidingEnabled( true);
        f.setFileFilter( new javax.swing.filechooser.FileFilter() {
                public boolean accept( File file) {
                    return file.isDirectory() ||
                        file.getName().toLowerCase().endsWith( ".tex");
                }  
                public String getDescription() { 
                    return JGloss.messages.getString( "filefilter.latex.description");
                }
            });


        // setup the encoding chooser
        JPanel p = new JPanel();
        p.setLayout( new GridLayout( 1, 1));
        Box b2 = Box.createVerticalBox();
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
        b2.add( b);
        b2.add( Box.createVerticalStrut( 3));

        b = Box.createHorizontalBox();
        JCheckBox writeReading = new JCheckBox( JGloss.messages.getString( "export.writereading"));
        writeReading.setSelected( JGloss.prefs.getBoolean( Preferences.EXPORT_LATEX_WRITEREADING));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( writeReading, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        b = Box.createHorizontalBox();
        JCheckBox writeTranslations = 
            new JCheckBox( JGloss.messages.getString( "export.writetranslations"));        
        writeTranslations.setSelected( JGloss.prefs.getBoolean
                                       ( Preferences.EXPORT_LATEX_WRITETRANSLATIONS));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( writeTranslations, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        b = Box.createHorizontalBox();
        JCheckBox writeHidden = 
            new JCheckBox( JGloss.messages.getString( "export.writehidden"));        
        writeHidden.setSelected( JGloss.prefs.getBoolean
                                 ( Preferences.EXPORT_LATEX_WRITEHIDDEN));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( writeHidden, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        p.add( createSpaceEater( b2, false));
        f.setAccessory( p);

        int r = f.showSaveDialog( frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            Writer out = null;
            try {
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()),
                      (String) encodings.getSelectedItem()));
                JGloss.prefs.set( Preferences.EXPORT_ENCODING, (String) encodings.getSelectedItem());
                JGloss.prefs.set( Preferences.EXPORT_LATEX_WRITEREADING,
                                  writeReading.isSelected());
                JGloss.prefs.set( Preferences.EXPORT_LATEX_WRITETRANSLATIONS,
                                  writeTranslations.isSelected());
                JGloss.prefs.set( Preferences.EXPORT_LATEX_WRITEHIDDEN,
                                  writeHidden.isSelected());
                LaTeXExporter.export( doc, (AnnotationModel) annotationEditor.getModel(),
                                      out, writeReading.isSelected(), writeTranslations.isSelected(),
                                      writeHidden.isSelected());
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( frame, JGloss.messages.getString
                      ( "error.export.exception", new Object[] 
                          { documentPath, ex.getLocalizedMessage() }),
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
     * Exports the document as HTML to a user-specified file.
     */
    private void doExportHTML() {
        JFileChooser f = new JFileChooser( JGloss.getCurrentDir());
        f.setDialogTitle( JGloss.messages.getString( "export.html.title"));
        f.setFileHidingEnabled( true);
        f.setFileFilter( new javax.swing.filechooser.FileFilter() {
                public boolean accept( File file) {
                    return file.isDirectory() ||
                        file.getName().toLowerCase().endsWith( ".html");
                }  
                public String getDescription() { 
                    return JGloss.messages.getString( "filefilter.html.description");
                }
            });

        // setup the encoding chooser
        JPanel p = new JPanel();
        p.setLayout( new GridLayout( 1, 1));
        Box b2 = Box.createVerticalBox();
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
        b2.add( b);
        b2.add( Box.createVerticalStrut( 3));

        b = Box.createHorizontalBox();
        JCheckBox interactive = 
            new JCheckBox( JGloss.messages.getString( "export.html.interactive"));        
        interactive.setSelected( JGloss.prefs.getBoolean
                                       ( Preferences.EXPORT_HTML_INTERACTIVE));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( interactive, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        b = Box.createHorizontalBox();
        JCheckBox backwardsCompatible = 
            new JCheckBox( JGloss.messages.getString( "export.html.backwardscompatible"));        
        backwardsCompatible.setSelected( JGloss.prefs.getBoolean
                                       ( Preferences.EXPORT_HTML_BACKWARDSCOMPATIBLE));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( backwardsCompatible, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        b = Box.createHorizontalBox();
        JCheckBox writeReading = new JCheckBox( JGloss.messages.getString( "export.writereading"));
        writeReading.setSelected( JGloss.prefs.getBoolean( Preferences.EXPORT_HTML_WRITEREADING));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( writeReading, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);
        b2.add( Box.createVerticalStrut( 3));

        b = Box.createHorizontalBox();
        JCheckBox writeTranslations = 
            new JCheckBox( JGloss.messages.getString( "export.writetranslations"));        
        writeTranslations.setSelected( JGloss.prefs.getBoolean
                                       ( Preferences.EXPORT_HTML_WRITETRANSLATIONS));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( writeTranslations, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        b = Box.createHorizontalBox();
        JCheckBox writeHidden = 
            new JCheckBox( JGloss.messages.getString( "export.writehidden"));        
        writeHidden.setSelected( JGloss.prefs.getBoolean
                                 ( Preferences.EXPORT_HTML_WRITEHIDDEN));
        b.add( Box.createHorizontalStrut( 3));
        b.add( createSpaceEater( writeHidden, true));
        b.add( Box.createHorizontalStrut( 3));
        b2.add( b);

        p.add( createSpaceEater( b2, false));
        f.setAccessory( p);

        int r = f.showSaveDialog( frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            Writer out = null;
            try {
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()),
                      (String) encodings.getSelectedItem()));
                JGloss.prefs.set( Preferences.EXPORT_ENCODING, (String) encodings.getSelectedItem());
                JGloss.prefs.set( Preferences.EXPORT_HTML_WRITEREADING,
                                  writeReading.isSelected());
                JGloss.prefs.set( Preferences.EXPORT_HTML_WRITETRANSLATIONS,
                                  writeTranslations.isSelected());
                JGloss.prefs.set( Preferences.EXPORT_HTML_BACKWARDSCOMPATIBLE,
                                  backwardsCompatible.isSelected());
                JGloss.prefs.set( Preferences.EXPORT_HTML_INTERACTIVE,
                                  interactive.isSelected());
                JGloss.prefs.set( Preferences.EXPORT_HTML_WRITEHIDDEN,
                                  writeHidden.isSelected());
                new HTMLExporter( out, (String) encodings.getSelectedItem(), doc,
                                  writeReading.isSelected(), writeTranslations.isSelected(),
                                  backwardsCompatible.isSelected(),
                                  interactive.isSelected(), writeHidden.isSelected()).write();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( frame, JGloss.messages.getString
                      ( "error.export.exception", new Object[] 
                          { documentPath, ex.getLocalizedMessage() }),
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
        String path;
        if (documentPath == null)
            path = JGloss.getCurrentDir();
        else
            path = new File( documentPath).getPath();
        JFileChooser f = new JFileChooser( path);
        f.setDialogTitle( JGloss.messages.getString( "export.annotationlist.title"));
        f.setFileHidingEnabled( true);

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
        p.add( createSpaceEater( b, false));
        f.setAccessory( p);

        int r = f.showSaveDialog( frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
            Writer out = null;
            try {
                out = new BufferedWriter( new OutputStreamWriter
                    ( new FileOutputStream( f.getSelectedFile()),
                      (String) encodings.getSelectedItem()));
                JGloss.prefs.set( Preferences.EXPORT_ENCODING, (String) encodings.getSelectedItem());
                out.write( JGloss.messages.getString( "export.annotationlist.header",
                                                      new Object[] 
                    { documentName, (String) encodings.getSelectedItem() }));
                // do not output duplicate annotations:
                Set seenAnnotations = new TreeSet();
                for ( Iterator i=((AnnotationModel) annotationEditor.getModel()).getAnnotationNodes();
                      i.hasNext(); ) {
                    AnnotationNode an = (AnnotationNode) i.next();
                    if (!an.isHidden()) {
                        String word = null;
                        String reading = null;
                        String translation = null;
                        // if the linked annotation entry is an inflected verb or adjective,
                        // output the full verb instead of just the kanji part
                        Parser.TextAnnotation ta = an.getLinkedAnnotation();
                        if (ta != null) {
                            if (ta instanceof Translation) {
                                DictionaryEntry de = ((Translation) ta).getDictionaryEntry();
                                word = de.getWord();
                                reading = de.getReading();
                            }
                        }
                        if (word == null) { // no verb or adjective, just use the annotation text
                            word = an.getKanjiText();
                            reading = an.getReadingNode().getText();
                        }
                        translation = an.getTranslationNode().getText();

                        String line = word;
                        if (reading!=null && !reading.equals( " "))
                            line += " [" + reading + "]";
                        if (translation!=null && !translation.equals( " "))
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
                    ( frame, JGloss.messages.getString
                      ( "error.export.exception", new Object[] 
                          { documentPath, ex.getLocalizedMessage() }),
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
} // class JGlossFrame
