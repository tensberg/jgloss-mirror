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

package jgloss.ui.doc;

import jgloss.*;
import jgloss.dictionary.*;

import java.util.*;

/**
 * Encodes a list of annotations as string suitable for use as a HTML attribute value and vice versa.
 * This creates output which is less complex than that of the java serialization mechanism and
 * is guaranteed (more or less) to be HTML-safe. The reason this class is in package
 * jgloss.ui.doc instead of jgloss.dictionary is that only information important for the JGloss
 * front end is encoded. For example, only the names of the dictionaries are encoded and not
 * the actual implementations used because no lookups will be made in these dictionaries.
 *
 * @author Michael Koch
 * @see jgloss.dictionary.Parser.TextAnnotation
 */
public abstract class TextAnnotationCodec {
    /**
     * The string used to encode the <CODE>null</CODE> value.
     */
    private static final String NULL_STRING = "_NULL_";
    /**
     * Character used to delimit fields in a singe annotation.
     */
    public final static char FIELD_SEPARATOR = '@';
    /**
     * Character used to delimit translations in a DictionaryEntry.
     */
    public final static char TRANSLATION_SEPARATOR = '/';
    /**
     * Character used to delimit the annotations in the list.
     */
    public final static char LIST_SEPARATOR = '|';

    /**
     * Remembers all dictionary names so far and the NullDictionaries created for them.
     */
    private static Map dictionaries = new HashMap();

    /**
     * A dummy dictionary which is only stores the dictionary name. Instances of this
     * class are used when reconstructing the dictionary from an annotation encoded as string.
     *
     * @author Michael Koch
     */
    private static class NullDictionary implements jgloss.dictionary.Dictionary {
        /**
         * Name of the dictionary.
         */
        private String name;

        /**
         * Construct a new dummy dictionary with the given name.
         *
         * @param name The name of this dictionary.
         */
        public NullDictionary( String name) {
            this.name = name;
        }

        /**
         * Does a dictionary search. Since this is a dummy dictionary, it will always return the
         * empty list.
         */
        public java.util.List search( String expression, short mode) throws SearchException {
            return java.util.Collections.EMPTY_LIST;
        }

        /**
         * Returns the name of the dictionary.
         *
         * @return The name of the dictionary.
         */
        public String getName() { return name; }
    }

    /**
     * Used to store annotation types unknown to the JGloss application. This is used to
     * make documents backwards compatible to older versions if new annotation types are
     * introduced. If an unknown annotation type is discovered when decoding a string,
     * it will be stored in an <CODE>UnknownAnnotation</CODE>, ignored by the application,
     * and encoded unchanged when storing the document.
     *
     * @author Michael Koch
     */
    public static class UnknownAnnotation implements Parser.TextAnnotation {
        private int start;
        private int length;
        /**
         * The code of this annotation as found in the encoded string.
         *
         */
        private String code;

        /**
         * Creates a new instance for an unknown annotation class.
         *
         * @param code The code of this annotation as found in the encoded string.
         * @param start Start offset of this annotation in the document.
         * @param length Length of the annotation.
         */
        public UnknownAnnotation( String code, int start, int length) {
            this.code = code;
            this.start = start;
            this.length = length;
        }

        /**
         * Returns the code of this annotation as found in the encoded string.
         *
         * @return The code of this annotation as found in the encoded string.
         */
        public String getCode() {
            return code;
        }

        public int getStart() { return start; }
        public int getLength() { return length; }
    }

    /**
     * Encodes a list of {@link jgloss.dictionary.Parser.TextAnnotation TextAnnotations} as
     * a string suitable for use as an HTML attribute value. The encoding is lossy: only
     * attributes important to the JGloss frontend will be encoded.
     *
     * @param tas List of annotations to encode.
     * @return The list encoded as string.
     */
    public static String encode( List tas) {
        StringBuffer code = new StringBuffer();
        for ( Iterator i=tas.iterator(); i.hasNext(); ) {
            code.append( encode( (Parser.TextAnnotation) i.next()));
            code.append( LIST_SEPARATOR);
        }

        return code.toString();
    }

