/*
 * Copyright (C) 2002-2015 Michael Koch (tensberg@gmx.net)
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
 */

package jgloss.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class Exclusions {

    private final List<ExclusionResourceLoader> exclusionResources = new ArrayList<>();

    public int size() {
        return exclusionResources.size();
    }

    public ExclusionResource getExclusionResource(int index) {
        return exclusionResources.get(index).getExclusionResource();
    }

    public Set<String> getExclusions() {
        Set<String> exclusions = new HashSet<>();
        for (ExclusionResourceLoader exclusionResource : exclusionResources) {
            if (exclusionResource.getExclusionResource().isEnabled()) {
                exclusions.addAll(exclusionResource.loadExclusions());
            }
        }
        return exclusions;
    }
}
