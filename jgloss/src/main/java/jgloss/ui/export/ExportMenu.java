/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import jgloss.JGloss;
import jgloss.ui.UIUtilities;
import jgloss.ui.gloss.JGlossFrameModel;
import jgloss.util.StringTools;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Menu with export items.
 *
 * @author Michael Koch
 */
public class ExportMenu extends JMenu implements ActionListener {
    private static List exporters = new ArrayList();

    private static final String EXPORTCONFIG_CLIENT_PROPERTY = "export config client property";
    
    /**
     * Register an export configuration. The supplied input source must be initialized to an
     * export descriptor XML document. For each registered exporter a menu item will be
     * created.
     */
    public static synchronized void registerExport( InputSource in) throws IOException,
                                                                           SAXException {
        exporters.add( new ExportConfiguration( in));
    }

    /**
     * Register the standard export menu items. The list of standard exporters are stored in
     * the resources, the corresponding files are stored in the JGloss jar file under
     * <code>data/exporters</code>.
     */
    public static synchronized void registerStandardExporters() {
        String[] resources = JGloss.messages.getString( "exporters").split(":");
        for ( int i=0; i<resources.length; i++) try {
            registerExport( new InputSource( ExportMenu.class.getResource( resources[i])
                                             .toExternalForm()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private JGlossFrameModel context;

    /**
     * Creates a new export menu. For each registered export configuration a menu item is created.
     */
    public ExportMenu() {
        super( JGloss.messages.getString( "main.menu.export"));

        for ( Iterator i=exporters.iterator(); i.hasNext(); ) {
            ExportConfiguration export = (ExportConfiguration) i.next();
            JMenuItem item = new JMenuItem();
            UIUtilities.initButton(item, export.getMenuKey());
            item.putClientProperty( EXPORTCONFIG_CLIENT_PROPERTY, export);
            item.addActionListener( this);
            item.setEnabled( false);
            this.add( item);
        }

        setEnabled( false);
    }

    /**
     * Set the export context. The context supplies the document which is exported as well
     * as meta-information about the document such as the file name.
     */
    public void setContext( JGlossFrameModel _context) {
        context = _context;
        boolean enabled = (context != null && !context.isEmpty());
        setEnabled( enabled);
        for ( int i=0; i<getItemCount(); i++)
            getItem( i).setEnabled( enabled);
    }

    /**
     * Starts the export process when an export menu item is selected.
     */
    @Override
	public void actionPerformed( ActionEvent a) {
        JMenuItem source = (JMenuItem) a.getSource();
        ExportConfiguration export = 
            (ExportConfiguration) source.getClientProperty( EXPORTCONFIG_CLIENT_PROPERTY);
        export.createExporter().export( export, context, context.getDocument().getDOMDocument(), 
                                        SwingUtilities.getRoot( this));
    }
} // class ExportMenu
