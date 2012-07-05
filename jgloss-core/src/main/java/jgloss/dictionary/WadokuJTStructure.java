package jgloss.dictionary;

import java.nio.ByteBuffer;

public class WadokuJTStructure extends FileBasedDictionaryStructure {

    @Override
	public boolean isFieldStart( ByteBuffer entry, int location, DictionaryEntryField field) {
        if (location == 0) {
	        return true;
        }

        try {
            byte b = entry.get( location-1);
            if (b==';' || b=='|' || b==10 || b==13) {
	            return true;
            }
            if (b == ' ') {
                byte b2 = entry.get( location-2);
                return (b2 == ';' || b2 == ']');
            }
            if (b=='(' && field==DictionaryEntryField.WORD) {
	            // ( followed by a 3-byte encoded character is assumed to be an alternative
                // spelling in the word field
                return true;
            }

            return false;
        } catch (IndexOutOfBoundsException ex) {
            return true;
        }
    }

    @Override
    public boolean isFieldEnd( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( location);
            if (b==';' || b=='|' || b==10 || b==13) {
	            return true;
            }
            if (b == '.') {
                // end of translation if followed by field end marker '|' or new range of meaning " [..."
                byte b2 = entry.get( location+1);
                if (b2 == '|') {
	                return true;
                } else if (b2 == ' ') {
	                return (entry.get( location+2) == '[');
                }
            }
            else if ((b==' ' || b==')') &&
                     field==DictionaryEntryField.WORD) {
	            return true;
            }
            return false;
        } catch (IndexOutOfBoundsException ex) {
            return true;
        }
    }

    @Override
    public DictionaryEntryField moveToNextField( ByteBuffer buf, int character,
                                                    DictionaryEntryField field) {
        if (field == null) {
            // first call to moveToNextField
            // skip first (comment) line
            while (!isEntrySeparator( buf.get()))
			 {
	            ; // buf.get() advances the loop
            }
            return DictionaryEntryField.WORD;
        }

        if (character == '|') {
            if (field==DictionaryEntryField.WORD) {
                field = DictionaryEntryField.READING;
            }
            else if (field==DictionaryEntryField.READING) {
                // skip to translation field
                field = DictionaryEntryField.TRANSLATION;
                byte c;
                do {
                    c = buf.get();
                    if (isEntrySeparator( c)) { // fallback for error in dictionary
                        field = DictionaryEntryField.WORD;
                        break;
                    }
                } while (c != '|');
            }
            else if (field==DictionaryEntryField.TRANSLATION) {
                // skip fields to next entry
                while (!isEntrySeparator( buf.get()))
				 {
	                ; // buf.get() advances the loop
                }
                field = DictionaryEntryField.WORD;
            } else {
	            throw new IllegalArgumentException();
            }
        } else if (character==10 || character==13) {
            // broken dictionary entry; reset for error recovery
            field = DictionaryEntryField.WORD;
        }

        return field;
    }

    @Override
    public DictionaryEntryField getFieldType( ByteBuffer buf, int entryStart, int entryEnd,
                                                 int position) {
        // count field delimiters from location to entry start or end (whatever is closer)
        // note: entryEnd is the first position not to be read
        int fields = 0;
        if (position-entryStart <= entryEnd-position-1) {
            // read from start to location
            buf.position( entryStart);
            while (buf.position() <= position) {
                if (buf.get() == '|') {
	                fields++;
                }
            }
            switch (fields) {
            case 0:
                return DictionaryEntryField.WORD;
            case 1:
                return DictionaryEntryField.READING;
            case 3:
                return DictionaryEntryField.TRANSLATION;
            default:
                return DictionaryEntryField.OTHER;
            }
        }
        else {
            // read from location to end
            buf.position( position);
            while (buf.position() < entryEnd) {
                if (buf.get() == '|') {
	                fields++;
                }
            }
            switch (fields) {
            case 2:
                return DictionaryEntryField.TRANSLATION;
            case 4:
                return DictionaryEntryField.READING;
            case 5:
                return DictionaryEntryField.WORD;
            default:
                return DictionaryEntryField.OTHER;
            }
        }
    }

}
