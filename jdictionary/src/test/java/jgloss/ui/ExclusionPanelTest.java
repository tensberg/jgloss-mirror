package jgloss.ui;

import static org.fest.assertions.Assertions.assertThat;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

import org.junit.Test;


public class ExclusionPanelTest {
    @Test
    public void testRemoveSelectionRemoveSingleEntry() {
        DefaultListModel<String> listModel = new DefaultListModel<String>();
        listModel.add(0, "foo");
        ListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionModel.setSelectionInterval(0, 0);
        ExclusionPanel.removeSelection(listModel, selectionModel);
        assertThat(listModel.size()).isEqualTo(0);
        assertThat(selectionModel.getMinSelectionIndex()).isEqualTo(-1);
    }

    @Test
    public void testRemoveSelectionRemoveMultipleEntries() {
        DefaultListModel<String> listModel = new DefaultListModel<String>();
        listModel.add(0, "foo");
        listModel.add(1, "bar");
        listModel.add(2, "baz");
        listModel.add(3, "quux");
        ListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionModel.addSelectionInterval(0, 0);
        selectionModel.addSelectionInterval(2, 3);
        ExclusionPanel.removeSelection(listModel, selectionModel);
        assertThat(listModel.size()).isEqualTo(1);
        assertThat(listModel.get(0)).isEqualTo("bar");
        assertThat(selectionModel.getMinSelectionIndex()).isEqualTo(-1);
    }
}
