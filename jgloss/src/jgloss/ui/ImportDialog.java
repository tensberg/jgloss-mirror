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

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.*;


/**
 * Dialog which allows the user to select a file to import. The user can also specify the
 * character encoding and the reading annotation delimiter chars.
 *
 * @author Michael Koch
 */
public class ImportDialog extends JDialog {
    /**
     * Path to the selected file.
     */
    private JTextField selection;
    /**
     * Widget to select the character encoding of the file.
     */
    private JComboBox encodings;
    /**
     * Used for choosing the parser for this import.
     */
    private ParserSelector parserSelector;
    /**
     * Result of the dialog run. <CODE>true</CODE> if the user hit OK.
     */
    private boolean result;

    /**
     * Construct a new import dialog with the specified frame as parent.
     *
     * @param parent Parent of this dialog.
     */
    public ImportDialog( Frame parent) {
        super( parent, JGloss.messages.getString( "import.title"));
        setModal( true);

        JPanel main = new JPanel( new BorderLayout());
        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalGlue());
        final Action ok = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    if (getSelection().length() > 0) {
                        result = true;
                        ImportDialog.this.hide();
                        JGloss.prefs.set( Preferences.IMPORT_PARSER, 
                                          parserSelector.getSelectedParser().getName());
                    }
                }
            };
        ok.setEnabled( true);
        UIUtilities.initAction( ok, "button.import");
        final Action cancel = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    result = false;
                    ImportDialog.this.hide();
                }
            };
        cancel.setEnabled( true);
        UIUtilities.initAction( cancel, "button.cancel");
        JButton bok = new JButton( ok);
        bok.setDefaultCapable( true);
        b.add( bok);
        b.add( Box.createHorizontalStrut( 5));
        b.add( new JButton( cancel));
        b.add( Box.createHorizontalStrut( 5));
        main.add( b, BorderLayout.SOUTH);

        this.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent e) {
                    cancel.actionPerformed( null);
                }
            });        

        b = Box.createVerticalBox();
        Box b2 = Box.createHorizontalBox();
        JLabel l = new JLabel( JGloss.messages.getString( "import.urlorfile"));
        b2.add( l);
        b2.add( Box.createHorizontalGlue());
        b.add( b2);
        b.add( Box.createVerticalStrut( 10));
        b2 = Box.createHorizontalBox();
        selection = new JTextField();
        b2.add( selection);
        
        final Action choosefile = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    JFileChooser f = new JFileChooser( JGloss.getCurrentDir());
                    f.setFileHidingEnabled( true);
                    int r = f.showOpenDialog( ImportDialog.this);
                    if (r == JFileChooser.APPROVE_OPTION) {
                        selection.setText( f.getSelectedFile().getAbsolutePath());
                        JGloss.setCurrentDir( f.getCurrentDirectory().getAbsolutePath());
                    }
                }
            };
        choosefile.setEnabled( true);
        UIUtilities.initAction( choosefile, "import.button.choosefile");
        b2.add( new JButton( choosefile));
        b.add( b2);
        b.add( Box.createVerticalStrut( 10));

        java.util.Vector v = new java.util.Vector();
        v.add( JGloss.messages.getString( "encodings.default"));
        String[] enc = JGloss.prefs.getList( Preferences.ENCODINGS, ',');
        for ( int i=0; i<enc.length; i++)
            v.add( enc[i]);
        encodings = new JComboBox( v);
        encodings.setEditable( true);

        b2 = Box.createHorizontalBox();
        b2.add( new JLabel( JGloss.messages.getString( "import.encodings")));
        b2.add( Box.createHorizontalStrut( 3));
        b2.add( encodings);
        b2.add( Box.createHorizontalGlue());
        b.add( b2);
        b.add( Box.createVerticalStrut( 5));

        parserSelector = new ParserSelector( true);
        parserSelector.setBorder( BorderFactory.createTitledBorder 
                                  ( JGloss.messages.getString( "import.parserselector")));
        try {
            parserSelector.setSelected( Class.forName( JGloss.prefs.getString( Preferences.IMPORT_PARSER)));
        } catch (ClassNotFoundException ex) {}
        parserSelector.setEnabled( ChasenParser.class, ChasenParser.isChasenExecutable
                                   ( ChasenParser.getDefaultExecutable()));
        JGloss.prefs.addPropertyChangeListener( new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent e) {
                    if (e.getPropertyName().equals( Preferences.CHASEN_LOCATION))
                        parserSelector.setEnabled( ChasenParser.class, ChasenParser.isChasenExecutable
                                                   ( (String) e.getNewValue()));
                };
            });

        b.add( parserSelector);
        b.add( Box.createVerticalStrut( 10));
        b.add( Box.createVerticalGlue());

        main.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10));
        main.add( b, BorderLayout.CENTER);
        this.getContentPane().add( main);

        this.pack();
        this.setResizable( false);
    }

    /**
     * Displays the dialog and manages user input. Since this dialog is modal, the
     * method will not return until the user has closed the dialog.
     *
     * @return <CODE>true</CODE>, if the user selected the OK button.
     */
    public boolean doDialog() {
        this.show(); // blocks until dialog is closed
        this.dispose();

        return result;
    }

    /**
     * Returns the filename or url of the file which should be imported.
     *
     * @return The filename or url.
     */
    public String getSelection() {
        return selection.getText();
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
    public Parser createParser( Dictionary[] dictionaries, java.util.Set exclusions) {
        return parserSelector.createParser( dictionaries, exclusions);
    }
} // class ImportDialog
