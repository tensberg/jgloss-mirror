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

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Utility class for forwarding cookies between client-servlet and servlet-remote server.
 *
 * @author Michael Koch
 */
public class CookieTools {
    private static final StringBuffer parser = new StringBuffer( 100);

    /**
     * Date format of the expires cookie attribute as defined in the Netscape cookie
     * specification.
     */
    public static final DateFormat expiresDateFormat = new SimpleDateFormat
        ( "EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);

    /**
     * Read cookies encapsulated by <CODE>addResponseCookies</CODE> from the client request and
     * forward them on the remote url connection. Both Netscape and RFC2965 cookies are supported.
     * 
     * @param connection URL connection on which the cookies will be forwarded. The connect method
     *                   must not have been already called.
     * @param cookies Cookies taken from the client request.
     * @see #addResponseCookies(URLConnection,HttpServletResponse,String,String,boolean,boolean,boolean)
     */
    public static void addRequestCookies( URLConnection connection, Cookie[] cookies,
                                          ServletContext context) {
        URL url = connection.getURL();
        context.log( "adding request cookies to " + url.toString());
        if (cookies == null) {
            context.log( "no cookies");
            return;
        }
        if (cookies.length == 0) {
            context.log( "cookies empty");
            return;
        }

        String host = url.getHost();
        if (host==null || host.length()==0)
            return;
        // build effective host name
        if (host.indexOf( '.') == -1) // this applies e.g. for "localhost"
            host += ".local";
        String path = url.getPath();
        if (path==null || path.length()==0)
            path = "/";
        int porti = url.getPort();
        if (porti == -1) {
            // this method is only called for protocols http and https
            if ("https".equals( url.getProtocol()))
                porti = 443;
            else
                porti = 80;
        }
        String port = String.valueOf( porti);

        StringBuffer cookietext = new StringBuffer( 10000);
        int maxVersion = 0;
        for ( int i=0; i<cookies.length; i++) {
            Cookie c = cookies[i];
            context.log( "adding cookie " + c.getName());

            // don't send secure cookies over an insecure connection
            if (c.getSecure() && !"https".equalsIgnoreCase( url.getProtocol())) {
                context.log( "security test failed");
                continue;
            }
            
            // the original domain|path|portlist|name is encoded in the cookie name
            try {
                String name = c.getName();
                int i1 = name.indexOf( '|');
                if (i1 == -1)
                    continue;
                int i2 = name.indexOf( '|', i1 + 1);
                if (i2 == -1)
                    continue;
                int i3 = name.indexOf( '|', i2 + 1);
                if (i3 == -1)
                    continue;

                String cdomain = unescape( name.substring( 0, i1));
                String cpath = unescape( name.substring( i1+1, i2));
                String cportlist = unescape( name.substring( i2+1, i3));
                if (!domainMatch( host, cdomain))
                    context.log(  "domainmatch failed");
                if (!portMatch( port, cportlist))
                    context.log(  "portmatch failed");
                if (!path.startsWith( cpath))
                    context.log(  "path/cpath failed");
                // match domain/path/port to target URL as described in RFC2965, sect. 3.3.4
                if (domainMatch( host, cdomain) &&
                    portMatch( port, cportlist) &&
                    path.startsWith( cpath)) {
                    name = name.substring( i3 + 1);

                    cookietext.append( ';');
                    cookietext.append( name);
                    cookietext.append( '=');
                    cookietext.append( c.getValue());

                    if (c.getVersion() > 0) {
                        // support RFC2965 cookie information
                        if (cpath.length() > 0) {
                            cookietext.append( "; $Path=");
                            cookietext.append( cpath);
                        }
                        if (cdomain.length() > 0) {
                            cookietext.append( "; $Domain=");
                            cookietext.append( cdomain);
                        }
                        if (cportlist.length() > 0) {
                            cookietext.append( "; $Port=");
                            cookietext.append( cportlist);
                        }
                    }
                }
                
                maxVersion = Math.max( maxVersion, c.getVersion());
            } catch (StringIndexOutOfBoundsException ex) {
                context.log( ex.getMessage());
                // Cookie not generated by this class. Should not happen.
            }
        }

        if (cookietext.length() > 0) { // == 0 if StringIndexOutOfBoundsExeption for all cookies
            cookietext.insert( 0, "$Version=" + maxVersion);
            connection.setRequestProperty( "Cookie", cookietext.toString());
        }
    }
    
    /**
     * Read the cookie headers from a url connection, encapsulate the cookies and add them
     * to the response. Both Netscape (Set-Cookie) and RFC2965 (Set-Cookie2) cookies are supported.
     *
     * @param connection URL connection from which the cookies will be read.
     * @param resp Response on which the cookies will be forwarded.
     * @param servletDomain Domain name of the servlet container.
     * @param servletPath Path to the servlet on the servlet container.
     * @param secure <CODE>true</CODE> if the connection between servlet and client is secure.
     * @return Map of cookies added to the response in encoded form, with the cookie name as key.
     */
    public static Map addResponseCookies( URLConnection connection, HttpServletResponse resp,
                                               String servletDomain, String servletPath, boolean secure,
                                               ServletContext context) {
        Map newCookies = new HashMap( 50);
        context.log( "adding response cookies");
        URL url = connection.getURL();
        String host = url.getHost();
        if (host==null || host.length()==0)
            return newCookies;
        String path = url.getPath();
        if (path==null || path.length()==0)
            path = "/";
        int porti = url.getPort();
        if (porti == -1) {
            // this method is only called for protocols http and https
            if ("https".equals( url.getProtocol()))
                porti = 443;
            else
                porti = 80;
        }
        String port = String.valueOf( porti);

        // Iterate over all headers. The same header can be set multiple times with
        // differing values.
        int i = 1; // header fields are 1-based. Header 0 does not exist.
        String header;
        Map cookie = new HashMap( 10);
        while ((header = connection.getHeaderFieldKey( i++)) != null) {
            boolean version1 = false;
            if (header.equalsIgnoreCase( "set-cookie2"))
                version1 = true;
            else if (!header.equalsIgnoreCase( "set-cookie"))
                continue;

            String cs = connection.getHeaderField( i-1); // i was incremented in while condition
            if (cs == null)
                continue;
            context.log( "header " + connection.getHeaderFieldKey( i-1) +  ": " +  cs);
            
            // iterate over list of cookies, separated by ','
            int next = -1;
            while ((next = parseCookie( cs, cookie, next+1, version1)) != -1) {
                context.log( "parse successful");
                //context.log( parser.toString());
                // skip this cookie if path servlet-client is not secure and the 
                // secure attribute is set
                if (cookie.containsKey( "secure") && !secure)
                    continue;
                    
                String name = (String) cookie.remove( " NAME");                   
                String value = (String) cookie.remove( " VALUE");
                context.log( "name " + name + " value " + value);
                if (value.length() == 0)
                    continue;
                String cdomain = (String) cookie.remove( "domain");
                if (cdomain == null)
                    cdomain = host;
                String cpath = (String) cookie.remove( "path");
                if (cpath == null)
                    cpath = path;
                String cportlist = (String) cookie.remove( "port");
                if (cportlist == null)
                    cportlist = "";
                else if (cportlist.length() == 0)
                    cportlist = "\"" + port + "\"";
                context.log( "cdomain: " + cdomain + " cpath: " + cpath + " cportlist: " + cportlist);

                // Reject cookies as described in RFC2965, section 3.3.2.
                // The rejection has to be done here because the cookie will be encapsulated and
                // the client browser cannot filter based on the original cookie.

                // The Version rule only applies to set-cookie2
                if (version1 && 
                    (!cookie.containsKey( "version") || cookie.get( "version").equals( ""))) {
                    context.log( "version test failed");
                    continue;
                }

                // Reject the cookie if any of the following conditions are met:

                // The value for the Path attribute is not a prefix of the
                // request-URI.
                if (!path.startsWith( cpath)) {
                    context.log( "path test failed: " + path + " : " + cpath);
                    continue;
                }
                // The value for the Domain attribute contains no embedded dots,
                // and the value is not .local.
                int dot = cdomain.indexOf( '.', 1);
                if ((dot==-1 || dot==cdomain.length()) && !".local".equals( cdomain)) {
                    context.log( "embedded dots test failed");
                    continue;
                }
                // The effective host name that derives from the request-host does
                // not domain-match the Domain attribute.
                String effectiveHost = host;
                if (host.indexOf( '.') == -1)
                    effectiveHost += ".local";
                if (!domainMatch( effectiveHost, cdomain)) {
                    context.log( "domain match test failed: " + effectiveHost + " / " + cdomain);
                    continue;
                }
                // The request-host is a HDN (not IP address) and has the form HD,
                // where D is the value of the Domain attribute, and H is a string
                // that contains one or more dots.
                int index = host.indexOf( '.');
                if (host.endsWith( cdomain) && host.length()>cdomain.length() &&
                    index != -1 &&
                    index < host.length()-cdomain.length()) {
                    context.log( "hd test failed");
                    continue;
                }
                // The Port attribute has a "port-list", and the request-port was
                // not in the list.
                if (cportlist.length()!=0 && !portMatch( port, cportlist)) {
                    context.log( "port test failed");
                    continue;
                }
                context.log( "all test succeeded");
                
                // build the new cookie
                Cookie c = new Cookie
                    ( escape( cdomain) + "|" + escape( cpath) + "|" + escape( cportlist) + "|" +
                      name, value);
                String expires = (String) cookie.get( "expires");
                if (expires != null) try {
                    Date d = expiresDateFormat.parse( expires);
                    c.setMaxAge( (int) ((d.getTime()-System.currentTimeMillis()) / 1000));
                } catch (ParseException ex) {
                    context.log( "expires " + expires + " " + ex.getMessage());
                }

                c.setPath( servletPath);
                c.setDomain( servletDomain);
                if (cookie.containsKey( "secure"))
                    c.setSecure( true);
                
                if (version1) {
                    c.setVersion( Integer.parseInt( (String) cookie.get( "version")));
                    if (cookie.containsKey( "max-age"))
                        c.setMaxAge( Integer.parseInt( (String) cookie.get( "max-age")));
                    if (cookie.containsKey( "comment"))
                        c.setComment( (String) cookie.get( "comment"));
                }
                resp.addCookie( c);
                newCookies.put( c.getName(), c);
            }
        }

        return newCookies;
    }
        
    /**
     * Do a domain-match test as specified in RFC2965.
     */
    protected static boolean domainMatch( String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        if (a.equals( b))
            return true;

        if (a.length()==0 && b.length()>0)
            return false;
        if (b.length() == 0)
            return true;

        // if b starts with '.' it can't be an ip address
        return (!(a.charAt( 0)=='.') && b.charAt( 0)=='.' &&
                a.endsWith( b));
    }

    /**
     * Do a port match in a list of ports. The port matches the portlist is empty, 
     * or if it is a substring of the 
     * portlist and the characters before and after are not numeric.
     */
    protected static boolean portMatch( String port, String portlist) {
        if (portlist.length() == 0)
            return true;

        int match = portlist.indexOf( port);
        while (match != -1) {
            boolean isMatch = true;
            if (match > 0) {
                char c = portlist.charAt( match-1);
                // test if it is an exact match
                if (c >= '0' && c <= '9')
                    isMatch = false;
            }
            if (match + port.length() < portlist.length()) {
                char c = portlist.charAt( match+port.length());
                if (c >= '0' && c <= '9')
                    isMatch = false;
            }
            if (isMatch)
                return true;
            
            match = portlist.indexOf( port, match+1);
        }
        return false;
    }

    /**
     * Parse a single cookie definition according to RFC2965 or the Netscape cookie definition.
     * <P>
     * The name/attribute pairs
     * will be stored in the attributes map, which will be cleared initially. The first
     * name/value pair, which has a special meaning in the cookie definition, will
     * be stored under the keys <CODE>" NAME"</CODE> and <CODE>" VALUE"</CODE>. All other
     * name/value pairs will be stored as a single pair with the name as key, converted
     * to all-lowercase.
     * </P><P>
     * A Netscape cookie definition may contain spaces and commas in unquoted values, and define at most
     * one valid cookie per cookie string. A RFC2965 cookie definition may contain several cookies,
     * separated by ','.
     * </P>
     *
     * @param cookie The definition string of a single cookie, without the Set-Cookie part.
     * @param attributes The name/value pairs will be placed in this map.
     * @param version1 <CODE>true</CODE> if the cookie string complies with RFC2965,
     *                 <CODE>false</CODE> if it is a Netscape cookie.
     * @return The index in the cookie string where the cookie ended, or -1 if there was no
     *         valid cookie definition.
     */
    protected static int parseCookie( String cookie, Map attributes, int from,
                                      boolean version1) {
        parser.delete( 0, parser.length());
        attributes.clear();

        final int BEFORE_NAME = 0;
        final int IN_NAME = 1;
        final int BEFORE_ASSIGN = 2;
        final int IN_VALUE = 3;
        final int AFTER_VALUE = 4;
        final int SKIP_INVALID = 5;
        int state = BEFORE_NAME;
        boolean inQuotedString = false;

        // the first name/value pair does not set an attribute but the cookies NAME and VALUE
        boolean firstPair = true;

        StringBuffer name = new StringBuffer( 100);
        StringBuffer value = new StringBuffer( 100);

        // Guarantee that the last attribute pair is written.
        cookie = cookie + (version1 ? "," : ";");

        for ( int i=from; i<cookie.length(); i++) {
            char c = cookie.charAt( i);
            parser.append( state);
            parser.append( c);
            
            switch (state) {
            case BEFORE_NAME:
                // skip whitespace
                if (c==32 || c==9)
                    continue;
                state = IN_NAME;
                // begin of name; fall through to IN_NAME
                
            case IN_NAME:
                if (c==32 || c==9)
                    state = BEFORE_ASSIGN;
                else if (c == '=')
                    state = IN_VALUE;
                else if (!version1 || isTokenChar( c))
                    name.append( c);
                else if (c==',' && version1 && name.length()==0 && firstPair) {
                    // cookie was empty except for whitespace. This does not define
                    // a cookie, but is allowed.
                    continue;
                }
                else {
                    // illegal character
                    if (version1)
                        state = SKIP_INVALID;
                    else
                        return -1;
                }
                break;
                
            case BEFORE_ASSIGN:
                if (c==32 || c==9)
                    continue;
                else if (c=='=')
                    state = IN_VALUE;
                else if (c==';' || version1 && c==',') {
                    // attribute without value
                    state = AFTER_VALUE;
                    i--; // re-examine character in new state
                    continue;
                }
                else { // illegal char
                    if (version1)
                        state = SKIP_INVALID;
                    else
                        return -1;
                }
                break;

            case IN_VALUE:
                if (inQuotedString) {
                    if (c == '"') {
                        inQuotedString = false;
                        state = AFTER_VALUE;
                        continue;
                    }
                    if (c<32 || c==127) {// control characters
                        if (version1)
                            state = SKIP_INVALID;
                        else
                            return -1;
                    }
                    if (c == '\\') { // quote char, examine next character
                        value.append( c);
                        i++;
                        c = cookie.charAt( i);
                        if (c > 127) { // not CHAR
                            if (version1)
                                state = SKIP_INVALID;
                            else
                                return -1;
                        }
                    }
                }
                else if (version1 && c == '"') { // Netscape cookies don't have quoted values
                    inQuotedString = true;
                    continue;
                }
                else if (c==32 || c==9) {
                    // version 0 cookies may contain whitespace in unquoted value definition
                    if (version1 && value.length() > 0) {
                        state = AFTER_VALUE;
                        continue;
                    }
                }
                else if (c==';' || version1 && c==',') {
                    if (value.length() == 0) { // if there was a '=', there must be some chars before ';'
                        if (version1) {
                            state = SKIP_INVALID;
                            if (c == ',') // re-examine in new state
                                i--;
                        }
                        else
                            return -1;
                    }
                    state = AFTER_VALUE;
                    i--; // re-examine char in new state
                    continue;
                }
                else if (version1 && !isTokenChar( c)) {
                    state = SKIP_INVALID;
                    if (c == ',')
                        i--;
                }
                value.append( c);
                break;
                
            case AFTER_VALUE:
                if (c==';' || c==',') { // end of name-value pair
                    if (firstPair) {
                        attributes.put( " NAME", name.toString());
                        attributes.put( " VALUE", value.toString().trim());
                        firstPair = false;
                    }
                    else
                        // with version0 cookies, the parser does not distinguish between
                        // whitespace in and whitespace after a value
                        attributes.put( name.toString().toLowerCase(), value.toString().trim());
                    name.delete( 0, name.length());
                    value.delete( 0, value.length());
                    state = BEFORE_NAME;
                    
                    // end of valid cookie definition
                    // due to the appended character this will always be reached at the end
                    // of the cookie string
                    if (!version1 && i==cookie.length()-1 || version1 && c==',')
                        return i;
                }
                else if (c!=32 && c!=9) { // illegal character
                    if (version1)
                        state = SKIP_INVALID;
                    else
                        return -1;
                }
                break;

            case SKIP_INVALID:
                // skip invalid version1 cookie definition by searching the next ','
                attributes.clear();
                name.delete( 0, name.length());
                value.delete( 0, value.length());
                
                if (c == ',')
                    state = BEFORE_NAME;
                break;
            }
        }

        return -1;
    }

    protected static String escape( String in) {
        StringBuffer out = new StringBuffer( in);
        for ( int i=out.length()-1; i>=0; i--) {
            char c = out.charAt( i);
            if (c=='|' || c=='%' || !isTokenChar( c)) {
                String hex = Integer.toHexString( (int) c);
                if (hex.length() == 1)
                    hex = "0" + hex;
                out.replace( i, i+1, "%" + hex);
            }
        }

        return out.toString();
    }

    protected static String unescape( String in) {
        StringBuffer out = new StringBuffer( in);
        for ( int i=out.length()-3; i>=0; i--) try {
            if (out.charAt( i) == '%') {
                out.setCharAt( i, (char) Integer.parseInt( out.substring( i+1, i+3), 16));
                out.delete( i+1, i+3);
            }
        } catch (NumberFormatException ex) {
            // should not happen with strings generated by escape()
        }
        return out.toString();
    }

    /**
     * Test if d is a token character as defined in RFC2616.
     */
    protected final static boolean isTokenChar( char c) {
        if (c <= 32) // this includes SP and HT characters from separators rule
            return false;

        switch (c) {
        case 127: // DEL, from CTL rule
        // separators
        case '(':
        case ')':
        case '<':
        case '>':
        case '@':
        case ',':
        case ';':
        case ':':
        case '\\':
        case '"':
        case '/':
        case '[':
        case ']':
        case '?':
        case '=':
        case '{':
        case '}':
            return false;

        default:
            return true;
        }
    }

} // class CookieTools
