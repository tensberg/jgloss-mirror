/**
 *   Copyright (C) 2002 Eric Crahen <crahen@cse.buffalo.edu>
 *   modified by Michael Koch <tensberg@gmx.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package jgloss.ui.im;

import java.util.HashMap;
import java.util.Map;

/**
 * @class RomajiTranslator
 *
 * Simple utlitly class to aid in the translation romanji text to
 * a unicode representation. This can translate into both kana and
 * hiragana.
 */
public class RomajiTranslator {
 
    private final static RomajiTranslator instance = new RomajiTranslator();

    public final static Conversion HIRAGANA = new Hiragana();
    public final static Conversion KATAKANA = new Katakana();
  
    /**
     * Translate a string of romanji text into a string of unicode
     * by applying the given conversion
     *
     * @param buf StringBuffer to place result into
     * @param conv Conversion to apply
     *
     * @return translated String
     */
    static public String translate(StringBuffer buf, Conversion c) {

        int len = buf.length();

        char x = (len > 0) ? buf.charAt(0) : 0;
        char y = (len > 1) ? buf.charAt(1) : 0;
        char z = (len > 2) ? buf.charAt(2) : 0;

        String s = null;

        // Replace vowels
        if(isVowel(x)) {

            s = c.get(buf.substring(0, 1));
            buf.delete(0, 1);

        }

        // Pull out '-'
        else if(x == '-') {

            s = c.get( buf.substring(0, 1));
            buf.delete(0, 1);

        }

        // Replace 'nn'
        else if(x == 'n' && y == 'n') {

            s = c.get("nn");
            buf.delete(0, 2);

        }

        // Replace 'n'
        else if(x == 'n' && y != 0 && !isVowel(y)) {

            s = c.get("nn");
            buf.delete(0, 1);

        }

        // Replace double syllables
        else if(isVowel(y)) {
      
            s = c.get(buf.substring(0, 2));
            buf.delete(0, 2);

        } 

        // Replace triple syllables
        else if(isVowel(z) && (y == 'h' || y == 'y')) {

            s = c.get(buf.substring(0, 3));
            buf.delete(0, 3);

        }

        else if (z != 0) {

            s = buf.substring( 0, 3);
            buf.delete(0, 3);

        }

        // Clean out the characters that can't match
        else if(y != 'y' && y != 'h' && y != 0) {

            s = buf.substring(0,2);
            buf.delete(0, 2);

        }

        return s;

    }

    public static final boolean isApplicableChar( char c) {
        return c>='a' && c<='z' || c=='-';
    }

