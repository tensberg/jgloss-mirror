package jgloss.ui;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PerformSearchPreconditionsTest {
    @Mock
    private LookupModel model;
    
    private final PerformSearchPreconditions performSearch = new PerformSearchPreconditions();
    
    @Test
    public void testPerformSearchIfExpressionSet() {
        when(model.getSearchExpression()).thenReturn("A");
        assertThat(performSearch.performSearch(model)).isTrue();
    }
    
    @Test
    public void testDontPerformSearchIfExpressionNull() {
        assertThat(performSearch.performSearch(model)).isFalse();
    }
    
    @Test
    public void testDontPerformSearchIfExpressionEmpty() {
        when(model.getSearchExpression()).thenReturn("");
        assertThat(performSearch.performSearch(model)).isFalse();
    }
}
