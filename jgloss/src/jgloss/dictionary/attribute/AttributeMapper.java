package jgloss.dictionary.attribute;

/**
 * Mapping from strings used in dictionaries to mark attributes or attribute values to
 * attribute/value objects. Dictionaries use strings, which are usually abbreviated words,
 * to mark attributes and their values in entries.
 *
 * @author Michael Koch
 */
public class AttributeMapper {
    

    public static class Mapping {
        private Attribute attribute;
        private AttributeValue value;
    }

    public AttributeMapper( StreamTokenizer mapping) {
    }
} // class AttributeMapper
