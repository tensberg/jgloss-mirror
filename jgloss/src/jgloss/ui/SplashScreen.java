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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

import jgloss.JGloss;

/**
 * Displays a splash screen at startup to keep the user entertained while the application
 * initializes.
 *
 * @author Michael Koch
 */
public class SplashScreen {
    /**
     * The splash screen window.
     */
    private JWindow splash;

    /**
     * Title of the application.
     */
    private JLabel title;
    /**
     * Application version.
     */
    private JLabel version;
    /**
     * Informational text on what the application is currently doing.
     */
    private JLabel info;

    /**
     * Creates a new splash screen for display at application startup. The splash screen
     * will be made visible after creation. The texts will be taken from the application
     * resource.
     */
    public SplashScreen( String applicationKey) {
        splash = new JWindow();
        splash.getContentPane().setLayout( new GridLayout( 1, 1));

        JPanel c = new JPanel( new GridLayout( 1, 1));
        c.setBorder( BorderFactory.createEtchedBorder());
        Box b = Box.createVerticalBox();

        Box b2 = Box.createHorizontalBox();
        b2.add( Box.createHorizontalGlue());
        title = new JLabel( JGloss.messages.getString( applicationKey + ".title"), SwingConstants.CENTER);
        title.setFont( new Font( "SansSerif", Font.PLAIN, 18));
        title.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10));
        b2.add( title);
        b2.add( Box.createHorizontalGlue());
        b.add( b2);
        version = new JLabel( JGloss.messages.getString( applicationKey + ".version"));
        b.add( UIUtilities.createFlexiblePanel( version, true));
        b2 = Box.createHorizontalBox();
        info = new JLabel( JGloss.messages.getString( "splashscreen.dummyinfo"));
        b2.add( UIUtilities.createFlexiblePanel( info, true));
        b.add( b2);
        c.add( b);
        splash.getContentPane().add( c);
        
        splash.validate();
        Dimension d = c.getPreferredSize();
        splash.setSize( d.width+10, d.height);

        Dimension s = splash.getToolkit().getScreenSize();
        splash.setLocation( (s.width-d.width)/2, (s.height-d.height)/2);

        splash.setVisible( true);
    }

    /**
     * Hides and disposes the splash screen.
     */
    public void close() {
        splash.hide();
        splash.dispose();
    }

    /**
     * Sets the informational message which the splash screen displays.
     *
     * @param text The new message.
     */
    public void setInfo( String text) {
        info.setText( text);
        splash.validate();
    }

    /**
     * Returns the informational message which the splash screen currently displays.
     *
     * @return The message.
     */
    public String getInfo() {
        return info.getText();
    }
} // class SplashScreen
