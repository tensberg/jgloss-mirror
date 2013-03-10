package jgloss.dictionary.filebased;

import static jgloss.dictionary.DictionaryEntryField.READING;
import static jgloss.dictionary.DictionaryEntryField.TRANSLATION;
import static jgloss.dictionary.DictionaryEntryField.WORD;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import jgloss.dictionary.DictionaryEntryField;

class EDictStructure extends FileBasedDictionaryStructure {

    @Override
	public boolean isFieldStart( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( --location);
            if (field == WORD && b == ';'
                            || field == READING && (b == '[' || b == ';')
                            || field == TRANSLATION && b == '/'
                            || b == '\r' || b == '\n') {
	            return true;
            }

            if (field == TRANSLATION) {
                // EDICT translation fields support multiple senses, which are marked
                // as (1),(2)... , and also POS markers in the form (pos), which are all
                // at the start of the translation field

                while (b==' ' && entry.get( --location)==')') {
                    // assume that a POS marker or sense marker was found, and skip it
                    do {
                        b = entry.get( --location);
                    } while (b!='/' && b!='(');
                    if (b=='/' ||
                        b=='(' && (b=entry.get( --location))=='/')
					 {
	                    return true;
                    // if b is now anything other than a space, in which case there could be
                    // another marker, the while loop will terminate and false will be returned
                    }
                }
            }

            return false;
        } catch (IndexOutOfBoundsException ex) {
            return true; // start of entry buffer
        }
    }

    @Override
    public boolean isFieldEnd( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( location);
            return field == WORD && (b == ' ' || b == ';' || b == '(')
                            || field == READING && (b == ']' || b == ';')
                            || field == TRANSLATION && b == '/'
                            || b == '\r' || b == '\n';
        } catch (IndexOutOfBoundsException ex) {
            return true; // end of entry buffer
        }
    }

    @Override
    public DictionaryEntryField moveToNextField( ByteBuffer buf, int character,
                                                    DictionaryEntryField field) {
        if (field == null) {
            // first call to moveToNextField
            return WORD;
        }

        if (field == WORD && character == ' ') {
            byte b = buf.get();
            if (b == '[') {
                field = READING;
            } else {
                field = TRANSLATION;
            }
        } else if (field == READING && character == ']') {
            buf.get(); // skip the ' '
            buf.get(); // skip the '/'
            field = TRANSLATION;
        } else if (field == TRANSLATION && character == '/') {
            byte nextChar = buf.get();
            if (nextChar == '\r' || nextChar == '\n') {
                field = WORD;
            } else {
                // unread the last character which is part of the next
                // translation
                buf.position(buf.position() - 1);
            }
        } else if (character == '\r' || character == '\n') {
            field = WORD;
        }

        return field;
    }

    @Override
    public DictionaryEntryField getFieldType( ByteBuffer buf, int entryStart, int entryEnd,
                                                 int location) {
        buf.position( location);
        byte b;
        try {
            do {
                b = buf.get();
            } while (b != ' ' && b != '/' && b != ']' && b != '\r' && b != '\n');

            if (b == '/') {
                return TRANSLATION;
            } else if (b == ']') {
                return READING;
            } else {
                // word or translation
                b = buf.get();
                if (b == '[') {
                    return WORD;
                } else if (b == '/') {
                    if (buf.get(buf.position() - 3) == ']') {
                        return READING;
                    } else {
                        return WORD;
                    }
                } else {
                    return TRANSLATION;
                }
            }
        } catch (BufferUnderflowException ex) {
            // reached end of entry, must be translation
            return TRANSLATION;
        }
    }


}
