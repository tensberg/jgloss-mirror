/*
 * Copyright (C) 2003-2004 Michael Koch (tensberg@gmx.net)
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
     * The class uses inflection to choose the appropriate member function of
     * the annotation instance.
     */
    private class TypeSelector {
        String type;
        Method annotationMethod;

        public TypeSelector(String _type, String methodName) {
            this.type = _type;
            
            try {
                annotationMethod = Annotation.class.getMethod(methodName, new Class[] {});
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }

        public String getType() { return type; }

        public String getAnnotationText(Annotation anno) {
            try {
                return (String) annotationMethod.invoke(anno, null);
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    private TypeSelector selector;

    LongestAnnotationParameter(Element elem) {
        super(elem);
    }

    protected void initFromElement( Element elem) {
        super.initFromElement(elem);
        initTypeSelector(elem.getAttribute(ParameterFactory.Attributes.TYPE));
    }
    
    private void initTypeSelector(String type) {
        if (type.equals(ParameterFactory.AttributeValues.WORD))
            selector = new TypeSelector(type, "getAnnotatedText");
        else if (type.equals(ParameterFactory.AttributeValues.READING))
            selector = new TypeSelector(type, "getAnnotatedTextReading");
        else if (type.equals(ParameterFactory.AttributeValues.DICTIONARY_WORD))
            selector = new TypeSelector(type, "getDictionaryForm");
        else if (type.equals(ParameterFactory.AttributeValues.DICTIONARY_READING))
            selector = new TypeSelector(type, "getDictionaryFormReading");
        else if (type.equals(ParameterFactory.AttributeValues.TRANSLATION))
            selector = new TypeSelector(type, "getTranslation");
        else // should be prevented by validation against DTD
            throw new IllegalArgumentException(type);
    }

    public Object getValue( JGlossFrameModel source, URL systemId) {
        String longestAnnotation = defaultValue;

        AnnotationListModel model = source.getAnnotationListModel();
        for ( int i=0; i<model.getAnnotationCount(); i++) {
            String annotation = selector.getAnnotationText(model.getAnnotation(i));
            if (annotation.length() > longestAnnotation.length())
                longestAnnotation = annotation;
        }

        return longestAnnotation;
    }
} // class LongestAnnotationParameter
