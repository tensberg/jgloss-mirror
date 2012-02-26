/*
 * Copyright (C) 2001-2004 Michael Koch (tensberg@gmx.net)
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

// WARNING .. this is unfinished code, it didn't compile in the first place !!! (truell)

package jgloss.www;
import jgloss.parser.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jgloss.dictionary.SearchException;
import jgloss.parser.Parser;

/**
 * Look up words in a Japanese HTML page and add a tag with the dictionary entries.
 * A CSS Style Sheet and JavaScript fragment will be added which displays the entries
 * when the mouse is over a word.
 */
public class HTMLAnnotator {
    /**
     * Path to the script fragment resource which will be embedded in the
     * HEAD part of the HTML file.
     */
    private final static String SCRIPT_RESOURCE = "/data/HTMLAnnotator";

    /**
     * Parser used for the document.
     */
    private Parser parser;
    /**
     * JavaScript segment inserted in the generated document.
     */
    private static String script;
    /**
     * HTML tag ID used for the next annotation inserted in the document.
     */
    private int antid;
    /**
     * Map from previously generated annotation texts to their corresponding ids.
     */
    private Map generatedAnnotations;

    /**
     * Constructs an annotator which uses the given parser and the default 
     * Style Sheet/JavaScript fragment.
     *
     * @param parser Parser used for parsing text in the page.
     * @exception IOException if the default script could not be read.
     */
    public HTMLAnnotator( Parser parser) throws IOException {
        this( parser, null);

        generatedAnnotations = new HashMap();

        // read the default script
        if (script == null) {
            synchronized (this) {
                Reader resource = new InputStreamReader
                    ( HTMLAnnotator.class.getResourceAsStream( HTMLAnnotator.SCRIPT_RESOURCE), "UTF-8");
                char[] buf = new char[4096];
                StringBuilder scriptbuf = new StringBuilder();
                int r;
                while ((r=resource.read( buf)) != -1) {
                    scriptbuf.append( buf, 0, r);
                }

                resource.close();
                script = scriptbuf.toString();
            }
        }
    }

    /**
     * Constructs an annotator which uses the given parser and Style Sheet/JavaScript.
     *
     * @param parser Parser used for parsing text in the page.
     * @param script Text which will be embedded in the resulting page, either directly before
     *               the &lt;/head&gt; tag, or if none is encountered before the &lt;body&gt; tag.
     */
    public HTMLAnnotator( Parser parser, String script) {
        this.parser = parser;
        HTMLAnnotator.script = script;
    }

