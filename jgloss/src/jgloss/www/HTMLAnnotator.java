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
import java.net.*;
import java.util.*;

public class HTMLAnnotator {
    /**
     * Path to the script fragment resource which will be embedded in the
     * HEAD part of the HTML file.
     */
    private final static String SCRIPT_RESOURCE = "/data/HTMLAnnotator";

    private Parser parser;
    private String script;

    public HTMLAnnotator( Parser parser) throws IOException {
        this.parser = parser;

        Reader resource = new InputStreamReader
            ( HTMLAnnotator.class.getResourceAsStream( HTMLAnnotator.SCRIPT_RESOURCE), "UTF-8");
        char[] buf = new char[512];
        StringBuffer scriptbuf = new StringBuffer();
        int r;
        while ((r=resource.read( buf)) != -1) {
            scriptbuf.append( buf, 0, r);
        }
        resource.close();
        script = scriptbuf.toString();
    }

    public void annotate( Reader in, Writer out, URLRewriter rewriter) throws IOException {
        in = new BufferedReader( in);
        
        StringBuffer text = new StringBuffer();
        text.ensureCapacity( 4096);

        boolean scriptWritten = false;
        boolean inBody = false;
        boolean inTag = false;
        boolean inString = false;
        char quote = '\0';
        int commentchar = 0;
        boolean inComment = false;

        int i = in.read();
        while (i != -1) {
            char c = (char) i;
            
            if (inTag) {
                text.append( c);
                switch (commentchar) {
                case 1:
                    if (c == '!')
                        commentchar++;
                    else
                        commentchar = 0;
                    break;
                case 2:
                    if (c == '-')
                        commentchar++;
                    else
                        commentchar = 0;
                    break;
                case 3:
                    if (c == '-')
                        inComment = true;
                    commentchar = 0;
                    break;
                }

                if (c=='>' && !inString && 
                    (!inComment || text.substring( text.length()-3).equals( "-->"))) {
                    inTag = false;
                    inComment = false;

                    // handle special tags
                    String tag = getTagName( text).toLowerCase();
                    //out.write( "<!-- tag " + tag + " -->");
                    if ((tag.equals( "/head") || 
                        tag.equals( "body")) && !scriptWritten) {
                        out.write( script);
                        scriptWritten = true;
                    }
                    if (tag.equals( "body"))
                        inBody = true;
                    else if (tag.equals( "/html"))
                        inBody = false;
                    else if (tag.equals( "/body")) {
                        inBody = false;
                        // Konqueror 2.1 needs an inline style definition for the element.style
                        // attribute to work. Stupid Konqueror.
                        out.write( "\n<div id=\"popup\" class=\"popup\" style=\"position: absolute;\">" + 
                                   "<pre id=\"annotation\"></pre></div>\n");
                    }
                    rewriteURL( tag, text, rewriter);

                    out.write( text.toString());
                    text.delete( 0, text.length());
                }
                if (!inString && (c=='"' || c=='\'')) {
                    inString = true;
                    quote = c;
                } else if (inString && c==quote) {
                    inString = false;
                }
            }
            else {
                if (c=='<' || c=='\n') {
                    if (inBody) {
                        annotateText( out, text);
                    }
                    out.write( text.toString());
                    text.delete( 0, text.length());
                    if (c=='<') {
                        inTag = true;
                        commentchar = 1;
                    }
                }
                text.append( c);
            }

            i = in.read();
        }
        // write remainder
        if (inBody && !inTag)
            annotateText( out, text);
        out.write( text.toString());
    }

    protected StringBuffer annotateText( Writer out, StringBuffer text) throws IOException {
        if (text.length() == 0)
            return text;

        char[] chars = new char[text.length()];
        text.getChars( 0, text.length(), chars, 0);

        try {
            List annotations = parser.parse( chars);
            
            // The annotations will have to be added last to first, because changing text
            // changes the start offsets returned by the parser.
            // All annotations with a start offset in [start,start+length[ of the first
            // annotation seen will form the text for one annotated word.
            List annotext = new LinkedList();
            List wordannos = new ArrayList( 10);
            int start = -1;
            int length = 0;
            // compile the annotation texts for annotated words
            for ( Iterator i=annotations.iterator(); i.hasNext(); ) {
                Parser.TextAnnotation a = (Parser.TextAnnotation) i.next();
                if (a.getStart()>=start+length) {
                    // new annotated word
                    String anno = getAnnotationText( wordannos);
                    if (anno.length() > 0) {
                        annotext.add( new Integer( start));
                        annotext.add( new Integer( length));
                        annotext.add( anno);
                    }
                    wordannos.clear();
                    start = a.getStart();
                    length = a.getLength();
                }
                wordannos.add( a);
            }
            // add the remaining annotations
            String anno = getAnnotationText( wordannos);
            if (anno.length() > 0) {
                annotext.add( new Integer( start));
                annotext.add( new Integer( length));
                annotext.add( anno);
            }
            
            // insert the annotation text
            for ( ListIterator i=annotext.listIterator( annotext.size()); i.hasPrevious(); ) {
                anno = (String) i.previous();
                length = ((Integer) i.previous()).intValue();
                start = ((Integer) i.previous()).intValue();
                
                text.insert( start+length, "</span>");
                text.insert( start, "<span class=\"an\" onMouseOver=\"sp(this,&quot;" + anno +
                             "&quot;)\" onMouseOut=\"hp(this)\">");
            }
        } catch (SearchException ex) {}

        return text;
    }

