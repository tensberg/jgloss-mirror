/*
 * Copyright (C) 2001-2012 Michael Koch (tensberg@gmx.net)
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
 *
 */

package jgloss.dictionary.filebased;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgloss.dictionary.DictionaryImplementation;
import jgloss.dictionary.DictionaryInstantiationException;
import jgloss.dictionary.TestResult;
import jgloss.util.CharacterEncodingDetector;

/**
 * Generic implementation for file-based dictionaries. The {@link #isInstance(String) isInstance}
 * test reads some bytes from the file to test, converts them to a string using the
 * superclass-supplied encoding and tests it against a regular expression.
 */
public class FileBasedDictionaryImplementation<T extends FileBasedDictionary> implements DictionaryImplementation<T> {
    private static final Logger LOGGER = Logger.getLogger(FileBasedDictionaryImplementation.class.getPackage()
            .getName());

    protected final String name;
    protected final String encoding;
    private final boolean doEncodingTest;
    private final java.util.regex.Pattern pattern;
    private final float maxConfidence;
    private final int lookAtLength;
    private final Constructor<T> dictionaryConstructor;

    /**
     * Creates a new implementation instance for some file based dictionary format.
     *
     * @param name Name of the dictionary format.
     * @param encoding Character encoding used by dictionary files.
     * @param doEncodingTest If <code>true</code>, Use the
     *        {@link CharacterEncodingDetector CharacterEncodingDetector} to guess the encoding
     *        of the tested file and return <code>ZERO_CONFIDENCE</code> if it does not match
     *        the encoding.
     * @param pattern Regular expression to match against the start of a tested file.
     * @param maxConfidence The confidence which is returned when the <code>linePattern</code>
     *                      matches.
     * @param lookAtLength Number of bytes read from the tested file. If the file is shorter
     *                     than the given length, the file will be read completely.
     * @param dictionaryConstructor Constructor used to create a new dictionary instance for
     *        a matching file. The constructor must take a single <code>File</code> as parameter.
     */
    public FileBasedDictionaryImplementation( String name, String encoding, boolean doEncodingTest,
                           java.util.regex.Pattern pattern,
                           float maxConfidence, int lookAtLength,
                           Constructor<T> dictionaryConstructor) {
        this.name = name;
        this.encoding = encoding;
        this.doEncodingTest = doEncodingTest;
        this.pattern = pattern;
        this.maxConfidence = maxConfidence;
        this.lookAtLength = lookAtLength;
        this.dictionaryConstructor = dictionaryConstructor;
    }

    /**
     * Test if the descriptor points to a dictionary file supported by this implementation.
     * The first {@link #lookAtLength lookAtLength} bytes are read from the file pointed to
     * by the descriptor. The byte array is converted to a string using {@link #encoding encoding}
     * and the {@link #pattern pattern} is tested against it. If the pattern matches, the file
     * is accepted and {@link #maxConfidence maxConfidence} is returned.
     */
    @Override
	public TestResult isInstance( String descriptor) {
		float confidence = ZERO_CONFIDENCE;
		String reason = "";

        try {
            File dic = new File( descriptor);
            int headlen = (int) Math.min( lookAtLength, dic.length());
            byte[] buffer = new byte[headlen];
            DataInputStream in = new DataInputStream( new FileInputStream( dic));
            try {
                in.readFully( buffer);
            } finally {
                in.close();
            }

			boolean encodingMatches = false;
            if (doEncodingTest) {
            	String bufferEncoding = CharacterEncodingDetector.guessEncodingName( buffer);

				encodingMatches = encoding.equals(bufferEncoding);
				if (!encodingMatches) {
                    reason = MessageFormat.format(FileBasedDictionary.NAMES.getString("dictionary.reason.encoding"),
                        bufferEncoding, encoding);
                }
            }

			if (!doEncodingTest || encodingMatches) {
              	if (pattern.matcher( new String( buffer, encoding)).find()) {
					confidence = maxConfidence;
					reason = FileBasedDictionary.NAMES.getString("dictionary.reason.ok");
				}
				else {
					reason = FileBasedDictionary.NAMES.getString("dictionary.reason.pattern");
				}
            }
        } catch (IOException ex) {
        	confidence = ZERO_CONFIDENCE;
        	reason = FileBasedDictionary.NAMES.getString("dictionary.reason.read");
        	LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return new TestResult(confidence, reason);
    }

    /**
     * Returns the confidence passed to the constructor.
     */
    @Override
	public float getMaxConfidence() { return maxConfidence; }
    /**
     * Returns the dictionary format name passed to the constructor.
     */
    @Override
	public String getName() { return name; }

    /**
     * Creates a new dictionary instance using
     * {@link FileBasedDictionary.Instance#dictionaryConstructor dictionaryConstructor}.
     * The constructor is passed a <code>File</code> wrapping the <code>descriptor</code> as only
     * argument.
     */
    @Override
	public T createInstance( String descriptor)
        throws DictionaryInstantiationException {
        try {
            return dictionaryConstructor.newInstance
                ( getConstructorParameters(descriptor));
        } catch (InvocationTargetException ex) {
            throw new DictionaryInstantiationException
                ( (Exception) ex.getTargetException());
        } catch (Exception ex) {
            // should never happen
            throw new DictionaryInstantiationException( ex);
        }
    }

    /**
     * Constructs the Object array passed to the Dictionary constructor.
     * Subclasses can override this method if the constructor takes more arguments.
     * Default is a single File object constructed using the descriptor.
     *
     * @param descriptor Dictionary descriptor passed to createInstance
     * @return The Object array used to pass the arguments to the dictionary constructor.
     */
    protected Object[] getConstructorParameters(String descriptor) {
        return new Object[] { new File(descriptor) };
    }

    @Override
	public Class<T> getDictionaryClass( String descriptor) {
        return dictionaryConstructor.getDeclaringClass();
    }
} // class Implementation