    /**
     * Reads an HTML document from <CODE>in</CODE>, annotates Japanese words with dictionary lookup results
     * and writes the resulting HTML page to <CODE>out</CODE>. 
     */
    public void annotate( String docBase, Reader in, Writer out, 
                          URLRewriter rewriter) throws IOException {
        rewriter.setDocumentBase( docBase);
        in = new BufferedReader( in);
        
        StringBuilder text = new StringBuilder();
        text.ensureCapacity( 4096);

        boolean scriptWritten = false;
        boolean inBody = false;
        boolean inTag = false;
        boolean inForm = false;
        char quote = '\0';
        int commentchar = 0;
        boolean inComment = false;
        boolean expectQuote = false;

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

                if (c=='>' &&
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
                    else if (tag.equals( "form"))
                        inForm = true;
                    else if (tag.equals( "/html"))
                        inBody = false;
                    else if (tag.equals( "/body")) {
                        inBody = false;
                        // Konqueror 2.1 needs an inline style definition for the element.style
                        // attribute to work. Stupid Konqueror.
                        out.write( "\n<div id=\"popup\" class=\"popup\" style=\"position: absolute;\">" + 
                                   "<pre id=\"annotation\"></pre></div>\n");
                    }
                    else if (tag.equals( "/form"))
                        inForm = false;
                    rewriteURL( tag, text, rewriter);

                    out.write( text.toString());
                    text.delete( 0, text.length());
                }
            }
            else {
                if (c=='<' || c=='\n') {
                    if (inBody && !inForm) {
                        annotateText( out, text);
                    }
                    else
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

    /**
     * Annotates the text with dictionary lookup results and writes it to <CODE>out</CODE>
     */
    protected void annotateText( Writer out, StringBuilder text)
        throws IOException {
        if (text.length() == 0)
            return;

        char[] chars = new char[text.length()];
        text.getChars( 0, text.length(), chars, 0);

        try {
            List annotations = parser.parse( chars, 0, chars.length);
            StringBuilder anno = new StringBuilder( 200);
            int start = 0; // index of first character of annotated word
            int end = 0; // index of first character after annotated word
            String[] prevannotation = new String[3];

            for ( Iterator i=annotations.iterator(); i.hasNext(); ) {
                TextAnnotation a = (TextAnnotation) i.next();
                if (a.getStart() >= end) {
                    // new annotated word; write the previous annotated word
                    if (anno.length() > 0) { // == 0 for first annotation in list
                        out.write( generateAnnotatedWord( anno.toString(), chars, start, end-start));
                        anno.delete( 0, anno.length());
                    }
                        
                    // write unannotated text
                    if (a.getStart() > end)
                        out.write( chars, end, a.getStart()-end);

                    start = a.getStart();
                    end = start + a.getLength();
                    prevannotation[0] = "";
                    prevannotation[1] = "";
                    prevannotation[2] = "";
                }
                addAnnotationText( anno, a, prevannotation);
            }
            // write the last annotation
            if (anno.length() > 0) // == 0 for first annotation in list
                out.write( generateAnnotatedWord( anno.toString(), chars, start, end-start));

            // write the remaining text
            if (end < chars.length)
                out.write( chars, end, chars.length-end);

        } catch (SearchException ex) {
            out.write( "<!-- JGloss parser error: " + ex.getMessage() + " -->)");
        }
    }

    /**
     * Appends the text for an annotation.
     *
     * @param intext Text to which the annotation is appended.
     * @param a The annotation to append.
     */
    protected StringBuilder addAnnotationText( StringBuilder text, TextAnnotation a,
                                              String[] prevannotation) {
        String lastdictionary = prevannotation[0];
        String lastword = prevannotation[1];
        String lasttranslation = prevannotation[2];

        // Some browsers don't interpret a \n in a JavaScript parameter string as a return escape,
        // probably because the \ is replaced by the Yen symbol in some Japanese character encodings.
        // I use the custom escape _R instead of \n and replace it at display time using JavaScript

        /*String dictionary = null;
        if (a instanceof Reading)
            dictionary = ((Reading) a).getWordReadingPair().getDictionary().getName();
        else if (a instanceof Translation)
            dictionary = ((Translation) a).getDictionaryEntry().getDictionary().getName();
        if (!dictionary.equals( lastdictionary)) {
            text.append( dictionary + ":_R");
            lastdictionary = dictionary;
            lastword = "";
            lasttranslation = "";
        }

        String translation = "";
        if (a instanceof Translation) {
            StringBuilder t = new StringBuilder();
            for ( Iterator j=((Translation) a).getDictionaryEntry().getTranslations().iterator();
                  j.hasNext(); ) {
                t.append( "    ");
                t.append( j.next().toString());
                t.append( "_R");
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
                if (pos < 0)
                    System.out.println( text + "\n" + lasttranslation);
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
                text.append( "_R" + translation);
                lastword = word;
                lasttranslation = translation;
            }
        }*/
    
        // escape special characters
        for ( int i=text.length()-2; i>=0; i--) {
            switch (text.charAt( i)) {
            case '"':
                text.replace( i, i+1, "\\\"");
                break;
            case '&':
                break;
            }
        }

        prevannotation[0] = lastdictionary;
        prevannotation[1] = lastword;
        prevannotation[2] = lasttranslation;

        return text;
    }

    protected String getTagName( StringBuilder text) {
        int end = 2;
        while (end<text.length()-1 && text.charAt( end)>0x20)
            end++;
        
        return text.substring( 1, end);
    }

    /**
     * Test if a tag contains an URL attribute and replaces it with a URL generated by the
     * URLRewriter. Currently supported are:
     * <table><tr align="center"><th>Tag</th><th>Attribute</th></tr>
     * <tr align="center"><td><CODE>a</CODE></td><td><CODE>href</CODE></td></tr>
     * <tr align="center"><td><CODE>area</CODE></td><td><CODE>href</CODE></td></tr>
     * <tr align="center"><td><CODE>base</CODE></td><td><CODE>href</CODE></td></tr>
     * <tr align="center"><td><CODE>img</CODE></td><td><CODE>src</CODE></td></tr>
     * <tr align="center"><td><CODE>frame</CODE></td><td><CODE>src</CODE></td></tr>
     * <tr align="center"><td><CODE>form</CODE></td><td><CODE>action</CODE></td></tr>
     * </table>
     * Additionally, this method handles <CODE>BASE</CODE> tags by changing the document
     * base of the URL rewriter to the URL of the HREF attribute.
     *
     * @param name Name of the tag.
     * @param tag Complete tag, including the leading &lt; and trailing &gt;.
     * @param rewriter Used to change the URL.
     */
    protected StringBuilder rewriteURL( String name, StringBuilder tag, URLRewriter rewriter) {
        String target = null;
        
        if (name.equals( "a") || name.equals( "area") || name.equals( "base"))
            target = "href";
        else if (name.equals( "img") || name.equals( "frame"))
            target = "src";
        else if (name.equals( "form"))
            target = "action";

        if (target != null) {
            String ts = tag.toString();
            int start = ts.toLowerCase().indexOf( target);
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

                        if (name.equals( "base")) {
                            // change the document base of the URL rewriter to the value of
                            // the HREF attribute of the BASE tag.
                            rewriter.setDocumentBase( ts.substring( start, end));
                        }

                        // replace the URL in the tag attribute with the rewritten URL.
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

    /**
     * Generate a HTML fragment which wraps the annotated word with a tag which triggers the
     * display of the annotation in the web browser.
     *
     * @param annotation Annotation text.
     * @param text Text in which the annotated word appears.
     * @param wordStart Start offset of the annotated word in the text.
     * @param wordLength Length of the word.
     */
    protected String generateAnnotatedWord( String annotation, char[] text, int wordStart,
                                            int wordLength) {
        StringBuilder out = new StringBuilder( annotation.length() + wordLength + 20);
        // look up ID of this annotation text if already generated
        Integer id = null;
        id = (Integer) generatedAnnotations.get( annotation);
        if (id == null) {
            // new annotation text, insert annotation in document
            id = new Integer( antid);
            out.append( "<script language=\"JavaScript\">ra(");
            out.append( antid);
            out.append( ", \"");
            out.append( annotation);
            out.append( "\");</script>");
            generatedAnnotations.put( annotation, id);
            antid++;
        }
        out.append( "<span class=\"an\" onMouseOver=\"sp(this,&quot;");
        out.append( id.toString());
        out.append( "&quot;)\" onMouseOut=\"hp(this)\">");
        out.append( text, wordStart, wordLength);
        out.append( "</span>");

        return out.toString();
    }
} // class HTMLAnnotator
