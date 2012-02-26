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

package jgloss.dictionary;

/**
 * Collection of standart search parameters which are used by the most common search 
 * modes.
 *
 * @author Michael Koch
 */
public class StandardSearchParameter implements SearchParameter {
    protected Class paramClass;
    protected String description;

    /**
     * Search expression string. Parameter class is <code>java.lang.String</code>.
     */
    public static final SearchParameter EXPRESSION = 
        new StandardSearchParameter( String.class, "Search expression");
    /**
     * Search field selection. This lets you select which of the word/reading/translation
     * fields should be searched. Parameter class is {@link SearchFieldSelection SearchFieldSelection}.
     */
    public static final SearchParameter SEARCH_FIELDS =
        new StandardSearchParameter( SearchFieldSelection.class, "Search fields");
    /**
     * Wildcard character used in wildcard search. Parameter class is <code>java.lang.Character</code>.
     */
    public static final SearchParameter WILDCARD =
        new StandardSearchParameter( Character.class, "wildcard");
    /**
     * Distance used in near match and radius search. Parameter class is <code>java.lang.Short</code>.
     */
    public static final SearchParameter DISTANCE =
        new StandardSearchParameter( Short.class, "Distance");

    public StandardSearchParameter( Class _paramClass, String _description) {
        this.paramClass = _paramClass;
        this.description = _description;
    }

    @Override
	public Class getParameterClass() { return paramClass; }
    @Override
	public String getDescription() { return description; }

    @Override
	public String toString() { return getDescription(); }
} // class StandardSearchParameter
