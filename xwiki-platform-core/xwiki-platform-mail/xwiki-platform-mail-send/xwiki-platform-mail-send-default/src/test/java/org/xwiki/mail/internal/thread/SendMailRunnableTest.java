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
package org.xwiki.mail.internal.thread;

import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import javax.inject.Provider;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStoreException;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.mail.internal.UpdateableMailStatusResult;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SendMailRunnable}.
 *
 * @version $Id$
 * @since 6.4
 */
@ComponentList({
    MemoryMailListener.class,
    SendMailQueueManager.class
})
public class SendMailRunnableTest
{
    @Rule
    public MockitoComponentMockingRule<SendMailRunnable> mocker =
        new MockitoComponentMockingRule<>(SendMailRunnable.class);

    @Before
    public void setUp() throws Exception
    {
        Provider<XWikiContext> xwikiContextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xwikiContextProvider.get()).thenReturn(Mockito.mock(XWikiContext.class));
    }

    @Test
    public void sendMailWhenSendingFails() throws Exception
    {
        // Create a Session with an invalid host so that it generates an error
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", "xwiki-unknown");
        Session session = Session.getDefaultInstance(properties);

        MimeMessage msg1 = new MimeMessage(session);
        msg1.setText("Content1");
        ExtendedMimeMessage message1 = new ExtendedMimeMessage(msg1);
        String id1 = message1.getUniqueMessageId();
        MimeMessage msg2 = new MimeMessage(session);
        msg2.setText("Content2");
        ExtendedMimeMessage message2 = new ExtendedMimeMessage(msg2);
        String id2 = message2.getUniqueMessageId();

        MemoryMailListener listener = this.mocker.getInstance(MailListener.class, "memory");
        String batchId = UUID.randomUUID().toString();
        listener.onPrepareBegin(batchId, Collections.emptyMap());
        ((UpdateableMailStatusResult) listener.getMailStatusResult()).setTotalSize(2);

        SendMailQueueItem item1 = new SendMailQueueItem(id1, session, listener, batchId, "xwiki");
        SendMailQueueItem item2 = new SendMailQueueItem(id2, session, listener, batchId, "xwiki");

        MailQueueManager mailQueueManager = this.mocker.getInstance(
            new DefaultParameterizedType(null, MailQueueManager.class, SendMailQueueItem.class));

        // Simulate loading the message from the content store
        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(session, batchId, id1)).thenReturn(message1);
        when(contentStore.load(session, batchId, id2)).thenReturn(message2);

        // Send 2 mails. Both will fail but we want to verify that the second one is processed even though the first
        // one failed.
        mailQueueManager.addToQueue(item1);
        mailQueueManager.addToQueue(item2);

        MailRunnable runnable = this.mocker.getComponentUnderTest();
        Thread thread = new Thread(runnable);
        thread.start();

        // Wait for the mails to have been processed.
        try {
            listener.getMailStatusResult().waitTillProcessed(10000L);
        } finally {
            runnable.stopProcessing();
            thread.interrupt();
            thread.join();
        }

        // This is the real test: we verify that there's been an error while sending each email.
        Iterator<MailStatus> statuses = listener.getMailStatusResult().getByState(MailState.SEND_ERROR);
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

    @Test
    public void sendMailWhenMailRetrievalFails() throws Exception
    {
        // Create a Session with an invalid host so that it generates an error
        Properties properties = new Properties();
        Session session = Session.getDefaultInstance(properties);

        MimeMessage msg1 = new MimeMessage(session);
        msg1.setText("Content1");
        ExtendedMimeMessage message1 = new ExtendedMimeMessage(msg1);
        String id1 = message1.getUniqueMessageId();
        MimeMessage msg2 = new MimeMessage(session);
        msg2.setText("Content2");
        ExtendedMimeMessage message2 = new ExtendedMimeMessage(msg2);
        String id2 = message2.getUniqueMessageId();

        MemoryMailListener listener = this.mocker.getInstance(MailListener.class, "memory");
        String batchId = UUID.randomUUID().toString();
        listener.onPrepareBegin(batchId, Collections.emptyMap());
        ((UpdateableMailStatusResult) listener.getMailStatusResult()).setTotalSize(2);

        listener.onPrepareMessageSuccess(message1, Collections.emptyMap());
        SendMailQueueItem item1 = new SendMailQueueItem(id1, session, listener, batchId, "xwiki");
        listener.onPrepareMessageSuccess(message2, Collections.emptyMap());
        SendMailQueueItem item2 = new SendMailQueueItem(id2, session, listener, batchId, "xwiki");

        MailQueueManager mailQueueManager = this.mocker.getInstance(
            new DefaultParameterizedType(null, MailQueueManager.class, SendMailQueueItem.class));

        // Simulate loading the message from the content store
        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        when(contentStore.load(session, batchId, id1)).thenThrow(new MailStoreException("Store failure on message 1"));
        when(contentStore.load(session, batchId, id2)).thenThrow(new MailStoreException("Store failure on message 2"));

        // Send 2 mails. Both will fail but we want to verify that the second one is processed even though the first
        // one failed.
        mailQueueManager.addToQueue(item1);
        mailQueueManager.addToQueue(item2);

        MailRunnable runnable = this.mocker.getComponentUnderTest();
        Thread thread = new Thread(runnable);
        thread.start();

        // Wait for the mails to have been processed.
        try {
            listener.getMailStatusResult().waitTillProcessed(10000L);
        } finally {
            runnable.stopProcessing();
            thread.interrupt();
            thread.join();
        }

        // This is the real test: we verify that there's been an error while sending each email.
        Iterator<MailStatus> statuses = listener.getMailStatusResult().getByState(MailState.SEND_FATAL_ERROR);
        int errorCount = 0;
        while (statuses.hasNext()) {
            MailStatus status = statuses.next();
            // Note: I would have liked to assert the exact message but it seems there can be different ones returned.
            // During my tests I got 2 different ones:
            // "UnknownHostException: xwiki-unknown"
            // "ConnectException: Connection refused"
            // Thus for now I only assert that there's an error set, but not its content.
            assertEquals("MailStoreException: Store failure on message " + ++errorCount, status.getErrorSummary());
        }
        assertEquals(2, errorCount);
    }
}
