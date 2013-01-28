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
 */

package jgloss.ui.download;

import java.util.List;
import java.util.Locale;

import jgloss.ui.download.schema.Dictionary.Description;
import jgloss.ui.download.schema.Download;

/**
 * Utility methods for working with the dictionaries xml file schema classes.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
class DictionarySchemaUtils {
    /**
     * Returns the description matching the language of the given locale or a default description if
     * no matching description is found. The default description is the first in the list of descriptions
     * 
     * @param descriptions List of descriptions from which the matching description string is returned.
     * @return Description matching the current language or a default description. Empty string if the description list is empty.
     */
    public static String getDescriptionForLocale(List<Description> descriptions, Locale locale) {
        String descriptionForLocale = "";
        String language = locale.getLanguage();
        
        for (Description description : descriptions) {
            if (description.getLang().equals(language)) {
                descriptionForLocale = description.getValue();
                break;
            } else if (descriptionForLocale.isEmpty()) {
                descriptionForLocale = description.getValue();
            }
        }
        
        return descriptionForLocale;
    }
    
    /**
     * Factory method which creates an unpacker for the given dictionary download.
     */
    public static DictionaryUnpacker createUnpackerFor(Download dictionaryDownload) {
        String compression = dictionaryDownload.getCompression();
        switch (compression) {
        case "gzip":
            return new GZipUnpacker();
            
        case "zip":
            return new ZipUnpacker();
            
        default:
            throw new IllegalArgumentException("unsupported compression type " + compression);
        }
    }
    
    private DictionarySchemaUtils() {
    }
}
