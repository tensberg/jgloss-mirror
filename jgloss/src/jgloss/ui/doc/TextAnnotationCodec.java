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
     * Escape character for special chars in text fields.
     */
    public final static char ESCAPE_CHARACTER = '#';
    /**
     * Annotation type string used to mark a reading annotation.
     */
    public final static String READING_TYPE = "R";
    /**
     * Annotation type string used to mark a translation annotation.
     */
    public final static String TRANSLATION_TYPE = "T";

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

        public void dispose() {}
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
        public UnknownAnnotation( String code) {
            this.code = code;
        }

        /**
         * Returns the code of this annotation as found in the encoded string.
         *
         * @return The code of this annotation as found in the encoded string.
         */
        public String getCode() {
            return code;
        }

        public int getStart() { return 0; }
        public int getLength() { return 1; }
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
        String code;
        if (ta.getClass().equals( Reading.class))
            code = READING_TYPE;
        else if (ta.getClass().equals( Translation.class))
            code = TRANSLATION_TYPE;
        else
            code = ta.getClass().getName();
        code += FIELD_SEPARATOR;

        if (ta instanceof Reading) {
            Reading annotation = (Reading) ta;
            String word = annotation.getWord();
            String reading = annotation.getReading();
            Conjugation c = annotation.getConjugation();
            jgloss.dictionary.Dictionary d = annotation.getWordReadingPair().getDictionary();
            String[] translations = null;
            if (ta instanceof Translation)
                translations = ((Translation) ta).getDictionaryEntry().getTranslations();

            code += escapeField( word) + FIELD_SEPARATOR;
            if (reading == null)
                code += NULL_STRING;
            else
                code += escapeField( reading);
            code += FIELD_SEPARATOR;

            if (translations != null) {
                if (translations.length>0) {
                    code += escapeField( translations[0]);
                    for ( int i=1; i<translations.length; i++)
                        code += TRANSLATION_SEPARATOR + escapeField( translations[i]);
                }
            }
            else
                code += NULL_STRING;
        
            code += FIELD_SEPARATOR + escapeField( d.getName()) + FIELD_SEPARATOR;
            if (c != null) {
                code += escapeField( c.getConjugatedForm()) + FIELD_SEPARATOR +
                    escapeField( c.getDictionaryForm()) + FIELD_SEPARATOR +
                    escapeField( c.getType());
            }
            code += FIELD_SEPARATOR;
        }
        else if (ta instanceof UnknownAnnotation) {
            code = ((UnknownAnnotation) ta).getCode();
        }
        else
            System.err.println( JGloss.messages.getString( "error.save.unknownannotation",
                                                           new Object[] { ta.getClass().getName() }));

        return escape( code);
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
        code = unescape( code);

        int i = code.indexOf( FIELD_SEPARATOR);
        String type = code.substring( 0, i);
        i++;

        // handle backwards compatibility
        if (type.equals( "jgloss.dictionary.Reading") ||
            type.equals( "jgloss.dictionary.Reading_1.1") ||
            type.equals( "jgloss.dictionary.Translation")) {
            // skip annotation start and length (not used any more)
            i = code.indexOf( FIELD_SEPARATOR, i) + 1;
            i = code.indexOf( FIELD_SEPARATOR, i) + 1;
        }
        if (type.equals( "jgloss.dictionary.Reading_1.1"))
            type = READING_TYPE;
        else if (type.equals( "jgloss.dictionary.Translation"))
            type = TRANSLATION_TYPE;
            
        if (type.equals( "jgloss.dictionary.Reading")) {
            // This is left in for backwards compatibility with JGloss V0.9 files.
            
            int j = code.indexOf( FIELD_SEPARATOR, i);
            final String reading = code.substring( i, j);
            final jgloss.dictionary.Dictionary d = getDictionary
                ( ResourceBundle.getBundle( "resources/messages-dictionary")
                  .getString( "parser.dictionary.document"));
            return new Reading( new WordReadingPair() {
                    public String getWord() { return reading; }
                    public String getReading() { return reading; }
                    public jgloss.dictionary.Dictionary getDictionary() {
                        return d;
                    }
                }, null);
        }
        else if (type.equals( READING_TYPE) || type.equals( TRANSLATION_TYPE)) {
            int j = code.indexOf( FIELD_SEPARATOR, i);
            final String word = unescapeField( code.substring( i, j));
            i = j + 1;

            j = code.indexOf( FIELD_SEPARATOR, i);
            String reading2 = unescapeField( code.substring( i, j));
            if (reading2.equals( NULL_STRING))
                reading2 = null;
            final String reading = reading2;
            i = j + 1;

            j = code.indexOf( FIELD_SEPARATOR, i);
            String t = code.substring( i, j);
            i = j + 1;
            String[] translations = null;
            if (!t.equals( NULL_STRING)) {
                List translationList = new ArrayList();
                int l = t.indexOf( TRANSLATION_SEPARATOR);
                int k = 0;
                while (l != -1) {
                    translationList.add( unescapeField( t.substring( k, l)));
                    k = l + 1;
                    l = t.indexOf( TRANSLATION_SEPARATOR, k);
                }
                if (t.length() > k)
                    translationList.add( unescapeField( t.substring( k)));
                
                if (translationList.size() > 0)
                    translations = (String[] ) translationList.toArray
                        ( new String[translationList.size()]);
            }

            j = code.indexOf( FIELD_SEPARATOR, i);
            final jgloss.dictionary.Dictionary dictionary = getDictionary
                ( unescapeField( code.substring( i, j)));
            i = j + 1;

            Conjugation conjugation = null;
            j = code.indexOf( FIELD_SEPARATOR, i);
            if (j!=-1 && j!=i) {
                String conjugatedForm = unescapeField( code.substring( i, j));
                i = j + 1;
                j = code.indexOf( FIELD_SEPARATOR, i);
                String dictionaryForm = unescapeField( code.substring( i, j));
                i = j + 1;
                j = code.indexOf( FIELD_SEPARATOR, i);
                String conjtype = unescapeField( code.substring( i, j));
                conjugation = Conjugation.getConjugation( conjugatedForm, dictionaryForm, conjtype);
            }

            if (translations != null) {
                return new Translation( new DictionaryEntry( word, reading, translations,
                                                             dictionary),
                                        conjugation);
            }
            else {
                return new Reading( new WordReadingPair() {
                        public String getWord() { return word; }
                        public String getReading() { return reading; }
                        public jgloss.dictionary.Dictionary getDictionary() { return dictionary; }
                    }, conjugation);
            }
        }
        else {
            System.err.println( JGloss.messages.getString( "error.save.unknownannotation",
                                                           new Object[] { type }));
            return new UnknownAnnotation( code);
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
    private static String escape( String text) {
        StringBuffer sb = null; // defer string buffer initialization until it is really needed

        for ( int i=text.length()-1; i>=0; i--) {
            String replaceBy = null;
            switch (text.charAt( i)) {
            case '"':
                replaceBy = "&quot;";
                break;
            case '&':
                replaceBy = "&amp;";
                break;
            case '<':
                replaceBy = "&lt;";
                break;
            case '>':
                replaceBy = "&gt;";
                break;
            }
            if (replaceBy != null) {
                if (sb == null) // first occurrence of an escaped character
                    sb = new StringBuffer( text);
                sb.replace( i, i+1, replaceBy);
            }
        }

        if (sb != null) // a character was escaped
            return sb.toString();
        else
            return text; // string was not changed
    }

    /**
     * Unescapes a string previously escaped with {@link #escape(String) escape}.
     *
     * @param text The text to unescape.
     * @return The unescaped text.
     */
    private static String unescape( String text) {
        StringBuffer sb = null; // defer string buffer initialization until it is really needed

        int end = -1;
        for ( int i=text.length()-1; i>=0; i--) {
            switch (text.charAt( i)) {
            case ';':
                end = i;
                break;
            case '&':
                if (end > i) {
                    String c = text.substring( i+1, end);
                    if (sb == null) // first occurrence of an escaped character
                        sb = new StringBuffer( text);
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
        
        if (sb != null) // a character was unescaped
            return sb.toString();
        else
            return text; // string was not changed
    }

    /**
     * Escapes special characters in a field entry.
     *
     * @param text Text to escape.
     * @return The escaped text.
     */
    private static String escapeField( String text) {
        StringBuffer sb = null; // defer string buffer initialization until it is really needed

        for ( int i=text.length()-1; i>=0; i--) {
            switch (text.charAt( i)) {
            case FIELD_SEPARATOR:
            case TRANSLATION_SEPARATOR:
            case LIST_SEPARATOR:
            case ESCAPE_CHARACTER:
                if (sb == null) // first occurrence of an escaped character
                    sb = new StringBuffer( text);
                sb.replace( i, i+1, ESCAPE_CHARACTER + 
                            Integer.toString( (int) text.charAt( i)) + ";");
                break;
            }
        }

        if (sb != null) // a character was escaped
            return sb.toString();
        else
            return text; // string was not changed
    }

    /**
     * Unescapes a string previously escaped with {@link #escapeField(String) escapeField}.
     *
     * @param text The text to unescape.
     * @return The unescaped text.
     */
    private static String unescapeField( String text) {
        StringBuffer sb = null; // defer string buffer initialization until it is really needed

        int end = -1;
        for ( int i=text.length()-1; i>=0; i--) {
            switch (text.charAt( i)) {
            case ';':
                end = i;
                break;
            case ESCAPE_CHARACTER:
                if (end > i) {
                    String code = text.substring( i+1, end);
                    try {
                        char c = (char) Integer.parseInt( code);
                        if (sb == null) // first occurrence of an escaped character
                            sb = new StringBuffer( text);
                        sb.setCharAt( i, c);
                        sb.delete( i+1, end+1);
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();
                    }
                    end = 0;
                }
                break;
            }
        }
        
        if (sb != null) // an escaped character was unescaped
            return sb.toString();
        else
            return text; // string was not changed
    }
} // class TextAnnotationCodec
