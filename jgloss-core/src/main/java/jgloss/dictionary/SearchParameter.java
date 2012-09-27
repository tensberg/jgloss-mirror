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

/**
 * Interface describing a parameter of a search. Each search in a {@link Dictionary Dictionary}
 * takes a {@link SearchMode search mode} argument and a set of parameters specified by the search
 * mode. Instances of this interface describe a parameter type. Constant objects implementing
 * this interface are used to define "well known" parameters, which should have user interface
 * widgets to control their value.
 *
 * @author Michael Koch
 * @see StandardSearchParameter
 */
public interface SearchParameter {
    /**
     * Get the class which objects used as values of this parameter type must be instances of.
     */
    Class<?> getParameterClass();
    /**
     * Return a short description of what this parameter controls. This could be as short as a single
     * word.
     */
    String getDescription();
} // interface SearchParameter
