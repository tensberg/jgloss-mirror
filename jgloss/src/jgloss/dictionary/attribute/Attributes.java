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

import java.util.ResourceBundle;

/**
 * Collection of standard attributes.
 *
 * @author Michael Koch
 */
public class Attributes implements Attribute {
    protected static final ResourceBundle NAMES = ResourceBundle.getBundle
        ( "resources/messages-dictionary");

    public static final Attribute PART_OF_SPEECH = new Attributes
        ( NAMES.getString( "att.part_of_speech.name"),
          NAMES.getString( "att.part_of_speech.desc"),
          true, PartOfSpeech.class, 
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL,
              DictionaryEntry.AttributeGroup.WORD });

    public static final Attribute PRIORITY = new Attributes
        ( NAMES.getString( "att.priority.name"),
          NAMES.getString( "att.priority.desc"),
          true, Priority.class, 
          new DictionaryEntry.AttributeGroup[] 
          { DictionaryEntry.AttributeGroup.GENERAL,
            DictionaryEntry.AttributeGroup.TRANSLATION });

    public static final Attribute EXAMPLE = new Attributes
        ( NAMES.getString( "att.example.name"),
          NAMES.getString( "att.example.desc"),
          false, null, 
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute ABBREVIATION = new Attributes
        ( NAMES.getString( "att.abbreviation.name"),
          NAMES.getString( "att.abbreviation.desc"),
          false, ReferenceAttributeValue.class, 
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute REFERENCE = new Attributes
        ( NAMES.getString( "att.reference.name"),
          NAMES.getString( "att.reference.desc"),
          false, ReferenceAttributeValue.class, 
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute SYNONYM = new Attributes
        ( NAMES.getString( "att.synonym.name"),
          NAMES.getString( "att.synonym.desc"),
          false, ReferenceAttributeValue.class, 
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute ANTONYM = new Attributes
        ( NAMES.getString( "att.antonym.name"),
          NAMES.getString( "att.antonym.desc"),
          false, ReferenceAttributeValue.class, 
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute USAGE = new Attributes
        ( NAMES.getString( "att.usage.name"),
          NAMES.getString( "att.usage.desc"),
          true, Usage.class, 
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL,
              DictionaryEntry.AttributeGroup.TRANSLATION });

    public static final Attribute CATEGORY = new Attributes
        ( NAMES.getString( "att.category.name"),
          NAMES.getString( "att.category.desc"),
          true, Category.class, 
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL,
              DictionaryEntry.AttributeGroup.TRANSLATION });

    public static final Attribute GAIRAIGO = new Attributes
        ( NAMES.getString( "att.gairaigo.name"),
          NAMES.getString( "att.gairaigo.desc"),
          false, Gairaigo.class, 
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute EXPLANATION = new Attributes
        ( NAMES.getString( "att.explanation.name"),
          NAMES.getString( "att.explanation.desc"),
          false, InformationAttributeValue.class,
          new DictionaryEntry.AttributeGroup[]
            { DictionaryEntry.AttributeGroup.TRANSLATION });

    protected String name;
    protected String description;
    protected boolean canHaveValue;
    protected Class valueClass;
    protected DictionaryEntry.AttributeGroup[] groups;

    public Attributes( String _name, String _description, boolean _canHaveValue,
                       Class _valueClass, DictionaryEntry.AttributeGroup[] _groups) {
        this.name = _name;
        this.description = _description;
        this.canHaveValue = _canHaveValue;
        this.valueClass = _valueClass;
        this.groups = _groups;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public boolean canHaveValue() { return canHaveValue; }

    public Class getAttributeValueClass() { return valueClass; }

    public boolean appliesTo( DictionaryEntry.AttributeGroup _group) {
        for ( int i=0; i<groups.length; i++)
            if (groups[i] == _group)
                return true;

        return false;
    }

    public String toString() { return getName(); }
} // class Attributes
