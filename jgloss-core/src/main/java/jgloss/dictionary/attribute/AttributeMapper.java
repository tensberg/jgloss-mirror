/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

package jgloss.dictionary.attribute;

import static java.util.Locale.ENGLISH;

import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mapping from strings used in dictionaries to mark attributes or attribute values to
 * attribute/value objects. Dictionaries use strings, which are usually abbreviated words,
 * to mark attributes and their values in entries.
 *
 * @author Michael Koch <tensberg@gmx.net>
 */
public class AttributeMapper {
    protected final static char COMMENT_CHAR = '#';

    /**
     * Mapping of an attribute to its value.
     * 
     * @author Michael Koch <tensberg@gmx.net>
     */
    public static class Mapping<T extends AttributeValue> {
        private final Attribute<T> attribute;
        private final T value;

        Mapping( Attribute<T> _attribute, T _value) {
            this.attribute = _attribute;
            this.value = _value;
        }

        public Attribute<T> getAttribute() { return attribute; }
        public boolean hasValue() { return (value != null); }
        public T getValue() { return value; }
    }

    protected Map<String, Mapping<?>> mappings;
    protected Map<Attribute<?>, Set<AttributeValue>> allAttributes;
    
    /**
     * Initializes a new mapping from dictionary-specific names to attribute/value objects by
     * reading the configuration from a reader.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AttributeMapper( LineNumberReader mapping) throws IOException {
        mappings = new HashMap<String, Mapping<?>>();
        allAttributes = new HashMap<Attribute<?>, Set<AttributeValue>>();

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
                    String name = lineMatcher.group( 1);
                    name = name.toLowerCase(ENGLISH).replace( '~', ' ');

                    String attributeS = lineMatcher.group( 2);
                    Class<?> attClass = Attributes.class;
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

                    Attribute<?> attribute;
                    try {
                        attribute = (Attribute<?>) attClass.getField( attributeS).get( null);
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
                        if (attValueName.indexOf( '.') == -1) {
	                        attValueName = "jgloss.dictionary.attribute." + attValueName;
                        }
                        Class<?> attValueClass;
                        try {
                            attValueClass = Class.forName( attValueName);
                        } catch (ClassNotFoundException ex) {
                            throw new IOException( "Unknown attribute value class " + 
                                                   attValueName + "; line " + 
                                                   mapping.getLineNumber(), ex);
                        }

                        if (!attribute.getAttributeValueClass().isAssignableFrom(attValueClass)) {
                        	throw new IllegalArgumentException("expected attribute value class " + attribute.getAttributeValueClass() + ", was " + attValueName);
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
                                                   mapping.getLineNumber(), ex);
                        }
                    }
                    

                    mappings.put( name, new Mapping( attribute, attValue));
                    Set<AttributeValue> attValues = allAttributes.get( attribute);
                    if (attValues == null) {
                        attValues = new HashSet<AttributeValue>();
                        allAttributes.put( attribute, attValues);
                    }
                    attValues.add( attValue);
                } else {
	                throw new IOException( "Invalid line " + mapping.getLineNumber());
                }
            }
        }
    }

    /**
     * Return the mapping for the dictionary-specific name. Returns <code>null</code> if there
     * is no such mapping.
     */
    public Mapping<?> getMapping( String name) {
        return mappings.get( name.toLowerCase());
    }

    /**
     * @return Unmodifiable view of all mapped attributes.
     */
    public Map<Attribute<?>, Set<AttributeValue>> getAttributes() { return Collections.unmodifiableMap( allAttributes); }
} // class AttributeMapper
