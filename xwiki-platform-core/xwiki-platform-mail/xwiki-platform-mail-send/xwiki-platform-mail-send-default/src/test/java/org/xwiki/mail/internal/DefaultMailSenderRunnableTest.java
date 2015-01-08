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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMailSenderRunnable}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@ComponentList({
    MemoryMailListener.class,
    DefaultMailQueueManager.class
})
public class DefaultMailSenderRunnableTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMailSenderRunnable> mocker =
        new MockitoComponentMockingRule<>(DefaultMailSenderRunnable.class);

    @Before
    public void setUp() throws Exception
    {
        Provider<XWikiContext> xwikiContextProvider = this.mocker.getInstance(
            new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        when(xwikiContextProvider.get()).thenReturn(Mockito.mock(XWikiContext.class));
    }

    @Test
    public void runInternalWhenMailSendingFails() throws Exception
    {
        // Create a Session with an invalid host so that it generates an error
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", "xwiki-unknown");
        Session session = Session.getDefaultInstance(properties);

        MimeMessage message1 = new MimeMessage(session);
        message1.setSubject("subject1");
        message1.setFrom(InternetAddress.parse("john1@doe.com")[0]);

        MimeMessage message2 = new MimeMessage(session);
        message2.setSubject("subject2");
        message2.setFrom(InternetAddress.parse("john2@doe.com")[0]);

        MemoryMailListener listener = this.mocker.getInstance(MailListener.class, "memory");
        UUID batchId = UUID.randomUUID();

        MailSenderQueueItem item1 =
            new MailSenderQueueItem(Arrays.asList(message1), session, listener, batchId, "wiki1");
        MailSenderQueueItem item2 =
            new MailSenderQueueItem(Arrays.asList(message2), session, listener, batchId, "wiki2");

        MailQueueManager mailQueueManager = this.mocker.getInstance(MailQueueManager.class);

        // Send 2 mails. Both will fail but we want to verify that the second one is processed even though the first
        // one failed.
        mailQueueManager.addToQueue(item1);
        mailQueueManager.addToQueue(item2);

        MailSenderRunnable runnable = this.mocker.getComponentUnderTest();
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
            assertTrue(status.getErrorSummary() != null);
            errorCount++;
        }
        assertEquals(2, errorCount);
    }
}
