/*
 * Copyright (C) 2003-2012 Michael Koch (tensberg@gmx.net)
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

import java.lang.reflect.Method;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgloss.ui.annotation.Annotation;
import jgloss.ui.annotation.AnnotationListModel;
import jgloss.ui.gloss.JGlossFrameModel;

import org.w3c.dom.Element;

/**
 * Returns the text of the longest word/reading/translation of all annotations in the document.
 *
 * @author Michael Koch
 */
class LongestAnnotationParameter extends AbstractParameter {
	/**
     * Return some annotation text based on the type of the annotation.
     * The class uses reflection to choose the appropriate member function of
     * the annotation instance.
     */
    private class TypeSelector {
        private Method annotationMethod;

        public TypeSelector(String methodName) {
            try {
                annotationMethod = Annotation.class.getMethod(methodName, new Class[] {});
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException(methodName);
            }
        }

        public String getAnnotationText(Annotation anno) {
            try {
                return (String) annotationMethod.invoke(anno);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                return null;
            }
        }
    }
	
    private static final Logger LOGGER = Logger.getLogger(LongestAnnotationParameter.class.getPackage().getName());
	
    private TypeSelector selector;

    LongestAnnotationParameter(Element elem) {
        super(elem);
    }

    @Override
	protected void initFromElement( Element elem) {
        super.initFromElement(elem);
        initTypeSelector(elem.getAttribute(ParameterFactory.Attributes.TYPE));
    }
    
    private void initTypeSelector(String type) {
        if (type.equals(ParameterFactory.AttributeValues.WORD)) {
	        selector = new TypeSelector("getAnnotatedText");
        } else if (type.equals(ParameterFactory.AttributeValues.READING)) {
	        selector = new TypeSelector("getAnnotatedTextReading");
        } else if (type.equals(ParameterFactory.AttributeValues.DICTIONARY_WORD)) {
	        selector = new TypeSelector("getDictionaryForm");
        } else if (type.equals(ParameterFactory.AttributeValues.DICTIONARY_READING)) {
	        selector = new TypeSelector("getDictionaryFormReading");
        } else if (type.equals(ParameterFactory.AttributeValues.TRANSLATION)) {
	        selector = new TypeSelector("getTranslation");
        } else {
	        throw new IllegalArgumentException(type);
        }
    }

    @Override
	public Object getValue( JGlossFrameModel source, URL systemId) {
        String longestAnnotation = defaultValue;

        AnnotationListModel model = source.getAnnotationListModel();
        for ( int i=0; i<model.getAnnotationCount(); i++) {
            String annotation = selector.getAnnotationText(model.getAnnotation(i));
            if (annotation.length() > longestAnnotation.length()) {
	            longestAnnotation = annotation;
            }
        }

        return longestAnnotation;
    }
} // class LongestAnnotationParameter
