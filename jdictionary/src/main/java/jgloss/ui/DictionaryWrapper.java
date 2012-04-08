package jgloss.ui;

import jgloss.dictionary.Dictionary;

/**
 * Wrapper for a dictionary and its descriptor. Used as elements in the list model.
 */
class DictionaryWrapper {
    /**
     * Descriptor used to create the dictionary. Usually the path to the dictionary file.
     *
     * @see jgloss.dictionary.DictionaryFactory
     */
    public String descriptor;
    public Dictionary dictionary;

    public DictionaryWrapper( String descriptor, Dictionary dictionary) {
        this.descriptor = descriptor;
        this.dictionary = dictionary;
    }

    /**
     * Returns the name of the dictionary.
     */
    @Override
	public String toString() {
        return dictionary.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
        result = prime * result + ((dictionary == null) ? 0 : dictionary.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DictionaryWrapper other = (DictionaryWrapper) obj;
        if (descriptor == null) {
            if (other.descriptor != null)
                return false;
        } else if (!descriptor.equals(other.descriptor))
            return false;
        if (dictionary == null) {
            if (other.dictionary != null)
                return false;
        } else if (!dictionary.equals(other.dictionary))
            return false;
        return true;
    }
}