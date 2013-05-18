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

package jgloss.ui;

/**
 * Preconditions which determine whether a dictionary lookup should be performed for the given lookup
 * model configuration. In certain configurations it is possible, but not very useful to perform a search.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
class PerformSearchPreconditions {
    /**
     * Returns whether a dictionary lookup should be performed for the given lookup model. In the current
     * implementation a lookup should only be performed if the {@link LookupModel#getSearchExpression() search expression}
     * is not empty.
     * 
     * @param model Lookup model which should be checked.
     * @return <code>true</code> if a dictionary lookup should be performed with the given configuration.
     */
    boolean performSearch(LookupModel model) {
        String searchExpression = model.getSearchExpression();
        return searchExpression != null && !searchExpression.isEmpty();
    }
}
