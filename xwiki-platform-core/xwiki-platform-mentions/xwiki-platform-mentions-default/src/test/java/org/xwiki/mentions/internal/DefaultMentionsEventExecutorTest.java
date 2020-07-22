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
package org.xwiki.mentions.internal;

import java.util.concurrent.BlockingQueue;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.mentions.internal.jmx.JMXMentions;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.DEBUG;

/**
 * Test of {@link DefaultMentionsEventExecutor}.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@ComponentTest
class DefaultMentionsEventExecutorTest
{
    private static final int NB_THREADS = 2;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(DEBUG);

    @InjectMockComponents
    private DefaultMentionsEventExecutor executor;

    @MockComponent
    private MentionsThreadsProvider threadPoolProvider;

    @MockComponent
    private MentionsBlockingQueueProvider blockingQueueProvider;

    private BlockingQueue<MentionsData> blockingQueue;

    @MockComponent
    private JMXBeanRegistration jmxRegistration;

    @MockComponent
    private MentionsDataConsumer dataConsumer;

    @MockComponent
    private ConfigurationSource configuration;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @BeforeComponent
    void beforeComponent()
    {
        when(this.threadPoolProvider.initializeThread(any(Runnable.class))).thenReturn(mock(Thread.class));
        this.blockingQueue = mock(BlockingQueue.class);
        when(this.blockingQueueProvider.initBlockingQueue()).thenReturn(this.blockingQueue);
        when(this.configuration.getProperty("mentions.poolSize", 1)).thenReturn(NB_THREADS);
    }

    @Test
    void initialize()
    {
        verify(this.blockingQueueProvider).initBlockingQueue();
        verify(this.jmxRegistration).registerMBean(any(JMXMentions.class), eq("name=mentions"));
    }

    @Test
    void execute() throws InterruptedException
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Author");
        when(this.serializer.serialize(documentReference)).thenReturn("xwiki:XWiki.Doc");
        when(this.serializer.serialize(authorReference)).thenReturn("xwiki:XWiki.Author");

        this.executor.execute(documentReference, authorReference, "1.0");

        verify(this.blockingQueue)
            .put(new MentionsData()
                     .setWikiId("xwiki")
                     .setAuthorReference("xwiki:XWiki.Author")
                     .setDocumentReference("xwiki:XWiki.Doc")
                     .setVersion("1.0"));
    }

    @Test
    void executeInterruptedException() throws InterruptedException
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Author");
        MentionsData data = new MentionsData()
                                .setDocumentReference("xwiki:XWiki.Doc")
                                .setAuthorReference("xwiki:XWiki.Author")
                                .setVersion("1.0")
                                .setWikiId("xwiki");
        
        when(this.serializer.serialize(documentReference)).thenReturn("xwiki:XWiki.Doc");
        when(this.serializer.serialize(authorReference)).thenReturn("xwiki:XWiki.Author");

        doThrow(new InterruptedException()).when(this.blockingQueue).put(data);

        this.executor.execute(documentReference, authorReference, "1.0");

        assertEquals(1, this.logCapture.size());
        assertEquals(WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(
            "Error while adding the task [documentReference = [xwiki:XWiki.Doc], version = [1.0], "
                + "authorReference = [xwiki:XWiki.Author], wikiId = [xwiki]] "
                + "to the mentions analysis queue. Cause [InterruptedException: ]",
            this.logCapture.getMessage(0));
    }

    @Test
    void getQueueSize()
    {
        this.executor.getQueueSize();
        verify(this.blockingQueue).size();
    }

    @Test
    void consume() throws Exception
    {
        MentionsData value = new MentionsData()
                                 .setDocumentReference("xwiki:XWiki.Doc")
                                 .setAuthorReference("xwiki:XWiki.Author")
                                 .setWikiId("xwiki")
                                 .setVersion("1.0");
        when(this.blockingQueue.poll()).thenReturn(value);
        this.executor.startThreads();
        verify(this.threadPoolProvider, times(NB_THREADS)).initializeThread(any(Runnable.class));

        this.executor.getConsumers().get(0).consume();

        assertEquals(1, this.logCapture.size());
        assertEquals("[xwiki:XWiki.Doc] - [1.0] consumed. Queue size [0]", this.logCapture.getMessage(0));
        verify(this.dataConsumer).consume(value);
    }

    @Test
    void consumeOneError() throws Exception
    {
        MentionsData value = new MentionsData()
                                 .setDocumentReference("xwiki:XWiki.Doc")
                                 .setAuthorReference("xwiki:XWiki.Author")
                                 .setWikiId("xwiki")
                                 .setVersion("1.0");
        when(this.blockingQueue.poll()).thenReturn(value);
        this.executor.startThreads();
        verify(this.threadPoolProvider, times(NB_THREADS)).initializeThread(any(Runnable.class));

        doThrow(new RuntimeException("...")).when(this.dataConsumer).consume(value);

        IntStream.range(0, 11).forEach(i -> this.executor.getConsumers().get(0).consume());

        assertEquals(23, this.logCapture.size());
        IntStream.of(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
            .forEach(i -> assertEquals(Level.DEBUG, this.logCapture.getLogEvent(i).getLevel()));
        IntStream.of(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21)
            .forEach(i -> assertEquals(WARN, this.logCapture.getLogEvent(i).getLevel()));
        assertEquals(ERROR, this.logCapture.getLogEvent(22).getLevel());
        IntStream.of(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
            .forEach(
                i -> assertEquals("[xwiki:XWiki.Doc] - [1.0] consumed. Queue size [0]", this.logCapture.getMessage(i)));
        IntStream.of(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21)
            .forEach(i -> assertEquals(
                "Error during mention analysis of task "
                    + "[documentReference = [xwiki:XWiki.Doc], version = [1.0], "
                    + "authorReference = [xwiki:XWiki.Author], wikiId = [xwiki]]. "
                    + "Cause [RuntimeException: ...].", this.logCapture.getMessage(i)));

        assertEquals(
            "[documentReference = [xwiki:XWiki.Doc], version = [1.0], authorReference = [xwiki:XWiki.Author], "
                + "wikiId = [xwiki]] abandoned because it has failed to many times.",
            this.logCapture.getMessage(22));
    }
}
