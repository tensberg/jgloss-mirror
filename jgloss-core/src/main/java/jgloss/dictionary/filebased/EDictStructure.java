package jgloss.dictionary.filebased;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import jgloss.dictionary.DictionaryEntryField;

class EDictStructure extends FileBasedDictionaryStructure {

    @Override
	public boolean isFieldStart( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( --location);
            if (field==DictionaryEntryField.READING && b=='['
                || field==DictionaryEntryField.TRANSLATION && b=='/'
                || b==10 || b==13) {
	            return true;
            }

            if (field == DictionaryEntryField.TRANSLATION) {
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
            return (field==DictionaryEntryField.WORD && b==' ' ||
                field==DictionaryEntryField.READING && b==']' ||
                field==DictionaryEntryField.TRANSLATION && b=='/'
                || b==10 || b==13);
        } catch (IndexOutOfBoundsException ex) {
            return true; // end of entry buffer
        }
    }

    @Override
    public DictionaryEntryField moveToNextField( ByteBuffer buf, int character,
                                                    DictionaryEntryField field) {
        if (field == null) {
            // first call to moveToNextField
            return DictionaryEntryField.WORD;
        }

        if (field==DictionaryEntryField.WORD && character==' ') {
            byte b = buf.get();
            if (b == '[') {
	            field = DictionaryEntryField.READING;
            } else {
	            field = DictionaryEntryField.TRANSLATION;
            }
        } else if (field==DictionaryEntryField.READING && character==']') {
            buf.get(); // skip the ' '
            field = DictionaryEntryField.TRANSLATION;
        } else if (character==10 || character==13) {
            field = DictionaryEntryField.WORD;
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
            } while (b!=' ' && b!='/' && b!=']');
            if (b == '/') {
	            return DictionaryEntryField.TRANSLATION;
            } else if (b == ']') {
	            return DictionaryEntryField.READING;
            } else {
                // word or translation
                b = buf.get();
                if (b=='/' || b=='[') {
	                return DictionaryEntryField.WORD;
                } else {
	                return DictionaryEntryField.TRANSLATION;
                }
            }
        } catch (BufferUnderflowException ex) {
            // reached end of entry, must be translation
            return DictionaryEntryField.TRANSLATION;
        }
    }


}
