package jgloss.ui.annotation;

import static jgloss.ui.html.AnnotationTags.ANNOTATION;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

import jgloss.ui.xml.JGlossDocument;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnnotationTest {
	@Mock
	private AnnotationListModel owner;
	
	@Mock
	private Element anno;
	
	@Mock
	private AttributeSet attributes;
	
	@Mock
	private Element word;
	
	@Mock
	private Element translation;
	
	@Before
	public void setupMocks() throws BadLocationException {
		when(anno.getName()).thenReturn(ANNOTATION.getId());
		when(anno.getElement(0)).thenReturn(word);
		when(anno.getElement(1)).thenReturn(translation);
		when(anno.getAttributes()).thenReturn(attributes);
	}
	
	@Test
	public void testGetWordRelatedFields() throws BadLocationException {
		mockAnno();
		Annotation annotation = new Annotation(owner, anno);
		
		assertThat(annotation.getAnnotatedText()).isEqualTo("barbaz");
		assertThat(annotation.getAnnotatedTextReading()).isEqualTo("foobaz");
		assertThat(annotation.getReadingCount()).isEqualTo(1);
		assertThat(annotation.getReading(0)).isEqualTo("foo");
		assertThat(annotation.getReadingBase(0)).isEqualTo("bar");
	}

	@Test
	public void testGetDictionaryFormFromAttribute() {
		when(attributes.getAttribute(JGlossDocument.Attributes.BASE)).thenReturn("foo");
		
		Annotation annotation = new Annotation(owner, anno);
		
		assertThat(annotation.getDictionaryForm()).isEqualTo("foo");
	}
	
	@Test
	public void testGetDictionaryFormFromAnnotatedText() throws BadLocationException {
		mockAnno();
		Annotation annotation = new Annotation(owner, anno);

		assertThat(annotation.getDictionaryForm()).isEqualTo("barbaz");
	}
	
	@Test
	public void testGetTranslation() throws BadLocationException {
		mockTextElement(translation, "foo");
		
		Annotation annotation = new Annotation(owner, anno);
		
		assertThat(annotation.getTranslation()).isEqualTo("foo");
	}
	
	@Test
	public void testSetReading() {
		
	}
	
	private void mockAnno() throws BadLocationException {
		when(word.getElementCount()).thenReturn(2);
		Element rb = mock(Element.class);
		when(word.getElement(0)).thenReturn(rb);
		when(rb.getElementCount()).thenReturn(2);
		Element reading = mock(Element.class);
		mockTextElement(reading, "foo");
		when(rb.getElement(0)).thenReturn(reading);
		Element rbase = mock(Element.class);
		mockTextElement(rbase, "bar");
		when(rb.getElement(1)).thenReturn(rbase);
		Element text = mock(Element.class);
		mockTextElement(text, "baz");
		when(word.getElement(1)).thenReturn(text);
	}

	private void mockTextElement(Element element, String text) throws BadLocationException {
	    when(element.getStartOffset()).thenReturn(0);
		when(element.getEndOffset()).thenReturn(text.length());
		Document document = mock(Document.class);
		when(element.getDocument()).thenReturn(document);
		when(document.getText(0, text.length())).thenReturn(text);
    }
}
