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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

import javax.swing.*;

/**
 * The about dialog which shows what this is all about. It will also create a frame
 * for displaying the GNU GPL.
 *
 * @author Michael Koch
 */
public class AboutFrame extends JFrame {
    /**
     * Action which displays the about dialog.
     */
    public final static Action showAction;

    static {
        showAction = new AbstractAction() {
                public void actionPerformed( ActionEvent e) {
                    getFrame().show();
                }
            };
        UIUtilities.initAction( showAction, "main.menu.about");
    }

    /**
     * The application-wide instance used to display the about information.
     */
    private static JFrame dialog;
    /**
     * The application-wide instance used to display the GNU GPL.
     */
    private static JFrame license;

    /**
     * Returns the application-wide unique instance of the about dialog.
     *
     * @return The about dialog.
     */
    public static JFrame getFrame() {
        if (dialog == null)
            dialog = new AboutFrame();
        return dialog;
    }

    /**
     * Creates the about dialog.
     */
    private AboutFrame() {
        super();
        setTitle( JGloss.messages.getString( "about.frame.title"));
        
        JLabel label = new JLabel( JGloss.messages.getString( "about.title"));
        label.setHorizontalAlignment( SwingConstants.CENTER);
        label.setFont( new Font( "SansSerif", Font.BOLD, label.getFont().getSize()+3));
        label.setForeground( Color.black);
        JTextArea area = new JTextArea( JGloss.messages.getString( "about.text"));
        area.setEditable( false);
        area.setOpaque( false);

        JPanel p = new JPanel( new BorderLayout());
        p.add( label, BorderLayout.NORTH);
        p.add( area, BorderLayout.CENTER);
        JPanel p2 = new JPanel();
        p2.setLayout( new GridLayout( 1, 1));
        p2.setBorder( BorderFactory.createCompoundBorder
                     ( BorderFactory.createEmptyBorder( 5, 5, 5, 5),
                       BorderFactory.createCompoundBorder
                       ( BorderFactory.createEtchedBorder(),
                         BorderFactory.createEmptyBorder( 5, 5, 5, 5))));
        p2.add( p);
        getContentPane().add( p2, BorderLayout.CENTER);

        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalGlue());
        b.add( new JButton( new AbstractAction( JGloss.messages.getString( "about.showlicense")) {
                public void actionPerformed( ActionEvent e) {
                    if (license == null)
                        createLicenseFrame();
                    license.setVisible( true);
                }
            }));
        b.add( Box.createHorizontalStrut( 5));
        b.add( new JButton( new AbstractAction( JGloss.messages.getString( "button.close")) {
                public void actionPerformed( ActionEvent e) {
                    hide();
                }
            }));
        p = new JPanel();
        p.setLayout( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5));
        p.add( b);
        getContentPane().add( p, BorderLayout.SOUTH);

        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE);
        pack();
        setResizable( false);
    }

    /**
     * Creates the frame which is used to display the GNU GPL.
     */
    private void createLicenseFrame() {
        try {
            BufferedReader r = new BufferedReader( new InputStreamReader
                ( AboutFrame.class.getResourceAsStream( "/data/COPYING"), "ASCII"));
            StringBuffer gpl = new StringBuffer();
            String line;
            while ((line=r.readLine()) != null)
                gpl.append( line + "\n");
            r.close();

            license = new JFrame( JGloss.messages.getString( "about.license.title"));
            JTextArea ta = new JTextArea( gpl.toString());
            ta.setEditable( false);
            ta.setCaretPosition( 0);
            ta.setFont( new Font( "Monospaced", Font.PLAIN, 11));
            JScrollPane p = new JScrollPane( ta, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            license.getContentPane().setLayout( new GridLayout( 1, 1));
            license.getContentPane().add( p);
            license.setSize( 600, 400);
            license.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
} // class AboutFrame
