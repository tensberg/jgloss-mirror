package jgloss.dictionary;

/**
 * Static helper methods for working with dictionaries.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
public class DictionaryUtils {
	/**
	 * Returns the dictionary at the root of the wrapper containment hierarchy.
	 * If the wrapped dictionary is itself a {@link DictionaryWrapper}, the method will recurse.
	 * 
	 * @param wrapper Wrapper containing a dictionary.
	 * @return The wrapped dictionary.
	 */
	public static Dictionary unwrap(DictionaryWrapper wrapper) {
		Dictionary dictionary = wrapper.getWrappedDictionary();
		if (dictionary instanceof DictionaryWrapper) {
			return unwrap((DictionaryWrapper) dictionary);
		} else {
			return dictionary;
		}
	}
	
	private DictionaryUtils() {
	}
}
