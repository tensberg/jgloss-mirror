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

/**
 * Export file chooser which lets the user select a template. This template can then be used with
 * a {@link TemplateExporter TemplateExporter} instance. Templates can be read from the resources,
 * with a list of available templates supplied by the caller, or from files which the user
 * selected.
 *
 * @author Michael Koch
 */
public class TemplateExportFileChooser extends ExportFileChooser implements ActionListener {
    /**
     * Interface for template wrappers. Implementing classes must provide a way to get a
     * reader for the represented template, and they must implement <code>toString</code>
     * to return a user-level description of the template.
     */
    protected static abstract class Template {
        protected String shortDescription;
        protected String description;

        /**
         * Return a reader for the template.
         */
        protected abstract InputStreamReader getReader() throws IOException;
        /**
         * Read the short description line from the template.
         *
         * @return The description, or <code>null</code> if there is no description.
         */
        protected String readShortDescription() {
            Reader reader = null;
            String description = null;
            try {
                reader = getReader();
                description = TemplateExporter.readShortDescription( reader);
            } catch (IOException ex) {
            } finally {
                if (reader != null) try {
                    reader.close();
                } catch (IOException ex) {}
            }
            return description;
        }

        /**
         * Read the long description from the template.
         *
         * @return The description, or <code>null</code> if there is no description.
         */
        protected String readDescription() {
            Reader reader = null;
            String description = null;
            try {
                reader = getReader();
                description = TemplateExporter.readDescription( reader);
            } catch (IOException ex) {
            } finally {
                if (reader != null) try {
                    reader.close();
                } catch (IOException ex) {}
            }
            return description;
        }

        public String getShortDescription() { return shortDescription; }
        public String getDescription() { 
            if (description == null) {
                description = readDescription();
                if (description == null) // no description or read error
                    description = "";
            }
            return description;
        }
        public String toString() { return shortDescription; }
    }

    /**
     * Template definition which is accessed as resource from the jgloss.jar.
     */
    protected static class ResourceTemplate extends Template {
        private String path;

        public ResourceTemplate( String path) {
            this.path = path;
            this.shortDescription = readShortDescription();
            if (shortDescription == null)
                shortDescription = path.substring( path.lastIndexOf( '/')+1);
        }

        public String getPath() { return path; }

        public InputStreamReader getReader() throws IOException {
            return TemplateExporter.getReader
                ( LaTeXExportFileChooser.class.getResourceAsStream( path));
        }

        public boolean equals( Object o) {
            return (o instanceof ResourceTemplate &&
                    path.equals( ((ResourceTemplate) o).path));
        }
    }

    /**
     * Template definition which is read from a local file.
     */
    protected static class FileTemplate extends Template {
        private File file;

        public FileTemplate( String path) {
            file = new File( path);
            shortDescription = readShortDescription();
            if (shortDescription == null)
                shortDescription = file.getName();
        }

        public File getFile() { return file; }

        public InputStreamReader getReader() throws IOException {
            return TemplateExporter.getReader
                ( new BufferedInputStream( new FileInputStream( file)));
        }

        public boolean equals( Object o) {
            return (o instanceof FileTemplate &&
                    file.equals( ((FileTemplate) o).file));
        }
    }

    protected static final String TEMPLATE_LIST_PREFERENCES_KEY = PREFERENCES_KEY + " template list";
    
    private static String ADD = JGloss.messages.getString( "button.add");
    
    private JComboBox templateChooser;

    private JTextArea templateDescription;

    public TemplateExportFileChooser( String path, String title) {
        super( path, title);
    }

    /**
     * Gets a reader for the template selected by the user.
     */
    public InputStreamReader getTemplate() throws IOException {
        return ((Template) templateChooser.getSelectedItem()).getReader();
    }

