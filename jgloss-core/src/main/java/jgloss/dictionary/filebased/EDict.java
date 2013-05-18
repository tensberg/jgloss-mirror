/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.dictionary.filebased;

import static java.util.logging.Level.SEVERE;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jgloss.dictionary.DictionaryFactory;
import jgloss.dictionary.DictionaryImplementation;
import jgloss.dictionary.attribute.AttributeValue;
import jgloss.dictionary.attribute.Attributes;

/**
 * Dictionary implementation for dictionaries in EDICT format
 * based on {@link FileBasedDictionary}. For a documentation of the format see
 * <a href="http://www.csse.monash.edu.au/~jwb/edict_doc.html">
 * http://www.csse.monash.edu.au/~jwb/edict_doc.html</a>.
 *
 * @author Michael Koch
 */
public class EDict extends FileBasedDictionary {
	private static final Logger LOGGER = Logger.getLogger(EDict.class.getPackage().getName());

    /**
     * Object describing this implementation of the <CODE>Dictionary</CODE> interface. The
     * Object can be used to register this class with the <CODE>DictionaryFactory</CODE>, or
     * test if a descriptor matches this class.
     *
     * @see DictionaryFactory
     */
	public static final DictionaryImplementation<EDict> IMPLEMENTATION_EUC =
		initImplementation("EDICT", "EUC-JP");
	public static final DictionaryImplementation<EDict> IMPLEMENTATION_UTF8 =
		initImplementation("EDICT (Unicode)", "UTF-8");

    /**
     * Returns a {@link FileBasedDictionaryImplementation FileBasedDictionary.Implementation}
     * which recognizes EUC-JP encoded EDICT dictionaries. Used to initialize the
     * {@link #IMPLEMENTATION implementation} final member because the constructor has to
     * be wrapped in a try/catch block.
     *
     */
    private static DictionaryImplementation<EDict> initImplementation(String name, String encoding) {
        try {
            // Explanation of the pattern:
            // The EDICT format is "word [reading] /translation/translation/.../", with
            // the reading being optional. The dictionary has to start with a line of this form,
            // therefore the pattern starts with a \A and ends with $
            // To distinguish an EDICT dictionary from a SKK dictionary, which uses a similar format,
            // it is tested that the first char in the translation is not a Kanji
            // (InCJKUnifiedIdeographs)
            return new FileBasedDictionaryImplementation<EDict>
                ( name, encoding, true, Pattern.compile
                  ( "\\A\\S+?(\\s\\[.+?\\])?\\s/\\P{InCJKUnifiedIdeographs}.*/$", Pattern.MULTILINE),
                  1.0f, 4096, EDict.class.getConstructor( new Class[] { File.class, String.class })) {
                        @Override
						protected Object[] getConstructorParameters(String descriptor) {
                            return new Object[] { new File(descriptor), this.encoding };
                        }
                  };
        } catch (Exception ex) {
            LOGGER.log(SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

    public EDict( File dicfile, String encoding) throws IOException {
        super(new EDictStructure(), new EDictEntryParser(), dicfile, encoding);
    }

    @Override
	protected void initSupportedAttributes() {
        super.initSupportedAttributes();

        supportedAttributes.putAll( EDictEntryParser.MAPPER.getAttributes());
        supportedAttributes.put( Attributes.PRIORITY, Collections.<AttributeValue> singleton( EDictEntryParser.PRIORITY_VALUE));
    }

    @Override
	public String toString() {
        return "EDICT " + getName();
    }

    /**
     * Escape LF/CR, '/' and all characters not supported by the encoding.
     */
    @Override
	protected boolean escapeChar( char c) {
        // some special characters need escaping
        if (c==10 || c==13 || c=='/') {
	        return true;
        }

        return !characterHandler.canEncode(c);
    }
} // class EDict
