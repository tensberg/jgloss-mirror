/*
 * Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
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

package jgloss.ui;

import jgloss.*;
import jgloss.dictionary.*;

import java.io.*;

/**
 * An editable EDICT that presents itself with the name "User dictionary".
 *
 * @author Michael Koch
 */
public class UserDictionary extends EditableEDict {
    /**
     * Implementation that will create a single instance of a user dictionary
     * for a file name.
     */
    public static class Implementation implements DictionaryFactory.Implementation {
        private String dicfile;

        /**
         * Creates an implementation which creates a user dictionary with a specific
         * file name.
         */
        public Implementation( String dicfile) {
            this.dicfile = dicfile;
        }

        /**
         * Test if the descriptor points to the specific user dictionary. This is
         * true if the descriptor is a file name and the file matches the dictionary
         * file, even if the dictionary file does not already exist.
         */
        public float isInstance( String descriptor) {
            if (new File( descriptor).equals( new File( dicfile)))
                return getMaxConfidence();

            return DictionaryFactory.Implementation.ZERO_CONFIDENCE;
        }

        public float getMaxConfidence() {
            return EditableEDict.implementation.getMaxConfidence()*2;
        }

        public String getName() {
            return JGloss.messages.getString( "userdictionary.implementation.name");
        }

        public String getDescriptor() {
            return dicfile;
        }

        public Dictionary createInstance( String descriptor) 
            throws DictionaryFactory.InstantiationException {
            try {
                return new UserDictionary( descriptor);
            } catch (IOException ex) {
                throw new DictionaryFactory.InstantiationException( ex);
            }
        }
    } // class Implementation

    protected UserDictionary( String dicfile) throws IOException {
        super( dicfile);
    }

    public String toString() {
        return JGloss.messages.getString( "userdictionary.name");
    }

    public String getName() {
        return toString();
    }
} // class UserDictionary
