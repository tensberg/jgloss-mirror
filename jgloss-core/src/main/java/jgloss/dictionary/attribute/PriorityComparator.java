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

package jgloss.dictionary.attribute;

import java.util.Comparator;

import jgloss.dictionary.DictionaryEntry;

/**
 * Compare two dictionary entries based on their priority.
 *
 * @author Michael Koch
 */
public class PriorityComparator implements Comparator<Priority> {
    @Override
	public int compare( Priority o1, Priority o2) {
        DictionaryEntry de1 = (DictionaryEntry) o1;
        DictionaryEntry de2 = (DictionaryEntry) o2;
        if (de1.getDictionary() != de2.getDictionary()) {
	        throw new IllegalArgumentException();
        }

        if (de1.getGeneralAttributes().containsKey( Attributes.PRIORITY, false)) {
            if (!de2.getGeneralAttributes().containsKey( Attributes.PRIORITY, false)) {
	            return 1;
            }
            
            Priority p1 = de1.getGeneralAttributes()
                .getAttribute( Attributes.PRIORITY, false).get( 0);
            Priority p2 = de2.getGeneralAttributes()
                .getAttribute( Attributes.PRIORITY, false).get( 0);
            return p1.compareTo( p2);
        }
        else if (de2.getGeneralAttributes().containsKey( Attributes.PRIORITY, false)) {
	        return -1;
        } else {
	        return 0;
        }
    }
} // class PriorityComparator
