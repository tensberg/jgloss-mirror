/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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

import java.util.Iterator;

import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.SearchException;

/**
 * Reference to other dictionary entries.
 *
 * @author Michael Koch
 */
public interface ReferenceAttributeValue extends AttributeValue {
    /**
     * Return an iterator over the {@link jgloss.dictionary.DictionaryEntry dictionary entries}
     * referenced by this attribute value. The value object will often not store the entries
     * directly, but will perform a dictionary search every time this method is called. The
     * iterator result should therefore be stored if it is used multiple times. The iterator
     * should usually have at least one result, though under exceptional circumstances it may
     * be empty (for example if at attribute value instantiation time it can't be easily checked if
     * the referenced entry exists).
     */
    Iterator<DictionaryEntry> getReferencedEntries() throws SearchException;
    /**
     * Return the title of the reference. This is usually the word field of the referenced entry.
     */
    String getReferenceTitle();
} // interface ReferenceAttributeValue
