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

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
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
    public void onErrorAndGetMailStatusResult() throws Exception
    {
        MemoryMailListener listener = this.mocker.getComponentUnderTest();

        String batchId = UUID.randomUUID().toString();

        UUID mailId1 = UUID.randomUUID();
        UUID mailId2 = UUID.randomUUID();

        MimeMessage message1 = mock(MimeMessage.class);
        when(message1.getHeader("X-BatchID", null)).thenReturn(batchId);
        when(message1.getHeader("X-MailID", null)).thenReturn(mailId1.toString());
        when(message1.getHeader("X-MailType", null)).thenReturn("mailtype1");
        listener.onError(message1, new Exception("error1"), Collections.<String, Object>emptyMap());

        MimeMessage message2 = mock(MimeMessage.class);
        when(message2.getHeader("X-BatchID", null)).thenReturn(batchId);
        when(message2.getHeader("X-MailID", null)).thenReturn(mailId2.toString());
        when(message2.getHeader("X-MailType", null)).thenReturn("mailtype2");
        listener.onError(message2, new Exception("error2"), Collections.<String, Object>emptyMap());

        Iterator<MailStatus> results = listener.getMailStatusResult().getByState(MailState.FAILED);
        assertTrue("These should be mails in error!", results.hasNext());

        MailStatus status = results.next();
        assertEquals("Exception: error1", status.getErrorSummary());
        assertTrue(status.getErrorDescription().contains("error1"));
        assertEquals(batchId, status.getBatchId());
        assertEquals(mailId1.toString(), status.getMessageId());
        assertEquals("mailtype1", status.getType());

        status = results.next();
        assertEquals("Exception: error2", status.getErrorSummary());
        assertTrue(status.getErrorDescription().contains("error2"));
        assertEquals(batchId, status.getBatchId());
        assertEquals(mailId2.toString(), status.getMessageId());
        assertEquals("mailtype2", status.getType());

        assertFalse(results.hasNext());
    }
}
