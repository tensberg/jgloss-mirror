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

package jgloss.dictionary.attributes;

import jgloss.dictionary.Attribute;

import java.util.ResourceBundle;

public class PartOfSpeechAttribute implements Attribute {
    private final static String name = ResourceBundle.getBundle
        ( "resources/messages-dictionary").getString( "partofspeech.name");
    private final static String description = ResourceBundle.getBundle
        ( "resources/messages-dictionary").getString( "partofspeech.description");

    public static final PartOfSpeechAttribute INSTANCE = new PartOfSpeechAttribute();

    protected PartOfSpeechAttribute() {
    }
    
    public String getName() { return name; }

    public String getDescription() { return description; }

    public String toString() { return name; }
} // class PartOfSpeechAttribute
