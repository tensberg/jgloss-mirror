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
 * $Id$
 *
 */

package jgloss.ui;

import static jgloss.JGloss.MESSAGES;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jgloss.ui.util.UIUtilities;

/**
 * The about dialog which shows what this is all about. It will also create a frame
 * for displaying the GNU GPL.
 *
 * @author Michael Koch
 */
public class AboutFrame extends JFrame {
	private static final Logger LOGGER = Logger.getLogger(AboutFrame.class.getPackage().getName());
	
    private static final long serialVersionUID = 1L;
    
    private static Action showAction;

    public static void createShowAction(final String prefix) {
    	Action showAction = new AbstractAction() {
    		private static final long serialVersionUID = 1L;
    		
    		@Override
    		public void actionPerformed( ActionEvent e) {
    			new AboutFrame(prefix).setVisible(true);
    		}
    	};
    	UIUtilities.initAction( showAction, prefix + ".main.menu.about");

    	AboutFrame.showAction = showAction;
    }
    
    public static Action getShowAction() {
        if (showAction == null) {
            throw new IllegalStateException("not yet initialized");
        }
        
        return showAction;
    }

    /**
     * Creates the about dialog.
     */
    private AboutFrame( String prefix) {
        setTitle( MESSAGES.getString( prefix + ".about.frame.title"));
        setBackground(Color.WHITE);
        setIconImages(JGlossLogo.ALL_LOGO_SIZES);
        
        JLabel label = new JLabel( MESSAGES.getString( prefix + ".about.title"), JGlossLogo.LOGO_LARGE,
        				SwingConstants.CENTER);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setFont( new Font( "SansSerif", Font.BOLD, label.getFont().getSize()+2));
        label.setForeground( Color.BLACK);
        label.setOpaque(false);
        String tagline = MESSAGES.getString(prefix + ".about.tagline");
        String text = MESSAGES.getString("jgloss.about.text", tagline);
        JEditorPane aboutText = new JEditorPane( "text/html", text);
        aboutText.setEditable( false);
        aboutText.setOpaque( false);
        aboutText.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        aboutText.addHyperlinkListener(new BrowseHyperlinkListener());
        aboutText.setSize(600, Integer.MAX_VALUE); // set the width explicitly so that the preferred height is computed based on this width
        aboutText.setPreferredSize(new Dimension(600, aboutText.getPreferredSize().height));

        JPanel p = new JPanel( new BorderLayout());
        p.add( label, BorderLayout.NORTH);
        p.add( aboutText, BorderLayout.CENTER);
        JPanel p2 = new JPanel();
        p2.setLayout( new GridLayout( 1, 1));
        p2.setBorder( BorderFactory.createCompoundBorder
        				( BorderFactory.createEmptyBorder( 5, 5, 5, 5),
        								BorderFactory.createCompoundBorder
        								( BorderFactory.createEtchedBorder(),
        												BorderFactory.createEmptyBorder( 5, 5, 5, 5))));
        p2.add( p);
        p.setOpaque(false);
        p2.setOpaque(false);
        getContentPane().add( p2, BorderLayout.CENTER);

        Box b = Box.createHorizontalBox();
        b.add( Box.createHorizontalGlue());
        b.add( new JButton( new AbstractAction( MESSAGES.getString( "about.showlicense")) {
        	private static final long serialVersionUID = 1L;

        	@Override
        	public void actionPerformed( ActionEvent e) {
        		createLicenseFrame().setVisible( true);
        	}
        }));
        b.add( Box.createHorizontalStrut( 5));
        b.add( new JButton( new AbstractAction( MESSAGES.getString( "button.close")) {
        	private static final long serialVersionUID = 1L;

        	@Override
        	public void actionPerformed( ActionEvent e) {
        		setVisible(false);
        	}
        }));
        b.setOpaque(false);
        p = new JPanel();
        p.setLayout( new GridLayout( 1, 1));
        p.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5));
        p.add( b);
        getContentPane().add( p, BorderLayout.SOUTH);
        getContentPane().setBackground(Color.WHITE);
 
        setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE);
        pack();
        setResizable( false);
        setLocationRelativeTo(null);
    }

    /**
     * Creates the frame which is used to display the GNU GPL.
     * @return 
     */
    private JFrame createLicenseFrame() {
    	StringBuilder gpl = new StringBuilder();
    	BufferedReader r = null;
        try {
			r = new BufferedReader( new InputStreamReader
                ( AboutFrame.class.getResourceAsStream( "/data/COPYING"), "ASCII"));
            String line;
            while ((line=r.readLine()) != null) {
	            gpl.append( line + "\n");
            }
        } catch (IOException ex) {
        	LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
        	if (r != null) {
        	    try {
                    r.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "failed to close input stream", ex);
                }
        	}
        }

        JFrame license = new JFrame( MESSAGES.getString( "about.license.title"));
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
        
        return license;
    }
} // class AboutFrame
