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

import jgloss.dictionary.*;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.MessageFormat;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Servlet which annotates an HTML page with the dictionary entries for
 * Japanese words. The servlet takes an URL, loads the specified page, annotates it
 * and forwards it to the client. The annotations will be displayed via JavaScript.
 *
 * @author Michael Koch
 */
public class JGlossServlet extends HttpServlet {
    public final static String MESSAGES = "resources/messages-www";

    public final static String DICTIONARIES = "dictionaries";
    public final static String ALLOWED_PROTOCOLS = "allowed_protocols";
    public final static String ENABLE_COOKIE_FORWARDING = "enable_cookie_forwarding";
    public final static String ENABLE_SECURE_INSECURE_COOKIE_FORWARDING =
        "enable_secure-to-insecure_cookie_forwarding";
    public final static String ENABLE_FORM_DATA_FORWARDING = "enable_form_data_forwarding";
    public final static String ENABLE_SECURE_INSECURE_FORM_DATA_FORWARDING =
        "enable_secure-to-insecure_form_data_forwarding";

    public final static String REMOTE_URL = "jgurl";
    public final static String ALLOW_COOKIE_FORWARDING = "jgforwardcookies";
    public final static String ALLOW_FORM_DATA_FORWARDING = "jgforwardforms";

    private jgloss.dictionary.Dictionary[] dictionaries;
    private Parser parser;
    private HTMLAnnotator annotator;
    private Set allowedProtocols;

    private boolean enableCookieForwarding;
    private boolean enableCookieSecureInsecureForwarding;
    private boolean enableFormDataForwarding;
    private boolean enableFormDataSecureInsecureForwarding;

    public JGlossServlet() {}

    public void init( ServletConfig config) throws ServletException {
        super.init( config);

        HttpURLConnection.setFollowRedirects( true);

        // register dictionaries
        DictionaryFactory.registerImplementation( EDict.class, EDict.implementation);
        DictionaryFactory.registerImplementation( KanjiDic.class, KanjiDic.implementation);
        DictionaryFactory.registerImplementation( SKKDictionary.class, SKKDictionary.implementation);

        // load the dictionaries
        List diclist = new LinkedList();
        String d = config.getInitParameter( DICTIONARIES);
        if (d==null || d.length()==0)
            throw new ServletException( MessageFormat.format
                                        ( ResourceBundle.getBundle( MESSAGES)
                                          .getString( "error.nodictionary"),
                                          new Object[] { DICTIONARIES }));
        for ( Iterator i=split( d, File.pathSeparatorChar).iterator(); i.hasNext(); ) {
            d = (String) i.next();
            jgloss.dictionary.Dictionary dic = null;
            try {
                dic = DictionaryFactory.createDictionary( d);
            } catch (Exception ex) {
                throw new ServletException( MessageFormat.format
                                            ( ResourceBundle.getBundle( MESSAGES)
                                              .getString( "error.loaddictionary"),
                                              new Object[] { d })
                                            , ex);
            }
            if (dic == null) // unrecognized dictionary format
                throw new ServletException( MessageFormat.format
                                            ( ResourceBundle.getBundle( MESSAGES)
                                              .getString( "error.unknowndictionary"),
                                              new Object[] { d }));
            diclist.add( dic);
        }
        dictionaries = new jgloss.dictionary.Dictionary[diclist.size()];
        dictionaries = (jgloss.dictionary.Dictionary[]) diclist.toArray( dictionaries);

        parser = new Parser( dictionaries);
        parser.setIgnoreNewlines( true);
        try {
            annotator = new HTMLAnnotator( parser);
        } catch (IOException ex) {
            throw new ServletException( ex);
        }

        // read allowed protocols
        allowedProtocols = new HashSet( 5);
        String p = config.getInitParameter( ALLOWED_PROTOCOLS);
        if (p==null || p.length()==0)
            throw new ServletException( MessageFormat.format
                                        ( ResourceBundle.getBundle( MESSAGES)
                                          .getString( "error.noprotocols"),
                                          new Object[] { ALLOWED_PROTOCOLS}));
        allowedProtocols.addAll( split( p, ':'));

        p = config.getInitParameter( ENABLE_COOKIE_FORWARDING);
        enableCookieForwarding = "true".equals( p);
        getServletContext().log( "cookie forwarding " + (enableCookieForwarding ? "enabled" : "disabled"));
        p = config.getInitParameter( ENABLE_SECURE_INSECURE_COOKIE_FORWARDING);
        enableCookieSecureInsecureForwarding = "true".equals( p);
        getServletContext().log( "secure-to-insecure cookie forwarding " + 
                                 (enableCookieSecureInsecureForwarding ? "enabled" : "disabled"));
        p = config.getInitParameter( ENABLE_SECURE_INSECURE_FORM_DATA_FORWARDING);
        enableFormDataSecureInsecureForwarding = "true".equals( p);
        getServletContext().log( "secure-to-insecure form data forwarding " + 
                                 (enableFormDataSecureInsecureForwarding ? "enabled" : "disabled"));
        p = config.getInitParameter( ENABLE_FORM_DATA_FORWARDING);
        enableFormDataForwarding = "true".equals( p);
        getServletContext().log( "form data forwarding " + 
                                 (enableFormDataForwarding ? "enabled" : "disabled"));
    }

