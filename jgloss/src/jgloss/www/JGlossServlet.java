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

public class JGlossServlet extends HttpServlet {
    public final static String MESSAGES = "resources/messages-www";

    public final static String DICTIONARIES = "dictionaries";
    public final static String ALLOWED_PROTOCOLS = "allowed_protocols";

    public final static String REMOTE_URL = "url";

    private jgloss.dictionary.Dictionary[] dictionaries;
    private Parser parser;
    private Set allowedProtocols;

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
            throw new ServletException( ResourceBundle.getBundle( MESSAGES)
                                        .getString( "error.nodictionary"));
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

        // read allowed protocols
        allowedProtocols = new TreeSet();
        String p = config.getInitParameter( ALLOWED_PROTOCOLS);
        if (p==null || p.length()==0)
            throw new ServletException( ResourceBundle.getBundle( MESSAGES)
                                        .getString( "error.noprotocols"));
        allowedProtocols.addAll( split( p, ':'));
    }

    public void destroy() {
        super.destroy();

        for ( int i=0; i<dictionaries.length; i++)
            dictionaries[i].dispose();
    }

    protected void doGet( HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, IOException {
        String urlstring = req.getParameter( REMOTE_URL);
        if (urlstring == null) {
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST,
                            ResourceBundle.getBundle( MESSAGES, req.getLocale())
                            .getString( "error.nourl"));
            return;
        }

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

        if (!allowedProtocols.contains( url.getProtocol())) {
            resp.sendError( HttpServletResponse.SC_FORBIDDEN,
                            MessageFormat.format
                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                              .getString( "error.protocolnotallowed"),
                              new Object[] { url.getProtocol()} ));
            return;
        }

        URLConnection connection = url.openConnection();
        connection.connect();
        String type = connection.getContentType();
        if (!type.startsWith( "text/html"))
            tunnel( connection, req, resp);
        else
            rewrite( connection, req, resp);
    }

    protected void tunnel( URLConnection connection, HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, IOException {
        byte[] buf = new byte[4096];
        
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

    protected void rewrite( URLConnection connection, HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, IOException {
        InputStreamReader in = CharacterEncodingDetector.getReader
            ( connection.getInputStream(), connection.getContentEncoding());
        try {
            resp.setContentType( "text/html; charset=" + in.getEncoding());
            new HTMLAnnotator( parser).annotate( in, resp.getWriter());
        } finally {
            in.close();
        }
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
