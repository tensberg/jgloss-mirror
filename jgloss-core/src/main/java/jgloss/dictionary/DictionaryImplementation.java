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
 * $Id$
 *
 */

package jgloss.dictionary;


/**
 * Interface which is used to match descriptors to <CODE>Dictionary</CODE> implementations.
 *
 * @author Michael Koch
 * @see Dictionary
 */
public interface DictionaryImplementation<T extends Dictionary> {
    /**
     * Confidence value meaning that the descriptor does not descripe a dictionary in the
     * format described by this implementation.
     */
    float ZERO_CONFIDENCE = 0.0f;

    /**
     * Test if the descriptor points to a dictionary in a format supported by the
     * <CODE>Dictionary</CODE> implementation. The format of the descriptor is dependent
     * on the dictionary implementation. For file-based dictionaries it is usually the
     * path to the file.
     * <P>
     * The returned value is the confidence.
     * If the implementation is certain that the descriptor does not match this
     * implementation, it must return <CODE>ZERO_CONFIDENCE</CODE>; if it is certain
     * that it matches it must return <CODE>getMaxConfidence()</CODE>. The
     * <CODE>DictionaryFactory</CODE> will use the dictionary with the largest
     * confidence to create a new instance for a descriptor.
     * </P><P>
     * The value of <CODE>getMaxConfidence</CODE> is not limited. This can be used
     * for specialized dictionary formats. For example, if every file in the format
     * of Dictionary A is also in the format of Dictionary B, but not every file
     * of B is in the format of A, B can return a confidence &gt; <CODE>A.getMaxConfidence()</CODE>
     * for files in format B to force opening them with B instead of A.
     * </P>
     *
     * @param descriptor Descriptor of the dictionary to test.
     * @return Result of the test (confidence and reason).
     */
    TestResult isInstance( String descriptor);
    /**
     * Returns the maximum confidence value used by this implementation.
     */
    float getMaxConfidence();

    /**
     * Returns the name of the dictionary format described by this implementation.
     */
    String getName();

    /**
     * Creates an instance of the dictionary described by this implementation.
     * The descriptor should have already been tested with <CODE>isInstance</CODE>
     * for compatibility.
     *
     * @param descriptor Descriptor describing the instance. For file-based dictionaries
     *                   this is usually the path to the file.
     * @exception DictionaryInstantiationException if the instantiation of the dictionary failed.
     */
    T createInstance( String descriptor) throws DictionaryInstantiationException;
    /**
     * Returns the class of the dictionary which would be created for this descriptor by
     * {@link #createInstance(String) createInstance}.
     */
    Class<? extends T> getDictionaryClass( String descriptor);
} // interface Implementation