package jgloss.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class JGlossLogoTest {

	@Test
	public void testAllLogosAvailable() {
		assertNotNull(JGlossLogo.LOGO_LARGE);
		assertNotNull(JGlossLogo.LOGO_64);
		assertNotNull(JGlossLogo.LOGO_48);
		assertNotNull(JGlossLogo.LOGO_32);
	}
	
	@Test
	public void testAllLogoSizes() {
		assertEquals(4, JGlossLogo.ALL_LOGO_SIZES.size());
	}
}
