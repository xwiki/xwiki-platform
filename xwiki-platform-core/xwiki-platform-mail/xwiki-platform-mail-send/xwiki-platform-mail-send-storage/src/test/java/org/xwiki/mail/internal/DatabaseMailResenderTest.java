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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatabaseMailResender}.
 *
 * @version $Id$
 */
@ComponentTest
class DatabaseMailResenderTest
{
    @InjectMockComponents
    private DatabaseMailResender databaseMailResender;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void resendAsynchronouslySingleMesssage() throws Exception
    {
        MailListener listener = this.componentManager.registerMockComponent(MailListener.class, "database");

        ExtendedMimeMessage message = new ExtendedMimeMessage();
        String batchId = UUID.randomUUID().toString();

        MailContentStore contentStore = this.componentManager.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(any(), eq(batchId), eq("messageId"))).thenReturn(message);

        MailSender sender = this.componentManager.getInstance(MailSender.class);
        when(sender.sendAsynchronously(eq(Collections.singletonList(message)), any(), any(MailListener.class)))
            .thenReturn(new DefaultMailResult(batchId));

        this.databaseMailResender.resendAsynchronously(batchId, "messageId");

        // The test is here
        verify(sender).sendAsynchronously(eq(Collections.singletonList(message)), any(), same(listener));
        verify(listener).getMailStatusResult();
    }

    @Test
    void resendAsynchronouslySingleMessageWhenMailContentStoreLoadingFails() throws Exception
    {
        MailContentStore contentStore = this.componentManager.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(any(), eq("batchId"), eq("messageId"))).thenThrow(
            new MailStoreException("error"));

        Throwable exception = assertThrows(MailStoreException.class,
            () -> this.databaseMailResender.resendAsynchronously("batchId", "messageId"));
        assertEquals("error", exception.getMessage());
    }

    @Test
    void resendAsynchronouslySeveralMessages() throws Exception
    {
        Map<String, Object> filterMap = Collections.singletonMap("state", "prepare_%");

        MailStatus status1 = new MailStatus();
        status1.setBatchId("batch1");
        status1.setMessageId("message1");

        MailStatus status2 = new MailStatus();
        status2.setBatchId("batch2");
        status2.setMessageId("message2");

        List<MailStatus> statuses = new ArrayList<>();
        statuses.add(status1);
        statuses.add(status2);

        MailStatusStore statusStore = this.componentManager.getInstance(MailStatusStore.class, "database");
        when(statusStore.load(filterMap, 0, 0, null, true)).thenReturn(statuses);

        MailContentStore contentStore = this.componentManager.getInstance(MailContentStore.class, "filesystem");
        ExtendedMimeMessage message1 = new ExtendedMimeMessage();
        when(contentStore.load(any(), eq("batch1"), eq("message1"))).thenReturn(message1);
        ExtendedMimeMessage message2 = new ExtendedMimeMessage();
        when(contentStore.load(any(), eq("batch2"), eq("message2"))).thenReturn(message2);

        MailSender sender = this.componentManager.getInstance(MailSender.class);

        this.databaseMailResender.resendAsynchronously(filterMap, 0, 0);

        // The test is here
        verify(sender).sendAsynchronously(eq(Collections.singletonList(message1)), any(), any(MailListener.class));
        verify(sender).sendAsynchronously(eq(Collections.singletonList(message2)), any(), any(MailListener.class));
    }

    @BeforeComponent("resendSynchronouslySeveralMessages")
    void setupResendSynchronouslySeveralMessages() throws Exception
    {
        this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, MailListener.class), "database");
    }

    @Test
    void resendSynchronouslySeveralMessages() throws Exception
    {
        Map<String, Object> filterMap = Collections.singletonMap("state", "prepare_%");

        MailStatus status1 = new MailStatus();
        status1.setBatchId("batch1");
        status1.setMessageId("message1");

        MailStatus status2 = new MailStatus();
        status2.setBatchId("batch2");
        status2.setMessageId("message2");

        List<MailStatus> statuses = new ArrayList<>();
        statuses.add(status1);
        statuses.add(status2);

        MailStatusStore statusStore = this.componentManager.getInstance(MailStatusStore.class, "database");
        when(statusStore.load(filterMap, 0, 0, null, true)).thenReturn(statuses);

        MailContentStore contentStore = this.componentManager.getInstance(MailContentStore.class, "filesystem");
        ExtendedMimeMessage message1 = new ExtendedMimeMessage();
        when(contentStore.load(any(), eq("batch1"), eq("message1"))).thenReturn(message1);
        ExtendedMimeMessage message2 = new ExtendedMimeMessage();
        when(contentStore.load(any(), eq("batch2"), eq("message2"))).thenReturn(message2);

        Provider<MailListener> databaseMailListenerProvider = this.componentManager.getInstance(
            new DefaultParameterizedType(null, Provider.class, MailListener.class), "database");
        DatabaseMailListener databaseMailListener1 = mock(DatabaseMailListener.class);
        DatabaseMailListener databaseMailListener2 = mock(DatabaseMailListener.class);
        when(databaseMailListenerProvider.get()).thenReturn(databaseMailListener1, databaseMailListener2);
        MailStatusResult mailStatusResult1 = mock(MailStatusResult.class);
        when(databaseMailListener1.getMailStatusResult()).thenReturn(mailStatusResult1);
        MailStatusResult mailStatusResult2 = mock(MailStatusResult.class);
        when(databaseMailListener2.getMailStatusResult()).thenReturn(mailStatusResult2);

        this.databaseMailResender.resend(filterMap, 0, 0);

        // The test is here
        verify(mailStatusResult1).waitTillProcessed(Long.MAX_VALUE);
        verify(mailStatusResult2).waitTillProcessed(Long.MAX_VALUE);
    }

    @Test
    void resendAsynchronouslySeveralMessagesWhenMailContentStoreLoadingFailsForFirstMessage() throws Exception
    {
        Map<String, Object> filterMap = Collections.singletonMap("state", "prepare_%");

        MailStatus status1 = new MailStatus();
        status1.setBatchId("batch1");
        status1.setMessageId("message1");
        status1.setState(MailState.SEND_ERROR);

        MailStatus status2 = new MailStatus();
        status2.setBatchId("batch2");
        status2.setMessageId("message2");
        status2.setState(MailState.SEND_FATAL_ERROR);

        List<MailStatus> statuses = new ArrayList<>();
        statuses.add(status1);
        statuses.add(status2);

        MailStatusStore statusStore = this.componentManager.getInstance(MailStatusStore.class, "database");
        when(statusStore.load(filterMap, 0, 0, null, true)).thenReturn(statuses);

        MailContentStore contentStore = this.componentManager.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(any(), eq("batch1"), eq("message1"))).thenThrow(
            new MailStoreException("error1"));
        ExtendedMimeMessage message2 = new ExtendedMimeMessage();
        when(contentStore.load(any(), eq("batch2"), eq("message2"))).thenReturn(message2);

        MailSender sender = this.componentManager.getInstance(MailSender.class);

        this.databaseMailResender.resendAsynchronously(filterMap, 0, 0);

        // The test is here
        assertEquals("Failed to resend mail message for batchId [batch1], messageId [message1]. "
            + "Root cause [MailStoreException: error1]", logCapture.getMessage(0));
        verify(sender).sendAsynchronously(eq(Collections.singletonList(message2)), any(), any(MailListener.class));
    }

    @Test
    void resendAsynchronouslySeveralMessagesWhenMailFailedPrepare() throws Exception
    {
        Map<String, Object> filterMap = Collections.singletonMap("state", "prepare_%");

        MailStatus status1 = new MailStatus();
        status1.setBatchId("batch1");
        status1.setMessageId("message1");
        status1.setState(MailState.PREPARE_ERROR);

        List<MailStatus> statuses = new ArrayList<>();
        statuses.add(status1);

        MailStatusStore statusStore = this.componentManager.getInstance(MailStatusStore.class, "database");
        when(statusStore.load(filterMap, 0, 0, null, true)).thenReturn(statuses);

        MailContentStore contentStore = this.componentManager.getInstance(MailContentStore.class, "filesystem");
        ExtendedMimeMessage message1 = new ExtendedMimeMessage();
        when(contentStore.load(any(), eq("batch1"), eq("message1"))).thenReturn(message1);

        MailSender sender = this.componentManager.getInstance(MailSender.class);

        this.databaseMailResender.resendAsynchronously(filterMap, 0, 0);

        // The test is here
        verify(sender, never()).sendAsynchronously(eq(Collections.singletonList(message1)), any(),
            any(MailListener.class));
    }
}
