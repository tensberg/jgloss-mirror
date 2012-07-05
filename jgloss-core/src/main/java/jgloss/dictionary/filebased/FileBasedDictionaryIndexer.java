/*
 * Copyright (C) 2002-2012 Michael Koch (tensberg@gmx.net)
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

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.logging.Logger;

import jgloss.dictionary.BinarySearchIndexBuilder;
import jgloss.dictionary.CharacterClass;
import jgloss.dictionary.DictionaryEntryField;
import jgloss.dictionary.EncodedCharacterHandler;
import jgloss.dictionary.Index;
import jgloss.dictionary.IndexBuilder;
import jgloss.dictionary.IndexContainer;
import jgloss.dictionary.IndexException;
import jgloss.dictionary.Indexable;

class FileBasedDictionaryIndexer {
	
	private static final Logger LOGGER = Logger.getLogger(FileBasedDictionaryIndexer.class.getPackage().getName());

	private final Indexable indexable;
	
	private final FileBasedDictionaryStructure structure;
	
	private final MappedByteBuffer dictionary;

	private final EncodedCharacterHandler characterHandler;
	
	FileBasedDictionaryIndexer(Indexable indexable, FileBasedDictionaryStructure structure, MappedByteBuffer dictionary, EncodedCharacterHandler characterHandler) {
		this.indexable = indexable;
		this.structure = structure;
		this.dictionary = dictionary;
		this.characterHandler = characterHandler;
	}
	
	void buildIndex(IndexContainer indexContainer, Index index) {
        IndexBuilder builder = new BinarySearchIndexBuilder(index.getType());
        builder.startBuildIndex(indexContainer, indexable);
        boolean commit = false;
        try {
        	addIndexTerms(builder, dictionary);
            commit = true;
        } catch (IOException ex) {
        	throw new IndexException(ex);
        } finally {
            builder.endBuildIndex(commit);
        }

	}
	
    /**
     * Adds all indexable terms in the dictionary to the index builder.
     *
     * @return The number of index entries created.
     */
    private int addIndexTerms( IndexBuilder builder, MappedByteBuffer dictionary) throws IOException, IndexException {
        // Indexes all terms in word, reading and translation fields.
        // Index term boundaries are determined using characterHandler.getCharacterClass():
        // if the character classes of two adjacent characters differ they are assumed to
        // belong to two different terms.
        // For kanji characters, each kanji in a term is indexed.
        // For kana characters, whole terms are indexed.
        // For romaji, terms of length >= 3 are indexed.

        int indexsize = 0;
        dictionary.position( 0);
        ArrayList<Integer> termStarts = new ArrayList<Integer>( 25);

        int previousTerm = -1;

        DictionaryEntryField field = structure.moveToNextField( dictionary, 0, null);
        while (dictionary.remaining() > 0) {
        	try {
        		boolean inWord = false;
        		int c;
        		CharacterClass clazz;
        		int termStart;
        		DictionaryEntryField termField;
        		// find first character of indexable term
        		do {
        			termStart = dictionary.position();
        			c = characterHandler.readCharacter( dictionary);
        			clazz = characterHandler.getCharacterClass( c, inWord);
        			field = structure.moveToNextField( dictionary, c, field);
        		} while (clazz == CharacterClass.OTHER && dictionary.remaining() > 0);
        		if (dictionary.remaining() == 0) {
        			break;
        		}
        		
        		termStarts.clear();
        		termStarts.add( Integer.valueOf( termStart));
        		termField = field;
        		if (clazz == CharacterClass.ROMAN_WORD) {
	                inWord = true;
                }

        		int termLength = 1; // term length in number of characters
        		// find end of term
        		int termEnd;
        		CharacterClass clazz2;
        		do {
        			termLength++;
        			termEnd = dictionary.position(); // first position not part of term
        			c = characterHandler.readCharacter( dictionary);
        			clazz2 = characterHandler.getCharacterClass( c, inWord);

        			// for kanji terms, each kanji in the term is indexed
        			if (clazz == CharacterClass.KANJI && clazz2==clazz) {
	                    termStarts.add( Integer.valueOf( termEnd));
                    }
        		} while (clazz2 == clazz);

        		// add the term to the index
        		if (clazz==CharacterClass.KANJI ||
        						clazz==CharacterClass.HIRAGANA ||
        						clazz==CharacterClass.KATAKANA ||
        						clazz==CharacterClass.ROMAN_WORD && termLength >= 3) {
        			for ( int i=0; i<termStarts.size(); i++) {
        				termStart = termStarts.get( i).intValue();

        				// debug index creation
        				if (termStart <= previousTerm) {
	                        LOGGER.warning( "Warning: possible duplicate index entry");
                        }
        				previousTerm = termStart;
        				// debug index creation

        				if (builder.addEntry( termStart, termEnd-termStart, termField)) {
	                        indexsize++;
                        }
        			}
        		}

        		if (clazz2 != CharacterClass.OTHER) {
        			// unread the last character, because it may be the start of the
        			// next indexable term
        			dictionary.position( termEnd);
        		}
        		else { // char may be a field seperator
        			field = structure.moveToNextField( dictionary, c, field);
        		}
        	} catch (BufferUnderflowException ex) {
        		throw new IndexException(ex);
        	} catch (IndexOutOfBoundsException ex) {
        		throw new IndexException(ex);
        	}
        	// end of dictionary file
        }

        return indexsize;
    }


}
