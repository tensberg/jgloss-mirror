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

package jgloss.dictionary.attribute;

import java.lang.reflect.Method;
import java.io.LineNumberReader;
import java.io.IOException;
import java.util.regex.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * Mapping from strings used in dictionaries to mark attributes or attribute values to
 * attribute/value objects. Dictionaries use strings, which are usually abbreviated words,
 * to mark attributes and their values in entries.
 *
 * @author Michael Koch
 */
public class AttributeMapper {
    protected final static char COMMENT_CHAR = '#';

    public static class Mapping {
        private Attribute attribute;
        private AttributeValue value;

        private Mapping( Attribute _attribute, AttributeValue _value) {
            this.attribute = _attribute;
            this.value = _value;
        }

        public Attribute getAttribute() { return attribute; }
        public boolean hasValue() { return (value != null); }
        public AttributeValue getValue() { return value; }
    }

    protected Map mappings;
    protected Set allAttributes;
    
    /**
     * Initializes a new mapping from dictionary-specific id's to attribute/value objects by
     * reading the configuration from a reader.
     */
    public AttributeMapper( LineNumberReader mapping) throws IOException {
        mappings = new HashMap( 61);
        allAttributes = new HashSet( 61);

        String line;

        // Format of a line
        // dictionary_id part attribute (attValueClass attValueID)

        Pattern linePattern = Pattern.compile
            ( "\\A(\\S+)\\s+(\\S+)(?:\\s+(\\S+)\\s+(\\S+))?(?:\\s+" + 
              COMMENT_CHAR + ".+)?");
        Matcher lineMatcher = linePattern.matcher( "");
        while ((line = mapping.readLine()) != null) {
            if (line.trim().length()>0 && line.charAt( 0) != COMMENT_CHAR) {
                lineMatcher.reset( line);
                if (lineMatcher.matches()) {
                    String id = lineMatcher.group( 1);

                    String attributeS = lineMatcher.group( 2);
                    Class attClass = Attributes.class;
                    int dot = attributeS.lastIndexOf( '.');
                    if (dot != -1) {
                        try {
                            attClass = Class.forName( attributeS.substring( 0, dot));
                        } catch (ClassNotFoundException ex) {
                            throw new IOException( "Unknown attribute class " + 
                                                   attributeS.substring( 0, dot) + "; line " + 
                                                   mapping.getLineNumber());
                        }
                        attributeS = attributeS.substring( dot);
                    }

                    Attribute attribute;
                    try {
                        attribute = (Attribute) attClass.getField( attributeS).get( null);
                    } catch (Exception ex) {
                        throw new IOException( "Unknown attribute " + 
                                               attributeS + "; line " + 
                                               mapping.getLineNumber());
                    }

                    AttributeValue attValue = null;
                    // attValue does not need to be defined on the line, in this case, groups
                    // 4 and 5 are null
                    String attValueName = lineMatcher.group( 3);
                    if (attValueName != null) {
                        if (attValueName.indexOf( '.') == -1)
                            attValueName = "jgloss.dictionary.attribute." + attValueName;
                        Class attValueClass;
                        try {
                            attValueClass = Class.forName( attValueName);
                        } catch (ClassNotFoundException ex) {
                            throw new IOException( "Unknown attribute value class " + 
                                                   attValueName + "; line " + 
                                                   mapping.getLineNumber());
                        }
                        try {
                            Method get = attValueClass.getMethod( "get", 
                                                                  new Class[] { String.class });
                            attValue = (AttributeValue) get.invoke
                                ( null, new Object[] { lineMatcher.group( 4) });
                        } catch (Exception ex) {
                            throw new IOException( "Unknown attribute value " +
                                                   lineMatcher.group( 4) + "; class " +
                                                   lineMatcher.group( 3) + "; line " + 
                                                   mapping.getLineNumber());
                        }
                    }

                    mappings.put( id, new Mapping( attribute, attValue));
                    allAttributes.add( attribute);
                }
                else
                    throw new IOException( "Invalid line " + mapping.getLineNumber());
            }
        }
    }

    /**
     * Return the mapping for the dictionary-specific id. Returns <code>null</code> if there
     * is no such mapping.
     */
    public Mapping getMapping( String id) {
        return (Mapping) mappings.get( id);
    }

    public Set getAttributes() { return Collections.unmodifiableSet( allAttributes); }
} // class AttributeMapper
