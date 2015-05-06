package jgloss.ui;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExclusionResourceLoaderTest {
    @Mock
    private ExclusionResource exclusionResource;

    @InjectMocks
    private ExclusionResourceLoader exclusionResourceLoader;

    @Test
    public void testLoadExclusions() throws IOException {
        File exclusionsFile = File.createTempFile("ExclusionsResourceLoaderTest", "txt");
        exclusionsFile.deleteOnExit();
        try (Writer out = new OutputStreamWriter(new FileOutputStream(exclusionsFile))) {
            out.write("foo\nbar");
        }
        when(exclusionResource.getLocation()).thenReturn(exclusionsFile.toURI().toURL());

        Set<String> exclusions = exclusionResourceLoader.loadExclusions();

        assertThat(exclusions).containsOnly("foo", "bar");
    }
}
