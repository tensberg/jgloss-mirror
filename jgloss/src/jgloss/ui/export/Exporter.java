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
import jgloss.util.XMLTools;
import jgloss.ui.ExtensionFileFilter;
import jgloss.ui.UIUtilities;
import jgloss.ui.JGlossFrameModel;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;

import javax.swing.JOptionPane;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;

class Exporter {
    interface Elements {
        String PARAMETERS = "parameters";
        String LOCALIZED_STRINGS = "localized-strings";
        String LABEL = "label";
        String LABEL_KEY = "label-key";
        String VALUE = "value";
        String JGLOSS_EXPORT = "jgloss-export";
        String TEMPLATES = "templates";
    } // interface Elements

    interface Attributes {
        String KEY = "key";
        String DESCRIPTION_KEY = "description-key";
        String LABEL_KEY = "label-key";
        String PREFS_KEY = "prefs-key";
        String NAME = "name";
        String EDITABLE = "editable";
        String SOURCE = "source";
    } // interface Attributes

    public static final String DTD_PUBLIC = "JGloss export/1.0/JGloss export template description/EN";
    public static final String DTD_SYSTEM = "http://jgloss.sourceforge.net/export-descriptor.dtd";
    public static final String DTD_RESOURCE = "/data/export/export-descriptor.dtd";

    private static final EntityResolver EXPORT_ENTITY_MAPPER =
        new EntityResolver() {
            public InputSource resolveEntity( String publicId, String systemId)
                throws SAXException, IOException {
                if (Exporter.DTD_PUBLIC.equals( publicId) ||
                  Exporter.DTD_SYSTEM.equals( systemId)) {
                    InputSource dtd = new InputSource( Exporter.class.getResource
                                                       ( Exporter.DTD_RESOURCE).toExternalForm());
                    dtd.setPublicId( publicId);
                    return dtd;
                }

                return null;
            }
        };

    private static final DocumentBuilderFactory docFactory = initDocFactory();

    private static DocumentBuilderFactory initDocFactory() {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setIgnoringComments( true);
        docFactory.setCoalescing( true);
        docFactory.setIgnoringElementContentWhitespace( true);
        docFactory.setValidating( true);
        return docFactory;
    }

    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

    private String menuKey;
    private EncodingParameter encoding;
    private ExportFileChooser filechooser;
    private String title;
    private List parameters;
    private FileFilter fileFilter;
    private List localizedStrings;
    private TemplateChooser templateChooser;
    private String template;
    private URL systemId;

    Exporter( InputSource _config) throws SAXException, IOException {
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            builder.setEntityResolver( EXPORT_ENTITY_MAPPER);
            Document config = builder.parse( _config);

            Element child = (Element) config.getDocumentElement().getFirstChild();
            initDescription( child);
            child = (Element) child.getNextSibling();
            
            if (child.getTagName().equals( Elements.PARAMETERS)) {
                initParameters( child);
                child = (Element) child.getNextSibling();
            }

            if (child.getTagName().equals( Elements.LOCALIZED_STRINGS)) {
                initLocalizedStrings( child);
                child = (Element) child.getNextSibling();
            }

            initTemplates( child, _config.getSystemId());
        } catch (ParserConfigurationException ex) {}
    }

    public AbstractButton initButton( AbstractButton b) { 
        return UIUtilities.initButton( b, menuKey);
    }

    public synchronized void export( JGlossFrameModel source, Document doc, Component parent) {
        if (filechooser == null) {
            initFileChooser();
        }

        filechooser.setCurrentDirectory( new File( JGloss.getCurrentDir()));
        int result = filechooser.showSaveDialog( parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            OutputStream out = null;
            try {
                out = new BufferedOutputStream( new FileOutputStream
                                                ( filechooser.getSelectedFile()));

                Transformer transformer = transformerFactory.newTransformer
                    ( new SAXSource( getSelectedTemplate()));

                if (encoding != null)
                    transformer.setOutputProperty( OutputKeys.ENCODING, encoding.getValue());

                Element parametersElement = createParametersElement( source, doc);
                Element jglossElement = doc.getDocumentElement();
                Element root = doc.createElement( Elements.JGLOSS_EXPORT);
                doc.replaceChild( root, jglossElement);
                root.appendChild( parametersElement);
                root.appendChild( jglossElement);

                try {
                    transformer.transform( new DOMSource( doc), new StreamResult( out));
                } finally {
                    doc.replaceChild( jglossElement, root);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showConfirmDialog
                    ( parent, JGloss.messages.getString
                      ( "error.export.exception", new Object[] 
                          { filechooser.getSelectedFile().getPath(), ex.getClass().getName(),
                            ex.getLocalizedMessage() }),
                      JGloss.messages.getString( "error.export.title"),
                      JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);    
            } finally {
                if (out != null) try {
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    private void initDescription( Element description) {
        Element child = (Element) description.getFirstChild();
        menuKey = child.getAttribute( Attributes.KEY);

        child = (Element) child.getNextSibling();
        title = JGloss.messages.getString( child.getAttribute( Attributes.KEY));

        child = (Element) child.getNextSibling();
        if (child != null) {
            fileFilter = new ExtensionFileFilter
                ( XMLTools.getText( child),
                  JGloss.messages.getString( child.getAttribute( Attributes.DESCRIPTION_KEY)));
        }
    }

    private void initParameters( Element _parameters) {
        parameters = new ArrayList();
        Node parameterNode = _parameters.getFirstChild();
        while (parameterNode != null) {
            if (parameterNode.getNodeType() == Node.ELEMENT_NODE) {
                Parameter parameter = ParameterFactory.createParameter( (Element) parameterNode);
                if (parameter instanceof EncodingParameter)
                    encoding = (EncodingParameter) parameter;
                parameters.add( parameter);
            }
            parameterNode = parameterNode.getNextSibling();
        }
    }

    private void initLocalizedStrings( Element localizedStrings) {
    }

    private void initTemplates( Element _template, String _systemId) {
        try {
            systemId = new URL( _systemId);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }

        if (_template.getTagName().equals( Elements.TEMPLATES)) {
            templateChooser = new TemplateChooser( _template);
            parameters.add( 0, templateChooser);
        }
        else {
            // single template definition
            template = XMLTools.getText( _template.getLastChild());
        }
    }

    private InputSource getSelectedTemplate() {
        String source = (templateChooser!=null) ? templateChooser.getValue() : template;

        try {
            source = new URL( systemId, source).toExternalForm();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }

        return new InputSource( source);
    }

    private Element createParametersElement( JGlossFrameModel source, Document doc) {
        Element param = doc.createElement( Elements.PARAMETERS);
        for ( Iterator i=parameters.iterator(); i.hasNext(); ) {
            Parameter p = (Parameter) i.next();
            Element pe = doc.createElement( p.getName());
            pe.appendChild( doc.createTextNode( p.getValue( source, systemId)));
            param.appendChild( pe);
        }
        return param;
    }

    private void initFileChooser() {
        List uiparameters = new ArrayList( parameters.size());
        for ( Iterator i=parameters.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof UIParameter)
                uiparameters.add( o);
        }
        filechooser = new ExportFileChooser( JGloss.getCurrentDir(), title, uiparameters);
        if (fileFilter != null)
            filechooser.setFileFilter( fileFilter);
    }
} // class Exporter
