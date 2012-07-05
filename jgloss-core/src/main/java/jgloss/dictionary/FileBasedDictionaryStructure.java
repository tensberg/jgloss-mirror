package jgloss.dictionary;

import java.nio.ByteBuffer;

/**
 * Describes the structure of a concrete {@link FileBasedDictionary} type.
 */
public abstract class FileBasedDictionaryStructure {
    /**
     * Test if the byte is the separator mark for two entries. This implementation uses
     * ASCII lf (10) and cr (13) bytes as entry separator.
     *
     * @param <CODE>true</CODE> if the byte separates two entries.
     */
	public boolean isEntrySeparator(byte c) {
		return (c==10 || c==13);
	}
	
    /**
     * Skip to the next indexable field. This method is called at index
     * creation time before any character is read from the dictionary and after each read character.
     * It can be used to skip entry fields which should not be indexed.
     *
     * @param buf Skip entries in this buffer by moving the current position of the buffer.
     * @param character The last character read from the buffer, or 0 at the first invocation. The
     *                  character format is dependent on
     *                  {@link EncodedCharacterHandler#readCharacter(ByteBuffer) readCharacter}
     *                  and not neccessaryly unicode.
     * @param field The current field.
     * @return The type of the field the method moved to.
     */
    public abstract DictionaryEntryField moveToNextField( ByteBuffer buf, int character,
                                                             DictionaryEntryField field);

    /**
     * Return the type of the entry field at the given location.
     */
    public abstract DictionaryEntryField getFieldType( ByteBuffer buf, int entryStart,
                                                          int entryEnd, int location);

    /**
     * Test if the character at the given location is the first in an entry field. If the dictionary
     * supports several words, readings or translations in one entry, each counts as its own field.
     *
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    public abstract boolean isFieldStart( ByteBuffer entry, int location, DictionaryEntryField field);
    
    /**
     * Test if the character at the given location is the last in an entry field. If the dictionary
     * supports several words, readings or translations in one entry, each counts as its own field.
     *
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    public abstract boolean isFieldEnd( ByteBuffer entry, int location, DictionaryEntryField field);

}
