/*
 * Copyright (c) 2001,2002 Michael Koch (tensberg@gmx.net)
 * Copyright (c) 1994 Yasuhiro Tonooka (tonooka@msi.co.jp)
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
 * The code is a java port of the encoding detection routine in Yasuhiro Tonooka's
 * "Kanji Code Converter" (kcc) unix program. I (Michael)
 * have left out a few bits like the detection of DEC encoding (whatever that is),
 * and the reduced mode.
 *
 */

package jgloss.util;

import java.io.*;

/**
 * Try to detect the character encoding of an input stream reading Japanese
 * text. Detected encodings are ISO-2022-JP, EUC-JP, Shift-JIS and UTF-8.
 * The test works by checking the validity of each byte from the input array
 * in each character encoding and eliminating the encodings where the byte combinations
 * are illegal.<br>
 * The encoding detection routine is taken from Yasuhiro Tonooka's
 * "Kanji Code Converter" (kcc) unix program.
 * 
 * @author Michael Koch, Yasuhiro Tonooka
 */
public class CharacterEncodingDetector {
    /**
     * Number of bytes which are read from the stream and looked at for detection.
     */
    private final static int DEFAULT_LOOK_AT_LENGTH = 10000;
    
    public final static String ENC_ISO_2022_JP = "ISO-2022-JP";
    public final static String ENC_EUC_JP = "EUC-JP";
    public final static String ENC_SHIFT_JIS = "Shift_JIS";
    public final static String ENC_UTF_8 = "UTF-8";
    public final static String ENC_ISO_8859_1 = "ISO-8859-1";
    public final static String ENC_ASCII = "ASCII";

    private final static int ESC = 0x1b;
    private final static int SO = 0x0e;
    private final static int SI = 0x0f;
    private final static int SS2 = 0xae; /* EUC single shift 2 */
    private final static int SS3 = 0x8f; /* EUC single shift 3 */

    /**
     * Flag set if the byte array contains values outside the ASCII character set.
     */
    public final static int NONASCII = 0x01; /* non-ASCII character */
    /**
     * Flag set if the byte array contains JIS values.
     */
    public final static int JIS = 0x02;
    /**
     * Flag set if the byte array contains EUC variables.
     */
    public final static int EUC = 0x10;
    /**
     * Flag set if the byte array contains Shift-JIS values.
     */
    public final static int SJIS = 0x20;
    /**
     * Flag set if the byte array contains 8-bit JIS values.
     */
    public final static int JIS8 = 0x40; /* 8-bit JIS */
    /**
     * Flag set if the byte array contains UTF-8 characters.
     */
    public final static int UTF8 = 0x80;

    private final static byte M_ASCII = 0;
    private final static byte M_KANJI = 1;
    private final static byte M_GAIJI = 2;
    private final static byte M_SO    = 3; /* hankaku kana with SO */

    private static byte[] DB;
    private static byte[] DA;
    private static byte[] OBB;
    private static byte[] OBI;
    private static byte[] OBJ;
    private static byte[] OBH;
    private static byte[] DOBD;
    private static byte[] KANJI_1990;

    static {
        try {
            DB = "$B".getBytes( ENC_ASCII);
            DA = "$@".getBytes( ENC_ASCII);
            OBB = "(B".getBytes( ENC_ASCII);
            OBI = "(I".getBytes( ENC_ASCII);
            OBJ = "(J".getBytes( ENC_ASCII);
            OBH = "(H".getBytes( ENC_ASCII);
            DOBD = "$(D".getBytes( ENC_ASCII);
            KANJI_1990 = "&@\033$B".getBytes( ENC_ASCII);
        } catch (UnsupportedEncodingException ex) { /* What? ASCII not supported? */ }
    }

    /**
     * Creates a reader for the input stream with a character encoding guessed by looking at
     * the beginning of the stream.
     *
     * @param in An input stream for Japanese characters.
     * @return Reader for the input stream.
     * @exception IOException if an error occurs while reading from the stream.
     */
    public static InputStreamReader getReader( InputStream in) throws IOException {
        return getReader( in, null);
    }

    /**
     * Creates a reader for the input stream with a character encoding guessed by looking at
     * the beginning of the stream.
     *
     * @param in An input stream for Japanese characters.
     * @param defaultencoding The encoding to use when the input stream encoding could not be
     *                        determined (currently unused).
     * @return Reader for the input stream.
     * @exception IOException if an error occurs while reading from the stream.
     */
    public static InputStreamReader getReader( InputStream in, String defaultencoding)
        throws IOException {
        return getReader( in, defaultencoding, DEFAULT_LOOK_AT_LENGTH);
    }

