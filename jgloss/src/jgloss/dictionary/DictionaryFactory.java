/*
 * Copyright (C) 2001,2002 Michael Koch (tensberg@gmx.net)
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

import java.util.*;

/**
 * The dictionary factory is used to create instances of dictionaries based on a descriptor.
 * <P>
 * Every instance of the <CODE>Dictionary</CODE> interface must register an <CODE>Implementation</CODE>
 * object. The objects are then used to find a matching dictionary implementation for a descriptor.
 * </P>
 *
 * @author Michael Koch
 * @see Dictionary
 * @see DictionaryFactory.Implementation
 */
public abstract class DictionaryFactory {
    /**
     * Base class for other <CODE>DictionaryFactory</CODE> exceptions.
     */
    public static class Exception extends java.lang.Exception {
        public Exception() {}
        public Exception( String message) {
            super( message);
        }
        public Exception( java.lang.Exception root) { super( root); }
        public Exception( String message, java.lang.Exception root) { super( message, root); }
    } // class Exception

    /**
     * Thrown when a descriptor does not match any known dictionary format.
     */
    public static class NotSupportedException extends DictionaryFactory.Exception {
        public NotSupportedException() {}
        public NotSupportedException( String message) {
            super( message);
        }
    } // class NotSupportedException

    /**
     * Thrown when the instantiation of a dictionary failed.
     */
    public static class InstantiationException extends DictionaryFactory.Exception {
        public InstantiationException() {
            super();
        }
        public InstantiationException( String message) {
            super( message);
        }
        public InstantiationException( java.lang.Exception rootCause) {
            super( rootCause);
        }
        public InstantiationException( String message, java.lang.Exception rootCause) {
            super( message, rootCause);
        }
    } // class InstantiationException

    /**
     * Map from <CODE>Class</CODE> objects of dictionary classes to <CODE>Implementation</CODE>
     * objects describing the classes.
     */
    private static Map implementations = new HashMap( 10);

    /**
     * Creates a dictionary instance based on the descriptor. The format of the descriptor
     * is dependent on the dictionary implementation. For file-based dictionaries it is usually
     * the path to the dictionary file. The factory will test all registered implementations
     * of dictionaries if they can use the descriptor and use the implementation with the
     * highest confidence to create a new <CODE>Dictionary</CODE> instance.
     *
     * @param descriptor Description of a dictionary instance.
     * @return A new <CODE>Dictionary</CODE> instance based on the descriptor.
     * @exception NotSupportedException if the descriptor can not be matched to a known dictionary
     *            format.
     * @exception InstantiationExeption if the creation of the dictionary instance fails.
     */
    public static Dictionary createDictionary( String descriptor) 
        throws NotSupportedException, InstantiationException {
        Implementation imp = getImplementation( descriptor);
        return imp.createInstance( descriptor);
    }
    
    /**
     * Returns the dictionary implementation which best matches the descriptor. 
     * The format of the descriptor is dependent on the dictionary implementation. 
     * For file-based dictionaries it is usually
     * the path to the dictionary file.
     *
     * @param descriptor Description of a dictionary instance.
     * @return The implementation which best matches the descriptor.
     * @exception NotSupportedException if the descriptor does not match a known dictionary format.
     */
    public static Implementation getImplementation( String descriptor) throws NotSupportedException {
        Implementation imp = null;
        float conf = Implementation.ZERO_CONFIDENCE;

        // search for implementation with greatest confidence that the descriptor is a
        // dictionary handled by the instance.
        for ( Iterator i=implementations.entrySet().iterator(); i.hasNext(); ) {
            Implementation ic = (Implementation) ((Map.Entry) i.next()).getValue();
            float cc = ic.isInstance( descriptor);
            if (cc > conf) {
                imp = ic;
                conf = cc;
            }
        }

        if (imp == null)
            throw new NotSupportedException( descriptor);

        return imp;
    }

    /**
     * Returns the <CODE>Implementation</CODE> registered for a dictionary class.
     */
    public static Implementation getImplementation( Class dictionary) {
        return (Implementation) implementations.get( dictionary);
    }

    /**
     * Registers an implementation of the <CODE>Dictionary</CODE> interface with the
     * <CODE>DictionaryFactory</CODE>. The registered implementation will be used
     * to match descriptors to the dictionary and create new instances of the dictionary.
     *
     * @param dictionary <CODE>Class</CODE> of an implementation of <CODE>Dictionary</CODE>.
     * @param imp <CODE>Implementation</CODE> object which describes the implementation.
     */
    public static void registerImplementation( Class dictionary, Implementation imp) {
        implementations.put( dictionary, imp);
    }

    /**
     * Interface which is used to match descriptors to <CODE>Dictionary</CODE> implementations.
     *
     * @autor Michael Koch
     * @see Dictionary
     */
    public interface Implementation {
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
         * @return Confidence that the descriptor describes an instance of the <CODE>Dictionary</CODE>
         *         implementation.
         */
        float isInstance( String descriptor);
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
         * @exception DictionaryFactory.InstantiationException if the instantiation of the dictionary failed.
         */
        Dictionary createInstance( String descriptor) throws DictionaryFactory.InstantiationException;
        /**
         * Returns the class of the dictionary which would be created for this descriptor by
         * {@link #createInstance(String) createInstance}.
         */
        Class getDictionaryClass( String descriptor);
    } // interface Implementation
} // class DictionaryFactory