    /**
     * Encodes a TextAnnotations as
     * a string suitable for use as an HTML attribute value. The encoding is lossy: only
     * attributes important to the JGloss frontend will be encoded.
     *
     * @param ta The annotation to encode.
     * @return The annotation encoded as string.
     */
    public static String encode( Parser.TextAnnotation ta) {
        String code = ta.getClass().getName() + FIELD_SEPARATOR 
            + ta.getStart() + FIELD_SEPARATOR + ta.getLength() + FIELD_SEPARATOR;

        if (ta instanceof Reading)
            code += ((Reading) ta).getReading() + FIELD_SEPARATOR;
        else if (ta instanceof Translation) {
            Translation t = (Translation) ta;
            DictionaryEntry d = t.getDictionaryEntry();
            Conjugation c = t.getConjugation();

            code += d.getWord() + FIELD_SEPARATOR;
            if (d.getReading() == null)
                code += NULL_STRING;
            else
                code += d.getReading();
            code += FIELD_SEPARATOR;

            String[] translations = d.getTranslations();
            if (translations!=null) {
                if (translations.length>0) {
                    code += translations[0];
                    for ( int i=1; i<translations.length; i++)
                        code += TRANSLATION_SEPARATOR + translations[i];
                }
            }
            else
                code += NULL_STRING;
        
            code += FIELD_SEPARATOR + d.getDictionary().getName() + FIELD_SEPARATOR;
            if (c != null) {
                code += c.getConjugatedForm() + FIELD_SEPARATOR +
                    c.getDictionaryForm() + FIELD_SEPARATOR +
                    c.getType();
            }
            code += FIELD_SEPARATOR;
        }
        else if (ta instanceof UnknownAnnotation) {
            code = ((UnknownAnnotation) ta).getCode();
        }
        else
            System.err.println( JGloss.messages.getString( "error.save.unknownannotation",
                                                           new Object[] { ta.getClass().getName() }));

        return quote( code);
    }

    /**
     * Decode a string which contains a list of 
     * {@link jgloss.dictionary.Parser.TextAnnotation TextAnnotations}.
     *
     * @param code The string containing the encoded list.
     * @return The decoded list.
     */
    public static List decodeList( String code) {
        int i=0;
        int j = code.indexOf( LIST_SEPARATOR);
        List out = new LinkedList();
        
        while (j != -1) {
            out.add( decodeAnnotation( code.substring( i, j)));
            i = j + 1;
            j = code.indexOf( LIST_SEPARATOR, i);
        }

        return out;
    }

