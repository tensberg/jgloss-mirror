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

package jgloss.parser;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.ExpressionSearchModes;
import jgloss.dictionary.ResultIterator;
import jgloss.dictionary.SearchException;
import jgloss.dictionary.SearchFieldSelection;

public class TextAnnotationCompleter {
    private Dictionary[] dictionaries;
    private Object[] searchParameters;

    public TextAnnotationCompleter( Dictionary[] _dictionaries) {
        dictionaries = _dictionaries;
        searchParameters = new Object[2];
        searchParameters[1] = new SearchFieldSelection( true, true, false, true, false);
    }

    public TextAnnotation complete( TextAnnotation anno) {
        if (anno.getDictionaryFormReading() != null &&
            anno.getTranslation() != null)
            return anno;

        searchParameters[0] = anno.getDictionaryForm();
        for ( int i=0; i<dictionaries.length; i++) try {
            ResultIterator r = dictionaries[i].search( ExpressionSearchModes.EXACT,
                                                       searchParameters);
            while (r.hasNext()) try {
                DictionaryEntry de = r.next();

                // if a reading is given in anno, test if any of the readings of this
                // de matches it
                boolean readingMatches = false;
                if (anno.getDictionaryFormReading() != null) {
                    for ( int j=0; j<de.getReadingAlternativeCount(); j++) {
                        if (anno.getDictionaryFormReading().equals
                            ( de.getReading( j))) {
                            readingMatches = true;
                            break;
                        }
                    }
                    if (!readingMatches)
                        continue;
                }

                // if a translation is given in anno, test if any of the translations of this
                // de matches it
                boolean translationMatches = false;
                if (anno.getTranslation() != null) {
                    for ( int j=0; j<de.getTranslationRomCount(); j++) {
                        for ( int k=0; j<de.getTranslationCrmCount( j); k++) {
                            for ( int l=0; j<de.getTranslationSynonymCount( j, k); l++) {
                                if (anno.getTranslation().equals
                                    ( de.getTranslation( j, k, l))) {
                                    translationMatches = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!translationMatches)
                        continue;
                }

                // use this entry to complete anno
                try {
                    anno.setTranslation( de.getTranslation( 0, 0, 0));
                } catch (IllegalArgumentException ex) {
                    // No translations, just reading. Try next dictionary entry.
                    continue;
                }
                anno.setDictionaryFormReading( de.getReading( 0));
                return anno;
            } catch (SearchException ex) { ex.printStackTrace(); }
        } catch (SearchException ex) { ex.printStackTrace(); }

        // no dictionary entry found, return anno unchanged.
        return anno;
    }
} // class TextAnnotationCompleter
