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
 */

package jgloss.ui.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jgloss.ui.download.schema.Dictionary;

/**
 * Unpacks the dictionary file from an already downloaded {@link Dictionary} archive.
 */
interface DictionaryUnpacker {
    
    /**
     * Unpack the dictionary file from the given dictionary download. Only the dictionary file itself
     * is unpacked, the other files are ignored. The archive type and dictionary file name is read
     * from the dictionary parameter.
     * 
     * @param dictionary describes the dictionary archive type and file name.
     * @param sourceStream stream from which the dictionary archive can be read.
     * @param outStream stream in which the dictionary file data is written.
     * @throws IOException if reading or writing the dictionary archives fails.
     */
    void unpack(Dictionary dictionary, InputStream sourceStream, OutputStream outStream) throws IOException;
}
