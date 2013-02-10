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

package jgloss.ui;

import static java.awt.BorderLayout.CENTER;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

import jgloss.JGloss;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

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
    private final JWindow splash;

    /**
     * Application version.
     */
    private final JLabel version;
    /**
     * Informational text on what the application is currently doing.
     */
    private final JLabel info;

    /**
     * Creates a new splash screen for display at application startup. The splash screen
     * will be made visible after creation. The texts will be taken from the application
     * resource.
     */
    public SplashScreen( String applicationKey) {
        splash = new JWindow();
        splash.setIconImages(JGlossLogo.ALL_LOGO_SIZES);

        splash.getContentPane().setLayout(new BorderLayout());

        JPanel content = new JPanel(new MigLayout(new LC().wrapAfter(1).fill()));
        content.setBorder( BorderFactory.createEtchedBorder());
        content.setBackground(Color.WHITE);

        JLabel logo = new JLabel(JGlossLogo.LOGO_LARGE);
		content.add(logo, "center, growx");

		version = new JLabel( JGloss.MESSAGES.getString( applicationKey + ".version"));
        version.setOpaque(false);
        content.add(version);

        info = new JLabel( JGloss.MESSAGES.getString( "splashscreen.dummyinfo"));
        info.setOpaque(false);
        content.add(info);

        splash.getContentPane().add(content, CENTER);

        splash.pack();
        splash.setLocationRelativeTo(null);
        splash.setVisible( true);
    }

    /**
     * Hides and disposes the splash screen.
     */
    public void close() {
        splash.setVisible(false);
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