    public void addTemplateChooser( String templatePrefsKey,
                                    String resourceMessagePrefsKey,
                                    String fileTemplatesPrefsKey) {
        Vector comboBoxItems = new Vector( 10);
        
        String[] templates;
        if (resourceMessagePrefsKey != null) {
            // add default templates stored in jgloss.jar
            templates = jgloss.dictionary.StringTools.split
                ( JGloss.messages.getString( resourceMessagePrefsKey), ':');
            for( int i=0; i<templates.length; i++)
                comboBoxItems.add( new ResourceTemplate( templates[i]));
        }

        // add user-selected templates
        templates = JGloss.prefs.getPaths( fileTemplatesPrefsKey);
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
        if (templateDeleted)
            JGloss.prefs.set( fileTemplatesPrefsKey, paths.toString());

        comboBoxItems.add( ADD);

        templateChooser = new JComboBox( comboBoxItems);
        templateChooser.setEditable( false);
        templateChooser.putClientProperty( PREFERENCES_KEY, templatePrefsKey);
        templateChooser.putClientProperty( TEMPLATE_LIST_PREFERENCES_KEY, fileTemplatesPrefsKey);

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

        templateDescription = new JTextArea( 4, 30);
        java.awt.Font ta = templateDescription.getFont();
        templateDescription.setFont( ta.deriveFont( (float) ta.getSize()-1));
        templateDescription.setEditable( false);
        templateDescription.setLineWrap( true);
        templateDescription.setWrapStyleWord( true);

        Box b2 = Box.createVerticalBox();
        b2.add( b);
        b2.add( Box.createVerticalStrut( 3));
        b2.add( new JScrollPane( templateDescription));
        addCustomElement( b2);
        
        updateTemplateDescriptionText();
    }

    /**
     * Show a template file chooser if the user selects "add" from the template combo box.
     */
    public void actionPerformed( ActionEvent event) {
        // fired from templateChooser

        if (templateChooser.getSelectedItem() == ADD) {
            // add a new template file
            JFileChooser chooser = new JFileChooser( JGloss.getCurrentDir());
            chooser.setDialogTitle( JGloss.messages.getString( "export.choosetemplate"));
            chooser.setFileView( CustomFileView.getFileView());
            chooser.setFileHidingEnabled( true);
            chooser.setFileFilter( new ExtensionFileFilter( "tmpl", 
                                                            JGloss.messages.getString
                                                            ( "filefilter.description.template")));
            int result = chooser.showOpenDialog( this);
            if (result == JFileChooser.APPROVE_OPTION) {
                JGloss.setCurrentDir( chooser.getCurrentDirectory().getAbsolutePath());
                FileTemplate template = new FileTemplate( chooser.getSelectedFile().getAbsolutePath());
                int index = ((DefaultComboBoxModel) templateChooser.getModel()).getIndexOf( template);
                if (index<0 || index>=templateChooser.getItemCount()) {
                    // insert new template before ADD item
                    index = templateChooser.getItemCount() - 1;
                    templateChooser.insertItemAt( template, index);
                    // save selection in resources
                    String paths = JGloss.prefs.getString( (String) templateChooser.getClientProperty
                                                           ( TEMPLATE_LIST_PREFERENCES_KEY));
                    if (paths.length() > 0)
                        paths += File.pathSeparator;
                    paths += chooser.getSelectedFile().getAbsolutePath();
                    JGloss.prefs.set( (String) templateChooser.getClientProperty
                                      ( TEMPLATE_LIST_PREFERENCES_KEY), paths);
                }
                templateChooser.setSelectedIndex( index);
            }
            else
                templateChooser.setSelectedIndex( templateChooser.getItemCount() - 2);
        }

        updateTemplateDescriptionText();
    }

    protected void updateTemplateDescriptionText() {
        String description = ((Template) templateChooser.getSelectedItem()).getDescription();
        if (description==null || description.length()==0)
            description = JGloss.messages.getString( "export.template.nodescription");
        templateDescription.setText( description);
    }

    /**
     * Save template choice if the user accepts the dialog.
     */
    protected void savePreferences() {
        super.savePreferences();
        if (templateChooser != null)
            JGloss.prefs.set( (String) templateChooser.getClientProperty( PREFERENCES_KEY),
                              templateChooser.getSelectedItem().toString());
    }
} // class TemplateExportFileChooser
