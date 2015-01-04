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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import javax.inject.Provider;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.MailSenderRunnable}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@ComponentList({
    MemoryMailListener.class
})
public class MailSenderRunnableTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private MailSenderConfiguration configuration;

    private Provider<XWikiContext> xwikiContextProvider;

    private ExecutionContextManager executionContextManager;

    @BeforeComponent
    public void setUpComponents() throws Exception
    {
        this.configuration = this.componentManager.registerMockComponent(MailSenderConfiguration.class);

        this.xwikiContextProvider = this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        when(this.xwikiContextProvider.get()).thenReturn(Mockito.mock(XWikiContext.class));

        this.executionContextManager = this.componentManager.registerMockComponent(ExecutionContextManager.class);
    }

    @Test
    public void runInternalWhenMailSendingFails() throws Exception
    {
        // Create a Session with an invalid host so that it generates an error
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", "xwiki-unknown");
        Session session = Session.getDefaultInstance(properties);
        MimeMessage message = new MimeMessage(session);
        message.setSubject("subject");
        message.setFrom(InternetAddress.parse("john@doe.com")[0]);
        MemoryMailListener listener = this.componentManager.getInstance(MailListener.class, "memory");
        UUID batchId = UUID.randomUUID();
        MailSenderQueueItem item = new MailSenderQueueItem(Arrays.asList(message), session, listener, batchId, "wiki");

        MailQueueManager mailQueueManager = new MailQueueManager();

        // Send 2 mails. Both will fail but we want to verify that the second one is processed even though the first
        // one failed.
        mailQueueManager.addToQueue(item);
        mailQueueManager.addToQueue(item);

        MailSenderRunnable runnable = new MailSenderRunnable(mailQueueManager, this.configuration,
            this.xwikiContextProvider, this.executionContextManager);
        Thread thread = new Thread(runnable);
        thread.start();

        // Wait for the mails to have been processed.
        try {
            mailQueueManager.waitTillSent(batchId, 10000L);
        } finally {
            runnable.stopProcessing();
            thread.interrupt();
            thread.join();
        }

        // This is the real test: we verify that there's been an error while sending each email.
        Iterator<MailStatus> statuses = listener.getMailStatusResult().getByState(MailState.FAILED);
        int errorCount = 0;
        while (statuses.hasNext()) {
            MailStatus status = statuses.next();
            // Note: I would have liked to assert the exact message but it seems there can be different ones returned.
            // During my tests I got 2 different ones:
            // "UnknownHostException: xwiki-unknown"
            // "ConnectException: Connection refused"
            // Thus for now I only assert that there's an error set, but not its content.
            assertTrue(status.getError() != null);
            errorCount++;
        }
        assertEquals(2, errorCount);
    }
}