    /**
     * Creates a reader for the input stream with a character encoding guessed by looking at
     * the beginning of the stream.
     *
     * @param in An input stream for Japanese characters.
     * @param defaultencoding The encoding to use when the input stream encoding could not be
     *                        determined (currently unused).
     * @param lookatlength Length in bytes the method looks at for detection.
     * @return Reader for the input stream.
     * @exception IOException if an error occurs while reading from the stream.
     */
    public static InputStreamReader getReader( InputStream in, String defaultencoding,
                                               int lookatlength) throws IOException {
        if (defaultencoding == null)
            defaultencoding = System.getProperty( "file.encoding");

        byte[] buf = new byte[lookatlength];
        PushbackInputStream pbin = new PushbackInputStream( new BufferedInputStream( in), buf.length);
        String enc = null;
        byte[] data;
        int size = 0;
        int len = -1;
        do {
            len = in.read( buf, size, buf.length-size);
            if (len > 0)
                size += len;
        } while (size<buf.length && len!=-1);
        if (size == 0) // empty file
            return new InputStreamReader( pbin);
        if (size < buf.length) {
            data = new byte[size];
            System.arraycopy( buf, 0, data, 0, size);
        }
        else
            data = buf;
        pbin.unread( data);

        enc = guessEncodingName( data);
        System.err.println( "CharacterEncodingDetector: using " + enc);
        return new InputStreamReader( pbin, enc);
    }

    /**
     * Guesses the lenght in characters encoded using <CODE>encoding</CODE> as byte array.
     *
     * @param dlength Length in bytes of an array of encoded characters.
     * @param encoding Encoding used.
     * @return A wild approximation of the number of encoded Japanese characters.
     */
    public static int guessLength( int dlength, String encoding) {
        encoding = encoding.toUpperCase();
        if (encoding.startsWith( ENC_UTF_8))
            return dlength/3; // Japanese characters take 3 bytes in UTF-8
        else
            return dlength/2;
    }

    /**
     * Detects the character encoding used for a byte array. If the detection is ambiguous,
     * EUC-JP will be preferred.
     *
     * @param An array with encoded Japanese characters.
     * @return The encoding name.
     */
    public static String guessEncodingName( byte[] data) {
        int code = guessEncoding( data);

        String enc;
        if ((code&JIS) > 0)
            enc = ENC_ISO_2022_JP;
        else if ((code&EUC) > 0) // might be ambiguous with shift_jis, in this case prefer euc
            enc = ENC_EUC_JP;
        else if ((code&SJIS) > 0)
            enc = ENC_SHIFT_JIS;
        else if ((code&UTF8) > 0)
            enc = ENC_UTF_8;
        else if ((code&NONASCII) > 0) // assume ISO-8859-1
            enc = ENC_ISO_8859_1;
        else
            enc = ENC_ASCII;

        return enc;
    }
    
