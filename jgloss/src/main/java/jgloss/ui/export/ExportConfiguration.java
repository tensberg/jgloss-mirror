/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui.export;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jgloss.JGloss;
import jgloss.ui.ExtensionFileFilter;
import jgloss.util.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Reads an exporter configuration from an export descriptor XML file. The export is done
 * by applying a XSLT template to the JGloss XML document.
 *
 * @author Michael Koch
 */
class ExportConfiguration {
	private static final Logger LOGGER = Logger.getLogger(ExportConfiguration.class.getPackage().getName());
	
	/**
     * Element names of an export descriptor XML file.
     */
    interface Elements {
        String EXPORT_CLASS = "export-class";
        String PARAMETERS = "parameters";
        String LOCALIZED_STRINGS = "localized-strings";
        String LABEL = "label";
        String LABEL_KEY = "label-key";
        String VALUE = "value";
        String JGLOSS_EXPORT = "jgloss-export";
        String TEMPLATES = "templates";
    } // interface Elements

    /**
     * Attribute names of an export descriptor XML file.
     */
    interface Attributes {
        String KEY = "key";
        String DESCRIPTION_KEY = "description-key";
        String LABEL_KEY = "label-key";
        String PREFS_KEY = "prefs-key";
        String NAME = "name";
        String EDITABLE = "editable";
        String SOURCE = "source";
    } // interface Attributes

    /**
     * Public ID of the export descriptor DTD.
     */
    public static final String DTD_PUBLIC = "JGloss export/1.0/JGloss export template description/EN";
    /**
     * Sytem ID of the export descriptor DTD.
     */
    public static final String DTD_SYSTEM = "http://jgloss.sourceforge.net/export-descriptor.dtd";
    /**
     * Path to the export descriptor DTD stored in the JGloss JAR file.
     */
    public static final String DTD_RESOURCE = "/export/export-descriptor.dtd";

    /**
     * Uses the locally stored export descriptor dtd instead of the system id link.
     */
    private static final EntityResolver EXPORT_ENTITY_MAPPER =
        new EntityResolver() {
            @Override
			public InputSource resolveEntity( String publicId, String systemId)
                throws SAXException, IOException {
                if (ExportConfiguration.DTD_PUBLIC.equals( publicId) ||
                    ExportConfiguration.DTD_SYSTEM.equals( systemId)) {
                    InputSource dtd = new InputSource( ExportConfiguration.class.getResource
                                                       ( ExportConfiguration.DTD_RESOURCE)
                                                       .toExternalForm());
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

    private String menuKey;
    private EncodingParameter encoding;
    private String title;
    private List<Parameter> parameters;
    private FileFilter fileFilter;
    private Parameter template;
    private URL systemId;
    private String exportClass;

    /**
     * Creates a new exporter from an export descriptor XML file.
     */
    ExportConfiguration( InputSource _config) throws SAXException, IOException {
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            builder.setEntityResolver( EXPORT_ENTITY_MAPPER);
            Document config = builder.parse( _config);

            Element child = (Element) config.getDocumentElement().getFirstChild();
            initDescription( child);
            child = (Element) child.getNextSibling();
            
            // this child must be export-class
            exportClass = XMLTools.getText(child).trim();
            child = (Element) child.getNextSibling();

            if (child.getTagName().equals( Elements.PARAMETERS)) {
                initParameters( child);
                child = (Element) child.getNextSibling();
            }

            if (child.getTagName().equals( Elements.LOCALIZED_STRINGS)) {
                initLocalizedStrings( child);
                child = (Element) child.getNextSibling();
            }

            systemId = new URL(_config.getSystemId());

            initTemplates( child, _config.getSystemId());
        } catch (ParserConfigurationException ex) {}
    }

    public FileFilter getFileFilter() { return fileFilter; }
    public String getTitle() { return title; }
    public List<Parameter> getParameters() { return parameters; }
    public String getEncoding() { return encoding!=null ? (String) encoding.getValue() : null; }
    public URL getSystemId() { return systemId; }
    public Parameter getTemplate() { return template; }
    public String getMenuKey() { return menuKey; }

    /**
     * Create and return a new instance of the {@link Exporter Exporter} implementation
     * specified in the export configuration XML file. The parameter-less constructor is used.
     */
    public Exporter createExporter() {
        try {
            return (Exporter) Class.forName(exportClass).newInstance();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

    private void initDescription( Element description) {
        Element child = (Element) description.getFirstChild();
        menuKey = child.getAttribute( Attributes.KEY);

        child = (Element) child.getNextSibling();
        title = JGloss.MESSAGES.getString( child.getAttribute( Attributes.KEY));

        child = (Element) child.getNextSibling();
        if (child != null) {
            fileFilter = new ExtensionFileFilter
                ( XMLTools.getText( child),
                  JGloss.MESSAGES.getString( child.getAttribute( Attributes.DESCRIPTION_KEY)));
        }
    }

    private void initParameters( Element _parameters) {
        parameters = new ArrayList<Parameter>();
        Node parameterNode = _parameters.getFirstChild();
        while (parameterNode != null) {
            if (parameterNode.getNodeType() == Node.ELEMENT_NODE) {
                Parameter parameter = ParameterFactory.createParameter( (Element) parameterNode);
                if (parameter instanceof EncodingParameter) {
	                encoding = (EncodingParameter) parameter;
                }
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
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        if (_template.getTagName().equals( Elements.TEMPLATES)) { 
            // choice of several templates
            template = new TemplateChooser( _template);
        }
        else {
            // single template definition; parameter name is not configurable
            String value = XMLTools.getText( _template.getLastChild());
            try {
                value = new URL( systemId, value).toExternalForm();
            } catch (MalformedURLException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
            template = new ConstantParameter("template", value);
        }
        parameters.add( 0, template);
    }

} // class ExportConfiguration
