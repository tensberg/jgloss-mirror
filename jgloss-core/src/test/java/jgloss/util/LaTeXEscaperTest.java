package jgloss.util;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;

public class LaTeXEscaperTest {

	@Test
	public void testGetEscapeMap() {
		Map<Character, String> escapeMap = new LaTeXEscaper().getEscapeMap();
		assertThat(escapeMap).isNotEmpty();
	}

}
