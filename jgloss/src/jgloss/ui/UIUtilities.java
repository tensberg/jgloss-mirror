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

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * Collection of static utility methods for user interface creation.
 *
 * @author Michael Koch
 */
public class UIUtilities {
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
    public static JPanel createSpaceEater( Component c, boolean horizontal) {
        JPanel se = new JPanel( new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = horizontal ? GridBagConstraints.VERTICAL : GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
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
} // class UIUtilities