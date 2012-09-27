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
 * $Id$
 *
 */

package jgloss.dictionary;

import static java.util.Collections.unmodifiableList;

import java.util.Arrays;
import java.util.List;

/**
 * Collection of distance search modes. Distance search modes take a positive integer as
 * distance.
 *
 * @author Michael Koch
 */
public class DistanceSearchModes extends AbstractSearchMode {
    public static final SearchMode NEAR = new DistanceSearchModes( "near");
    public static final SearchMode RADIUS = new DistanceSearchModes( "radius");
    
    private final static List<SearchParameter> PARAMETERS = unmodifiableList(Arrays.asList(
    				StandardSearchParameter.EXPRESSION,
    				StandardSearchParameter.SEARCH_FIELDS,
    				StandardSearchParameter.DISTANCE));

    private DistanceSearchModes( String id) {
        super( id);
    }

    /**
     * Parameters are {@link StandardSearchParameter#EXPRESSION EXPRESSION}, 
     * {@link StandardSearchParameter#SEARCH_FIELDS SEARCH_FIELDS} and
     * {@link StandardSearchParameter#DISTANCE DISTANCE}
     */
    @Override
	public List<SearchParameter> getParameters() { return PARAMETERS; }
} // class DistanceSearchModes
