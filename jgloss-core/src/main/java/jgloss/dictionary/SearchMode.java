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
 *
 */

package jgloss.dictionary;

import java.util.List;

/**
 * Interface which describes a search mode. Each {@link Dictionary Dictionary} supports some
 * search modes, which take different parameters. Each instance of this interface describes
 * a search mode and the parameters which it needs to be executed. The same search mode
 * objects can be used by different dictionary implementations. The search mode interface
 * does not describe how a dictionary implements a search using a given search mode. It is
 * up to the {@link Dictionary Dictionary} implementor to choose an appropriate algorithm.
 *
 * @author Michael Koch
 */
public interface SearchMode {
    /**
     * Returns the name of the search mode, suitable for presenting to the user.
     */
    String getName();
    /**
     * Return a short explanation of the search mode. This could be used e.g. for tooltips.
     */
    String getDescription();
    /**
     * Return the list of parameters which are needed for a search using this search mode
     * to be executed.
     */
    List<SearchParameter> getParameters();
} // interface SearchMode
