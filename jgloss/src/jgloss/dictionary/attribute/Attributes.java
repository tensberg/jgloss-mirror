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

import java.util.ResourceBundle;

/**
 * Collection of standard attributes.
 *
 * @author Michael Koch
 */
public class Attributes implements Attribute {
    protected static final MessageBundle NAMES = ResourceBundle.getBundle
        ( "resources/messages-dictionary");

    public static final Attribute PART_OF_SPEECH = new Attributes
        ( NAMES.getString( "attribute.part_of_speech.name"),
          NAMES.getString( "attribute.part_of_speech.desc"),
          true, false, PartOfSpeech.class);

    public static final Attribute PRIORITY = new Attributes
        ( NAMES.getString( "attribute.priority.name"),
          NAMES.getString( "attribute.priority.desc"),
          true, true, Priority.class);

    public static final Attribute EXAMPLE = new Attributes
        ( NAMES.getString( "attribute.example.name"),
          NAMES.getString( "attribute.example.desc"),
          false, false, null);

    public static final Attribute ABBREVIATION = new Attributes
        ( NAMES.getString( "attribute.abbreviation.name"),
          NAMES.getString( "attribute.abbreviation.desc"),
          false, false, ReferenceAttributeValue.class);

    public static final Attribute SYNONYM = new Attributes
        ( NAMES.getString( "attribute.synonym.name"),
          NAMES.getString( "attribute.synonym.desc"),
          true, true, ReferenceAttributeValue.class);

    public static final Attribute ANTONYM = new Attributes
        ( NAMES.getString( "attribute.antonym.name"),
          NAMES.getString( "attribute.antonym.desc"),
          true, true, ReferenceAttributeValue.class);

    public static final Attribute USAGE = new Attributes
        ( NAMES.getString( "attribute.antonym.name"),
          NAMES.getString( "attribute.antonym.desc"),
          true, true, Usage.class);

    protected String name;
    protected String description;
    protected boolean canHaveValue;
    protected boolean inheritable;
    protected Class valueClass;

    public Attributes( String _name, String _description, boolean _canHaveValue, boolean _inheritable,
                       Class _valueClass) {
        this.name = _name;
        this.description = _description;
        this.canHaveValue = _canHaveValue;
        this.inheritable = _inhertiable;
        this.valueClass = _valueClass;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public boolean canHaveValue() { return canHaveValue; }

    public boolean isInheritable() { return inheritable; }

    public Class getAttributeValueClass() { return valueClass; }
} // class Attributes
