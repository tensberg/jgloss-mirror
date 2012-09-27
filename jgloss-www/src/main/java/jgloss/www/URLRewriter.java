/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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

package jgloss.www;

/**
 * Used when URLs in an HTML document need to be changed based on a new context.
 *
 * @author Michael Koch
 */
public interface URLRewriter {
    /**
     * Changes a URL in some way.
     *
     * @param in The original URL.
     * @exception java.net.MalformedURLException if the given URL string or a newly constructed URL
     *            is malformed.
     */
    String rewrite( String in) throws java.net.MalformedURLException;

    /**
     * Changes a URL in some way.
     *
     * @param in The original URL.
     * @param tag Name of the tag in which the URL will be placed.
     * @exception java.net.MalformedURLException if the given URL string or a newly constructed URL
     *            is malformed.
     */
    String rewrite( String in, String tag) throws java.net.MalformedURLException;

    /**
     * Sets the base URL of the document from which the URLs to rewrite originate. Relative URLs should
     * be interpreted relative to this URL.
     */
    void setDocumentBase( String docBase);

    /**
     * Returns the base URL of the document from which the URLs to rewrite originate. 
     */
    String getDocumentBase();
} // interface URLRewriter
