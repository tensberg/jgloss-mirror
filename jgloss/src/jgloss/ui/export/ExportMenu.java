package jgloss.ui.export;

import jgloss.JGloss;
import jgloss.util.StringTools;
import jgloss.ui.JGlossFrame;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.SwingUtilities;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ExportMenu extends JMenu implements ActionListener {
    private static List exporters = new ArrayList();

    private static final String EXPORTER_CLIENT_PROPERTY = "exporter client property";
    
    public static synchronized void registerExporter( InputSource in) throws IOException,
                                                                             SAXException {
        exporters.add( new Exporter( in));
    }

    public static synchronized void registerStandardExporters() {
        String[] resources = StringTools.split( JGloss.messages.getString( "exporters"), ':');
        for ( int i=0; i<resources.length; i++) try {
            registerExporter( new InputSource( ExportMenu.class.getResource( resources[i])
                                               .toExternalForm()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private JGlossFrame context;

    public ExportMenu() {
        super( JGloss.messages.getString( "main.menu.export"));

        for ( Iterator i=exporters.iterator(); i.hasNext(); ) {
            Exporter exporter = (Exporter) i.next();
            JMenuItem item = new JMenuItem();
            exporter.initButton( item);
            item.putClientProperty( EXPORTER_CLIENT_PROPERTY, exporter);
            item.addActionListener( this);
            item.setEnabled( false);
            this.add( item);
        }

        setEnabled( false);
    }

    public void setContext( JGlossFrame _context) {
        context = _context;
        boolean enabled = (context != null && context.isDocumentLoaded());
        setEnabled( enabled);
        for ( int i=0; i<getItemCount(); i++)
            getItem( i).setEnabled( enabled);
    }

    public void actionPerformed( ActionEvent a) {
        JMenuItem source = (JMenuItem) a.getSource();
        Exporter exporter = (Exporter) source.getClientProperty( EXPORTER_CLIENT_PROPERTY);
        exporter.export( context, context.getJGlossDocument().getDOMDocument(), 
                         SwingUtilities.getRoot( this));
    }
} // class ExportMenu