    protected String getAnnotationText( List annotations) {
        StringBuffer text = new StringBuffer();
        String lastdictionary = "";
        String lastword = "";
        String lasttranslation = "";

        for ( Iterator i=annotations.iterator(); i.hasNext(); ) {
            Parser.TextAnnotation a = (Parser.TextAnnotation) i.next();

            String dictionary = null;
            if (a instanceof Reading)
                dictionary = ((Reading) a).getWordReadingPair().getDictionary().getName();
            else if (a instanceof Translation)
                dictionary = ((Translation) a).getDictionaryEntry().getDictionary().getName();
            if (!dictionary.equals( lastdictionary)) {
                text.append( dictionary + ":\\n");
                lastdictionary = dictionary;
                lastword = "";
                lasttranslation = "";
            }

            String translation = "";
            if (a instanceof Translation) {
                StringBuffer t = new StringBuffer();
                String[] translations = ((Translation) a).getDictionaryEntry().getTranslations();
                for ( int j=0; j<translations.length; j++) {
                    t.append( "    ");
                    t.append( translations[j]);
                    t.append( "\\n");
                }
                translation = t.toString();
            }

            if (a instanceof AbstractAnnotation) {
                AbstractAnnotation aa = (AbstractAnnotation) a;
                String word = aa.getWord();
                String reading = aa.getReading();
                if (word.equals( lastword) && translation.equals( lasttranslation)) {
                    // merge two word entries if reading and translation are equal
                    // insert new reading
                    int pos = text.length() - lasttranslation.length() - 3;
                    if (reading != null) {
                        if (text.charAt( pos) != ']')
                                // last word had no reading
                                text.insert( pos, " []");
                        else {
                            text.insert( pos, '\u3001');
                            pos++;
                        }
                        text.insert( pos, reading);
                    }
                }
                else {
                    text.append( "  " + word);
                    if (reading != null)
                        text.append( " [" + reading + "]");
                    text.append( "\\n" + translation);
                    lastword = word;
                    lasttranslation = translation;
                }
            }
        }

        // escape special characters
        for ( int i=text.length()-1; i>=0; i--) {
            switch (text.charAt( i)) {
            case '"':
                text.replace( i, i+1, "\\&quot;");
                break;
            case '&':
                text.insert( i+1, "amp;");
                break;
            }
        }

        return text.toString();
    }

    protected String getTagName( StringBuffer text) {
        int end = 2;
        while (end<text.length()-1 && text.charAt( end)>0x20)
            end++;
        
        return text.substring( 1, end);
    }

    protected StringBuffer rewriteURL( String name, StringBuffer tag, URLRewriter rewriter) {
        String target = null;
        
        if (name.equals( "a"))
            target = "href";
        else if (name.equals( "img"))
            target = "src";
        else if (name.equals( "form"))
            target = "action";

        if (target != null) {
            String ts = tag.toString().toLowerCase();
            int start = ts.indexOf( target);
            if (start != -1) {
                start += target.length();
                // tag is guaranteed to end with a '>'
                while (ts.charAt( start) == ' ')
                    start++;
                if (ts.charAt( start) == '=') {
                    int eq = start;
                    start++;
                    while (ts.charAt( start) <= 0x20)
                        start++;
                    char quote = ts.charAt( start);
                    if (quote=='"' || quote=='\'')
                        start++;
                    else
                        quote = '\0';
                    int end = start;
                    while (end<ts.length()-1 && 
                           ts.charAt( end)!=quote &&
                           ts.charAt( end)>0x20)
                        end++;
                    
                    try {
                        if (ts.charAt( end) == quote)
                            tag.deleteCharAt( end);
                        if (quote == '\0')
                            quote = '"';
                        tag.replace( eq+1, end, quote + 
                                     rewriter.rewrite( ts.substring( start, end), name) + quote);
                    } catch (MalformedURLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        return tag;
    }
} // class HTMLAnnotator
