/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.export;

import jgloss.JGloss;
import jgloss.Preferences;
import jgloss.ui.ExtensionFileFilter;
import jgloss.ui.CustomFileView;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Vector;

import javax.swing.*;

public class LaTeXExportFileChooser extends ExportFileChooser implements ActionListener {
    private interface Template {
        InputStreamReader getReader() throws IOException;
    }

    private static class ResourceTemplate implements Template {
        private String path;
        private String description;

        public ResourceTemplate( String path, String description) {
            this.path = path;
            this.description = description;
        }

        public String getPath() { return path; }
        public String getDescription() { return description; }
        public String toString() { return description; }

        public InputStreamReader getReader() throws IOException {
            return TemplateExporter.getReader
                ( LaTeXExportFileChooser.class.getResourceAsStream( path));
        }
    }

    private static class FileTemplate implements Template {
        private File file;
        private String name;

        public FileTemplate( String path) {
            file = new File( path);
            name = file.getName();
        }

        public File getFile() { return file; }
        public String toString() { return name; }

        public InputStreamReader getReader() throws IOException {
            return TemplateExporter.getReader
                ( new BufferedInputStream( new FileInputStream( file)));
        }
    }

    protected static final String TEMPLATE_LIST_PREFERENCES_KEY = PREFERENCES_KEY + " template list";
    
    private static String ADD = JGloss.messages.getString( "button.add");
    
    private JComboBox templateChooser;
    private JComboBox fontSizeChooser;

    public LaTeXExportFileChooser( String path) {
        super( path, JGloss.messages.getString( "export.latex.title"));
        setFileFilter( new ExtensionFileFilter( "tex", 
                                                JGloss.messages.getString
                                                ( "filefilter.description.latex")));
    }

    public InputStreamReader getTemplate() throws IOException {
        return ((Template) templateChooser.getSelectedItem()).getReader();
    }

    public String getFontSize() {
        return (String) fontSizeChooser.getSelectedItem();
    }

    public void addTemplateChooser( String templatePrefsKey, String templateListPrefsKey) {
        Vector comboBoxItems = new Vector( 10);
        
        // add default templates stored in jgloss.jar
        String[] templates = jgloss.dictionary.StringTools.split
            ( JGloss.messages.getString( "export.latex.templates"), ':');
        // format for export.latex.templates is "resource path:description:..."
        for( int i=0; i<templates.length; i+=2) {
            comboBoxItems.add( new ResourceTemplate( templates[i],
                                                     templates[i+1]));
        }

        // add user-selected templates
        templates = JGloss.prefs.getPaths( templateListPrefsKey);
        StringBuffer paths = new StringBuffer();
        boolean templateDeleted = false;
        for ( int i=0; i<templates.length; i++) {
            if (new File( templates[i]).exists()) {
                comboBoxItems.add( new FileTemplate( templates[i]));
                if (paths.length() > 0)
                    paths.append( File.pathSeparator);
                paths.append( templates[i]);
            }
            else
                templateDeleted = true;
        }
        // if any template files were deleted, write new preferences
        JGloss.prefs.set( templateListPrefsKey, paths.toString());

        comboBoxItems.add( ADD);

        templateChooser = new JComboBox( comboBoxItems);
        templateChooser.setEditable( false);
        templateChooser.putClientProperty( PREFERENCES_KEY, templatePrefsKey);
        templateChooser.putClientProperty( TEMPLATE_LIST_PREFERENCES_KEY, templateListPrefsKey);

        // select user item
        String templateSelection = JGloss.prefs.getString( templatePrefsKey);
        for ( int i=0; i<comboBoxItems.size(); i++) {
            if (comboBoxItems.get( i).toString().equals( templateSelection)){
                templateChooser.setSelectedIndex( i);
                break;
            }
        }
        templateChooser.addActionListener( this);
        
        Box b = Box.createHorizontalBox();
        b.add( new JLabel( JGloss.messages.getString( "export.templates")));
        b.add( Box.createHorizontalStrut( 5));
        b.add( templateChooser);
        addCustomElement( b);
    }

    public void addFontSizeChooser( String prefsKey) {
        fontSizeChooser = new JComboBox( JGloss.prefs.getList( Preferences.EXPORT_LATEX_FONTSIZES, ','));
        fontSizeChooser.setEditable( false);
        fontSizeChooser.setSelectedItem( JGloss.prefs.getString( prefsKey));
        fontSizeChooser.putClientProperty( PREFERENCES_KEY, prefsKey);

        Box b = Box.createHorizontalBox();
        b.add( new JLabel( JGloss.messages.getString( "export.latex.fontsize")));
        b.add( Box.createHorizontalStrut( 5));
        b.add( fontSizeChooser);
        b.add( new JLabel( JGloss.messages.getString( "export.latex.fontsize_pt")));
        addCustomElement( b);
    }

    public void actionPerformed( ActionEvent event) {
        // fired from templateChooser
        if (templateChooser.getSelectedItem() != ADD)
            return;

        // add a new template file
        JFileChooser chooser = new JFileChooser( JGloss.getCurrentDir());
        chooser.setDialogTitle( JGloss.messages.getString( "export.choosetemplate"));
        chooser.setFileView( CustomFileView.getFileView());
        chooser.setFileHidingEnabled( true);
        int result = chooser.showOpenDialog( this);
        if (result == JFileChooser.APPROVE_OPTION) {
            JGloss.setCurrentDir( chooser.getCurrentDirectory().getAbsolutePath());
            FileTemplate template = new FileTemplate( chooser.getSelectedFile().getAbsolutePath());
            // insert new template before ADD item
            templateChooser.insertItemAt( template, templateChooser.getItemCount()-1);
            // save selection in resources
            String paths = JGloss.prefs.getString( (String) templateChooser.getClientProperty
                                                   ( TEMPLATE_LIST_PREFERENCES_KEY));
            if (paths.length() > 0)
                paths += File.pathSeparator;
            paths += chooser.getSelectedFile().getAbsolutePath();
            JGloss.prefs.set( (String) templateChooser.getClientProperty
                              ( TEMPLATE_LIST_PREFERENCES_KEY), paths);
        }
        // select item before ADD (new template if chosen)
        templateChooser.setSelectedIndex( templateChooser.getItemCount()-2);
    }

    protected void savePreferences() {
        super.savePreferences();
        if (templateChooser != null)
            JGloss.prefs.set( (String) templateChooser.getClientProperty( PREFERENCES_KEY),
                              templateChooser.getSelectedItem().toString());
        if (fontSizeChooser != null)
            JGloss.prefs.set( (String) fontSizeChooser.getClientProperty( PREFERENCES_KEY),
                              fontSizeChooser.getSelectedItem().toString());
    }
} // class LaTeXExportFileChooser
