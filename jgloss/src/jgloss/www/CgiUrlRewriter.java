/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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

package jgloss.www;

import java.net.*;
import java.util.Set;

public class CgiUrlRewriter implements URLRewriter {
    protected String cgiBase;
    protected URL docBase;
    protected String cgiParamName;
    protected Set protocols;
    protected Set tags;

    public CgiUrlRewriter( String cgiBase, URL docBase, String cgiParamName, 
                           Set protocols, Set tags) {
        this.cgiBase = cgiBase;
        this.docBase = docBase;
        this.cgiParamName = cgiParamName;
        this.protocols = protocols;
        this.tags = tags;
    }

    public String rewrite( String in, String tag) throws MalformedURLException {
        try {
            if (in.length() == 0)
                // rewriting an empty URL makes no sense
                return in;

            URL target = new URL( docBase, in);

            if ((tag==null || tags.contains( tag)) && protocols.contains( target.getProtocol()))
                return cgiBase + "?" + cgiParamName + "=" + escape( target.toExternalForm());
            else
                return target.toExternalForm();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return in;
        }
    }

    public String rewrite( String in) throws MalformedURLException {
        return rewrite( in, null);
    }

    public String getCgiBase() { return cgiBase; }
    public void setCgiBase( String cgiBase) { this.cgiBase = cgiBase; }

    public URL getDocBase() { return docBase; }
    public void setDocBase( URL docBase) { this.docBase = docBase; }

    public String getCgiParamName() { return cgiParamName; }
    public void setCgiParamName( String paramName) { this.cgiParamName = cgiParamName; }

    /**
     * Escapes a string so that it can be used as query parameter in a URL.
     * Characters not in ISO-8859-1 are not supported and may not appear in the string.
     */
    public static String escape( String in) {
        StringBuffer out = new StringBuffer( (int) (in.length()*1.5));
        for ( int i=0; i<in.length(); i++) {
            boolean escape = true;
            char c = in.charAt( i);
            // test alphanumerical
            if (c>='a' && c<='z' || c>='A' && c<='Z' || c>='0' && c<='9')
                escape = false;
            else {
                // test unreserved mark
                switch (c) {
                case '-':
                case '_':
                case '.':
                case '!':
                case '~':
                case '*':
                case '\'':
                case '(':
                case ')':
                    escape = false;
                    break;
                }
            }

            if (escape)
                out.append( "%" + Integer.toHexString( (int) c));
            else
                out.append( c);
        }

        return out.toString();
    }
} // interface URLRewriter
