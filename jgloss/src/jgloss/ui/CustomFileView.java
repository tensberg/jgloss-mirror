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

import jgloss.JGloss;

import java.io.File;

import javax.swing.*;
import javax.swing.filechooser.FileView;

/**
 * File view which adds icons and descriptions for the filetypes JGloss, HTML and TeX.
 *
 * @author Michael Koch
 */
public class CustomFileView extends FileView {
    protected static CustomFileView FILE_VIEW;

    /**
     * Returns the unique instance of the custom file view which can be
     * used with JFileChooser instances.
     */
    public static FileView getFileView() {
        if (FILE_VIEW == null)
            FILE_VIEW = new CustomFileView();
        return FILE_VIEW;
    }

    protected static String JGLOSS_DESCRIPTION;
    protected static String HTML_DESCRIPTION;
    protected static String TEX_DESCRIPTION;
    protected static String TEXT_DESCRIPTION;

    protected static Icon JGLOSS_ICON;
    protected static Icon HTML_ICON;
    protected static Icon TEX_ICON;
    protected static Icon TEXT_ICON;

    protected CustomFileView() {
        if (JGLOSS_DESCRIPTION == null) {
            // initialize static members
            JGLOSS_DESCRIPTION = JGloss.messages.getString( "fileview.description.jgloss");
            HTML_DESCRIPTION = JGloss.messages.getString( "fileview.description.html");
            TEX_DESCRIPTION = JGloss.messages.getString( "fileview.description.tex");
            TEXT_DESCRIPTION = JGloss.messages.getString( "fileview.description.text");

            JGLOSS_ICON = new ImageIcon( CustomFileView.class.getResource( "/resources/icons/jgloss.png"));
            HTML_ICON = new ImageIcon( CustomFileView.class.getResource( "/resources/icons/html.png"));
            TEX_ICON = new ImageIcon( CustomFileView.class.getResource( "/resources/icons/tex.png"));
            TEXT_ICON = new ImageIcon( CustomFileView.class.getResource( "/resources/icons/txt.png"));
        }
    }

    public String getTypeDescription( File f) {
        String name = f.getName().toLowerCase();
        if (name.endsWith( ".jgloss"))
            return JGLOSS_DESCRIPTION;
        if (name.endsWith( ".htm") || name.endsWith( ".html"))
            return HTML_DESCRIPTION;
        if (name.endsWith( ".tex"))
            return TEX_DESCRIPTION;
        if (name.endsWith( ".txt"))
            return TEXT_DESCRIPTION;
        
        return null; // let L&F file view determine the description
    }

    public Icon getIcon( File f) {
        String name = f.getName().toLowerCase();
        if (name.endsWith( ".jgloss"))
            return JGLOSS_ICON;
        if (name.endsWith( ".htm") || name.endsWith( ".html"))
            return HTML_ICON;
        if (name.endsWith( ".tex"))
            return TEX_ICON;
        if (name.endsWith( ".txt"))
            return TEXT_ICON;
        
        return null; // let L&F file view determine the icon
    }
} // class CustomFileView