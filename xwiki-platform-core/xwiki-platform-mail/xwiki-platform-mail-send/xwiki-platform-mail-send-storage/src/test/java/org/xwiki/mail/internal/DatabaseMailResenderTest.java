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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatabaseMailResender}.
 *
 * @version $Id$
 */
public class DatabaseMailResenderTest
{
    @Rule
    public MockitoComponentMockingRule<DatabaseMailResender> mocker =
        new MockitoComponentMockingRule<>(DatabaseMailResender.class);

    @Test
    public void resendAsynchronouslySingleMesssage() throws Exception
    {
        MailListener listener = this.mocker.registerMockComponent(MailListener.class, "database");

        ExtendedMimeMessage message = new ExtendedMimeMessage();
        String batchId = UUID.randomUUID().toString();

        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(any(), eq(batchId), eq("messageId"))).thenReturn(message);

        MailSender sender = this.mocker.getInstance(MailSender.class);
        when(sender.sendAsynchronously(eq(Arrays.asList(message)), any(), any(MailListener.class)))
            .thenReturn(new DefaultMailResult(batchId));

        this.mocker.getComponentUnderTest().resendAsynchronously(batchId, "messageId");

        // The test is here
        verify(sender).sendAsynchronously(eq(Arrays.asList(message)), any(), same(listener));
        verify(listener).getMailStatusResult();
    }

    @Test
    public void resendAsynchronouslySingleMessageWhenMailContentStoreLoadingFails() throws Exception
    {
        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(any(), eq("batchId"), eq("messageId"))).thenThrow(
            new MailStoreException("error"));

        try {
            this.mocker.getComponentUnderTest().resendAsynchronously("batchId", "messageId");
            fail("Should have thrown an exception here");
        } catch (MailStoreException expected) {
            assertEquals("error", expected.getMessage());
        }
    }

    @Test
    public void resendAsynchronouslySeveralMessages() throws Exception
    {
        Map filterMap = Collections.singletonMap("state", "prepare_%");

        MailStatus status1 = new MailStatus();
        status1.setBatchId("batch1");
        status1.setMessageId("message1");

        MailStatus status2 = new MailStatus();
        status2.setBatchId("batch2");
        status2.setMessageId("message2");

        List<MailStatus> statuses = new ArrayList<>();
        statuses.add(status1);
        statuses.add(status2);

        MailStatusStore statusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        when(statusStore.load(filterMap, 0, 0, null, true)).thenReturn(statuses);

        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        ExtendedMimeMessage message1 = new ExtendedMimeMessage();
        when(contentStore.load(any(), eq("batch1"), eq("message1"))).thenReturn(message1);
        ExtendedMimeMessage message2 = new ExtendedMimeMessage();
        when(contentStore.load(any(), eq("batch2"), eq("message2"))).thenReturn(message2);

        MailSender sender = this.mocker.getInstance(MailSender.class);

        this.mocker.getComponentUnderTest().resendAsynchronously(filterMap, 0, 0);

        // The test is here
        verify(sender).sendAsynchronously(eq(Arrays.asList(message1)), any(), any(MailListener.class));
        verify(sender).sendAsynchronously(eq(Arrays.asList(message2)), any(), any(MailListener.class));
    }

    @Test
    public void resendAsynchronouslySeveralMessagesWhenMailContentStoreLoadingFailsForFirstMessage() throws Exception
    {
        Map filterMap = Collections.singletonMap("state", "prepare_%");

        MailStatus status1 = new MailStatus();
        status1.setBatchId("batch1");
        status1.setMessageId("message1");

        MailStatus status2 = new MailStatus();
        status2.setBatchId("batch2");
        status2.setMessageId("message2");

        List<MailStatus> statuses = new ArrayList<>();
        statuses.add(status1);
        statuses.add(status2);

        MailStatusStore statusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        when(statusStore.load(filterMap, 0, 0, null, true)).thenReturn(statuses);

        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(any(), eq("batch1"), eq("message1"))).thenThrow(
            new MailStoreException("error1"));
        ExtendedMimeMessage message2 = new ExtendedMimeMessage();
        when(contentStore.load(any(), eq("batch2"), eq("message2"))).thenReturn(message2);

        MailSender sender = this.mocker.getInstance(MailSender.class);

        this.mocker.getComponentUnderTest().resendAsynchronously(filterMap, 0, 0);

        // The test is here
        verify(this.mocker.getMockedLogger()).warn(
            "Failed to resend mail message for batchId [{}], messageId [{}]. Root cause [{}]",
            "batch1", "message1", "MailStoreException: error1");
        verify(sender).sendAsynchronously(eq(Arrays.asList(message2)), any(), any(MailListener.class));
    }
}
