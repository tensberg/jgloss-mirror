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

package jgloss.util;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class IOUtilsTest {
    @Mock
    private InputStream in;

    @Mock
    private OutputStream out;

    @Captor
    private ArgumentCaptor<byte[]> outBufferCaptor;

    @Test
    public void testCopy() throws IOException {
        when(in.read(Mockito.notNull(byte[].class))).thenAnswer(new Answer<Integer>() {

            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] buffer = (byte[]) invocation.getArguments()[0];
                buffer[0] = 'f';
                buffer[1] = 'o';
                buffer[2] = 'o';
                return 3;
            }

        }).thenReturn(-1);

        IOUtils.copy(in, out);

        verify(out).write(outBufferCaptor.capture(), eq(0), eq(3));
        byte[] outBuffer = outBufferCaptor.getValue();
        assertThat(outBuffer[0]).isEqualTo((byte) 'f');
        assertThat(outBuffer[1]).isEqualTo((byte) 'o');
        assertThat(outBuffer[2]).isEqualTo((byte) 'o');
        verifyNoMoreInteractions(out);
    }
}
