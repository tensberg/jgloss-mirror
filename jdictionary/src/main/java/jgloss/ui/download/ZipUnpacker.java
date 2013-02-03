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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jgloss.ui.download.schema.Dictionary;
import jgloss.util.IOUtils;

/**
 * Dictionary unpacker which unpacks a single dictionary file from the Zip archive.
 */
class ZipUnpacker implements DictionaryUnpacker {

    @Override
    public void unpack(Dictionary dictionary, InputStream sourceStream, OutputStream outStream) throws IOException {
        String dictionaryFile = dictionary.getDownload().getDictionaryFile();
        ZipInputStream zipIn = new ZipInputStream(sourceStream);

        boolean dictionaryFileFound = false;
        ZipEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {
            if (filenameMatches(dictionaryFile, entry.getName())) {
                dictionaryFileFound = true;
                IOUtils.copy(zipIn, outStream);
                break;
            }
        }

        if (!dictionaryFileFound) {
            throw new FileNotFoundException("no file " + dictionaryFile + " found in zip archive");
        }
    }

    private static boolean filenameMatches(String filename, String pathname) {
        return pathname.equals(filename) || pathname.endsWith("/" + filename);
    }

}
