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
import java.util.zip.*;
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
    /**
     * Path to the messages ressource.
     */
    public final static String MESSAGES = "resources/messages-www";

    /**
     * Initialization parameter name.
     */
    public final static String DICTIONARIES = "dictionaries";
    /**
     * Initialization parameter name.
     */
    public final static String ALLOWED_PROTOCOLS = "allowed_protocols";
    /**
     * Initialization parameter name.
     */
    public final static String SECURE_ALLOWED_PROTOCOLS = "secure_allowed_protocols";
    /**
     * Initialization parameter name.
     */
    public final static String REWRITTEN_TYPES = "rewritten_types";
    /**
     * Initialization parameter name.
     */
    public final static String ENABLE_COOKIE_FORWARDING = "enable_cookie_forwarding";
    /**
     * Initialization parameter name.
     */
    public final static String ENABLE_SECURE_INSECURE_COOKIE_FORWARDING =
        "enable_secure-to-insecure_cookie_forwarding";
    /**
     * Initialization parameter name.
     */
    public final static String ENABLE_FORM_DATA_FORWARDING = "enable_form_data_forwarding";
    /**
     * Initialization parameter name.
     */
    public final static String ENABLE_SECURE_INSECURE_FORM_DATA_FORWARDING =
        "enable_secure-to-insecure_form_data_forwarding";
    /**
     * Initialization parameter name.
     */
    public final static String RESPONSE_BUFFER_SIZE = "response-buffer-size";
    /**
     * Initialization parameter name.
     */
    public final static String ENABLE_COMPRESSION = "enable_compression";

    /**
     * CGI parameter name.
     */
    public final static String REMOTE_URL = "jgurl";
    /**
     * CGI parameter name.
     */
    public final static String ALLOW_COOKIE_FORWARDING = "jgforwardcookies";
    /**
     * CGI parameter name.
     */
    public final static String ALLOW_FORM_DATA_FORWARDING = "jgforwardforms";

    private jgloss.dictionary.Dictionary[] dictionaries;
    /**
     * Parser used to annotate text.
     */
    private Parser parser;
    /**
     * Used to rewrite web pages.
     */
    private HTMLAnnotator annotator;
    /**
     * Set of protocols allowed in remote urls.
     */
    private Set allowedProtocols;
    /**
     * Set of protocols allowed in remote urls used when the Client-Servlet connection is
     * secure.
     */
    private Set secureAllowedProtocols;
    /**
     * List of MIME types which will be annotated.
     */
    private String[] rewrittenContentTypes;
    /**
     * Set of request and response header keys which will not be forwarded.
     */
    private Set noForwardHeaders;

    /**
     * Flag if cookie forwarding should be enabled globally. If enabled, it can still
     * be disabled on a per-request basis.
     */
    private boolean enableCookieForwarding;
    /**
     * Flag if cookies should be forwarded from a secure to an insecure connection.
     * Regardless of the setting of this flag, a cookie with the 'secure' attribute set will
     * never be forwarded over an insecure connection.
     */
    private boolean enableCookieSecureInsecureForwarding;
    /**
     * Flag if form data forwarding should be enabled globally. If enabled, it can still
     * be disabled on a per-request basis.
     */
    private boolean enableFormDataForwarding;
    /**
     * Flag if form data should be forwarded from a secure to an insecure connection.
     */
    private boolean enableFormDataSecureInsecureForwarding;
    /**
     * Size of the write buffer of HttpServletResponses in bytes, or -1 to use
     * the default size.
     */
    private int responseBufferSize;
    /**
     * Flag if compression content encodings should be enabled.
     */
    private boolean enableCompression;

    public JGlossServlet() {}

    /**
     * Reads the initialization parameters and loads the dictionaries.
     */
    public void init( ServletConfig config) throws ServletException {
        super.init( config);

        HttpURLConnection.setFollowRedirects( false);

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
        // write log entry
        p = "";
        for ( Iterator i=allowedProtocols.iterator(); i.hasNext(); ) {
            if (p.length() > 0)
                p += ", ";
            p += (String) i.next();
        }
        if (p.length() == 0)
            p = "none";
        getServletContext().log( "allowed protocols: " + p);

        // read secure allowed protocols
        secureAllowedProtocols = new HashSet( 5);
        p = config.getInitParameter( SECURE_ALLOWED_PROTOCOLS);
        if (p != null)
            secureAllowedProtocols.addAll( split( p, ':'));
        // write log entry
        p = "";
        for ( Iterator i=secureAllowedProtocols.iterator(); i.hasNext(); ) {
            if (p.length() > 0)
                p += ", ";
            p += (String) i.next();
        }
        if (p.length() == 0)
            p = "none";
        getServletContext().log( "secure allowed protocols: " + p);

        // Set of all headers which are not forwarded in forwardRequestHeaders or
        // forwardResponseHeaders. All unknown headers must be forwarded as per rfc2616.
        noForwardHeaders = new HashSet( 20);
        // general header fields
        noForwardHeaders.add( "connection"); // the content of this field is currently not handled
        noForwardHeaders.add( "upgrade");
        noForwardHeaders.add( "via");
        noForwardHeaders.add( "transfer-encoding");
        // request header fields
        // range and if-range are not supported because rewriting changes the size of
        // files by an undetermined amount.
        noForwardHeaders.add( "range");
        noForwardHeaders.add( "if-range");
        noForwardHeaders.add( "accept-encoding");
        noForwardHeaders.add( "authorization");
        noForwardHeaders.add( "referer"); // the referer header is treated specially
        // response header fields
        noForwardHeaders.add( "accept-ranges");
        // entity header fields
        noForwardHeaders.add( "content-encoding");
        noForwardHeaders.add( "content-length");
        noForwardHeaders.add( "content-md5");
        noForwardHeaders.add( "content-type");

        // headers not in rfc1616
        noForwardHeaders.add( "set-cookie");
        noForwardHeaders.add( "set-cookie2");
        noForwardHeaders.add( "cookie");
        noForwardHeaders.add( "cookie2");
        noForwardHeaders.add( "keep-alive");

        // read supported types
        rewrittenContentTypes = new String[0];
        p = config.getInitParameter( REWRITTEN_TYPES);
        if (p != null) {
            rewrittenContentTypes = (String[]) split( p, ':').toArray( rewrittenContentTypes);
        }
        if (rewrittenContentTypes.length == 0) {
            rewrittenContentTypes = new String[1];
            rewrittenContentTypes[0] = "text/html";
        }
        // write log entry
        p = "";
        for ( int i=0; i<rewrittenContentTypes.length; i++) {
            if (p.length() > 0)
                p += ", ";
            p += rewrittenContentTypes[i];
        }
        if (p.length() == 0)
            p = "none";
        getServletContext().log( "rewritten content types: " + p);

        p = config.getInitParameter( ENABLE_COOKIE_FORWARDING);
        enableCookieForwarding = "true".equals( p);
        getServletContext().log( "cookie forwarding " + (enableCookieForwarding ? "enabled" : "disabled"));
        p = config.getInitParameter( ENABLE_SECURE_INSECURE_COOKIE_FORWARDING);
        enableCookieSecureInsecureForwarding = "true".equals( p);
        getServletContext().log( "secure-to-insecure cookie forwarding " + 
                                 (enableCookieSecureInsecureForwarding ? "enabled" : "disabled"));
        p = config.getInitParameter( ENABLE_FORM_DATA_FORWARDING);
        enableFormDataForwarding = "true".equals( p);
        getServletContext().log( "form data forwarding " + 
                                 (enableFormDataForwarding ? "enabled" : "disabled"));
        p = config.getInitParameter( ENABLE_SECURE_INSECURE_FORM_DATA_FORWARDING);
        enableFormDataSecureInsecureForwarding = "true".equals( p);
        getServletContext().log( "secure-to-insecure form data forwarding " + 
                                 (enableFormDataSecureInsecureForwarding ? "enabled" : "disabled"));
        try {
            responseBufferSize = Integer.parseInt( config.getInitParameter( RESPONSE_BUFFER_SIZE));
            getServletContext().log( "response buffer size set to " + responseBufferSize + " bytes");
        } catch (Exception ex) { // NullPointerException or NumberFormatException
            responseBufferSize = -1;
            getServletContext().log( "response buffer size not set, using default size");
        }

        enableCompression = "true".equals( config.getInitParameter( ENABLE_COMPRESSION));
        getServletContext().log( "content compression " + (enableCompression ? "enabled" : "disabled"));
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
        if (responseBufferSize > 0)
            resp.setBufferSize( responseBufferSize);

        String pathinfo = req.getPathInfo();
        if (pathinfo == null) {
            // encode CGI parameters as a pathinfo string and send a redirect response
            // to this location.
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
                  new URL( HttpUtils.getRequestURL( req).toString()), null,
                  allowCookieForwarding, allowFormDataForwarding).rewrite( urlstring, true);
            resp.sendRedirect( target);
            return;
        }

        Set connectionAllowedProtocols;
        if (req.isSecure())
            connectionAllowedProtocols = secureAllowedProtocols;
        else
            connectionAllowedProtocols = allowedProtocols;

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
            if (req.isSecure()) {
                if (secureAllowedProtocols.contains( "https"))
                    urlstring = "https://" + urlstring;
            }
            else {
                if (allowedProtocols.contains( "http"))
                    urlstring = "http://" + urlstring;
            }
        }

        URL url;
        try {
            url = new URL( urlstring);
        } catch (MalformedURLException ex) {
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST,
                            MessageFormat.format
                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                              .getString( "error.malformedurl"),
                              new Object[] { urlstring} ));
            return;
        }

        String protocol = url.getProtocol();
        if (!connectionAllowedProtocols.contains( protocol)) {
            resp.sendError( HttpServletResponse.SC_FORBIDDEN,
                            MessageFormat.format
                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                              .getString( "error.protocolnotallowed"),
                              new Object[] { protocol } ));
            // note that due to a bug in tomcat 3.2.3 instead of the error string
            // "<h1>SSL required to access this page</H1>" will be returned to the client
            getServletContext().log( "protocol not allowed accessing " + url.toString());
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

        Cookie[] cookies = req.getCookies();
        boolean followingRedirect = false;
        // detect cycles in redirection
        Set redirects = new HashSet( 50);
        redirects.add( url);
        String acceptEncoding = buildAcceptEncoding( req.getHeader( "accept-encoding"));
        getServletContext().log( "accept-encoding: " + acceptEncoding);
        do {
            JGlossURLRewriter rewriter = new JGlossURLRewriter
                ( req.getContextPath() + req.getServletPath(),
                  url, connectionAllowedProtocols,
                  allowCookieForwarding, allowFormDataForwarding);

            URLConnection connection = url.openConnection();
            if (acceptEncoding != null)
                connection.setRequestProperty( "accept-encoding", acceptEncoding);

            if (forwardFormData && post && connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod( "POST");
                connection.setDoInput( true);
                connection.setDoOutput( true);
            }
            
            forwardRequestHeaders( connection, req);
            if (forwardCookies)
                CookieTools.addRequestCookies( connection, cookies, getServletContext());
            
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

            if (forwardFormData && post && 
                connection instanceof HttpURLConnection) {
                InputStream is = req.getInputStream();
                OutputStream os = connection.getOutputStream();
                byte[] buf = new byte[512];
                int len;
                while ((len=is.read( buf)) != -1)
                    os.write( buf, 0, len);
                is.close();
                os.close();
            }
            
            // test redirect directive
            followingRedirect = false;
            int response = 0;
            if (connection instanceof HttpURLConnection) {
                response = ((HttpURLConnection) connection).getResponseCode();
                getServletContext().log( "response code " + response);
                if ((response==301 || response==302 || response==307) && !post ||
                    response==303) {
                    String location = connection.getHeaderField( "location");
                    if (location != null) try {
                        url = new URL( location);
                        if (!connectionAllowedProtocols.contains( url.getProtocol())) {
                            resp.sendError( HttpServletResponse.SC_FORBIDDEN,
                                            MessageFormat.format
                                            ( ResourceBundle.getBundle( MESSAGES, req.getLocale())
                                              .getString( "error.forwardprotocolnotallowed"),
                                              new Object[] { url.toString(), url.getProtocol() } ));
                            getServletContext().log( "forward protocol not allowed accessing " +
                                                     url.toString());
                        }
                        else if (!redirects.contains( url) &&
                            // While rfc2616 does not set a limit on redirects, I want to
                            // avoid an endless number of redirects by a broken or malicious server.
                            redirects.size() < 30) {
                            followingRedirect = true;
                            redirects.add( url);
                            if (response == 303) // use GET instead of POST when following
                                post = false;
                            getServletContext().log( "following redirect to " + location);
                        }
                    } catch (MalformedURLException ex) {
                        getServletContext().log( "malformed url " + location + "/" + ex.getMessage());
                    }
                }
            }

            if (forwardCookies) {
                Map newCookies = 
                    CookieTools.addResponseCookies( connection, resp, req.getServerName(),
                                                    req.getContextPath() + req.getServletPath(),
                                                    req.isSecure(), getServletContext());
                if (followingRedirect && newCookies.size()>0) {
                    // Add the newly generated cookies to the next redirection.
                    // The new and old cookies have to be merged. For equality test a test
                    // on the cookie name is sufficient because the domain/path/port/name is
                    // encoded in the name.
                    if (cookies != null) {
                        for ( int i=0; i<cookies.length; i++)
                            newCookies.put( cookies[i].getName(), cookies[i]);
                    }
                    cookies = (Cookie[]) newCookies.values().toArray( cookies);
                }
            }
            
            if (!followingRedirect) {
                forwardResponseHeaders( connection, req, resp);

                if (response != 0)
                    resp.setStatus( response);
                if (response == 304) // 304 Not Modified/empty response
                    return;

                String encoding = connection.getContentEncoding();
                getServletContext().log( "Content-Encoding: " + encoding);
                if (encoding != null)
                    resp.setHeader( "Content-Encoding", encoding);

                String type = connection.getContentType();
                boolean supported = false;
                if (type != null) {
                    for ( int i=0; i<rewrittenContentTypes.length; i++)
                        if (type.startsWith( rewrittenContentTypes[i])) {
                            supported = true;
                            break;
                        }
                }   
                getServletContext().log( "content type " + type + " url " +
                                         connection.getURL().toString());
                if (supported) {
                    // If the content encoding cannot be decoded by the servlet,
                    // the content is tunneled to the browser.
                    // Multiple encodings are currently not supported and may lead to wrong
                    // behavior.
                    supported = encoding==null || encoding.endsWith( "gzip") || 
                        encoding.endsWith( "deflate") || encoding.equals( "identity");
                }

                if (supported)
                    rewrite( connection, req, resp, rewriter);
                else
                    tunnel( connection, req, resp);
            }
        } while (followingRedirect);
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
            out.close();
        }
    }

    protected void rewrite( URLConnection connection, HttpServletRequest req, HttpServletResponse resp,
                            URLRewriter rewriter) 
        throws ServletException, IOException {
        InputStream in = new BufferedInputStream( connection.getInputStream());
        // Decode the content.
        // Multiple encodings are currently not supported and may lead to wrong
        // behavior.
        String encoding = connection.getContentEncoding();
        boolean usegzip = false;
        boolean usedeflate = false;
        if (encoding != null) {
            if (encoding.endsWith( "gzip"))
                usegzip = true;
            else if (encoding.endsWith( "deflate"))
                usedeflate = true;
        }
        if (usegzip)
            in = new GZIPInputStream( in);
        else if (usedeflate)
            in = new InflaterInputStream( in);
        
        InputStreamReader reader = CharacterEncodingDetector.getReader( in, null, 5000);
        try {
            resp.setContentType( "text/html; charset=" + reader.getEncoding());

            // re-compress the generated content
            OutputStream out = resp.getOutputStream();
            if (usegzip)
                out = new GZIPOutputStream( out);
            else if (usedeflate)
                out = new DeflaterOutputStream( out);
            Writer writer = new OutputStreamWriter( out, reader.getEncoding());

            annotator.annotate( reader, writer, rewriter);
            writer.close();
        } finally {
            in.close();
        }
    }

    protected void forwardRequestHeaders( URLConnection connection, HttpServletRequest req) {
        String via = req.getHeader( "Via");
        if (via == null)
            via = "";
        else
            via += ", ";
        via += req.getProtocol() + " " +
            req.getServerName() + ":" + req.getServerPort();
        connection.setRequestProperty( "Via", via);

        // HTTP request headers
        if (connection.getURL().getProtocol().toLowerCase().startsWith( "http")) { // http and https
            // Add the referrer, removing the servlet-encoding if neccessary.
            // The referer remains the original URI if a redirect is followed.
            String referer = req.getHeader( "referer");
            if (referer!=null && referer.indexOf( req.getContextPath() + 
                                                  req.getServletPath() + "/")!=-1) {
                Object[] out = JGlossURLRewriter.parseEncodedPath( referer);
                if (out != null) {
                    connection.setRequestProperty( "referer", (String) out[2]);
                    getServletContext().log( "referer: " + (String) out[2]);
                }
            }

            // According to the documentation, some servet containers don't allow getHeaderNames,
            // which returns null in that case.
            for ( Enumeration names=req.getHeaderNames(); names!=null && names.hasMoreElements(); ) {
                String name = (String) names.nextElement();
                if (!noForwardHeaders.contains( name.toLowerCase())) {
                    StringBuffer value = new StringBuffer();
                    for ( Enumeration values=req.getHeaders( name); values.hasMoreElements(); ) {
                        if (value.length() > 0)
                            value.append( ',');
                    value.append( (String) values.nextElement());
                    }
                    connection.setRequestProperty( name, value.toString());
                    getServletContext().log( "request header " + name + ": " + value.toString());
                }
            }
        }
    }

    protected void forwardResponseHeaders( URLConnection connection, HttpServletRequest req,
                                           HttpServletResponse resp) {
        String via = connection.getHeaderField( "Via");
        if (via == null)
            via = "";
        else
            via += ", ";
        via += req.getProtocol() + " " +
        req.getServerName() + ":" + req.getServerPort();
        resp.setHeader( "Via", via);

        // HTTP response headers
        if (connection.getURL().getProtocol().toLowerCase().startsWith( "http")) { // http and https
            int i = 1; // header fields are 1-based
            String name;
            while ((name = connection.getHeaderFieldKey( i)) != null) {
                if (!noForwardHeaders.contains( name.toLowerCase())) {
                    resp.addHeader( name, connection.getHeaderField( i));
                    getServletContext().log( "response header " + name + ": " +
                                             connection.getHeaderField( i));
                }
                i++;
            }
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

    /**
     * Build the Accept-Encoding header sent to the remote server. The accepted encodings are
     * a subset of the encodings understood by the client and the servlet. Currently the "q" attribute
     * is not supported.
     */
    protected String buildAcceptEncoding( String acceptEncoding) {
        if (!enableCompression)
            return "identity";

        if (acceptEncoding == null)
            return null; // don't set the header

        boolean usegzip;
        boolean usedeflate;
        if (acceptEncoding.indexOf( '*') != -1) {
            usegzip = true;
            usedeflate = true;
        }
        else {
            usegzip = (acceptEncoding.indexOf( "gzip") != -1);
            usedeflate = (acceptEncoding.indexOf( "deflate") != -1);
        }

        StringBuffer out = new StringBuffer( 30);
        if (usegzip)
            out.append( "gzip,");
        if (usedeflate)
            out.append( "deflate,");
        out.append( "identity");

        return out.toString();
    }
} // class JGlossServlet
