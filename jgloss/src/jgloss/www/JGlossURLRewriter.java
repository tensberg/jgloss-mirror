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

public class JGlossURLRewriter implements URLRewriter {
    protected String cgiBase;
    protected URL docBase;
    protected Set protocols;
    protected boolean forwardFormData;

    public JGlossURLRewriter( String cgiBase, URL docBase, Set protocols,
                              boolean allowCookieForwarding, boolean allowFormDataForwarding) {
        this.cgiBase = cgiBase;
        this.docBase = docBase;
        this.protocols = protocols;
        this.forwardFormData = allowFormDataForwarding;

        this.cgiBase += "/" + 
            (allowCookieForwarding ? "1" : "0") +
            (allowFormDataForwarding ? "1": "0") + "/"; 
    }

    public String rewrite( String in, String tag) throws MalformedURLException {
        try {
            if (in.length() == 0)
                // rewriting an empty URL makes no sense
                return in;

            URL target = new URL( docBase, in);

            if ((tag==null || tag.equalsIgnoreCase( "a") || tag.equalsIgnoreCase( "area")
                 || forwardFormData && tag.equalsIgnoreCase( "form"))
                && protocols.contains( target.getProtocol()))
                return cgiBase + escapeURL( target.toExternalForm());
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

    /**
     * Escapes a string so that it can be used in the path component in a URL. Because
     * the web server may or may not unescape the path before passing it to the servlet
     * context, the standard '%' escape mechanism cannot be used. Instead, '_' will
     * be used as escape mark.
     * Characters not in ISO-8859-1 are not supported and may not appear in the string.
     */
    public static String escapeURL( String in) {
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
                    //case '_': used as escape mark
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
                out.append( "_" + Integer.toHexString( (int) c));
            else
                out.append( c);
        }

        return out.toString();
    }

    /**
     * Unescape an URL generated by {@link #escapeURL(String) escapeURL}.
     */
    public static String unescapeURL( String in) {
        StringBuffer out = new StringBuffer( in);
        for ( int i=out.length()-3; i>=0; i--) {
            if (out.charAt( i) == '_') {
                out.setCharAt( i, (char) Integer.parseInt( out.substring( i+1, i+3), 16));
                out.delete( i+1, i+3);
            }
        }
        return out.toString();
    }

    /**
     * Parses the path info part of a call to the JGloss-WWW servlet.
     */
    public static Object[] parseEncodedPath( String path) {
        Object[] out = new Object[3];

        // path includes the leading '/'
        if (path.length()<4 || path.charAt( 3)!='/') {
            return null;
        }

        // allowCookieForwarding
        out[0] = (path.charAt( 1) == '1') ? Boolean.TRUE : Boolean.FALSE;
        // allowFormDataForwarding
        out[1] = (path.charAt( 2) == '1') ? Boolean.TRUE : Boolean.FALSE;
        // remote url
        out[2] = unescapeURL( path.substring( 4));

        return out;
    }
} // interface URLRewriter
