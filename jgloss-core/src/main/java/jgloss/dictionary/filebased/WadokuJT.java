/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
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

package jgloss.dictionary.filebased;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.DictionaryFactory;
import jgloss.dictionary.DictionaryImplementation;
import jgloss.dictionary.EncodedCharacterHandler;
import jgloss.dictionary.UTF8CharacterHandler;
import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.Attributes;
import jgloss.dictionary.attribute.ReferenceAttributeValue;
import jgloss.dictionary.attribute.WithoutValue;

/**
 * Implementation for dictionaries in WadokuJT.txt format.
 * WadokuJT is a Japanese-German dictionary directed by Ulrich Apel
 * (see <a href="http://www.wadoku.org">http://www.wadoku.org</a>).
 * The WadokuJT.txt file form of the dictionary is maintained by Hans-Joerg Bibiko and available
 * from <a href="http://www.bibiko.com/dlde.htm">http://www.bibiko.com/dlde.htm</a>.
 *
 * @author Michael Koch
 */
public class WadokuJT extends FileBasedDictionary {

	private static final Logger LOGGER = Logger.getLogger(WadokuJT.class.getPackage().getName());

    /**
     * Name of the dictionary format.
     */
    public static final String FORMAT_NAME = "WadokuJT";

    public static final Attribute<WithoutValue> MAIN_ENTRY = new Attributes<WithoutValue>
        ( NAMES.getString( "wadoku.att.main_entry.name"),
          NAMES.getString( "wadoku.att.main_entry.desc"),
          new DictionaryEntry.AttributeGroup[]
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute<ReferenceAttributeValue> MAIN_ENTRY_REF = new Attributes<ReferenceAttributeValue>
        ( NAMES.getString( "wadoku.att.main_entry_ref.name"),
          NAMES.getString( "wadoku.att.main_entry_ref.desc"),
          true, ReferenceAttributeValue.class, Attributes.EXAMPLE_REFERENCE_VALUE,
          new DictionaryEntry.AttributeGroup[]
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute<ReferenceAttributeValue> ALT_READING = new Attributes<ReferenceAttributeValue>
        ( NAMES.getString( "wadoku.att.alt_reading.name"),
          NAMES.getString( "wadoku.att.alt_reading.desc"),
          true, ReferenceAttributeValue.class, Attributes.EXAMPLE_REFERENCE_VALUE,
          new DictionaryEntry.AttributeGroup[]
            { DictionaryEntry.AttributeGroup.GENERAL });

    /**
     * Object describing this implementation of the <CODE>Dictionary</CODE> interface. The
     * Object can be used to register this class with the <CODE>DictionaryFactory</CODE>, or
     * test if a descriptor matches this class.
     *
     * @see DictionaryFactory
     */
    public static final DictionaryImplementation<WadokuJT> IMPLEMENTATION =
        initImplementation();

    /**
     * Returns a {@link FileBasedDictionaryImplementation FileBasedDictionary.Implementation}
     * which recognizes UTF-8 encoded Wadoku dictionaries. Used to initialize the
     * {@link #IMPLEMENTATION implementation} final member because the constructor has to
     * be wrapped in a try/catch block.
     *
     */
    private static DictionaryImplementation<WadokuJT> initImplementation() {
        try {
            // Dictionary entries are of the form
            // japanese|reading|part of speech|translation|comment|reference
            // reading,part of speech, comment and reference may be empty.
            // At least four of the fields must be present in the first line of the file for
            // the match to be successful.
            return new FileBasedDictionaryImplementation<WadokuJT>
                ( FORMAT_NAME, "UTF-8", true, Pattern.compile
                  ( "\\A(.*?\\|){3}.*$", Pattern.MULTILINE),
                  1.0f, 4096, WadokuJT.class.getConstructor( new Class[] { File.class }));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

    public WadokuJT( File dicfile) throws IOException {
        super(new WadokuJTStructure(), new WadokuJTEntryParser(), dicfile, "UTF-8");
    }

    @Override
	protected void initSupportedAttributes() {
        super.initSupportedAttributes();

        supportedAttributes.putAll( WadokuJTEntryParser.MAPPER.getAttributes());
        supportedAttributes.put( Attributes.ABBREVIATION, null);
        supportedAttributes.put( Attributes.GAIRAIGO, null);
        supportedAttributes.put( Attributes.EXPLANATION, null);
        supportedAttributes.put( Attributes.REFERENCE, null);
        supportedAttributes.put( Attributes.SYNONYM, null);
        supportedAttributes.put( Attributes.ANTONYM, null);
        supportedAttributes.put( ALT_READING, null);
        supportedAttributes.put( MAIN_ENTRY, null);
        supportedAttributes.put( MAIN_ENTRY_REF, null);
    }

    protected EncodedCharacterHandler createCharacterHandler() {
        return new UTF8CharacterHandler();
    }

    @Override
	public String toString() {
        return FORMAT_NAME + " " + getName();
    }

    /**
     * Escape all dictionary special characters.
     */
    @Override
	protected boolean escapeChar( char c) {
        switch (c) {
        case 10:
        case 13:
        case '|':
        case ';':
            return true;
        }

        return !characterHandler.canEncode(c);
    }
} // class WadokuJT
