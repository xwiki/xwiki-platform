/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.mail.internal;

import java.util.Iterator;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MemoryMailListener}.
 *
 * @version $Id$
 * @since 6.2M1
 */
public class MemoryMailListenerTest
{
    @Rule
    public MockitoComponentMockingRule<MemoryMailListener> mocker =
        new MockitoComponentMockingRule<>(MemoryMailListener.class);

    @Test
    public void errorAndRetrieveError() throws Exception
    {
        MemoryMailListener listener = this.mocker.getInstance(MailListener.class, "memory");
        MimeMessage message = mock(MimeMessage.class);
        listener.onError(message, new Exception("error"));
        Iterator<MailStatus> results = listener.getMailStatusResult().getByState(MailState.FAILED);
        assertTrue("These should be mails in error!", results.hasNext());
        String error = results.next().getError();
        assertEquals("Exception: error", error );
    }

    @Test
    public void getErrorsForBatchId() throws Exception
    {
        UUID batchId = UUID.randomUUID();

        UUID mailId1 = UUID.randomUUID();
        UUID mailId2 = UUID.randomUUID();
        UUID mailId3 = UUID.randomUUID();

        MemoryMailListener listener = this.mocker.getInstance(MailListener.class, "memory");

        MimeMessage message1 = mock(MimeMessage.class);
        when(message1.getHeader("X-BatchID", null)).thenReturn(batchId.toString());
        when(message1.getHeader("X-MailID", null)).thenReturn(mailId1.toString());
        when(message1.getHeader("X-MailType", null)).thenReturn("watchlist");
        listener.onError(message1, new Exception("error"));

        MimeMessage message2 = mock(MimeMessage.class);
        when(message2.getHeader("X-BatchID", null)).thenReturn(batchId.toString());
        when(message2.getHeader("X-MailID", null)).thenReturn(mailId2.toString());
        when(message2.getHeader("X-MailType", null)).thenReturn("watchlist");
        listener.onError(message2, new Exception("error"));

        MimeMessage message3 = mock(MimeMessage.class);
        when(message3.getHeader("X-BatchID", null)).thenReturn(batchId.toString());
        when(message3.getHeader("X-MailID", null)).thenReturn(mailId3.toString());
        when(message3.getHeader("X-MailType", null)).thenReturn("watchlist");
        listener.onError(message3, new Exception("error"));

        Iterator<MailStatus> iterator = listener.getMailStatusResult().getByState(MailState.FAILED);

        assertTrue(iterator.hasNext());
        MailStatus status1 = iterator.next();
        assertEquals("Exception: error", status1.getError());
        assertEquals(status1.getBatchId(), batchId.toString());

        assertTrue(iterator.hasNext());
        MailStatus status2 = iterator.next();
        assertEquals("Exception: error", status2.getError());
        assertEquals(status2.getBatchId(), batchId.toString());

        assertTrue(iterator.hasNext());
        MailStatus status3 = iterator.next();
        assertEquals("Exception: error", status3.getError());
        assertEquals(status3.getBatchId(), batchId.toString());

        assertFalse(iterator.hasNext());
    }
}
