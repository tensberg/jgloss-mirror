package jgloss.ui;

import static jgloss.ui.DictionaryEntryFormat.DecorationType.READING;
import static jgloss.ui.DictionaryEntryFormat.DecorationType.TRANSLATION_SYN;
import static jgloss.ui.DictionaryEntryFormat.DecorationType.WORD;
import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeFormatter;
import jgloss.dictionary.attribute.Attributes;
import jgloss.ui.DictionaryEntryFormat.DecorationPosition;
import jgloss.ui.DictionaryEntryFormat.DecorationType;
import jgloss.util.ListFormatter;

class LookupResultMarker implements DictionaryEntryFormat.Decorator {
    private final MarkerListFormatter.Group markerGroup = 
        new MarkerListFormatter.Group( "<font color=\"blue\">", "</font>");
    private final DictionaryEntryFormat.Decorator child;

    LookupResultMarker( DictionaryEntryFormat.Decorator _child) {
        child = _child;
    }

    @Override
	public ListFormatter decorateList( ListFormatter formatter, DecorationType type) {
        if (type==WORD || type==READING || type==TRANSLATION_SYN) {
            formatter = new MarkerListFormatter( markerGroup, formatter);
        }

        if (child != null) {
            formatter = child.decorateList( formatter, type);
        }

        return formatter;
    }

    @Override
	public ListFormatter decorateList( ListFormatter formatter, Attribute<?> type,
					DecorationPosition position) {
        if (type == Attributes.EXPLANATION) {
            formatter = new MarkerListFormatter( markerGroup, formatter);
        }

        if (child != null) {
            formatter = child.decorateList( formatter, type, position);
        }
        
        return formatter;
    }

    @Override
	public AttributeFormatter decorateAttribute( AttributeFormatter formatter, Attribute<?> type,
					DecorationPosition position) {
        if (child != null) {
            formatter = child.decorateAttribute( formatter, type, position);
        }

        return formatter;
    }

    public void setMarkedText( String text) {
        markerGroup.setMarkedText( text);
    }
} // class Marker