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

import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.ResultIterator;

import java.util.ResourceBundle;
import java.util.NoSuchElementException;

/**
 * Collection of standard attributes.
 *
 * @author Michael Koch
 */
public class Attributes implements Attribute {
    protected static final ResourceBundle NAMES = ResourceBundle.getBundle
        ( "resources/messages-dictionary");

    public static final Priority EXAMPLE_PRIORITY_VALUE = 
        new Priority() {
            public String getPriority() { return NAMES.getString( "example.priority"); }
            public int compareTo( Priority p) {
                throw new IllegalArgumentException();
            }
        };

    public static final ReferenceAttributeValue EXAMPLE_REFERENCE_VALUE =
        new ReferenceAttributeValue() {
            public String getReferenceTitle() { return NAMES.getString( "example.reference"); }
            public ResultIterator getReferencedEntries() {
                return new ResultIterator() {
                        public boolean hasNext() { return false; }
                        public DictionaryEntry next() { throw new NoSuchElementException(); }
                    };
            }
        };

    public static final Gairaigo EXAMPLE_GAIRAIGO_VALUE =
        new Gairaigo( NAMES.getString( "example.gairaigo.lang"), 
                      NAMES.getString( "example.gairaigo.word"));

    public static final InformationAttributeValue EXAMPLE_INFORMATION_VALUE =
        new InformationAttributeValue( NAMES.getString( "example.information"));

    public static final Attribute PART_OF_SPEECH = new Attributes
        ( NAMES.getString( "att.part_of_speech.name"),
          NAMES.getString( "att.part_of_speech.desc"),
          PartOfSpeech.class, PartOfSpeech.get( "example"),
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL,
              DictionaryEntry.AttributeGroup.WORD });

    public static final Attribute PRIORITY = new Attributes
        ( NAMES.getString( "att.priority.name"),
          NAMES.getString( "att.priority.desc"),
          Priority.class, EXAMPLE_PRIORITY_VALUE,
          new DictionaryEntry.AttributeGroup[] 
          { DictionaryEntry.AttributeGroup.GENERAL,
            DictionaryEntry.AttributeGroup.TRANSLATION });

    public static final Attribute EXAMPLE = new Attributes
        ( NAMES.getString( "att.example.name"),
          NAMES.getString( "att.example.desc"),
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute ABBREVIATION = new Attributes
        ( NAMES.getString( "att.abbreviation.name"),
          NAMES.getString( "att.abbreviation.desc"),
          ReferenceAttributeValue.class,
          EXAMPLE_REFERENCE_VALUE,
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute REFERENCE = new Attributes
        ( NAMES.getString( "att.reference.name"),
          NAMES.getString( "att.reference.desc"),
          ReferenceAttributeValue.class,
          EXAMPLE_REFERENCE_VALUE,
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute SYNONYM = new Attributes
        ( NAMES.getString( "att.synonym.name"),
          NAMES.getString( "att.synonym.desc"),
          ReferenceAttributeValue.class,
          EXAMPLE_REFERENCE_VALUE,
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute ANTONYM = new Attributes
        ( NAMES.getString( "att.antonym.name"),
          NAMES.getString( "att.antonym.desc"),
          ReferenceAttributeValue.class,
          EXAMPLE_REFERENCE_VALUE,
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute USAGE = new Attributes
        ( NAMES.getString( "att.usage.name"),
          NAMES.getString( "att.usage.desc"),
          Usage.class, Usage.get( "example"),
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL,
              DictionaryEntry.AttributeGroup.TRANSLATION });

    public static final Attribute CATEGORY = new Attributes
        ( NAMES.getString( "att.category.name"),
          NAMES.getString( "att.category.desc"),
          Category.class, Category.get( "example"),
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL,
              DictionaryEntry.AttributeGroup.TRANSLATION });

    public static final Attribute GAIRAIGO = new Attributes
        ( NAMES.getString( "att.gairaigo.name"),
          NAMES.getString( "att.gairaigo.desc"),
          Gairaigo.class, EXAMPLE_GAIRAIGO_VALUE,
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute EXPLANATION = new Attributes
        ( NAMES.getString( "att.explanation.name"),
          NAMES.getString( "att.explanation.desc"),
          InformationAttributeValue.class, EXAMPLE_INFORMATION_VALUE,
          new DictionaryEntry.AttributeGroup[]
            { DictionaryEntry.AttributeGroup.TRANSLATION });

    protected String name;
    protected String description;
    protected boolean canHaveValue;
    protected Class valueClass;
    protected AttributeValue exampleAttributeValue;
    protected DictionaryEntry.AttributeGroup[] groups;

    public Attributes( String _name, String _description, DictionaryEntry.AttributeGroup[] _groups) {
        this( _name, _description, null, null, _groups);
    }

    public Attributes( String _name, String _description,
                       Class _valueClass, AttributeValue _exampleAttributeValue,
                       DictionaryEntry.AttributeGroup[] _groups) {
        name = _name;
        description = _description;
        canHaveValue = _valueClass != null;
        valueClass = _valueClass;
        exampleAttributeValue = _exampleAttributeValue;
        groups = _groups;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public boolean canHaveValue() { return canHaveValue; }

    public Class getAttributeValueClass() { return valueClass; }

    public AttributeValue getExampleValue() { return exampleAttributeValue; }

    public boolean appliesTo( DictionaryEntry.AttributeGroup _group) {
        for ( int i=0; i<groups.length; i++)
            if (groups[i] == _group)
                return true;

        return false;
    }

    public String toString() { return getName(); }
} // class Attributes
