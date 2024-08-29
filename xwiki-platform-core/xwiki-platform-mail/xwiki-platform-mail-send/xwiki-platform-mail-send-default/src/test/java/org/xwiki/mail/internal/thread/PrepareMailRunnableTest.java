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
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStoreException;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.mail.internal.UpdateableMailStatusResult;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.thread.PrepareMailRunnable}.
 *
 * @version $Id$
 * @since 6.4
 */
@ComponentTest
@ComponentList({
    MemoryMailListener.class,
    PrepareMailQueueManager.class
})
class PrepareMailRunnableTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private PrepareMailRunnable runnable;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @MockComponent
    private MailQueueManager<SendMailQueueItem> sendMailQueueManager;

    @BeforeComponent
    void beforeInitializable() throws Exception
    {
        MailSenderConfiguration configuration =
            this.componentManager.registerMockComponent(MailSenderConfiguration.class);
        when(configuration.getPrepareQueueCapacity()).thenReturn(10);
    }

    @BeforeEach
    void setUp()
    {
        when(this.xwikiContextProvider.get()).thenReturn(Mockito.mock(XWikiContext.class));
    }

    @Test
    void prepareMailWhenContentStoreFails() throws Exception
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

        MemoryMailListener listener1 = this.componentManager.getInstance(MailListener.class, "memory");
        PrepareMailQueueItem item1 =
            new PrepareMailQueueItem(Arrays.asList(message1), session, listener1, batchId1, context1);
        MemoryMailListener listener2 = this.componentManager.getInstance(MailListener.class, "memory");
        PrepareMailQueueItem item2 =
            new PrepareMailQueueItem(Arrays.asList(message2), session, listener2, batchId2, context2);

        MailQueueManager mailQueueManager =
            this.componentManager.getInstance(new DefaultParameterizedType(null, MailQueueManager.class,
                PrepareMailQueueItem.class));

        // Make the content store save fail
        MailContentStore contentStore = this.componentManager.getInstance(MailContentStore.class, "filesystem");
        doThrow(new MailStoreException("error")).when(contentStore).save(any(String.class),
            any(ExtendedMimeMessage.class));

        // Prepare 2 mails. Both will fail, but we want to verify that the second one is processed even though the first
        // one failed.
        mailQueueManager.addToQueue(item1);
        mailQueueManager.addToQueue(item2);

        Thread thread = new Thread(this.runnable);
        thread.start();

        // Wait for the mails to have been processed.
        try {
            listener1.getMailStatusResult().waitTillProcessed(10000L);
            listener2.getMailStatusResult().waitTillProcessed(10000L);
        } finally {
            this.runnable.stopProcessing();
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
    void prepareMailWhenIteratorFails() throws Exception
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

        MemoryMailListener listener1 = this.componentManager.getInstance(MailListener.class, "memory");
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
        MemoryMailListener listener2 = this.componentManager.getInstance(MailListener.class, "memory");
        PrepareMailQueueItem item2 =
            new PrepareMailQueueItem(Arrays.asList(message2), session, listener2, batchId2, context2);

        MailQueueManager prepareMailQueueManager =
            this.componentManager.getInstance(new DefaultParameterizedType(null, MailQueueManager.class,
                PrepareMailQueueItem.class));

        MailQueueManager sendMailQueueManager =
            this.componentManager.getInstance(new DefaultParameterizedType(null, MailQueueManager.class,
                SendMailQueueItem.class));

        MailContentStore contentStore = this.componentManager.getInstance(MailContentStore.class, "filesystem");
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
                return true;
            }
        }).when(sendMailQueueManager).addMessageToQueue(any(SendMailQueueItem.class), anyLong(), any(TimeUnit.class));


        // Prepare 2 mails. Both will fail, but we want to verify that the second one is processed even though the first
        // one failed.
        prepareMailQueueManager.addToQueue(item1);
        prepareMailQueueManager.addToQueue(item2);

        Thread thread = new Thread(this.runnable);
        thread.start();

        // Wait for the mails to have been processed.
        try {
            listener1.getMailStatusResult().waitTillProcessed(10000L);
            listener2.getMailStatusResult().waitTillProcessed(10000L);
        } finally {
            this.runnable.stopProcessing();
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

        assertEquals("Failure during preparation phase of thread [" + batchId1 + "]", logCapture.getMessage(0));
    }

    @Test
    void prepareMailWhenSendQueueFull() throws Exception
    {
        Properties properties = new Properties();
        Session session = Session.getDefaultInstance(properties);

        MimeMessage message = new MimeMessage(session);
        message.setText("Content");

        String batchId = UUID.randomUUID().toString();

        ExecutionContext context = new ExecutionContext();
        XWikiContext xContext = new XWikiContext();
        xContext.setWikiId("wiki");
        context.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xContext);

        MemoryMailListener listener = this.componentManager.getInstance(MailListener.class, "memory");
        PrepareMailQueueItem item =
            new PrepareMailQueueItem(Arrays.asList(message), session, listener, batchId, context);

        MailQueueManager mailQueueManager =
            this.componentManager.getInstance(new DefaultParameterizedType(null, MailQueueManager.class,
                PrepareMailQueueItem.class));

        // Simulate a full sender queue
        when(this.sendMailQueueManager.addMessageToQueue(any(SendMailQueueItem.class), anyLong(),
            any(TimeUnit.class))).thenReturn(false);

        // The mail is expected to fail since the queue is full.
        mailQueueManager.addToQueue(item);

        Thread thread = new Thread(this.runnable);
        thread.start();

        // Wait for the mail to have been processed.
        try {
            listener.getMailStatusResult().waitTillProcessed(10000L);
        } finally {
            this.runnable.stopProcessing();
            thread.interrupt();
            thread.join();
        }

        assertEquals(1, logCapture.size());
        assertLinesMatch(List.of("The following mail items couldn't be added to the send queue, which is full: \\["
            + ".*\\]. They will need to be resent later on."), List.of(logCapture.getMessage(0)));
    }

    @Test
    void prepareMailWhenAddInterrupted() throws Exception
    {
        Properties properties = new Properties();
        Session session = Session.getDefaultInstance(properties);

        MimeMessage message = new MimeMessage(session);
        message.setText("Content");

        String batchId = UUID.randomUUID().toString();

        ExecutionContext context = new ExecutionContext();
        XWikiContext xContext = new XWikiContext();
        xContext.setWikiId("wiki");
        context.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xContext);

        MailListener listener = mock(MailListener.class);
        PrepareMailQueueItem item =
            new PrepareMailQueueItem(Arrays.asList(message), session, listener, batchId, context);

        MailQueueManager mailQueueManager =
            this.componentManager.getInstance(new DefaultParameterizedType(null, MailQueueManager.class,
                PrepareMailQueueItem.class));

        // Simulate an interruption when adding to the send queue
        doThrow(new InterruptedException("error")).when(this.sendMailQueueManager)
            .addMessageToQueue(any(SendMailQueueItem.class), anyLong(), any(TimeUnit.class));

        mailQueueManager.addToQueue(item);

        Thread thread = new Thread(this.runnable);
        thread.start();
        // Wait at most 5mn for the thread to die. This should happen since the interrupt exception should cause it
        // to stop. This is part of the test.
        thread.join(5*60*1000L);

        // Verify that the listener has received a fatal error and that the exception is the one we expect.
        ArgumentCaptor<RuntimeException> exceptionArgumentCaptor = ArgumentCaptor.forClass(RuntimeException.class);
        verify(listener).onPrepareFatalError(exceptionArgumentCaptor.capture(), any());
        assertLinesMatch(List.of("The following mail items couldn't be added to the send queue as it was interrupted: "
            + "\\[.*\\]. The messages are not lost and can be resent later on."),
            List.of(exceptionArgumentCaptor.getValue().getMessage()));
    }
}