    /**
     * Decodes a string which contains a <CODE>TextAnnotation</CODE>.
     *
     * @param code The string containing the encoded annotation.
     * @return The decoded annotation.
     */
    public static Parser.TextAnnotation decodeAnnotation( String code) {
        code = unquote( code);

        int i = code.indexOf( FIELD_SEPARATOR);
        int j = code.indexOf( FIELD_SEPARATOR, i+1);
        int k = code.indexOf( FIELD_SEPARATOR, j+1);
        String c = code.substring( 0, i);
        int start = Integer.parseInt( code.substring( i+1, j));
        int length = Integer.parseInt( code.substring( j+1, k));
        i = k + 1;
        if (c.equals( Reading.class.getName())) {
            j = code.indexOf( FIELD_SEPARATOR, i);
            return new Reading( start, length, code.substring( i, j));
        }
        else if (c.equals( Translation.class.getName())) {
            j = code.indexOf( FIELD_SEPARATOR, i);
            String word = code.substring( i, j);
            i = j + 1;

            j = code.indexOf( FIELD_SEPARATOR, i);
            String reading = code.substring( i, j);
            if (reading.equals( NULL_STRING))
                reading = null;
            i = j + 1;

            j = code.indexOf( FIELD_SEPARATOR, i);
            String t = code.substring( i, j);
            i = j + 1;
            String[] translations = null;
            if (!t.equals( NULL_STRING)) {
                List translationList = new ArrayList();
                int l = t.indexOf( TRANSLATION_SEPARATOR);
                k = 0;
                while (l != -1) {
                    translationList.add( t.substring( k, l));
                    k = l + 1;
                    l = t.indexOf( TRANSLATION_SEPARATOR, k);
                }
                if (t.length() > k)
                    translationList.add( t.substring( k));
                
                if (translationList.size() > 0)
                    translations = (String[] ) translationList.toArray
                        ( new String[translationList.size()]);
            }

            j = code.indexOf( FIELD_SEPARATOR, i);
            String dictionary = code.substring( i, j);
            i = j + 1;

            Conjugation conjugation = null;
            j = code.indexOf( FIELD_SEPARATOR, i);
            if (j!=-1 && j!=i) {
                String conjugatedForm = code.substring( i, j);
                i = j + 1;
                j = code.indexOf( FIELD_SEPARATOR, i);
                String dictionaryForm = code.substring( i, j);
                i = j + 1;
                j = code.indexOf( FIELD_SEPARATOR, i);
                String type = code.substring( i, j);
                conjugation = new Conjugation( conjugatedForm, dictionaryForm, type);
            }

            return new Translation( start, length, 
                                    new DictionaryEntry( word, reading, translations,
                                                         getDictionary( dictionary)),
                                    conjugation);
        }
        else {
            System.err.println( JGloss.messages.getString( "error.save.unknownannotation",
                                                           new Object[] { c }));
            return new UnknownAnnotation( code, start, length);
        }
    }

    /**
     * Returns a dummy dictionary with the given name. This will return instances of
     * {@link TextAnnotationCodec.NullDictionary NullDictionary}.
     *
     * @param dictionary Name used by the dummy dictionary.
     * @return A dictionary from the cache or a new instance if it is the first dictionary
     *         with this name.
     */
    private static jgloss.dictionary.Dictionary getDictionary( String dictionary) {
        jgloss.dictionary.Dictionary d = (jgloss.dictionary.Dictionary) dictionaries.get( dictionary);
        if (d == null) {
            d = new NullDictionary( dictionary);
            dictionaries.put( dictionary, d);
        }

        return d;
    }

    /**
     * Replaces HTML special characters which are not allowed in an attribute value with
     * character entities.
     *
     * @param text The text containing unsafe characters.
     * @return The string containing the text with unsafe characters replaced by named entities.
     */
    private static String quote( String text) {
        StringBuffer sb = new StringBuffer( text);

        for ( int i=sb.length()-1; i>=0; i--) {
            switch (sb.charAt( i)) {
            case '"':
                sb.replace( i, i+1, "&quot;");
                break;
            case '&':
                sb.replace( i, i+1, "&amp;");
                break;
            case '<':
                sb.replace( i, i+1, "&lt;");
                break;
            case '>':
                sb.replace( i, i+1, "&gt;");
                break;
            }
        }

        return sb.toString();
    }

    /**
     * Unquotes a string previously quoted with {@link #quote(String) quote}.
     *
     * @param text The text to unquote.
     * @return The unquoted text.
     */
    private static String unquote( String text) {
        StringBuffer sb = new StringBuffer( text);

        int end = -1;
        for ( int i=sb.length()-1; i>=0; i--) {
            switch (sb.charAt( i)) {
            case ';':
                end = i;
                break;
            case '&':
                if (end > i) {
                    String c = sb.substring( i+1, end);
                    if (c.equals( "quot"))
                        sb.replace( i, end+1, "\"");
                    else if (c.equals( "amp"))
                        sb.replace( i, end+1, "&");
                    else if (c.equals( "lt"))
                        sb.replace( i, end+1, "<");
                    else if (c.equals( "gt"))
                        sb.replace( i, end+1, ">");
                    end = 0;
                }
                break;
            }
        }
        
        return sb.toString();
    }
} // class TextAnnotationCodec
