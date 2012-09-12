package jgloss.dictionary;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;

import jgloss.dictionary.attribute.AttributeSet;
import jgloss.dictionary.attribute.DefaultAttributeSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MultiWordEntryTest {
	private static final int TEST_ENTRY_MARKER = 1;
	
	@Mock
	private Dictionary dictionary;
	
	@Test
	public void testEntryWithNullWordsAttributeArray() {
		MultiWordEntry entry = new MultiWordEntry(TEST_ENTRY_MARKER, new String[] { "word" }, new String[] { "reading" },
						Collections.singletonList(Collections.singletonList("translation")), 
						new DefaultAttributeSet(), new DefaultAttributeSet(),
						null, 
						new DefaultAttributeSet(), Collections.<AttributeSet> emptyList(), dictionary);
		assertThat(entry.getGeneralAttributes()).isNotNull();
		assertThat(entry.getWordAlternativeCount()).isEqualTo(1);
		assertThat(entry.getWord(0)).isEqualTo("word");
		assertThat(entry.getWordAttributes()).isNotNull();
		assertThat(entry.getWordAttributes(0)).isNotNull();
		assertThat(entry.getReadingAlternativeCount()).isEqualTo(1);
		assertThat(entry.getReading(0)).isEqualTo("reading");
		assertThat(entry.getReadingAttributes()).isNotNull();
		assertThat(entry.getReadingAttributes(0)).isNotNull();
		assertThat(entry.getTranslationRomCount()).isEqualTo(1);
		assertThat(entry.getTranslationCrmCount(0)).isEqualTo(1);
		assertThat(entry.getTranslationSynonymCount(0, 0)).isEqualTo(1);
		assertThat(entry.getTranslation(0, 0, 0)).isEqualTo("translation");
		assertThat(entry.getTranslationAttributes()).isNotNull();
		assertThat(entry.getTranslationAttributes(0)).isNotNull();
		assertThat(entry.getTranslationAttributes(0, 0)).isNotNull();
		assertThat(entry.getTranslationAttributes(0, 0, 0)).isNotNull();
		assertThat(entry.getDictionary()).isSameAs(dictionary);
	}
}
