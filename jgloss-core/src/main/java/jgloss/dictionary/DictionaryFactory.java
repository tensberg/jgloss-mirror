/*
 * Copyright (C) 2001-2004 Michael Koch (tensberg@gmx.net)
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

import java.util.HashSet;
import java.util.Set;

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
public abstract class DictionaryFactory {
    /**
     * Base class for other <CODE>DictionaryFactory</CODE> exceptions.
     */
    public static class DictionaryFactoryException extends Exception {
        private static final long serialVersionUID = 1L;

        public DictionaryFactoryException() {}
        public DictionaryFactoryException( String message) {
            super( message);
        }
        public DictionaryFactoryException( Exception root) { super( root); }
        public DictionaryFactoryException( String message, java.lang.Exception root) { super( message, root); }
    } // class Exception

    /**
     * Thrown when a descriptor does not match any known dictionary format.
     */
    public static class NotSupportedException extends DictionaryFactoryException {
        private static final long serialVersionUID = 1L;

        public NotSupportedException() {}
        public NotSupportedException( String message) {
            super( message);
        }
    } // class NotSupportedException

    /**
     * Thrown when the instantiation of a dictionary failed.
     */
    public static class InstantiationException extends DictionaryFactory.DictionaryFactoryException {
        private static final long serialVersionUID = 1L;

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
     * Collection of dictionary implementations.
     */
    private static Set<Implementation<?>> implementations = new HashSet<Implementation<?>>( 10);

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
        Implementation<?> imp = getImplementation( descriptor);
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
    public static Implementation<?> getImplementation( String descriptor) throws NotSupportedException {
        Implementation<?> imp = null;
        float conf = Implementation.ZERO_CONFIDENCE;

        // search for implementation with greatest confidence that the descriptor is a
        // dictionary handled by the instance.
        String reasons = ""; // reasons for dictionary confidences
        for (Implementation<?> ic : implementations) {
            TestResult result = ic.isInstance( descriptor);
            if (result.getConfidence() > conf) {
                imp = ic;
                conf = result.getConfidence();
            }
            
            if (reasons.length() > 0) {
	            reasons += '\n';
            }
            reasons += ic.getName() + ":" + result.getReason();
        }

        if (imp == null) {
	        throw new NotSupportedException( reasons);
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
    public static <T extends Dictionary> void registerImplementation(Implementation<T> imp) {
        implementations.add(imp);
    }

    /**
     * Interface which is used to match descriptors to <CODE>Dictionary</CODE> implementations.
     *
     * @author Michael Koch
     * @see Dictionary
     */
    public interface Implementation<T extends Dictionary> {
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
         * @exception DictionaryFactory.InstantiationException if the instantiation of the dictionary failed.
         */
        T createInstance( String descriptor) throws DictionaryFactory.InstantiationException;
        /**
         * Returns the class of the dictionary which would be created for this descriptor by
         * {@link #createInstance(String) createInstance}.
         */
        Class<? extends T> getDictionaryClass( String descriptor);
    } // interface Implementation
    
    /**
     * Test result for {@link DictionaryFactory#Implementation}.
     * 
     * @author Michael Koch
     */
    public static class TestResult {
    	private final float confidence;
    	private final String reason;
    	
    	public TestResult(float _confidence, String _reason) {
    		this.confidence = _confidence;
    		this.reason = _reason;
    	}
    	
    	/**
    	 * Confidence of the descriptor pointing to an instance of this dictionary type.
    	 * The higher the number, the greater the confidence.
    	 * Returns {@link #ZERO_CONFIDENCE ZERO_CONFIDENCE} if the descriptor does not
    	 * match this type.
    	 */
    	public float getConfidence() { return confidence; }
    	/**
    	 * Short description of how the confidence was calculated.
    	 */
    	public String getReason() { return reason; }
    }
} // class DictionaryFactory
