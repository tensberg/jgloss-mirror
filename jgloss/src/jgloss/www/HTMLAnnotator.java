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
    private Parser parser;

    public HTMLAnnotator( Parser parser) {
        this.parser = parser;
    }

    public void annotate( Reader in, Writer out) throws IOException {
        in = new BufferedReader( in);
        
        StringBuffer text = new StringBuffer();
        text.ensureCapacity( 4096);

        boolean inBody = false;
        boolean inTag = false;
        
        int i = in.read();
        while (i != -1) {
            char c = (char) i;
            
            if (inTag) {
                text.append( c);
                if (c == '>') {
                    inTag = false;
                    out.write( text.toString());
                    text.delete( 0, text.length());
                }
            }
            else {
                if (c=='<' || c=='\n') {
                    if (inBody)
                        annotate( text);
                    out.write( text.toString());
                    text.delete( 0, text.length());
                    if (c=='<')
                        inTag = true;
                }
                text.append( c);
            }

            i = in.read();
        }
        // write remainder
        out.write( text.toString());
    }

    protected StringBuffer annotate( StringBuffer text) {
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
            for ( ListIterator i=wordannos.listIterator(); i.hasPrevious(); ) {
                anno = (String) i.previous();
                length = ((Integer) i.previous()).intValue();
                start = ((Integer) i.previous()).intValue();
                
                text.insert( start+length, "</div>");
                text.insert( start, "<div class=\"an\" onMouseOver=\"sp(this,&quot;" + anno +
                             "&quot;)\" onMouseOut=\"hp()\">");
            }
        } catch (SearchException ex) {}

        return text;
    }

    protected String getAnnotationText( List annotations) {
        // List of annotations from a single dictionary, in the order in which they appear in
        // the annotations list
        List dictionaries = new LinkedList();
        // Map with StringBuffers for each distinct Dictionary
        Map dicmap = new TreeMap();

        for ( Iterator i=annotations.iterator(); i.hasNext(); ) {
            Parser.TextAnnotation a = (Parser.TextAnnotation) i.next();

            String d = null;
            if (a instanceof Reading)
                d = ((Reading) a).getWordReadingPair().getDictionary().getName();
            else if (a instanceof Translation)
                d = ((Translation) a).getDictionaryEntry().getDictionary().getName();

            StringBuffer text = (StringBuffer) dicmap.get( d);
            if (text == null) {
                text = new StringBuffer( d);
                text.append( ":\n");
                dicmap.put( d, text);
                dictionaries.add( text);
            }
            
            int insertAt = text.length();
            if (a instanceof Translation) {
                StringBuffer t = new StringBuffer();
                String[] translations = ((Translation) a).getDictionaryEntry().getTranslations();
                for ( int j=0; j<translations.length; j++) {
                    t.append( "    ");
                    t.append( translations[j]);
                    t.append( "\\n");
                }
                String translation = t.toString();
                if (text.length()>translation.length() &&
                    text.substring( text.length()-translation.length()).equals( translation)) {
                    // The new word has the same translations as the last word.
                    // Combine the two entries.
                    insertAt = text.length()-translation.length();
                }
                else
                    text.append( translation);
            }
            if (a instanceof AbstractAnnotation) {
                AbstractAnnotation aa = (AbstractAnnotation) a;
                if (aa.getReading() != null && !aa.getReading().equals( aa.getWord()))
                    text.insert( insertAt, " [" + aa.getReading() + "]");
                text.insert( insertAt, "  " + aa.getWord());
            }
        }

        StringBuffer out = new StringBuffer();
        for ( Iterator i=dictionaries.iterator(); i.hasNext(); )
            out.append( i.next().toString());

        // escape special characters
        for ( int i=out.length(); i>=0; i--) {
            switch (out.charAt( i)) {
            case '"':
                out.replace( i, i+1, "\\&quot;");
                break;
            case '&':
                out.insert( i+1, "amp;");
                break;
            }
        }

        return out.toString();
    }
} // class HTMLAnnotator
