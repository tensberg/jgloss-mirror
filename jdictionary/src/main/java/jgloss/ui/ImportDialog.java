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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.dictionary.Dictionary;
import jgloss.parser.Chasen;
import jgloss.parser.ChasenParser;
import jgloss.parser.Parser;
import jgloss.parser.ReadingAnnotationFilter;
import jgloss.ui.util.UIUtilities;
import jgloss.util.CharacterEncodingDetector;

/**
 * Dialog which allows the user to select a file to import. The user can also specify the
 * character encoding and the reading annotation delimiter chars.
 *
 * @author Michael Koch
 */
public class ImportDialog extends JDialog implements TextListener {
    private static final long serialVersionUID = 1L;
	/**
     * Path to the selected file or a URL.
     */
    private final JTextField filename;
    /**
     * Area in which the user can enter some text, which should be imported if no filename is specified.
     */
    private final TextArea pastearea;
    /**
     * Remember if the text area was empty prior to a text changed event.
     */
    private boolean pasteareaWasEmpty = true;
    /**
     * Pane which lets the user choose between entering a file name and the text itself.
     */
    private final JTabbedPane selectionPane;
    /**
     * Widget to select the character encoding of the file.
     */
    private final JComboBox encodings;
    /**
     * Used for choosing the parser for this import.
     */
    private final ParserSelector parserSelector;
    /**
     * Result of the dialog run. <CODE>true</CODE> if the user hit OK.
     */
    private boolean result;

