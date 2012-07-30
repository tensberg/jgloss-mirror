package jgloss.ui.annotation;

import static jgloss.ui.html.JGlossHTMLDoc.EMPTY_ELEMENT_PLACEHOLDER;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TextElementTest {
	@Mock
	private Element element;
	
	@Mock
	private Document document;
	
	@Mock
	private AttributeSet attributes;
	
	private TextElement textElement;
	
	@Before
	public void createTextElement() {
		when(element.getDocument()).thenReturn(document);
		when(element.getAttributes()).thenReturn(attributes);
		
		textElement = new TextElement(element);
	}
	
	@Test
	public void testGetElement() {
		assertThat(textElement.getElement()).isSameAs(element);
	}
	
	@Test
	public void testSetText() throws BadLocationException {
		when(element.getStartOffset()).thenReturn(1);
		when(element.getEndOffset()).thenReturn(2);
		
		textElement.setText("foo");
		
		assertThat(textElement.getText()).isEqualTo("foo");
		verify(document).insertString(2, "foo", attributes);
		verify(document).remove(1, 2-1);
	}
	
	@Test
	public void testSetEmptyText() throws BadLocationException {
		when(element.getStartOffset()).thenReturn(1);
		when(element.getEndOffset()).thenReturn(2);
		
		textElement.setText("");
		
		assertThat(textElement.getText()).isEqualTo("");
		verify(document).insertString(2, EMPTY_ELEMENT_PLACEHOLDER, attributes);
		verify(document).remove(1, 2-1);
	}
	
	@Test
	public void testGetTextReadFromDocument() throws BadLocationException {
		when(element.getStartOffset()).thenReturn(1);
		when(element.getEndOffset()).thenReturn(4);
		when(document.getText(1, 3)).thenReturn("foo");
		
		assertThat(textElement.getText()).isEqualTo("foo");
	}
	
	@Test
	public void testGetTextReadEmptyFromDocument() throws BadLocationException {
		when(element.getStartOffset()).thenReturn(1);
		when(element.getEndOffset()).thenReturn(1 + EMPTY_ELEMENT_PLACEHOLDER.length());
		when(document.getText(1, EMPTY_ELEMENT_PLACEHOLDER.length())).thenReturn(EMPTY_ELEMENT_PLACEHOLDER);
		
		assertThat(textElement.getText()).isEqualTo("");
	}
}
