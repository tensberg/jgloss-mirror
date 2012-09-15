package jgloss.dictionary;

/**
 * Implemented by classes which contain a dictionary.
 * 
 * @see DictionaryUtils#unwrap(DictionaryWrapper)
 */
public interface DictionaryWrapper {
	/**
	 * 
	 * @return Dictionary which is wrapped by this object.
	 */
	Dictionary getWrappedDictionary();
}
