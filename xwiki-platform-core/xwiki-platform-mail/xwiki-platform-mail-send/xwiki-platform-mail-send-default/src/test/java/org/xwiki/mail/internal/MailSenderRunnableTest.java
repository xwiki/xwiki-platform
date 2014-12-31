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
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

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
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertTrue;
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

        Queue<MailSenderQueueItem> mailQueue = new ConcurrentLinkedQueue<>();

        // Send 2 mails. Both will fail but we want to verify that the second one is processed even though the first
        // one failed.
        mailQueue.add(item);
        mailQueue.add(item);

        MailSenderRunnable runnable = new MailSenderRunnable(mailQueue, this.configuration, this.xwikiContextProvider,
            this.executionContextManager);
        Thread thread = new Thread(runnable);
        thread.start();

        // This is the real test: we verify that there's been an error while sending each email and that it's been
        // logged. This also proves that the Mail Sender Thread doesn't stop when there's an error sending an email.
        boolean success = true;
        try {
            long time = System.currentTimeMillis();
            while (listener.getErrorsNumber() != 2) {
                if (System.currentTimeMillis() - time > 5000L) {
                    success = false;
                    break;
                }
                Thread.sleep(100L);
            }
        } finally {
            runnable.stopProcessing();
            thread.interrupt();
            thread.join();
        }
        assertTrue(success);
    }
}
