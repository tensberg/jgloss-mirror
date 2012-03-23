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
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jgloss.JGloss;

/**
 * The about dialog which shows what this is all about. It will also create a frame
 * for displaying the GNU GPL.
 *
 * @author Michael Koch
 */
public class AboutFrame extends JFrame {
	private static final Logger LOGGER = Logger.getLogger(AboutFrame.class.getPackage().getName());
	
    private static final long serialVersionUID = 1L;

    /**
     * The application-wide instance used to display the about information.
     */
    private static AboutFrame dialog;
    private static final Object dialogLock = new Object();
    /**
     * Action which displays the about dialog. The action is initialized when calling
     * {@link #createFrame(String) createFrame}.
     */
    private static Action showAction;
    private static final Object showActionLock = new Object();
    /**
     * The application-wide instance used to display the GNU GPL.
     */
    private static JFrame license;

    /**
     * Creates the standard about frame instance and the {@link #showAction showAction}.
     *
     * @param prefix Prefix to the resource key strings. Used to differentiate between JGloss and
     *       JDictionary about dialog.
     */
    public static void createFrame( String prefix) {
        synchronized (dialogLock) {
            dialog = new AboutFrame( prefix);
            dialogLock.notifyAll();
        }
    }

    public static void createShowAction( String prefix) {
        synchronized (showActionLock) {
            showAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed( ActionEvent e) {
                        getFrame().setVisible(true);
                    }
                };
            UIUtilities.initAction( showAction, prefix + ".main.menu.about");
            
            showActionLock.notifyAll();
        }        
    }

    /**
     * Returns the standard about frame instance. The method will block until the about frame
     * instance is initialized by calling {@link #createFrame(String) createFrame]}.
     */
    public static AboutFrame getFrame() {
        synchronized (dialogLock) {
            if (dialog == null) {
	            try {
	                // wait until frame is created
	                dialogLock.wait();
	            } catch (InterruptedException ex) {}
            }
            return dialog;
        }
    }

    /**
     * Returns the show action which shows the about dialog. The method will block until the 
     * action is initialized by calling {@link #createFrame(String) createFrame]}.
     */
    public static Action getShowAction() {
        synchronized (showActionLock) {
            if (showAction == null) {
	            try {
	                // wait until frame is created
	                showActionLock.wait();
	            } catch (InterruptedException ex) {}
            }
            return showAction;
        }
    }

    /**
     * Creates the about dialog.
     */
    private AboutFrame( String prefix) {
        super();
        setTitle( JGloss.messages.getString( prefix + ".about.frame.title"));
        
        JLabel label = new JLabel( JGloss.messages.getString( prefix + ".about.title"));
        label.setHorizontalAlignment( SwingConstants.CENTER);
        label.setFont( new Font( "SansSerif", Font.BOLD, label.getFont().getSize()+3));
        label.setForeground( Color.black);
        JTextArea area = new JTextArea( JGloss.messages.getString( prefix + ".about.text"));
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
            private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    if (license == null) {
	                    createLicenseFrame();
                    }
                    license.setVisible( true);
                }
            }));
        b.add( Box.createHorizontalStrut( 5));
        b.add( new JButton( new AbstractAction( JGloss.messages.getString( "button.close")) {
            private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( ActionEvent e) {
                    setVisible(false);
                }
            }));
        p = new JPanel();
        p.setLayout( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5));
        p.add( b);
        getContentPane().add( p, BorderLayout.SOUTH);

        setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE);
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
            StringBuilder gpl = new StringBuilder();
            String line;
            while ((line=r.readLine()) != null) {
	            gpl.append( line + "\n");
            }
            r.close();

            license = new JFrame( JGloss.messages.getString( "about.license.title"));
            JTextArea ta = new JTextArea( gpl.toString());
            ta.setEditable( false);
            ta.setCaretPosition( 0);
            ta.setFont( new Font( "Monospaced", Font.PLAIN, 11));
            JScrollPane p = new JScrollPane( ta, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            license.getContentPane().setLayout( new GridLayout( 1, 1));
            license.getContentPane().add( p);
            license.setSize( 600, 400);
            license.setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
} // class AboutFrame
