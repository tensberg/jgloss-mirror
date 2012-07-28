package jgloss.ui.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Convert a JGloss 1 file to JGloss 2 XML format. This is done in two steps: first, the input
 * stream is wrapped in a {@link CloseMetaTagInputStream}. This is a primitive way to make the
 * JGloss 1 doc (which has a simple HTML format) XML conformant so that the Java 6 default XSLT
 * processor can be used on it. Then an {@link #JGLOSS1_TO_2_STYLE_SHEET XSLT style sheet} is
 * applied to convert the document to JGloss 2 format.
 * 
 * @author Michael Koch <tensberg@gmx.net>
 */
public class JGloss1Converter {
	private static final Logger LOGGER = Logger.getLogger(JGloss1Converter.class.getPackage().getName());

	private static final String JGLOSS1_TO_2_STYLE_SHEET = "/xml/jgloss1to2.xslt";
	
	/**
	 * JGloss 1 files start with these bytes. This is just a primitive heuristics which might give false
	 * positives on HTML files, but JGloss files have their own extension so in practice this should not
	 * be a big problem.
	 */
	private static final byte[] JGLOSS_1_FILE_MARKER = initJGloss1FileMarker();

	private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
	
	private static byte[] initJGloss1FileMarker() {
	    try {
	        return "<html>".getBytes("ASCII");
        } catch (UnsupportedEncodingException ex) {
	        throw new ExceptionInInitializerError(ex);
        }
    }

	private final Transformer transformer;
	
	/**
	 * Tests if the input stream contains a JGloss 1 document which must be converted. This
	 * is done by checking the first few bytes of the input stream. The input stream will be reset
	 * to its original location afterwards.
	 * 
	 * @param in Input Stream to test.
	 * @return <code>true</code> if the input stream contains a JGloss 1 document which must be converted.
	 * @throws IOException if reading from the input stream fails.
	 * @throws IllegalArgumentException if the input stream does not {@link InputStream#markSupported() support mark()}.
	 */
	public static boolean needsConversion(InputStream in) throws IOException {
		if (!in.markSupported()) {
			throw new IllegalArgumentException("input stream must support mark()");
		}
		
		boolean needsConversion = true;
		
		in.mark(JGLOSS_1_FILE_MARKER.length);
		try {
			for (int i=0; i<JGLOSS_1_FILE_MARKER.length; i++) {
				int nextByte = in.read();
				if (nextByte == -1) {
					// end of file reached
					needsConversion = false;
					break;
				}

				if (((byte) nextByte) != JGLOSS_1_FILE_MARKER[i]) {
					// file does not start with JGloss 1 header
					needsConversion = false;
					break;
				}
			}
		} finally {
			in.reset();
		}
		
		return needsConversion;
	}
	
	public JGloss1Converter() throws TransformerConfigurationException {
		transformer = TRANSFORMER_FACTORY.newTransformer(new StreamSource(JGloss1Converter.class.getResourceAsStream(JGLOSS1_TO_2_STYLE_SHEET)));
		transformer.setErrorListener(new ErrorListener() {
			
			@Override
			public void warning(TransformerException exception) throws TransformerException {
				LOGGER.info(exception.getMessageAndLocation());
			}
			
			@Override
			public void fatalError(TransformerException exception) throws TransformerException {
				LOGGER.severe(exception.getMessageAndLocation());
			}
			
			@Override
			public void error(TransformerException exception) throws TransformerException {
				LOGGER.warning(exception.getMessageAndLocation());
			}
		});
	}

	/**
	 * Convert the JGloss 1 document read by the given input stream to a JGloss 2 document.
	 * 
	 * @param in Input stream reading a JGloss 1 document.
	 * @return Input stream which reads the JGloss document converted to JGloss 2 format.
	 * @throws TransformerException if the document conversion fails.
	 */
	public InputStream convert(InputStream in) throws TransformerException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    transformer.transform(new StreamSource(new CloseMetaTagInputStream(in)), new StreamResult(out));
		return new ByteArrayInputStream(out.toByteArray());
    }
}
