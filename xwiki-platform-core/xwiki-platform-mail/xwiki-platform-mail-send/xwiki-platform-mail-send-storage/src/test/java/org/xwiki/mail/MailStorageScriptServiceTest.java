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
package org.xwiki.mail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.inject.Provider;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.internal.DefaultMailResult;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.mail.internal.thread.MailQueueManager;
import org.xwiki.mail.internal.thread.SendMailQueueItem;
import org.xwiki.mail.script.ScriptMailResult;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.MailStorageScriptService}.
 *
 * @version $Id$
 * @since 6.4
 */
@ComponentList({
    MemoryMailListener.class
})
public class MailStorageScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<MailStorageScriptService> mocker =
        new MockitoComponentMockingRule<>(MailStorageScriptService.class);

    @Before
    public void setUp() throws Exception
    {
        Provider<ComponentManager> componentManagerProvider = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.mocker);

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);
    }

    @Test
    public void resendWhenDatabaseListenerNotFound() throws Exception
    {
        ScriptMailResult result = this.mocker.getComponentUnderTest().resend("batchId", "messageId");

        assertNull(result);
        assertEquals("Can't find descriptor for the component [role = [interface org.xwiki.mail.MailListener] "
            + "hint = [database]]", this.mocker.getComponentUnderTest().getLastError().getMessage());
    }

    @Test
    public void resendWhenMailContentStoreLoadingFails() throws Exception
    {
        this.mocker.registerComponent(MailListener.class, "database",
            this.mocker.getInstance(MailListener.class, "memory"));

        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(any(Session.class), eq("batchId"), eq("messageId"))).thenThrow(
            new MailStoreException("error"));

        ScriptMailResult result = this.mocker.getComponentUnderTest().resend("batchId", "messageId");

        assertNull(result);
        assertEquals("error", this.mocker.getComponentUnderTest().getLastError().getMessage());
    }

    @Test
    public void resend() throws Exception
    {
        MailListener memoryMailListener = this.mocker.getInstance(MailListener.class, "memory");
        this.mocker.registerComponent(MailListener.class, "database", memoryMailListener);

        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        String batchId = UUID.randomUUID().toString();
        message.setHeader("X-BatchID", batchId);
        message.setHeader("X-MailID", "messageId");

        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(any(Session.class), eq(batchId), eq("messageId"))).thenReturn(message);

        MailQueueManager<SendMailQueueItem> mailQueueManager = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, MailQueueManager.class, SendMailQueueItem.class));

        MailSender sender = this.mocker.getInstance(MailSender.class);
        when(sender.sendAsynchronously(eq(Arrays.asList(message)), any(Session.class),
            same(memoryMailListener))).thenReturn(new DefaultMailResult(batchId, mailQueueManager));

        ScriptMailResult result = this.mocker.getComponentUnderTest().resend(batchId, "messageId");

        assertNotNull(result);
        assertEquals(batchId, result.getBatchId());
    }

    @Test
    public void loadWhenNotAuthorized() throws Exception
    {
        ContextualAuthorizationManager authorizationManager =
            this.mocker.getInstance(ContextualAuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);

        List<MailStatus> result = this.mocker.getComponentUnderTest().load(
            Collections.<String, Object>emptyMap(), 0, 0);

        assertNull(result);
        assertEquals("You need Admin rights to load mail statuses",
            this.mocker.getComponentUnderTest().getLastError().getMessage());
    }
}
