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

package jgloss.dictionary;

/**
 * Collection of standard expression search modes.
 *
 * @author Michael Koch
 */
public class ExpressionSearchModes implements SearchMode {
    public static final SearchMode EXACT = new ExpressionSearchModes( "Exact match");
    public static final SearchMode PREFIX = new ExpressionSearchModes( "Starts with");
    public static final SearchMode SUFFIX = new ExpressionSearchModes( "Ends with");
    public static final SearchMode ANY = new ExpressionSearchModes( "Any match");

    private String name;
    
    private final static SearchParameters PARAMETERS = new SearchParameters
        ( new SearchParameter[] { StandardSearchParameter.EXPRESSION,
                                  StandardSearchParameter.SEARCH_FIELDS });

    private ExpressionSearchModes( String _name) {
        this.name = _name;
    }

    public String getName() { return name; }

    /**
     * Parameters are {@link StandardSearchParameter.EXPRESSION EXPRESSION} and
     * {@link StandardSearchParameter.SEARCH_FIELDS SEARCH_FIELDS}.
     */
    public SearchParameters getParameters() { return PARAMETERS; }
} // class ExpressionSearchModes