    /**
     * Construct a new import dialog with the specified frame as parent.
     *
     * @param parent Parent of this dialog.
     */
    @SuppressWarnings("unchecked")
    public ImportDialog( Frame parent) {
        super( parent, JGloss.MESSAGES.getString( "import.title"));
        setModal( true);

        JPanel main = new JPanel( new BorderLayout());
        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalGlue());
        final Action ok = new AbstractAction() {
            private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    if (getSelection().length() > 0) {
                        result = true;
                        ImportDialog.this.setVisible(false);
                        JGloss.PREFS.set( Preferences.IMPORT_PARSER, 
                                          parserSelector.getSelectedParser().getName());
                        JGloss.PREFS.set( Preferences.IMPORT_FIRSTOCCURRENCE,
                                          parserSelector.isFirstOccurrenceOnly());
                        JGloss.PREFS.set( Preferences.IMPORT_DETECTPARAGRAPHS,
                                          parserSelector.isDetectParagraphs());
                        if (parserSelector.isNoReadingBrackets()) {
	                        JGloss.PREFS.set( Preferences.IMPORT_READINGBRACKETS, "");
                        } else {
	                        JGloss.PREFS.set( Preferences.IMPORT_READINGBRACKETS,
                                              new String( new char[] { parserSelector.getReadingStart(),
                                                                       parserSelector.getReadingEnd() }));
                        }
                    }
                }
            };
        ok.setEnabled( true);
        UIUtilities.initAction( ok, "button.import");
        final Action cancel = new AbstractAction() {
                /**
             * 
             */
            private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    result = false;
                    ImportDialog.this.setVisible(false);
                }
            };
        cancel.setEnabled( true);
        UIUtilities.initAction( cancel, "button.cancel");
        JButton bok = new JButton( ok);
        getRootPane().setDefaultButton( bok);
        b.add( bok);
        b.add( Box.createHorizontalStrut( 5));
        b.add( new JButton( cancel));
        b.add( Box.createHorizontalStrut( 5));
        main.add( b, BorderLayout.SOUTH);

        this.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener( new WindowAdapter() {
                @Override
				public void windowClosing( WindowEvent e) {
                    cancel.actionPerformed( null);
                }
            });        

        UIUtilities.setCancelAction( this, cancel);

        b = Box.createVerticalBox();

        selectionPane = new JTabbedPane( SwingConstants.TOP);
        Box b2 = Box.createVerticalBox();
        Box b3 = Box.createHorizontalBox();
        JLabel l = new JLabel( JGloss.MESSAGES.getString( "import.urlorfile"));
        b3.add( l);
        b3.add( Box.createHorizontalGlue());
        b2.add( b3);
        b2.add( Box.createVerticalStrut( 10));
        b3 = Box.createHorizontalBox();
        filename = new JTextField();
        b3.add( filename);
        
        final Action choosefile = new AbstractAction() {
                /**
             * 
             */
            private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    JFileChooser f = new JFileChooser( JGloss.getApplication().getCurrentDir());
                    f.setFileHidingEnabled( true);
                    f.setFileView( CustomFileView.getFileView());
                    int r = f.showOpenDialog( ImportDialog.this);
                    if (r == JFileChooser.APPROVE_OPTION) {
                        filename.setText( f.getSelectedFile().getAbsolutePath());
                        JGloss.getApplication().setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
                    }
                }
            };
        choosefile.setEnabled( true);
        UIUtilities.initAction( choosefile, "import.button.choosefile");
        b3.add( new JButton( choosefile));
        b2.add( b3);
        b2.add( Box.createVerticalStrut( 10));

        Vector<String> v = new Vector<String>();
        v.add( JGloss.MESSAGES.getString( "encodings.default"));
        String[] enc = JGloss.PREFS.getList( Preferences.ENCODINGS, ',');
        for (String element : enc) {
	        v.add( element);
        }
        encodings = new JComboBox( v);
        encodings.setEditable( true);

        b3 = Box.createHorizontalBox();
        b3.add( new JLabel( JGloss.MESSAGES.getString( "import.encodings")));
        b3.add( Box.createHorizontalStrut( 3));
        b3.add( encodings);
        b3.add( Box.createHorizontalGlue());
        b2.add( b3);
        selectionPane.add( JGloss.MESSAGES.getString( "import.file"), b2);

        final Box b4 = Box.createVerticalBox();
        b3 = Box.createHorizontalBox();
        l = new JLabel( JGloss.MESSAGES.getString( "import.pastetext"));
        b3.add( l);
        b3.add( Box.createHorizontalGlue());
        b4.add( b3);
        // An AWT TextArea is used because JDK1.3 AWT cut&paste is implemented differently than
        // Swing cut&paste and works with X11 primary selection copying and MacOSX.
        pastearea = new TextArea( 3, 50);
        pastearea.setEditable( true);
        pastearea.setFont( filename.getFont());
        // use Swing colors
        pastearea.setBackground( filename.getBackground());
        pastearea.setForeground( filename.getForeground());
        b4.add( pastearea);
        pastearea.addTextListener( this);
        b4.add( Box.createVerticalStrut( 5));
        selectionPane.addTab( JGloss.MESSAGES.getString( "import.text"), b4);

        // Mixing AWT "heavyweight" and Swing "lightweight" components is nasty.
        // To get the layering in the tabbed pane correct, I have to remove the paste area
        // if the tab is not selected.
        selectionPane.addChangeListener( new ChangeListener() {
                @Override
				public void stateChanged( ChangeEvent e) {
                    pastearea.setVisible( selectionPane.getSelectedIndex() == 1);
                }
            });
        b.add( selectionPane);
        b.add( Box.createVerticalStrut( 10));

        parserSelector = new ParserSelector( true);
        parserSelector.setBorder( BorderFactory.createTitledBorder 
                                  ( JGloss.MESSAGES.getString( "import.parserselector")));
        try {
            parserSelector.setSelected( (Class<? extends Parser>) Class.forName( JGloss.PREFS.getString( Preferences.IMPORT_PARSER)));
        } catch (ClassNotFoundException ex) {}
        parserSelector.setFirstOccurrenceOnly( JGloss.PREFS.getBoolean
                                               ( Preferences.IMPORT_FIRSTOCCURRENCE, true));
        parserSelector.setDetectParagraphs( JGloss.PREFS.getBoolean
                                            ( Preferences.IMPORT_DETECTPARAGRAPHS, true));
        String brackets = JGloss.PREFS.getString( Preferences.IMPORT_READINGBRACKETS);
        if (brackets.length() == 2) {
	        parserSelector.setReadingBrackets( brackets.charAt( 0), brackets.charAt( 1));
        } else {
	        parserSelector.setNoReadingBrackets();
        }
        parserSelector.setEnabled( ChasenParser.class, Chasen.isChasenExecutable
                                   ( Chasen.getDefaultExecutable()));
        JGloss.PREFS.addPropertyChangeListener( new PropertyChangeListener() {
                @Override
				public void propertyChange( PropertyChangeEvent e) {
                    if (e.getPropertyName().equals( Preferences.CHASEN_LOCATION)) {
	                    parserSelector.setEnabled( ChasenParser.class, Chasen.isChasenExecutable
                                                   ( (String) e.getNewValue()));
                    }
                }
            });

        b.add( parserSelector);
        b.add( Box.createVerticalStrut( 10));
        b.add( Box.createVerticalGlue());

        main.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10));
        main.add( b, BorderLayout.CENTER);
        this.getContentPane().add( main);

        this.pack();
        this.setResizable( false);
        pastearea.setVisible( false);
    }

    /**
     * Displays the dialog and manages user input. Since this dialog is modal, the
     * method will not return until the user has closed the dialog.
     *
     * @return <CODE>true</CODE>, if the user selected the OK button.
     */
    public boolean doDialog() {
        this.setVisible(true); // blocks until dialog is closed
        this.dispose();

        return result;
    }

    /**
     * Returns the filename or url of the file which should be imported.
     *
     * @return The filename or url.
     */
    public String getSelection() {
        if (selectionIsFilename()) {
	        return filename.getText();
        } else {
	        return pastearea.getText();
        }
    }

    /**
     * Returns if the selection returned by {@link #getSelection() getSelection} should be as URL or
     * filename, or if it is the text which is to be imported.
     */
    public boolean selectionIsFilename() {
        return selectionPane.getSelectedIndex() == 0;
    }

    /**
     * Returns the character encoding which the user entered.
     *
     * @return The character encoding.
     */
    public String getEncoding() {
        return (String) encodings.getSelectedItem();
    }

    /**
     * Creates a new parser instance using the parser class selected by the user.
     */
    public Parser createParser( Dictionary[] dictionaries, Set<String> exclusions) {
        return parserSelector.createParser( dictionaries, exclusions);
    }

    /**
     * Creates a new reading annotation filter using the user-selected reading brackets.
     */
    public ReadingAnnotationFilter createReadingAnnotationFilter() {
        return parserSelector.createReadingAnnotationFilter();
    }

    /**
     * Convert the encoding of text pasted in the paste text area on the fly if it wasn't converted
     * correctly by the Java environment. This is only done if the text area was empty prior to the
     * change which triggered the event.
     */
    @Override
	public void textValueChanged( TextEvent e) {
        String text = pastearea.getText();
        if (!pasteareaWasEmpty) {
            pasteareaWasEmpty = text.length() == 0;
            return;
        }
        if (text.length() == 0) {
	        return;
        }

        pasteareaWasEmpty = false;
        pastearea.setEditable( false);

        // construct a new string the hacky way because sometimes the charset is ignored
        // when pasting japanese text from the X primary selection
        byte[] tb = new byte[text.length()];
        boolean convert = true;
        for ( int i=0; i<text.length(); i++) {
            char c = text.charAt( i);
            if (c > 255) { 
                // non-ISO-8859-1, seems that the paste operation did not ignore the charset
                convert = false;
                break;
            }
            tb[i] = (byte) c;
        }

        if (convert) {
            String enc = CharacterEncodingDetector.guessEncodingName( tb);
            if (!enc.equals( CharacterEncodingDetector.ENC_UTF_8)) { // don't trust UTF-8 detection
                try {
                    pastearea.setText( new String( tb, enc));
                    pastearea.setCaretPosition( pastearea.getText().length());
                    // the text changed event triggered by this setText is ignored because
                    // pasteareaWasEmpty is already set to false
                } catch (java.io.UnsupportedEncodingException ex) {}
            }
        }

        pastearea.setEditable( true);
    }

    /**
     * Returns the state of the paragraph detection checkbox. If the box is selected, the corresponding
     * option of the {@link jgloss.ui.xml.JGlossifyReader JGlossifyReader} should be set.
     */
    public boolean isDetectParagraphs() {
        return parserSelector.isDetectParagraphs();
    }
} // class ImportDialog
