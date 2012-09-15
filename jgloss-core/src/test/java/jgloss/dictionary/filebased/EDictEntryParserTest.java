package jgloss.dictionary.filebased;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.attribute.Attributes;
import jgloss.dictionary.attribute.InformationAttributeValue;
import jgloss.dictionary.attribute.Priority;
import jgloss.dictionary.attribute.ReferenceAttributeValue;
import jgloss.dictionary.attribute.SearchReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EDictEntryParserTest {
	
	private static final String KANJI = "KANJI";
	
	private static final String KANA = "KANA";
	
	private static final String GLOSS = "gloss";
	
	private static final String GLOSS2 = "gloss2";
	
	private static final String ELLIPSE = "...";

	private static final String GENERAL_INFORMATION = "general information";

	private static final int TEST_OFFSET = 23;
	
	@Mock
	private EDict dictionary;

	private EDictEntryParser parser;

	@Before
	public void createEDictEntryParser() {
		parser = new EDictEntryParser();
		parser.setDictionary(dictionary);
	}
	
	@Test
	public void testParseSimpleEntry() {
		DictionaryEntry entry = parser.parseEntry("KANJI [KANA] /(general information) gloss/gloss2/.../" , TEST_OFFSET);
		assertThat(entry).isNotNull();
		assertThat(entry.getDictionary()).isEqualTo(dictionary);
		assertThat(entry.getWordAlternativeCount()).isEqualTo(1);
		assertThat(entry.getWord(0)).isEqualTo(KANJI);
		assertThat(entry.getReadingAlternativeCount()).isEqualTo(1);
		assertThat(entry.getReading(0)).isEqualTo(KANA);
		assertThat(entry.getTranslationRomCount()).isEqualTo(1);
		assertThat(entry.getTranslationCrmCount(0)).isEqualTo(3);
		for (int i=0; i<3; i++) {
			assertThat(entry.getTranslationSynonymCount(0, i)).isEqualTo(1);
		}
		assertThat(entry.getTranslation(0, 0, 0)).isEqualTo(GLOSS);
		assertThat(entry.getTranslation(0, 1, 0)).isEqualTo(GLOSS2);
		assertThat(entry.getTranslation(0, 2, 0)).isEqualTo(ELLIPSE);
		List<InformationAttributeValue> explanations = entry.getTranslationAttributes().getAttribute(Attributes.EXPLANATION, false);
		assertThat(explanations).isNotNull();
		assertThat(explanations).hasSize(1);
		InformationAttributeValue explanation = explanations.get(0);
		assertThat(explanation).isNotNull();
		assertThat(explanation.getInformation()).isEqualTo(GENERAL_INFORMATION);
	}
	
	@Test
	public void testParsePriorityEntry() {
		DictionaryEntry entry = parser.parseEntry("KANJI [KANA] /(P)/gloss/" , TEST_OFFSET);
		assertThat(entry).isNotNull();
		assertThat(entry.getDictionary()).isEqualTo(dictionary);
		assertThat(entry.getWordAlternativeCount()).isEqualTo(1);
		assertThat(entry.getWord(0)).isEqualTo(KANJI);
		assertThat(entry.getReadingAlternativeCount()).isEqualTo(1);
		assertThat(entry.getReading(0)).isEqualTo(KANA);
		assertThat(entry.getTranslationRomCount()).isEqualTo(1);
		assertThat(entry.getTranslationCrmCount(0)).isEqualTo(1);
		assertThat(entry.getTranslationSynonymCount(0, 0)).isEqualTo(1);
		assertThat(entry.getTranslation(0, 0, 0)).isEqualTo(GLOSS);
		List<Priority> priorities = entry.getGeneralAttributes().getAttribute(Attributes.PRIORITY, false);
		assertThat(priorities).isNotNull();
		assertThat(priorities).hasSize(1);
		Priority priority = priorities.get(0);
		assertThat(priority).isNotNull();
	}
	
	@Test
	public void testParseSimpleEntryWithRangeOfMeaning() {
		DictionaryEntry entry = parser.parseEntry("KANJI [KANA] /(general information) (1) gloss/(2) (rom-specific) gloss2/.../" , TEST_OFFSET);
		assertThat(entry).isNotNull();
		assertThat(entry.getDictionary()).isEqualTo(dictionary);
		assertThat(entry.getWordAlternativeCount()).isEqualTo(1);
		assertThat(entry.getWord(0)).isEqualTo(KANJI);
		assertThat(entry.getReadingAlternativeCount()).isEqualTo(1);
		assertThat(entry.getReading(0)).isEqualTo(KANA);
		assertThat(entry.getTranslationRomCount()).isEqualTo(2);
		assertThat(entry.getTranslationCrmCount(0)).isEqualTo(1);
		assertThat(entry.getTranslationCrmCount(1)).isEqualTo(2);
		assertThat(entry.getTranslationSynonymCount(0, 0)).isEqualTo(1);
		for (int i=0; i<2; i++) {
			assertThat(entry.getTranslationSynonymCount(1, i)).isEqualTo(1);
		}
		assertThat(entry.getTranslation(0, 0, 0)).isEqualTo(GLOSS);
		assertThat(entry.getTranslation(1, 0, 0)).isEqualTo(GLOSS2);
		assertThat(entry.getTranslation(1, 1, 0)).isEqualTo(ELLIPSE);
		List<InformationAttributeValue> explanations = entry.getTranslationAttributes().getAttribute(Attributes.EXPLANATION, false);
		assertThat(explanations).isNotNull();
		assertThat(explanations).hasSize(1);
		InformationAttributeValue explanation = explanations.get(0);
		assertThat(explanation).isNotNull();
		assertThat(explanation.getInformation()).isEqualTo(GENERAL_INFORMATION);
		
		assertThat(entry.getTranslationAttributes(0).isEmpty()).isTrue();
		List<InformationAttributeValue> romExplanations = entry.getTranslationAttributes(1).getAttribute(Attributes.EXPLANATION, false);
		assertThat(romExplanations).isNotNull();
		assertThat(romExplanations).hasSize(1);
		InformationAttributeValue romExplanation = romExplanations.get(0);
		assertThat(romExplanation).isNotNull();
		assertThat(romExplanation.getInformation()).isEqualTo("rom-specific");
	}
	
	@Test
	public void testParseSimpleEntryWithoutKanji() {
		DictionaryEntry entry = parser.parseEntry("KANA /(general information) gloss/gloss2/.../" , TEST_OFFSET);
		assertThat(entry).isNotNull();
		assertThat(entry.getDictionary()).isEqualTo(dictionary);
		assertThat(entry.getWordAlternativeCount()).isEqualTo(1);
		assertThat(entry.getWord(0)).isEqualTo(KANA);
		assertThat(entry.getReadingAlternativeCount()).isEqualTo(1);
		assertThat(entry.getReading(0)).isEqualTo(KANA);
		assertThat(entry.getTranslationRomCount()).isEqualTo(1);
		assertThat(entry.getTranslationCrmCount(0)).isEqualTo(3);
		for (int i=0; i<3; i++) {
			assertThat(entry.getTranslationSynonymCount(0, i)).isEqualTo(1);
		}
		assertThat(entry.getTranslation(0, 0, 0)).isEqualTo(GLOSS);
		assertThat(entry.getTranslation(0, 1, 0)).isEqualTo(GLOSS2);
		assertThat(entry.getTranslation(0, 2, 0)).isEqualTo(ELLIPSE);

		List<InformationAttributeValue> explanations = entry.getTranslationAttributes().getAttribute(Attributes.EXPLANATION, false);
		assertThat(explanations).isNotNull();
		assertThat(explanations).hasSize(1);
		InformationAttributeValue explanation = explanations.get(0);
		assertThat(explanation).isNotNull();
		assertThat(explanation.getInformation()).isEqualTo(GENERAL_INFORMATION);
	}
	
	@Test
	public void testParseComplexEntry() {
		DictionaryEntry entry = parser.parseEntry("KANJI-1;KANJI-2 [KANA-1;KANA-2] /(general information) (see xxxx,yyyy・reading・subentry) gloss/gloss2/.../EntL12345678X/" , TEST_OFFSET);
		assertThat(entry).isNotNull();
		assertThat(entry.getDictionary()).isEqualTo(dictionary);
		assertThat(entry.getWordAlternativeCount()).isEqualTo(2);
		assertThat(entry.getWord(0)).isEqualTo("KANJI-1");
		assertThat(entry.getWord(1)).isEqualTo("KANJI-2");
		assertThat(entry.getReadingAlternativeCount()).isEqualTo(2);
		assertThat(entry.getReading(0)).isEqualTo("KANA-1");
		assertThat(entry.getReading(1)).isEqualTo("KANA-2");
		assertThat(entry.getTranslationRomCount()).isEqualTo(1);
		assertThat(entry.getTranslationCrmCount(0)).isEqualTo(3);
		for (int i=0; i<3; i++) {
			assertThat(entry.getTranslationSynonymCount(0, i)).isEqualTo(1);
		}
		assertThat(entry.getTranslation(0, 0, 0)).isEqualTo(GLOSS);
		assertThat(entry.getTranslation(0, 1, 0)).isEqualTo(GLOSS2);
		assertThat(entry.getTranslation(0, 2, 0)).isEqualTo(ELLIPSE);
		
		List<ReferenceAttributeValue> references = entry.getGeneralAttributes().getAttribute(Attributes.REFERENCE, false);
		assertThat(references).isNotNull();
		assertThat(references).hasSize(2);
		ReferenceAttributeValue referenceX = references.get(0);
		assertReference(referenceX, "xxxx", "xxxx");
		ReferenceAttributeValue referenceY = references.get(1);
		assertReference(referenceY, "yyyy・reading・subentry", "yyyy");

		List<InformationAttributeValue> explanations = entry.getTranslationAttributes().getAttribute(Attributes.EXPLANATION, false);
		assertThat(explanations).isNotNull();
		assertThat(explanations).hasSize(1);
		InformationAttributeValue explanation = explanations.get(0);
		assertThat(explanation).isNotNull();
		assertThat(explanation.getInformation()).isEqualTo(GENERAL_INFORMATION);
	}

	private void assertReference(ReferenceAttributeValue reference, String referenceTitle, String referenceText) {
	    assertThat(reference).isNotNull();
		assertThat(reference).isInstanceOf(SearchReference.class);
		assertThat(reference.getReferenceTitle()).isEqualTo(referenceTitle);
		SearchReference searchReference = (SearchReference) reference;
		assertThat(searchReference.getReference()).isEqualTo(referenceText);
		assertThat(searchReference.getDictionary()).isEqualTo(dictionary);
		assertThat(searchReference.getSearchMode()).isNotNull();
		assertThat(searchReference.getSearchFieldSelection()).isNotNull();
    }
}