    /**
     * A convience function to check for a vowel.
     *
     * @param ch character to check.
     *
     * @return bool true if the character is a vowel, otherwise false.
     */
    private final static boolean isVowel(char ch) {
        return (ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u');
    }

    private RomajiTranslator() { }
  
  
  
    public abstract static class Conversion {
        protected Map translation;
        //protected Set prefixes;

        protected Map getMap() { return translation; }

        abstract public String getName();   

        public String get( String key) {
            return (String) getMap().get(key);
        }

        /*        protected void initPrefixesFromTranslation() {
            prefixes = new HashSet( translation.size());

            for ( Iterator i=translation.keySet().iterator(); i.hasNext(); ) {
                String key = i.next().toString();
                for ( int j=0; j<key.length()-1; j++) {
                    prefixes.put( key.substring( 0, j));
                }
            }
        }

        public boolean isTranslationPrefix( String s) {
            if (prefixes == null)
                initPrefixesFromTranslation();
            return prefixes.contains( s);
            }*/
    }

    public static class Hiragana extends Conversion {
  
        private String name = "\u5e73\u4eee\u540d";
    
        private Hiragana() {
            translation = new HashMap( 101);
            // Vowels
    
            translation.put("a", "\u3042");
            translation.put("i", "\u3044");
            translation.put("u", "\u3046");
            translation.put("e", "\u3048");
            translation.put("o", "\u304A");
    
            translation.put("-", "\u3063"); 
            // Syllables
    
            translation.put("ka", "\u304B");
            translation.put("ki", "\u304D");
            translation.put("ku", "\u304F");
            translation.put("ke", "\u3051");
            translation.put("ko", "\u3053");
    
            translation.put("sa", "\u3055");
            translation.put("si", "\u3057");
            translation.put("su", "\u3059");
            translation.put("se", "\u305B");
            translation.put("so", "\u305D");
    
            translation.put("ta", "\u305F");
            translation.put("ti", "\u3061");
            translation.put("tu", "\u3064");
            translation.put("te", "\u3066");
            translation.put("to", "\u3068");

            translation.put("tsu", translation.get("tu"));
            translation.put("chi", translation.get("ti"));
    
            translation.put("nn", "\u3093"); 
    
            translation.put("na", "\u306A");
            translation.put("ni", "\u306B");
            translation.put("nu", "\u306C");
            translation.put("ne", "\u306D");
            translation.put("no", "\u306E");
    
            translation.put("ha", "\u306F");
            translation.put("hi", "\u3072");
            translation.put("hu", "\u3075");
            translation.put("he", "\u3078");
            translation.put("ho", "\u307B");
    
            translation.put("ma", "\u307E");
            translation.put("mi", "\u307F");
            translation.put("mu", "\u3080");
            translation.put("me", "\u3081");
            translation.put("mo", "\u3082");
    
            translation.put("ya", "\u3084");
            translation.put("yu", "\u3086");
            translation.put("yo", "\u3088");
    
            translation.put("ra", "\u3089");
            translation.put("ri", "\u308A");
            translation.put("ru", "\u308B");
            translation.put("re", "\u308C");
            translation.put("ro", "\u308D");
    
            translation.put("wa", "\u308F");
            translation.put("wo", "\u3092");
    
            // Phonetic change
    
            translation.put("ga", "\u304C");
            translation.put("gi", "\u304E");
            translation.put("gu", "\u3050");
            translation.put("ge", "\u3052");
            translation.put("go", "\u3054");
    
            translation.put("za", "\u3056");
            translation.put("zi", "\u3058");
            translation.put("zu", "\u305A");
            translation.put("ze", "\u305C");
            translation.put("zo", "\u305E");

            translation.put("ji", translation.get("zi"));
    
            translation.put("da", "\u3060");
            translation.put("di", "\u3062");
            translation.put("du", "\u3065");
            translation.put("de", "\u3067");
            translation.put("do", "\u3069");
    
            translation.put("ba", "\u3070");
            translation.put("bi", "\u3073");
            translation.put("bu", "\u3076");
            translation.put("be", "\u3079");
            translation.put("bo", "\u307C");
    
            translation.put("pa", "\u3071");
            translation.put("pi", "\u3074");
            translation.put("pu", "\u3077");
            translation.put("pe", "\u307A");
            translation.put("po", "\u307D");
    
            // Compounds
    
            translation.put("ja", "\u3058\u3083");
            translation.put("ju", "\u3058\u3085");
            translation.put("je", "\u3058\u3047");
            translation.put("jo", "\u3058\u3087");
    
            translation.put("fa", "\u3075\u3041");
            translation.put("fu", "\u3075\u3045");
            translation.put("fe", "\u3075\u3047");
            translation.put("fo", "\u3075\u3089");
    
            translation.put("kya", "\u304D\u3083");
            translation.put("kyu", "\u304D\u3085");
            translation.put("kyo", "\u304D\u3087");
    
            translation.put("gya", "\u304E\u3083");
            translation.put("gyu", "\u304E\u3085");
            translation.put("gyo", "\u304E\u3087");
    
            translation.put("nya", "\u306B\u3083");
            translation.put("nyu", "\u306B\u3085");
            translation.put("nyo", "\u306B\u3087");
    
            translation.put("hya", "\u3072\u3083");
            translation.put("hyu", "\u3072\u3085");
            translation.put("hyo", "\u3072\u3087");
    
            translation.put("bya", "\u3073\u3083");
            translation.put("byu", "\u3073\u3085");
            translation.put("byo", "\u3073\u3087");
    
            translation.put("pya", "\u3074\u3083");
            translation.put("pyu", "\u3074\u3085");
            translation.put("pyo", "\u3074\u3087");
    
            translation.put("mya", "\u307F\u3083");
            translation.put("myu", "\u307F\u3085");
            translation.put("myo", "\u307F\u3087");
    
            translation.put("rya", "\u308A\u3083");
            translation.put("ryu", "\u308A\u3085");
            translation.put("ryo", "\u308A\u3087");
    
            translation.put("sha", "\u3057\u3083");
            translation.put("shu", "\u3057\u3085");
            translation.put("she", "\u3057\u3047");
            translation.put("sho", "\u3057\u3087");
            translation.put("shi", "\u3057");

            translation.put("cha", "\u3061\u3083");
            translation.put("chu", "\u3061\u3085");
            translation.put("che", "\u3061\u3047");
            translation.put("cho", "\u3061\u3087");

        }
  
        public String getName() {
            return name;
        }
    
        public String toString() {
            return getName();
        }
    
    }

  
    public static class Katakana extends Conversion {
  
        private String name = "\u7247\u4eee\u540d";
    
        private Katakana() {
            translation = new HashMap( 101);

            // Vowels
    
            translation.put("a", "\u30A2");
            translation.put("i", "\u30A4");
            translation.put("u", "\u30A6");
            translation.put("e", "\u30A8");
            translation.put("o", "\u30AA");
    
    
            translation.put("-", "\u3063"); 
    
            // Syllables
    
            translation.put("ka", "\u30AB");
            translation.put("ki", "\u30AD");
            translation.put("ku", "\u30AF");
            translation.put("ke", "\u30B1");
            translation.put("ko", "\u30B3");
    
            translation.put("sa", "\u30B5");
            translation.put("si", "\u30B7");
            translation.put("su", "\u30B9");
            translation.put("se", "\u30BB");
            translation.put("so", "\u30BD");
    
            translation.put("ta", "\u30BF");
            translation.put("ti", "\u30C1");
            translation.put("tu", "\u30C4");
            translation.put("te", "\u30C6");
            translation.put("to", "\u30C8");
    
            translation.put("tsu", translation.get("tu"));
            translation.put("chi", translation.get("ti"));
    
            translation.put("nn", "\u30F3"); 
    
            translation.put("na", "\u30CA");
            translation.put("ni", "\u30CB");
            translation.put("nu", "\u30CC");
            translation.put("ne", "\u30CD");
            translation.put("no", "\u30CE");
    
            translation.put("ha", "\u30CF");
            translation.put("hi", "\u30D2");
            translation.put("hu", "\u30D5");
            translation.put("he", "\u30D8");
            translation.put("ho", "\u30DB");
    
            translation.put("ma", "\u30DE");
            translation.put("mi", "\u30DF");
            translation.put("mu", "\u30E0");
            translation.put("me", "\u30E1");
            translation.put("mo", "\u30E2");
    
            translation.put("ya", "\u30E4");
            translation.put("yu", "\u30E6");
            translation.put("yo", "\u30E8");
    
            translation.put("ra", "\u30E9");
            translation.put("ri", "\u30EA");
            translation.put("ru", "\u30EB");
            translation.put("re", "\u30EC");
            translation.put("ro", "\u30ED");
    
            translation.put("wa", "\u30EF");
            translation.put("wo", "\u30F2");
    
            // Phonetic change
    
            translation.put("ga", "\u30AC");
            translation.put("gi", "\u30AE");
            translation.put("gu", "\u30B0");
            translation.put("ge", "\u30B2");
            translation.put("go", "\u30B4");
    
            translation.put("za", "\u30B6");
            translation.put("zi", "\u30B8");
            translation.put("zu", "\u30BA");
            translation.put("ze", "\u30BC");
            translation.put("zo", "\u30BE");

            translation.put("ji", translation.get("zi"));
    
            translation.put("da", "\u30C0");
            translation.put("di", "\u30C2");
            translation.put("du", "\u30C5");
            translation.put("de", "\u30C7");
            translation.put("do", "\u30C9");
    
            translation.put("ba", "\u30D0");
            translation.put("bi", "\u30D3");
            translation.put("bu", "\u30D6");
            translation.put("be", "\u30D9");
            translation.put("bo", "\u30DC");
    
            translation.put("pa", "\u30D1");
            translation.put("pi", "\u30D4");
            translation.put("pu", "\u30D7");
            translation.put("pe", "\u30DA");
            translation.put("po", "\u30DD");
    
            // Compounds
    
            translation.put("ja", "\u30B8\u30E3");
            translation.put("ju", "\u30B8\u30E5");
            translation.put("je", "\u30B8\u30A7");
            translation.put("jo", "\u30B8\u30E7");
    
            translation.put("fa", "\u30D5\u30A1");
            translation.put("fu", "\u30D5\u30A5");
            translation.put("fe", "\u30D5\u30A7");
            translation.put("fo", "\u30D5\u30A9");
    
            translation.put("kya", "\u30AD\u30E3");
            translation.put("kyu", "\u30AD\u30E5");
            translation.put("kyo", "\u30AD\u30E7");
    
            translation.put("gya", "\u30AE\u30E3");
            translation.put("gyu", "\u30AE\u30E5");
            translation.put("gyo", "\u30AE\u30E7");
    
            translation.put("nya", "\u30CB\u30E3");
            translation.put("nyu", "\u30CB\u30E5");
            translation.put("nyo", "\u30CB\u30E7");
    
            translation.put("hya", "\u30D2\u30E3");
            translation.put("hyu", "\u30D2\u30E5");
            translation.put("hyo", "\u30D2\u30E7");
    
            translation.put("bya", "\u30D3\u30E3");
            translation.put("byu", "\u30D3\u30E5");
            translation.put("byo", "\u30D3\u30E7");
    
            translation.put("pya", "\u30D4\u30E3");
            translation.put("pyu", "\u30D4\u30E5");
            translation.put("pyo", "\u30D4\u30E7");
    
            translation.put("mya", "\u30DF\u30E3");
            translation.put("myu", "\u30DF\u30E5");
            translation.put("myo", "\u30DF\u30E7");
    
            translation.put("rya", "\u30EA\u30E3");
            translation.put("ryu", "\u30EA\u30E5");
            translation.put("ryo", "\u30EA\u30E7");
    
            translation.put("sha", "\u30B7\u30E3");
            translation.put("shu", "\u30B7\u30E5");
            translation.put("she", "\u30B7\u30A7");
            translation.put("sho", "\u30B7\u30E7");
            translation.put("shi", "\u30B7");

            translation.put("cha", "\u30C1\u30E3");
            translation.put("chu", "\u30C1\u30E5");
            translation.put("che", "\u30C1\u30A7");
            translation.put("cho", "\u30C1\u30E7");
        }
    
        public String getName() {
            return name;
        }
    
        public String toString() {
            return getName();
        }

    }
} // class RomajiTranslator
