/*
 * Written by Michael Koch <tensberg@gmx.net>
 *
 * This file is public domain and comes with no warranties.
 * $Id$
 */

/*
 * Generic functions for working with JGloss-WWW URLs.
 */

function hexToBin( hex) {
    var hi = hex.charCodeAt( 0);
    hi -= (hi >= 97) ? 87 : 48;
    var lo = hex.charCodeAt( 1);
    lo -= (lo >= 97) ? 87 : 48;
    return hi*16 + lo;
}

function binToHex( bin) {
    var hi = Math.floor( bin/16);
    hi += (hi >= 10) ? 87 : 48;
    var lo = bin%16;
    lo += (lo >= 10) ? 87 : 48;
    return String.fromCharCode( hi, lo);
}

function escapeURL( url) {
    var i;
    for ( i=url.length-1; i>=0; i--) {
        var escape = true;
        var c = url.charAt( i);
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
            url = url.substring( 0, i) +  "_" + binToHex( url.charCodeAt( i)) +
                url.substring( i+1, url.length);
    }

    return url;
}

function unescapeURL( url) {
    var i;
    for ( i=url.length-3; i>=0; i--) {
        if (url.charAt( i) == '_') {
            url = url.substring( 0, i) + 
                String.fromCharCode( hexToBin( url.substring( i+1, i+3))) +
                url.substring( i+3, url.length);
        }
    }

    return url;
}

/**
 * Convert a URL pointing to a document to a URL which forwards the
 * document through a JGloss-WWW servlet.
 *
 * @param url An URL.
 * @param servleturl URL pointing to the location of the JGloss-WWW servlet.
 * @param forwardCookies true if cookies should be forwarded by the servlet.
 * @param forwardFormData true if form data should be forwarded by the servlet.
 * @return The tranformed URL.
 */
function toJGlossURL( url, servleturl, forwardCookies, forwardFormData) {
    var out = servleturl;
    if (out.charAt( out.length-1) != '/')
        out += "/";
    
    out += forwardCookies ? "1" : "0";
    out += forwardFormData ? "1/" : "0/";

    return out + escapeURL( url);
}

/**
 * Converts a URL which forwards a document through a JGloss-WWW servlet to the original
 * URL.
 */
function toBaseURL( url) {
    var i = url.lastIndexOf( '/');
    
    if (i != -1)
        return unescapeURL( url.substring( i+1, url.length));
    return null;
}

/*
 * Functions specific to the JGloss-WWW index page.
 */
function baseToJGloss() {
    var servletURL = document.URL.substring( 0, document.URL.lastIndexOf( '/')+1)
        + "jgloss-www";
    
    document.urlconverter.jglossurl.value = toJGlossURL
        ( document.urlconverter.baseurl.value, servletURL,
          document.urlconverter.forwardcookies.checked, document.urlconverter.forwardformdata.checked);
}

function jglossToBase() {
    var url = document.urlconverter.jglossurl.value;
    document.urlconverter.baseurl.value = toBaseURL
        ( document.urlconverter.jglossurl.value);
    var i = url.lastIndexOf( '/');
    if (i > 2) {
        document.urlconverter.forwardcookies.checked = (url.charAt( i-2) == '1');
        document.urlconverter.forwardformdata.checked = (url.charAt( i-1) == '1');
    }
}
