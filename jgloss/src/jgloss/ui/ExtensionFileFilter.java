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

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Filters files based on their extension.
 *
 * @author Michael Koch
 */
public class ExtensionFileFilter extends FileFilter {
    /**
     * Extension of accepted files. The leading '.' is added to the extension string in the
     * constructor and the string is changed to lower case if ignoreCase is enabled.
     */
    private String extension;
    private String extensionOrig;
    private String description;
    private boolean acceptDirectories;
    private boolean ignoreCase;

    public ExtensionFileFilter( String extension, String description) {
        this( extension, description, true, true);
    }

    /**
     * Creates a new file filter which shows files with the given extension.
     *
     * @param extension File extension of the files shown. For "*.jgloss" files the extension
     *                  is "jgloss".
     * @param description The description of the file format.
     * @param acceptDirectories Flag if directories should be accepted by the filter.
     * @param ignoreCase Flag if the case of file extension should be ignored.
     */
    public ExtensionFileFilter( String extension, String description,
                                boolean acceptDirectories, boolean ignoreCase) {
        this.extensionOrig = extension;
        this.extension = "." + extension;
        if (ignoreCase)
            this.extension = this.extension.toLowerCase();
        this.description = description;
        this.acceptDirectories = acceptDirectories;
        this.ignoreCase = ignoreCase;
    }

    public boolean accept( File f) {
        String name = f.getName();
        if (ignoreCase)
            name = name.toLowerCase();
        return acceptDirectories && f.isDirectory() ||
            name.endsWith( extension);
    }
    
    public String getDescription() { 
        return description;
    }

    public String getExtension() {
        return extension.substring( 1);
    }

    public boolean acceptsDirectories() {
        return acceptDirectories;
    }

    public boolean ignoresCase() {
        return ignoreCase;
    }

    /**
     * Creates a new file by adding the extension of this file filter to the filename. If the file
     * already has an extension, it is returned unchanged.
     */
    public File addExtension( File f) {
        if (f.getName().indexOf( '.') == -1)
            f = new File( f.getPath() + extension);

        return f;
    }
} // class ExtensionFileFilter
