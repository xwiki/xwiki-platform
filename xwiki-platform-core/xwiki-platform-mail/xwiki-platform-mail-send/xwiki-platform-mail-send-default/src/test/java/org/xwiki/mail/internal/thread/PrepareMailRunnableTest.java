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

import java.util.Arrays;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStoreException;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.mail.internal.UpdateableMailStatusResult;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.thread.PrepareMailRunnable}.
 *
 * @version $Id$
 * @since 6.4
 */
@ComponentList({MemoryMailListener.class, PrepareMailQueueManager.class})
public class PrepareMailRunnableTest
{
    @Rule
    public AllLogRule logRule = new AllLogRule();

    @Rule
    public MockitoComponentMockingRule<PrepareMailRunnable> mocker = new MockitoComponentMockingRule<>(
        PrepareMailRunnable.class);

    @Before
    public void setUp() throws Exception
    {
        Provider<XWikiContext> xwikiContextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xwikiContextProvider.get()).thenReturn(Mockito.mock(XWikiContext.class));
    }

    @Test
    public void prepareMailWhenContentStoreFails() throws Exception
    {
        Properties properties = new Properties();
        Session session = Session.getDefaultInstance(properties);

        MimeMessage message1 = new MimeMessage(session);
        message1.setText("Content1");

        MimeMessage message2 = new MimeMessage(session);
        message2.setText("Content2");

        String batchId1 = UUID.randomUUID().toString();
        String batchId2 = UUID.randomUUID().toString();

        ExecutionContext context1 = new ExecutionContext();
        XWikiContext xContext1 = new XWikiContext();
        xContext1.setWikiId("wiki1");
        context1.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xContext1);

        ExecutionContext context2 = new ExecutionContext();
        XWikiContext xContext2 = new XWikiContext();
        xContext2.setWikiId("wiki2");
        context2.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xContext2);

        MemoryMailListener listener1 = this.mocker.getInstance(MailListener.class, "memory");
        PrepareMailQueueItem item1 =
            new PrepareMailQueueItem(Arrays.asList(message1), session, listener1, batchId1, context1);
        MemoryMailListener listener2 = this.mocker.getInstance(MailListener.class, "memory");
        PrepareMailQueueItem item2 =
            new PrepareMailQueueItem(Arrays.asList(message2), session, listener2, batchId2, context2);

        MailQueueManager mailQueueManager =
            this.mocker.getInstance(new DefaultParameterizedType(null, MailQueueManager.class,
                PrepareMailQueueItem.class));

        // Make the content store save fail
        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        doThrow(new MailStoreException("error")).when(contentStore).save(any(String.class),
            any(ExtendedMimeMessage.class));

        // Prepare 2 mails. Both will fail but we want to verify that the second one is processed even though the first
        // one failed.
        mailQueueManager.addToQueue(item1);
        mailQueueManager.addToQueue(item2);

        MailRunnable runnable = this.mocker.getComponentUnderTest();
        Thread thread = new Thread(runnable);
        thread.start();

        // Wait for the mails to have been processed.
        try {
            listener1.getMailStatusResult().waitTillProcessed(10000L);
            listener2.getMailStatusResult().waitTillProcessed(10000L);
        } finally {
            runnable.stopProcessing();
            thread.interrupt();
            thread.join();
        }

        MailStatusResult result1 = listener1.getMailStatusResult();
        MailStatusResult result2 = listener2.getMailStatusResult();

        // Despite the errors, both process should be ended with known total number of mails
        assertTrue(result1.isProcessed());
        assertTrue(result2.isProcessed());

        // This is the real test: we verify that there's been an error while sending each email.
        MailStatus status1 = result1.getByState(MailState.PREPARE_ERROR).next();
        assertEquals("MailStoreException: error", status1.getErrorSummary());
        MailStatus status2 = result2.getByState(MailState.PREPARE_ERROR).next();
        assertEquals("MailStoreException: error", status2.getErrorSummary());
    }

    @Test
    public void prepareMailWhenIteratorFails() throws Exception
    {
        Properties properties = new Properties();
        Session session = Session.getDefaultInstance(properties);

        final MimeMessage message1 = new MimeMessage(session);
        message1.setText("Content1");

        MimeMessage message2 = new MimeMessage(session);
        message2.setText("Content2");

        String batchId1 = UUID.randomUUID().toString();
        String batchId2 = UUID.randomUUID().toString();

        ExecutionContext context1 = new ExecutionContext();
        XWikiContext xContext1 = new XWikiContext();
        xContext1.setWikiId("wiki1");
        context1.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xContext1);

        ExecutionContext context2 = new ExecutionContext();
        XWikiContext xContext2 = new XWikiContext();
        xContext2.setWikiId("wiki2");
        context2.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xContext2);

        MemoryMailListener listener1 = this.mocker.getInstance(MailListener.class, "memory");
        PrepareMailQueueItem item1 =
            new PrepareMailQueueItem(new Iterable<MimeMessage>()
                {
                    @Override
                    public Iterator<MimeMessage> iterator()
                    {
                        return new Iterator<MimeMessage>()
                        {
                            int index = 0;

                            @Override
                            public boolean hasNext()
                            {
                                return true;
                            }

                            @Override
                            public MimeMessage next()
                            {
                                if (index++ == 0) {
                                    return message1;
                                }
                                throw new RuntimeException("Iterator failure");
                            }

                            @Override
                            public void remove()
                            {

                            }
                        };
                    }
                }, session, listener1, batchId1, context1);
        MemoryMailListener listener2 = this.mocker.getInstance(MailListener.class, "memory");
        PrepareMailQueueItem item2 =
            new PrepareMailQueueItem(Arrays.asList(message2), session, listener2, batchId2, context2);

        MailQueueManager prepareMailQueueManager =
            this.mocker.getInstance(new DefaultParameterizedType(null, MailQueueManager.class,
                PrepareMailQueueItem.class));

        MailQueueManager sendMailQueueManager =
            this.mocker.getInstance(new DefaultParameterizedType(null, MailQueueManager.class,
                SendMailQueueItem.class));

        MailContentStore contentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                Object[] args = invocationOnMock.getArguments();
                MimeMessage message = (MimeMessage) args[1];
                message.saveChanges();
                return null;
            }
        }).when(contentStore).save(any(String.class), any(ExtendedMimeMessage.class));

        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock)
            {
                Object[] args = invocationOnMock.getArguments();
                SendMailQueueItem item = (SendMailQueueItem) args[0];
                ((UpdateableMailStatusResult)item.getListener().getMailStatusResult()).incrementCurrentSize();
                return null;
            }
        }).when(sendMailQueueManager).addToQueue(any(SendMailQueueItem.class));


        // Prepare 2 mails. Both will fail but we want to verify that the second one is processed even though the first
        // one failed.
        prepareMailQueueManager.addToQueue(item1);
        prepareMailQueueManager.addToQueue(item2);

        MailRunnable runnable = this.mocker.getComponentUnderTest();
        Thread thread = new Thread(runnable);
        thread.start();

        // Wait for the mails to have been processed.
        try {
            listener1.getMailStatusResult().waitTillProcessed(10000L);
            listener2.getMailStatusResult().waitTillProcessed(10000L);
        } finally {
            runnable.stopProcessing();
            thread.interrupt();
            thread.join();
        }

        MailStatusResult result1 = listener1.getMailStatusResult();
        MailStatusResult result2 = listener2.getMailStatusResult();

        // Despite the errors, both process should be ended with known total number of mails
        assertTrue(result1.isProcessed());
        assertTrue(result2.isProcessed());

        // This is the real test: we verify that there's been an error while sending each email.
        assertNotNull(listener1.getMailStatusResult().getByState(MailState.PREPARE_SUCCESS).next());
        assertNotNull(listener2.getMailStatusResult().getByState(MailState.PREPARE_SUCCESS).next());
        assertFalse(listener1.getMailStatusResult().getByState(MailState.PREPARE_ERROR).hasNext());
        assertFalse(listener2.getMailStatusResult().getByState(MailState.PREPARE_ERROR).hasNext());

        assertEquals("Failure during preparation phase of thread [" + batchId1 + "]", logRule.getMessage(0));
    }
}