    /**
     * Detects the character encoding used for a byte array. The flags set in the return value
     * can be one or more of NONASCII, JIS, EUC, SJIS or JIS8. The detection is not always unambiguous.
     *
     * @param data An array with encoded Japanese characters.
     * @return Result of the detection as a number of flags.
     */
    public static int guessEncoding( byte[] data) {
        int euc, sjis, utf8;
        int utf8charlen = 1; // length of utf8-encoded char in bytes
        boolean jis8;
        int code;
        int i = 1;
        int gsmode = M_ASCII;
        int oldmode;
      
        euc = sjis = utf8 = 1;
        jis8 = true;
        code = 0;
        for ( int s=0; s<data.length; s+=i) {
            int c = byteToUnsignedByte( data[s]);
            i = 1;
            switch (c) {
            case ESC:
                if (gsmode == M_SO)
                    continue;
                oldmode = gsmode;
                if (compare(DB, data, s + 1) || compare(DA, data, s + 1)) {
                    gsmode = M_KANJI; /* kanji */
                    i = DB.length + 1;
                } else if (compare(KANJI_1990, data, s + 1)) {
                    gsmode = M_KANJI; /* kanji 1990 */
                    i = KANJI_1990.length + 1;
                } else if (compare(OBB, data, s + 1) ||
                           compare(OBJ, data, s + 1) || compare(OBH, data, s + 1)) {
                    gsmode = M_ASCII; /* kanji end */
                    i = OBB.length + 1;
                } else if (compare(OBI, data, s + 1)) {
                    gsmode = M_KANJI; /* "ESC(I" */
                    i = OBI.length + 1;
                } else if (compare(DOBD, data, s + 1)) {
                    gsmode = M_GAIJI; /* gaiji */
                    i = DOBD.length + 1;
                } else
                    break;
                code |= JIS;
                if (oldmode != M_ASCII)
                    continue;
                break;

            case SO:
                if (gsmode == M_ASCII) {
                    code |= JIS;
                    gsmode = M_SO;
                    break;
                }
                continue;

            case SI:
                if (gsmode == M_SO) {
                    gsmode = M_ASCII;
                    continue;
                }
                /* fall thru */

            default:
                if (gsmode != M_ASCII)
                    continue;
                break;
            }

            if ((c&0x80) > 0)
                code |= NONASCII;

            switch (euc) {
            case 1:
                /*
                 * EUC first byte.
                 */
                if ((c&0x80) > 0) {
                    if (0xa0 < c && c < 0xff ||
			c == SS2) {
                        euc = 2;
                        break;
                    }
                    if (c == SS3) {
                        euc = 2;
                        break;
                    } else if (c < 0xa0)
                        break;
                    euc = 0;	/* not EUC */
                }
                break;
            case 2:
                /*
                 * EUC second byte or third byte of CS3.
                 */
                if (byteToUnsignedByte( data[s-1]) == SS2) {
                    if (0xa0 < c && c < 0xff) {
                        euc = 1;	/* hankaku kana */
                        break;
                    }
                } else
                    if (0xa0 < c && c < 0xff) {
                        if (byteToUnsignedByte( data[s-1]) != SS3)
                            euc = 1;/* zenkaku */
                        break;
                    }
                euc = 0;		/* not EUC */
                break;
            }

            switch (sjis) {
            case 1:
                /*
                 * shift-JIS first byte.
                 */
                if ((c&0x80) > 0) {
                    if (0xa0 < c && c < 0xe0) {
                        break;	/* hankaku */
                    } else if (c != 0x80 &&
                               c != 0xa0 &&
                               c <= 0xfc) {
                        sjis = 2;	/* zenkaku */
                        jis8 = false;
                        break;
                    }
                    sjis = 0;	/* not SJIS */
                }
                break;
            case 2:
                /*
                 * shift-JIS second byte.
                 */
                if (0x40 <= c && c != 0x7f &&
		    c <= 0xfc)
                    sjis = 1;
                else {
                    sjis = 0;	/* not SJIS */
                }
                break;
            }

            if (utf8 > 0) {
                if (utf8==1 && (c&0x80) > 0) {
                    // First byte of utf8 char. Figure out length.
                    if ((c&0xe0) == 0xc0)
                        utf8charlen = 2; // 2-byte char
                    else if ((c&0xf0) == 0xe0)
                        utf8charlen = 3;
                    else if ((c&0xf8) == 0xf0)
                        utf8charlen = 4;
                    else if ((c&0xfc) == 0xf8)
                        utf8charlen = 5;
                    else if ((c&0xfe) == 0xfc)
                        utf8charlen = 6;
                    else
                        utf8 = 0; // not UTF-8
                    utf8++;
                }
                else if (utf8 > 1) {
                    if ((c&0xc0) != 0x80) // invalid utf8 n-th byte
                        utf8 = 0; // not UTF-8
                    if (utf8 == utf8charlen)
                        utf8 = 1;
                    else
                        utf8++;
                }
            }
        }
        if (euc != 0)
            code |= EUC;
        if (sjis != 0)
            code |= !jis8 ? SJIS : SJIS | JIS8;
        if (utf8 != 0)
            code |= UTF8;

        return code;
    }

    /**
     * Converts the byte value to an int with the value of the 8 bits
     * interpreted as an unsigned byte.
     *
     * @param b The byte value to convert.
     * @return The unsigned byte value of b.
     */
    private final static int byteToUnsignedByte( byte b) {
        return b & 0xff;
    }

    /**
     * Compares two byte arrays for equality.
     *
     * @param d1 First byte array to compare.
     * @param d2 Second byte array to compare.
     * @param o Offset into the second byte array from where comparison starts.
     *          The first byte array will allways start at offset 0.
     * @return <CODE>true</CODE> if the two byte arrays are equal.
     */
    private final static boolean compare( byte[] d1, byte[] d2, int o) {
        for ( int i=0; i<d1.length; i++) {
            if (i+o >= d2.length)
                return false;
            if (d1[i] != d2[o+i])
                return false;
        }

        return true;
    }
} // class CharacterEncodingDetector
