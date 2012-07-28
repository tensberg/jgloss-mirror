package jgloss.ui.xml;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class CloseMetaTagInputStreamTest {
	@Test
	public void testCloseNoMetaTag() throws IOException {
		String doc = "<html><head><title></title></head><body/>";
		assertConvertedEquals(doc, doc);
	}
	
	@Test
	public void testCloseSimpleMetaTag() throws IOException {
		assertConvertedEquals("<meta/>", "<meta>");
	}
	
	@Test
	public void testCloseMetaTagWithAttribute() throws IOException {
		assertConvertedEquals("<meta foo=\"foo\" />", "<meta foo=\"foo\" >");
	}
	
	@Test
	public void testCloseTwoMetaTags() throws IOException {
		assertConvertedEquals("<html><meta/><meta foo=\"foo\" /></html>", "<html><meta><meta foo=\"foo\" ></html>");
	}
	
	private void assertConvertedEquals(String expected, String source) throws IOException {
		byte[] sourceBytes = source.getBytes("UTF-8");
		byte[] expectedByteBuffer = new byte[expected.getBytes("UTF-8").length];
		
		InputStream convertedInputStream = new CloseMetaTagInputStream(new ByteArrayInputStream(sourceBytes));
		int bytesRead = convertedInputStream.read(expectedByteBuffer, 0, expectedByteBuffer.length);
		convertedInputStream.close();

		assertEquals(expectedByteBuffer.length, bytesRead);
		assertEquals(expected, new String(expectedByteBuffer, "UTF-8"));
	}
}
