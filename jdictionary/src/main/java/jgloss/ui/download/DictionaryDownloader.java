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

import static java.lang.Math.min;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static jgloss.JGloss.MESSAGES;
import static jgloss.ui.download.DictionarySchemaUtils.createUnpackerFor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import jgloss.ui.Dictionaries;
import jgloss.ui.download.schema.Dictionary;
import jgloss.ui.download.schema.Download;
import jgloss.ui.util.JGlossWorker;

/**
 * Downloads and optionally unpacks a dictionary and adds to the list of dictionaries.
 */
class DictionaryDownloader extends JGlossWorker<File, Void> {

    private static final Logger LOGGER = Logger.getLogger(DictionaryDownloader.class.getPackage().getName());

    private final Dictionary dictionary;

    private final File dictionaryDir;

    private final Dictionaries dictionaries;

    private File dictionaryFile;

    DictionaryDownloader(Dictionary dictionary) {
        this(dictionary, Dictionaries.getDictionariesDir(), Dictionaries.getInstance());
    }

    DictionaryDownloader(Dictionary dictionary, File dictionaryDir, Dictionaries dictionaries) {
        this.dictionary = dictionary;
        this.dictionaryDir = dictionaryDir;
        this.dictionaries = dictionaries;
        setMessage(MESSAGES.getString("dictionarydownloader.downloading", dictionary.getDownload().getUrl()));
    }

    @Override
    protected File doInBackground() throws Exception {
        Download download = dictionary.getDownload();

        prepareDictionaryDir();
        dictionaryFile = new File(dictionaryDir, download.getDictionaryFile());
        if (dictionaryFile.exists()) {
            LOGGER.log(INFO, "dictionary file {0} already exists, skipping download", dictionaryFile.getAbsolutePath());
            return dictionaryFile;
        }

        File tmpDictionaryFile = File.createTempFile(download.getDictionaryFile(), null, dictionaryDir);
        tmpDictionaryFile.deleteOnExit();
        URL url = new URL(download.getUrl());
        URLConnection connection = url.openConnection();

        try {
            download(connection, tmpDictionaryFile);
            unpackDictionary(tmpDictionaryFile);
            setProgress(100);

            return dictionaryFile;
        } finally {
            deleteTempFile(tmpDictionaryFile);
        }
    }

    private void unpackDictionary(File tmpDictionaryFile) throws IOException {
        setMessage(MESSAGES.getString("dictionarydownloader.unpacking", dictionaryFile.getName()));
        try (
             InputStream in = new BufferedInputStream(new FileInputStream(tmpDictionaryFile));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dictionaryFile))
            ) {
            DictionaryUnpacker unpacker = createUnpackerFor(dictionary.getDownload());
            unpacker.unpack(dictionary, in, out);
        }
    }

    private void download(URLConnection connection, File tmpDictionaryFile) throws IOException {
        long position = 0;
        long transferred;
        long contentLength = connection.getContentLengthLong();
        Double contentLengthMb = toMegabytes(contentLength);

        try (
             ReadableByteChannel in = Channels.newChannel(connection.getInputStream());
             FileChannel out = FileChannel.open(tmpDictionaryFile.toPath(), WRITE)
             ) {
            do {
                transferred = out.transferFrom(in, position, 16000);
                position += transferred;
                if (contentLength > 0) {
                    setProgress(min(((int) (position * 100 / contentLength)), 100));
                    setMessage(MESSAGES.getString("dictionarydownloader.downloadprogress", toMegabytes(position), contentLengthMb));
                }
            } while (transferred > 0);
        }
    }

    private static Double toMegabytes(long value) {
        return Double.valueOf(value / ((double) (1024 * 1024)));
    }

    private void deleteTempFile(File tmpDictionaryFile) {
        try {
            Files.deleteIfExists(tmpDictionaryFile.toPath());
        } catch (IOException ex) {
            LOGGER.log(WARNING, "failed to delete temporary file", ex);
        }
    }

    private void prepareDictionaryDir() throws FileNotFoundException {
        if (!dictionaryDir.exists()) {
            LOGGER.log(INFO, "creating dictionary directory {0}", dictionaryDir.getAbsolutePath());
            if (!dictionaryDir.mkdirs()) {
                throw new FileNotFoundException("could not create dictionary directory " + dictionaryDir.getAbsolutePath());
            }
        }
    }

    @Override
    protected void done() {
        try {
            dictionaries.addDictionaryFiles(new File[] { get() });
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.log(SEVERE, "failed to download dictionary from " + dictionary.getDownload().getUrl(), ex);
        }
    }

}
