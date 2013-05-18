/*
 * Copyright (C) 2001-2013 Michael Koch (tensberg@gmx.net)
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

package jgloss.dictionary;

import static java.util.logging.Level.INFO;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * The dictionary factory is used to create instances of dictionaries based on a descriptor.
 * <P>
 * Every instance of the {@link Dictionary Dictionary} interface must register an
 * {@link DictionaryFactory#Implementation Implementation}
 * object. The objects are then used to find a matching dictionary implementation for a descriptor.
 * </P>
 *
 * @author Michael Koch
 */
public class DictionaryFactory {

    private static final Logger LOGGER = Logger.getLogger(DictionaryFactory.class.getPackage().getName());

    /**
     * Collection of dictionary implementations.
     */
    private static final List<DictionaryImplementation<?>> IMPLEMENTATIONS = new CopyOnWriteArrayList<>();

    /**
     * Creates a dictionary instance based on the descriptor. The format of the descriptor
     * is dependent on the dictionary implementation. For file-based dictionaries it is usually
     * the path to the dictionary file. The factory will test all registered implementations
     * of dictionaries if they can use the descriptor and use the implementation with the
     * highest confidence to create a new <CODE>Dictionary</CODE> instance.
     *
     * @param descriptor Description of a dictionary instance.
     * @return A new <CODE>Dictionary</CODE> instance based on the descriptor.
     * @exception UnsupportedDescriptorException if the descriptor can not be matched to a known dictionary
     *            format.
     * @exception InstantiationExeption if the creation of the dictionary instance fails.
     */
    public static Dictionary createDictionary( String descriptor)
        throws UnsupportedDescriptorException, DictionaryInstantiationException {
        DictionaryImplementation<?> imp = getImplementation( descriptor);
        return imp.createInstance( descriptor);
    }

    /**
     * Returns a synchronized (thread-safe) dictionary wrapped by the specified
     * dictionary.
     *
     * @param dictionary Dictionary to be wrapped in a synchronized instance.
     * @return Thread-safe synchronized wrapper instance for the given dictionary. If the
     *         given dictionary is a {@link IndexedDictionary}, the returned instance
     *         will also be.
     */
    public static Dictionary synchronizedDictionary(Dictionary dictionary) {
        Dictionary synchronizedDictionary;

        if (dictionary instanceof IndexedDictionary) {
            synchronizedDictionary = synchronizedIndexedDictionary((IndexedDictionary) dictionary);
        } else {
            synchronizedDictionary = new SynchronizedDictionary(dictionary);
        }

        return synchronizedDictionary;
    }

    /**
     * Returns a synchronized (thread-safe) indexed dictionary wrapped by the specified
     * indexed dictionary.
     *
     * @param dictionary Dictionary to be wrapped in a synchronized instance.
     * @return Thread-safe synchronized wrapper instance for the given dictionary.
     */
    public static IndexedDictionary synchronizedIndexedDictionary(IndexedDictionary dictionary) {
        return new SynchronizedIndexedDictionary(dictionary);
    }

    /**
     * Returns the dictionary implementation which best matches the descriptor.
     * The format of the descriptor is dependent on the dictionary implementation.
     * For file-based dictionaries it is usually
     * the path to the dictionary file.
     *
     * @param descriptor Description of a dictionary instance.
     * @return The implementation which best matches the descriptor.
     * @exception UnsupportedDescriptorException if the descriptor does not match a known dictionary format.
     */
    public static DictionaryImplementation<?> getImplementation( String descriptor) throws UnsupportedDescriptorException {
        DictionaryImplementation<?> imp = null;
        float conf = DictionaryImplementation.ZERO_CONFIDENCE;

        // search for implementation with greatest confidence that the descriptor is a
        // dictionary handled by the instance.
        StringBuilder reasons = new StringBuilder(); // reasons for dictionary confidences
        for (DictionaryImplementation<?> ic : IMPLEMENTATIONS) {
            TestResult result = ic.isInstance( descriptor);
            if (result.getConfidence() > conf) {
                imp = ic;
                conf = result.getConfidence();
            }

            if (reasons.length() > 0) {
	            reasons.append('\n');
            }
            reasons.append(ic.getName())
                .append(":")
                .append(result.getReason());
        }

        LOGGER.log(INFO, "dictionary implementation detection results for {0}:\n{1}", new Object[] { descriptor,
                        reasons.toString() });

        if (imp == null) {
	        throw new UnsupportedDescriptorException( reasons.toString());
        }

        return imp;
    }

    /**
     * Registers an implementation of the <CODE>Dictionary</CODE> interface with the
     * <CODE>DictionaryFactory</CODE>. The registered implementation will be used
     * to match descriptors to the dictionary and create new instances of the dictionary.
     *
     * @param imp <CODE>Implementation</CODE> object which describes the implementation.
     */
    public static <T extends Dictionary> void registerImplementation(DictionaryImplementation<T> imp) {
        synchronized (IMPLEMENTATIONS) {
            IMPLEMENTATIONS.add(imp);
        }
    }

    private DictionaryFactory() {
    }
} // class DictionaryFactory