    public void destroy() {
        super.destroy();

        for ( int i=0; i<dictionaries.length; i++)
            dictionaries[i].dispose();
    }

    protected void doGet( HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doGetPost( req, resp, false);
    }

    protected void doPost( HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doGetPost( req, resp, true);
    }

    protected void doGetPost( HttpServletRequest req, HttpServletResponse resp,
                              boolean post) 
        throws ServletException, IOException {
        String pathinfo = req.getPathInfo();
        if (pathinfo == null) {
            String urlstring = req.getParameter( REMOTE_URL);
            if (urlstring == null) {
                resp.sendError( HttpServletResponse.SC_BAD_REQUEST,
                                ResourceBundle.getBundle( MESSAGES, req.getLocale())
                                .getString( "error.nourl"));
                return;
            }
            boolean allowCookieForwarding = "true".equals( req.getParameter( ALLOW_COOKIE_FORWARDING));
            boolean allowFormDataForwarding = 
                "true".equals( req.getParameter( ALLOW_FORM_DATA_FORWARDING));
            String target = new JGlossURLRewriter
                ( req.getContextPath() + req.getServletPath(),
                  new URL( HttpUtils.getRequestURL( req).toString()), allowedProtocols,
                  allowCookieForwarding, allowFormDataForwarding).rewrite( urlstring);
            resp.sendRedirect( target);
            return;
        }

        Object[] oa = JGlossURLRewriter.parseEncodedPath( pathinfo);
        // pathinfo includes the leading '/'
        if (oa == null) {
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST,
                            MessageFormat.format
                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                              .getString( "error.malformedrequest"),
                              new Object[] { pathinfo } ));
            return;
        }
        boolean allowCookieForwarding = ((Boolean) oa[0]).booleanValue();
        boolean allowFormDataForwarding = ((Boolean) oa[1]).booleanValue();
        String urlstring = (String) oa[2];
            
        // don't allow the servlet to call itself
        if (urlstring.toLowerCase().indexOf( req.getServletPath().toLowerCase()) != -1) {
            resp.sendError( HttpServletResponse.SC_FORBIDDEN,
                            MessageFormat.format
                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                              .getString( "error.addressnotallowed"),
                              new Object[] { urlstring } ));
            return;
        }

        // prepend protocol if neccessary
        if (urlstring.indexOf( ':') == -1) {
            if (allowedProtocols.contains( "http"))
                urlstring = "http://" + urlstring;
        }

        URL url = null;
        try {
            url = new URL( urlstring);
        } catch ( MalformedURLException ex) {
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST,
                            MessageFormat.format
                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                              .getString( "error.malformedurl"),
                              new Object[] { urlstring} ));
            return;
        }

        String protocol = url.getProtocol();
        if (!allowedProtocols.contains( protocol)) {
            resp.sendError( HttpServletResponse.SC_FORBIDDEN,
                            MessageFormat.format
                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                              .getString( "error.protocolnotallowed"),
                              new Object[] { protocol } ));
            return;
        }

        boolean forwardCookies = (protocol.equals( "http") || protocol.equals( "https")) &&
            enableCookieForwarding && allowCookieForwarding &&
            (enableCookieSecureInsecureForwarding ||
             !"https".equalsIgnoreCase( url.getProtocol()) 
             || req.isSecure());
        boolean forwardFormData = (protocol.equals( "http") || protocol.equals( "https")) &&
            enableFormDataForwarding && allowFormDataForwarding &&
            (enableFormDataSecureInsecureForwarding ||
             !req.isSecure() || "https".equalsIgnoreCase( url.getProtocol()));

        // add query parameters which may have been appended by a GET form
        if (forwardFormData) {
            String query = req.getQueryString();
            if (query!=null && query.length()>0) {
                if (url.getQuery()==null || url.getQuery().length()==0)
                    url = new URL( url.toExternalForm() + "?" + query);
                else
                    url = new URL( url.toExternalForm() + "&" + query);
            }
        }
            
        URLConnection connection = url.openConnection();

        if (forwardFormData && post && connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setRequestMethod( "POST");
            connection.setDoInput( true);
            connection.setDoOutput( true);
        }

        forwardRequestHeaders( connection, req);
        if (forwardCookies)
            CookieTools.addRequestCookies( connection, req.getCookies(), getServletContext());

        try {
            connection.connect();
        } catch (UnknownHostException ex) {
            resp.sendError( HttpServletResponse.SC_BAD_GATEWAY,
                            MessageFormat.format
                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                              .getString( "error.unknownhost"),
                              new Object[] { url.toExternalForm(), url.getHost() } ));
            return;
        } catch (IOException ex) {
            resp.sendError( HttpServletResponse.SC_BAD_GATEWAY,
                            MessageFormat.format
                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                              .getString( "error.connect"),
                              new Object[] { url.toExternalForm(), ex.getClass().getName(),
                                             ex.getMessage() } ));
            return;
        }

        if (forwardFormData && post && connection instanceof HttpURLConnection) {
            InputStream is = req.getInputStream();
            OutputStream os = connection.getOutputStream();
            byte[] buf = new byte[512];
            int len;
            while ((len=is.read( buf)) != -1)
                os.write( buf, 0, len);
            is.close();
            os.close();
        }

        if (forwardCookies)
            CookieTools.addResponseCookies( connection, resp, req.getServerName(),
                                            req.getContextPath() + req.getServletPath(),
                                            req.isSecure(), getServletContext());
        forwardResponseHeaders( connection, resp);

        String type = connection.getContentType();
        if (!type.startsWith( "text/html"))
            tunnel( connection, req, resp);
        else
            rewrite( connection, req, resp, allowCookieForwarding, allowFormDataForwarding);
    }

    protected void tunnel( URLConnection connection, HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, IOException {
        byte[] buf = new byte[1024];
        
        if (connection.getContentType() != null)
            resp.setContentType( connection.getContentType());
        if (connection.getContentLength() > 0)
            resp.setContentLength( connection.getContentLength());
        InputStream in = connection.getInputStream();
        OutputStream out = resp.getOutputStream();
        try {
            int len = in.read( buf);
            while (len != -1) {
                out.write( buf, 0, len);
                len = in.read( buf);
            }
        } finally {
            in.close();
        }
    }

    protected void rewrite( URLConnection connection, HttpServletRequest req, HttpServletResponse resp,
                            boolean allowCookieForwarding, boolean allowFormDataForwarding) 
        throws ServletException, IOException {
        InputStreamReader in = CharacterEncodingDetector.getReader
            ( new BufferedInputStream( connection.getInputStream()), connection.getContentEncoding());
        try {
            resp.setContentType( "text/html; charset=" + in.getEncoding());

            annotator.annotate( in, resp.getWriter(), new JGlossURLRewriter
                                ( req.getContextPath() + req.getServletPath(),
                                  connection.getURL(), allowedProtocols,
                                  allowCookieForwarding, allowFormDataForwarding));
        } finally {
            in.close();
        }
    }

    protected void forwardRequestHeaders( URLConnection connection, HttpServletRequest req) {
    }

    protected void forwardResponseHeaders( URLConnection connection, HttpServletResponse resp) {
    }

    protected List split( String s, char c) {
        List out = new LinkedList();

        int from = 0;
        int to = s.indexOf( c);
        if (to == -1)
            to = s.length();
        while (from < to) {
            out.add( s.substring( from, to).trim());

            from = to + 1;
            to = s.indexOf( c, from);
            if (to == -1)
                to = s.length();
        }

        return out;
    }

} // class JGlossServlet
