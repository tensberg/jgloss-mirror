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
import java.util.List;

/**
 * Implementation for dictionaries in GDict format. GDict is a Japanese-German dictionary
 * available from <a href="http://www.bibiko.com/dlde.htm">http://www.bibiko.com/dlde.htm</a>.
 *
 * @author Michael Koch
 */
public class GDict extends FileBasedDictionary {
    public static void main( String[] args) throws Exception {
        GDict g = new GDict( new File( "/home/michael/japan/dictionaries/gdict/gdictutf.txt"), true);
        g.search( "\u6f22\u5b57", SEARCH_EXACT_MATCHES);
    }

    public GDict( File dicfile, boolean createindex) throws IOException {
        super( dicfile, createindex);
    }

    public String getEncoding() { return "UTF-8"; }

    protected boolean isEntryStart( int offset) {
        byte b = dictionary.get( offset-1);
        return (b==';' || b==' ' || b=='|' || b==10 || b==13);
    }
    
    protected boolean isEntryEnd( int offset) {
        byte b = dictionary.get( offset);
        return (b==';' || b=='|' || b==10 || b==13);
    }

    protected void parseEntry( List result, String entry) {
        System.err.println( entry);
    }

    protected int readNextCharacter() {
        byte b = dictionary.get();
        int c;
        // the dictionary is UTF-8 encoded; if the highest bit is set, more than one byte must be read
        if ((b&0x80) == 0) {
            // ASCII character
            c = b;
        }
        else if ((b&0xe0) == 0xc0) { // 2-byte encoded char
            byte b2 = dictionary.get();
            if ((b2&0xc0) == 0x80) // valid second byte
                c = ((b&0x1f) << 6) | (b&0x3f);
            else {
                System.err.println( "invalid 2-byte character");
                c = '?';
            }
        }
        else if ((b&0xf0) == 0xe0) { // 3-byte encoded char
            byte b2 = dictionary.get();
            byte b3 = dictionary.get();
            if (((b2&0xc0) == 0x80) &&
                ((b3&0xc0) == 0x80)) { // valid second and third byte
                c = ((b&0x0f) << 12) | ((b2&0x3f) << 6) | (b3&0x3f);
            }
            else {
                c = '?';
                System.err.println( "invalid 3-byte character");
            }
        }
        else { // 4-6 byte encoded char or invalid char
            System.err.println( "invalid char");
            c = '?';
        }

        if (c > 127) {
            if (c>=0x4e00 && c<0xa000)
                return 0; // kanji
            else if (c>=3000 && c<3100)
                return 1; // katakana, hiragana, symbols
            else // probably umlauts in translation word
                return 3;
        }
        else if (c>='a' && c<='z' ||
                 c>='A' && c<='Z' ||
                 c>='0' && c<='9' ||
                 c=='-' || c=='.')
            return 3; // translation word character
        else
            return -1; // not in word
    }
} // class GDict
