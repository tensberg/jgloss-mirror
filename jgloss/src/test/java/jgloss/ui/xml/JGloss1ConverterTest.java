package jgloss.ui.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

public class JGloss1ConverterTest {
	@Test
	public void testNeedsConversion() throws IOException {
		assertTrue(JGloss1Converter.needsConversion(getResource("jgloss1.jgloss")));
		assertFalse(JGloss1Converter.needsConversion(getResource("jgloss2.jgloss")));
	}

	private InputStream getResource(String file) {
	    return JGloss1ConverterTest.class.getResourceAsStream("/" + file);
    }
	
	@Test
	public void testConvert() throws TransformerConfigurationException, TransformerException, IOException {
		InputStream expectedStream = getResource("jgloss2.jgloss");
		InputStream convertedStream = new JGloss1Converter().convert(getResource("jgloss1.jgloss"));
		
		try {
			int bytesRead = 0;
			int expectedData;
			int convertedData;
			do {
				expectedData = expectedStream.read();
				convertedData = convertedStream.read();
				
				if (expectedData == -1 && convertedData != -1) {
					fail("converted data is longer than expected at byte offset" + bytesRead);
				}
				if (expectedData != -1 && convertedData == -1) {
					fail("converted data is shorter than expected at byte offset " + bytesRead);
				}
				
				assertEquals(
					"converted data does not match expected data, expected " + ((char) expectedData) + ", got " + ((char) convertedData) + " at byte offset " + bytesRead,
					expectedData, convertedData);

				bytesRead++;
			} while (convertedData != -1);
		} finally {
			expectedStream.close();
			convertedStream.close();
		}
	}
}
