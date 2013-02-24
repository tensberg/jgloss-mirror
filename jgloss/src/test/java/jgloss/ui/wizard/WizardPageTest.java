/*
 * Copyright (C) 2002-2013 Michael Koch (tensberg@gmx.net)
 *
 * This file is part of JGloss.
 *
 * JGloss is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JGloss is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGloss; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package jgloss.ui.wizard;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WizardPageTest {
    @Mock
    private PropertyChangeListener propertyChangeListener;

    @Captor
    private ArgumentCaptor<PropertyChangeEvent> eventCaptor;

    private final WizardPage wizardPage = new WizardPage() {

        private static final long serialVersionUID = 1L;

        @Override
        public String getTitle() {
            return "unit test";
        }
    };

    @Test
    public void testForwardEnabledDefaultTrue() {
        assertThat(wizardPage.isForwardEnabled()).isTrue();
    }

    @Test
    public void testSetForwardEnabled() {
        wizardPage.addPropertyChangeListener(WizardPage.FORWARD_ENABLED_PROPERTY, propertyChangeListener);
        wizardPage.setForwardEnabled(false);
        assertThat(wizardPage.isForwardEnabled()).isFalse();
        verify(propertyChangeListener).propertyChange(eventCaptor.capture());
        PropertyChangeEvent event = eventCaptor.getValue();
        assertThat(event.getSource()).isEqualTo(wizardPage);
        assertThat(event.getPropertyName()).isEqualTo(WizardPage.FORWARD_ENABLED_PROPERTY);
        assertThat(event.getOldValue()).isEqualTo(TRUE);
        assertThat(event.getNewValue()).isEqualTo(FALSE);
    }
}
