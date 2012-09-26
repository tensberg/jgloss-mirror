package jgloss.ui;

import static jgloss.ui.DictionaryEntryFormat.DecorationPosition.POSITION_BEFORE;
import static jgloss.ui.DictionaryEntryFormat.DecorationType.READING;
import static jgloss.ui.DictionaryEntryFormat.DecorationType.TRANSLATION_SYN;
import static jgloss.ui.DictionaryEntryFormat.DecorationType.WORD;

import java.util.HashMap;
import java.util.Map;

import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeFormatter;
import jgloss.dictionary.attribute.ReferenceAttributeValue;
import jgloss.ui.DictionaryEntryFormat.DecorationPosition;
import jgloss.ui.DictionaryEntryFormat.DecorationType;
import jgloss.util.ListFormatter;

public class LookupResultHyperlinker extends DictionaryEntryFormat.IdentityDecorator {
    public static final String WORD_PROTOCOL = "wo";
    public static final String READING_PROTOCOL = "re";
    public static final String TRANSLATION_PROTOCOL = "tr";
    public static final String REFERENCE_PROTOCOL = "ref";
    public static final String ATTRIBUTE_BEFORE_PROTOCOL = "atb";
    public static final String ATTRIBUTE_AFTER_PROTOCOL = "ata";

    private final boolean words;
    private final boolean readings;
    private final boolean translations;
    private final boolean references;
    private final boolean allAttributes;

    private final Map<String, Object> hyperrefs;

    public LookupResultHyperlinker() {
        this( false, false, false, true, false);
    }

    public LookupResultHyperlinker( boolean _words, boolean _readings, boolean _translations,
                        boolean _references, boolean _allAttributes) {
        words = _words;
        readings = _readings;
        translations = _translations;
        references = _references | _allAttributes;
        allAttributes = _allAttributes;

        hyperrefs = new HashMap<String, Object>();
    }

    @Override
	public ListFormatter decorateList( ListFormatter formatter, DecorationType type) {
        if (words && type==WORD) {
            formatter = new HyperlinkListFormatter( WORD_PROTOCOL, hyperrefs, formatter);
        } else if (readings && type==READING) {
            formatter = new HyperlinkListFormatter( READING_PROTOCOL, hyperrefs, formatter);
        } else if (translations && type==TRANSLATION_SYN) {
            formatter = new HyperlinkListFormatter( TRANSLATION_PROTOCOL, hyperrefs, formatter);
        }

        return formatter;
    }

    @Override
	public AttributeFormatter decorateAttribute( AttributeFormatter formatter,
                                                 Attribute<?> type, DecorationPosition position) {
        if (references && type.canHaveValue() && 
            ReferenceAttributeValue.class.isAssignableFrom
            ( type.getAttributeValueClass())) {
            formatter = new HyperlinkAttributeFormatter( REFERENCE_PROTOCOL,
                                                         true, hyperrefs, formatter);
        }
        else if (allAttributes) {
            formatter = new HyperlinkAttributeFormatter( (position==POSITION_BEFORE) ? 
                                                         ATTRIBUTE_BEFORE_PROTOCOL :
                                                         ATTRIBUTE_AFTER_PROTOCOL,
                                                         true, hyperrefs, formatter);
        }
        
        return formatter;
    }

    public Map<String, Object> getReferences() {
        return hyperrefs;
    }

    public void clearReferences() {
        hyperrefs.clear();
    }
} // class Hyperlinker