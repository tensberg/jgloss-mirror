package jgloss.dictionary;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class DictionaryUtilsTest {

	private interface DictionaryAndWrapper extends Dictionary, DictionaryWrapper {
		
	}
	
	@Test
	public void testUnwrap() {
		DictionaryWrapper outerWrapper = mock(DictionaryWrapper.class);
		DictionaryAndWrapper innerWrapper = mock(DictionaryAndWrapper.class);
		Dictionary dictionary = mock(Dictionary.class);
		
		when(outerWrapper.getWrappedDictionary()).thenReturn(innerWrapper);
		when(innerWrapper.getWrappedDictionary()).thenReturn(dictionary);
		
		assertThat(DictionaryUtils.unwrap(outerWrapper)).isSameAs(dictionary);
	}

}
