/*
 * Copyright (C) 2002 Michael Koch (tensberg@gmx.net)
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

package jgloss.dictionary;

import java.io.*;
import java.nio.*;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Dictionary implementation for dictionaries in EDICT format 
 * based on {@link FileBasedDictionary} and using the J2 1.4 NIO API. For a documentation of the format see
 * <a href="http://www.csse.monash.edu.au/~jwb/edict_doc.html">
 * http://www.csse.monash.edu.au/~jwb/edict_doc.html</a>.
 *
 * @author Michael Koch
 */
public class EDictNIO extends FileBasedDictionary {
    /**
     * Object describing this implementation of the <CODE>Dictionary</CODE> interface. The
     * Object can be used to register this class with the <CODE>DictionaryFactory</CODE>, or
     * test if a descriptor matches this class.
     *
     * @see DictionaryFactory
     */
    public final static DictionaryFactory.Implementation implementation = 
        initImplementation();

    /**
     * Returns a {@link FileBasedDictionary.Implementation FileBasedDictionary.Implementation}
     * which recognizes EUC-JP encoded EDICT dictionaries. Used to initialize the
     * {@link #implementation implementation} final member because the constructor has to
     * be wrapped in a try/catch block.
     * 
     */
    private static DictionaryFactory.Implementation initImplementation() {
        try {
            // Explanation of the pattern:
            // The EDICT format is "word [reading] /translation/translation/.../", with
            // the reading being optional. The dictionary has to start with a line of this form,
            // therefore the pattern starts with a \A and ends with $
            // To distinguish an EDICT dictionary from a SKK dictionary, which uses a similar format,
            // it is tested that the first char in the translation is not a Kanji
            // (InCJKUnifiedIdeographs)
            return new FileBasedDictionary.Implementation
                ( "EDICT", "EUC-JP", true, Pattern.compile
                  ( "\\A\\S+?(\\s\\[.+?\\])?\\s/\\P{InCJKUnifiedIdeographs}.*/$", Pattern.MULTILINE),
                  1.0f, 4096, EDictNIO.class.getConstructor( new Class[] { File.class }));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public EDictNIO( File dicfile) throws IOException, IndexCreationException {
        this( dicfile, true);
    }

    public EDictNIO( File dicfile, boolean createindex) throws IOException, IndexCreationException {
        super( dicfile, createindex);
    }

    public String getEncoding() { return "EUC_JP"; }

    protected boolean isEntryStart( int offset) {
        try {
            byte b = dictionary.get( offset-1);
            return (b=='[' || b=='/' || b==10 || b==13);
        } catch (IndexOutOfBoundsException ex) {
            return true;
        }
    }
    
    protected boolean isEntryEnd( int offset) {
        try {
            byte b = dictionary.get( offset);
            if (b==']' || b=='/' || b==10 || b==13)
                return true;
            else if (b == ' ') { // end of word if followed by [ or /
                b = dictionary.get( offset+1);
                return (b=='[' || b=='/');
            }
            else
                return false;
        } catch (IndexOutOfBoundsException ex) {
            return true;
        }
    }

    /**
     * Parses an EDICT formatted entry. The format is
     * <CODE>word [reading] /translation 1/translation 2/...</CODE> with the reading
     * being optional.
     */
    protected void parseEntry( List result, String entry, int entrystart, int where, String expression,
                               ByteBuffer exprbuf, short searchmode, short resultmode) {
        int j, k;
        // word:
        String word;
        try {
            int i = entry.indexOf( ' ');
            word = unescape( entry.substring( 0, i));

            // reading:
            String reading = null;
            i = entry.indexOf( '[');
            if (i != -1) {
                j = entry.indexOf( ']', i+1);
                reading = unescape( entry.substring( i+1, j));
            } // else: no reading
        
            // translations
            i = entry.indexOf( '/', i);
            ArrayList translations = new ArrayList( 10);
            while ((k=entry.indexOf( '/', i+1)) != -1) {
                translations.add( unescape( entry.substring( i+1, k)));
                i = k;
            }
            translations.trimToSize();
            result.add( new DefaultDictionaryEntry( word, reading, translations, this));
        } catch (StringIndexOutOfBoundsException ex) {
            System.err.println( "EDICT warning: " + dicfile +
                                "\nMalformed dictionary entry: " + entry);
        }
    }
    
    /**
     * Reads a EUC-JP encoded character from the buffer. ASCII uppercase is converted to lowercase,
     * Katakana is converted to hiragana. The value returned is the 1-3 bytes long EUC-JP encoded
     * character.
     */
    protected int readCharacter( ByteBuffer buf) throws BufferUnderflowException {
        int c = byteToUnsignedByte( buf.get());
        if (c < 128) { // 1-Byte ASCII
            // uppercase -> lowercase conversion
            if ((c >= 'A') && (c <= 'Z')) c |= 0x20;
        }
        else { // 2/3-Byte Japanese
            boolean threebyte = false;
            if (c == 0xA5) // convert katakana to hiragana
                c = 0xA4;
            else if (c == 0x8f) // JIS X 0212 3-Byte Kanji
                threebyte = true;
            // read second byte
            c = (c<<8) | byteToUnsignedByte( buf.get());
            if (threebyte) // read third byte
                c = (c<<8) | byteToUnsignedByte( buf.get());
        }

        return c;
    }

    /**
     * Decide how a character should be treated for index creation.
     *
     * @param c EUC-JP encoded, 1-3 bytes long character.
     */
    protected int isWordCharacter( int c, boolean inWord) {
        if (c > 127) { // multibyte character in EUC-JP encoding
            if (c >= 0xb000) // 2- or 3-byte kanji
                return 0;
            // otherwise kana
            return 1;
        }
        else { // ASCII character
            if (c>='a' && c<='z' ||
                c>='A' && c<='Z' ||
                c>='0' && c<='9' ||
                c=='\\' || // possible start of unicode escape
                (inWord && c=='-'))
                return 3; // word character
            else
                return -1; // not in index word
        }
    }

    public String toString() {
        return "EDICT " + getName();
    }

    /**
     * Escape LF/CR, '/' and all characters except ASCII, kanji and kana.
     */
    protected boolean escapeChar( char c) {
        // some special characters need escaping
        if (c==10 || c==13 || c=='/')
            return true;

        // ASCII and Kanji/Kana characters don't need escaping
        // (Danger: The range covered may be too large)
        if (c<128 || c>=0x2e80 && c<0xa000)
            return false;

        // escape all other characters
        return true;
    }
} // class EDictNIO
