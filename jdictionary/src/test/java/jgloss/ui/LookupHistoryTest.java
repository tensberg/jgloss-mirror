package jgloss.ui;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LookupHistoryTest {
	private static final int TEST_MAX_HISTORY_SIZE = 2;
	
	@Mock
	private LookupFrame lookupFrame;

	private LookupHistory history;
	
	private Action back;
	
	private Action forward;
	
	@Mock
	private HistoryItem item1;
	
	@Mock
	private HistoryItem item2;
	
	@Mock
	private HistoryItem item3;
	
	@Before
	public void initLookupHistory() {
		history = new LookupHistory(lookupFrame, TEST_MAX_HISTORY_SIZE);
		back = history.getHistoryBackAction();
		forward = history.getHistoryForwardAction();
	}
	
	@Test
	public void testEmptyHistoryNoNavigationPossible() {
		assertThat(back).isNotNull();
		assertThat(back.isEnabled()).isFalse();
		assertThat(forward).isNotNull();
		assertThat(forward.isEnabled()).isFalse();
	}
	
	@Test
	public void testAddInitialHistoryItemDoesNotEnableBack() {
		history.addCurrentState(item1);
		assertThat(back.isEnabled()).isFalse();
		assertThat(forward.isEnabled()).isFalse();
	}
	
	@Test
	public void testAddSecondHistoryItemEnabledBack() {
		testAddInitialHistoryItemDoesNotEnableBack();
		history.addCurrentState(item2);
		assertThat(back.isEnabled()).isTrue();
		assertThat(forward.isEnabled()).isFalse();
	}
	
	@Test
	public void testMoveBackShowsHistoryItem1() {
		testAddSecondHistoryItemEnabledBack();
		
		back.actionPerformed(mock(ActionEvent.class));
		
		verify(lookupFrame).showHistoryItem(item1);
		assertThat(back.isEnabled()).isFalse();
		assertThat(forward.isEnabled()).isTrue();
	}
	
	@Test
	public void testMoveForwardShowsHistoryItem2() {
		testMoveBackShowsHistoryItem1();
		
		forward.actionPerformed(mock(ActionEvent.class));
		
		verify(lookupFrame).showHistoryItem(item2);
		assertThat(back.isEnabled()).isTrue();
		assertThat(forward.isEnabled()).isFalse();
	}
	
	@Test
	public void testAddHistoryItemReplacesItemAfterBack() {
		history.addCurrentState(item1);
		history.addCurrentState(item2);
		
		back.actionPerformed(mock(ActionEvent.class));

		history.addCurrentState(item3);
		
		assertThat(back.isEnabled()).isTrue();
		assertThat(forward.isEnabled()).isFalse();

		back.actionPerformed(mock(ActionEvent.class));

		assertThat(back.isEnabled()).isFalse();
		assertThat(forward.isEnabled()).isTrue();
		
		forward.actionPerformed(mock(ActionEvent.class));
		
		assertThat(back.isEnabled()).isTrue();
		assertThat(forward.isEnabled()).isFalse();
		
		InOrder inOrder = inOrder(lookupFrame);
		inOrder.verify(lookupFrame, times(2)).showHistoryItem(item1);
		inOrder.verify(lookupFrame).showHistoryItem(item3);
	}
	
	@Test
	public void testRemoveFirstItemIfMaxHistorySizeExceeded() {
		history.addCurrentState(item1);
		history.addCurrentState(item2);
		history.addCurrentState(item3);

		assertThat(back.isEnabled()).isTrue();
		assertThat(forward.isEnabled()).isFalse();

		back.actionPerformed(mock(ActionEvent.class));
		assertThat(back.isEnabled()).isFalse();
		assertThat(forward.isEnabled()).isTrue();
		
		verify(lookupFrame).showHistoryItem(item2);
	}
}
