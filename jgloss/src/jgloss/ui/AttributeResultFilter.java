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

package jgloss.ui;

import java.awt.Component;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.attribute.Attribute;

public class AttributeResultFilter implements LookupResultFilter {
    protected String name;
    protected String description;
    protected Attribute attribute;
    protected boolean acceptIfSet;

    public AttributeResultFilter( String _name, String _description,
                                  Attribute _attribute, boolean _acceptIfSet) {
        name = _name;
        description = _description;
        attribute = _attribute;
        acceptIfSet = _acceptIfSet;
    }

    public boolean accept( DictionaryEntry de) { 
        if (!enableFor( de.getDictionary()))
            return true;

        if (acceptIfSet)
            return acceptIfSet( de);
        else
            return !acceptIfSet( de);
    }

    protected boolean acceptIfSet( DictionaryEntry de) {
        if (attribute.appliesTo( DictionaryEntry.AttributeGroup.GENERAL) &&
            de.getGeneralAttributes().containsKey( attribute, false))
            return true;

        if (attribute.appliesTo( DictionaryEntry.AttributeGroup.WORD)) {
            if (de.getWordAttributes().containsKey( attribute, false))
                return true;

            for ( int i=0; i<de.getWordAlternativeCount(); i++) {
                if (de.getWordAttributes( i).containsKey( attribute, false)) {
                    return true;
                }
            }
        }

        if (attribute.appliesTo( DictionaryEntry.AttributeGroup.READING)) {
            if (de.getReadingAttributes().containsKey( attribute, false))
                return true;

            for ( int i=0; i<de.getReadingAlternativeCount(); i++) {
                if (de.getReadingAttributes( i).containsKey( attribute, false)) {
                    return true;
                }
            }
        }

        if (attribute.appliesTo( DictionaryEntry.AttributeGroup.TRANSLATION)) {
            if (de.getTranslationAttributes().containsKey( attribute, false))
                return true;

            for ( int i=0; i<de.getTranslationRomCount(); i++) {
                if (de.getTranslationAttributes( i).containsKey( attribute, false))
                    return true;

                for ( int j=0; j<de.getTranslationCrmCount( i); j++) {
                    if (de.getTranslationAttributes( i, j).containsKey( attribute, false))
                        return true;

                    for ( int k=0; k<de.getTranslationSynonymCount( i, j); k++) {
                        if (de.getTranslationAttributes( i, j, k).containsKey( attribute, false))
                            return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean enableFor( Dictionary dic) { 
        return dic.getSupportedAttributes().contains( attribute);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Component getCustomConfigUI() { return null; }
} // class AttributeResultFilter
