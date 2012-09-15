package jgloss.ui;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryWrapper;

public class DictionaryStateWrapper extends StateWrapper<Dictionary> implements DictionaryWrapper {

	public DictionaryStateWrapper(Dictionary _obj) {
	    super(_obj);
    }

	public DictionaryStateWrapper(Dictionary _obj, boolean _selected, boolean _enabled) {
	    super(_obj, _selected, _enabled);
    }

	@Override
	public Dictionary getWrappedDictionary() {
		return getObject();
	}

}
