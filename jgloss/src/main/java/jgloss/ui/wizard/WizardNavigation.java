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
 */

package jgloss.ui.wizard;

/**
 * Enumeration of navigation controls available.
 */
public enum WizardNavigation {
    /**
     * Navigate to the next page. Close the wizard if the current page is the
     * last one.
     */
    FORWARD,
    /**
     * Navigate to the previous page. Not available on the first page.
     */
    BACK,
    /**
     * Cancel the wizard instead of moving to a different page.
     */
    CANCEL
}
