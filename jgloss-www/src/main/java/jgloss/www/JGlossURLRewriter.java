/*
 * Copyright (C) 2001-2013 Michael Koch (tensberg@gmx.net)
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * Rewrites a URL so that the request will be routed through the JGloss servlet.
 *
 * @author Michael Koch
 */
public class JGlossURLRewriter implements URLRewriter {
    /**
     * URL (absolute or relative) pointing to the servlet.
     */
    protected String servletBase;
    /**
     * URL of the document from which the URLs to rewrite originate.
     */
    protected URL docBase;
    /**
     * Set with protocol names.
     */
    protected Set<String> protocols;
    /**
     * Flag if form URLs should be changed.
     */
    protected boolean forwardFormData;

    /**
     * Creates a rewriter for a specific page.
     *
     * @param servletBase URL (absolute or relative) pointing to the servlet. Must not contain
     *                    a trailing slash.
     * @param docBase URL of the document from which the URLs to rewrite originate.
     * @param protocols Set of supported protocols.
     * @param allowCookieForwarding Flag if the cookie forwarding flag should be set in the
     *                              rewritten URL.
     * @param allowFormDataForwarding Flag if the form data forwarding flag should be set in the
     *                                rewritten URL and if <CODE>FORM</CODE> URLs should be rewritten.
     */
    public JGlossURLRewriter( String servletBase, URL docBase, Set<String> protocols,
                              boolean allowCookieForwarding, boolean allowFormDataForwarding) {
        this.servletBase = servletBase;
        this.docBase = docBase;
        this.protocols = protocols;
        this.forwardFormData = allowFormDataForwarding;

        this.servletBase += "/" + 
            (allowCookieForwarding ? "1" : "0") +
            (allowFormDataForwarding ? "1": "0") + "/"; 
    }

    /**
     * Rewrites a URL. The URL will be made absolute using the {@link #docBase docBase} URL.
     * If <CODE>tag</CODE> is <CODE>null</CODE>, or one of <CODE>a</CODE>, <CODE>area</CODE>
     * <CODE>frame</CODE> or form data forwarding is enabled and the tag is <CODE>form</CODE>,
     * and the protocol is in {@link #protocols protocols}, the URL will be changed to point 
     * to the JGloss servlet.
     *
     * @param in The URL to change.
     * @param tag Name of the tag in which the URL was found.
     * @param forceServletRelative Flag if the tag and protocol should be ignored and
     *                             the URL be changed to point to the servlet.
     */
    protected String rewrite( String in, String tag, boolean forceServletRelative)
        throws MalformedURLException {
        if (in.length() == 0) {
	        // rewriting an empty URL makes no sense
            return in;
        }

        if (tag != null) {
            tag = tag.toLowerCase();
            
            if (tag.equals( "base")) {
	            // ignore BASE tags
                return in;
            }
        }

        // make URL absolute using document base URL
        URL target = new URL( docBase, in);
        
        if (forceServletRelative ||
            ((tag==null || 
              tag.equals( "a") || 
              tag.equals( "area") ||
              tag.equals( "frame") ||
              forwardFormData && tag.equals( "form"))
             && protocols.contains( target.getProtocol()))) {
	        return servletBase + escapeURL( target.toExternalForm());
        } else {
	        return target.toExternalForm();
        }
    }

    @Override
	public String rewrite( String in, String tag) throws MalformedURLException {
        return rewrite( in, tag, false);
    }

    public String rewrite( String in, boolean forceServletRelative) throws MalformedURLException {
        return rewrite( in, null, forceServletRelative);
    }

    @Override
	public String rewrite( String in) throws MalformedURLException {
        return rewrite( in, null, false);
    }

    /**
     * Sets the base URL of the document from which the URLs to rewrite originate. Relative URLs should
     * be interpreted relative to this URL.
     */
    @Override
	public void setDocumentBase( String docBase) {
        try {
            synchronized (this.docBase) {
                this.docBase = new URL( docBase);
            }
        } catch (MalformedURLException ex) {}
    }

    /**
     * Returns the base URL of the document from which the URLs to rewrite originate. 
     */
    @Override
	public String getDocumentBase() {
        return docBase.toString();
    }

    /**
     * Escapes a string so that it can be used in the path component in a URL. Because
     * the web server may or may not unescape the path before passing it to the servlet,
     * the standard '%' escape mechanism cannot be used. Instead, '_' will
     * be used as escape mark.
     * Characters not in ISO-8859-1 are not supported and may not appear in the string.
     */
    public static String escapeURL( String in) {
        StringBuilder out = new StringBuilder( (int) (in.length()*1.5));
        for ( int i=0; i<in.length(); i++) {
            boolean escape = true;
            char c = in.charAt( i);
            // test alphanumerical
            if (c>='a' && c<='z' || c>='A' && c<='Z' || c>='0' && c<='9') {
	            escape = false;
            } else {
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

            if (escape) {
	            out.append( "_" + Integer.toHexString( c));
            } else {
	            out.append( c);
            }
        }

        return out.toString();
    }

    /**
     * Unescape an URL generated by {@link #escapeURL(String) escapeURL}.
     */
    public static String unescapeURL( String in) {
        StringBuilder out = new StringBuilder( in);
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
     *
     * @param path The pathinfo part of the call to the servlet.
     * @return Array of Boolean/Boolean/String with meaning allow cookie forwarding/
     *         allow form data forwarding/base URL; or <code>null</code> if the URL is not
     *         in the expected format.
     */
    public static Object[] parseEncodedPath( String path) {
        Object[] out = new Object[3];

        // path includes the leading '/'
        if (path.length()<5 || // base URL must have length at least one
            path.charAt( 3)!='/') {
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
