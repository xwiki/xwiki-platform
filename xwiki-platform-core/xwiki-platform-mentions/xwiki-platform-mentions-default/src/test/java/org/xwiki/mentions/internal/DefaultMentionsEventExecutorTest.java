package org.xwiki.mentions.internal;/*
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xwiki.mentions.internal.DefaultMentionsEventExecutor.MentionsConsumer;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultMentionsEventExecutor}.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@ComponentTest
class DefaultMentionsEventExecutorTest
{
    @InjectMockComponents
    private DefaultMentionsEventExecutor executor;

    @MockComponent
    private MentionsThreadPoolProvider threadPoolProvider;

    @MockComponent
    private MentionsBlockingQueueProvider blockingQueueProvider;

    private ThreadPoolExecutor threadPoolExecutor;

    private BlockingQueue<MentionsData> blockingQueue;

    @BeforeComponent
    void beforeComponent()
    {
        when(this.threadPoolProvider.getPoolSize()).thenReturn(2);
        this.threadPoolExecutor = mock(ThreadPoolExecutor.class);
        when(this.threadPoolProvider.initializePool()).thenReturn(this.threadPoolExecutor);
        this.blockingQueue = mock(BlockingQueue.class);
        when(this.blockingQueueProvider.initBlockingQueue()).thenReturn(this.blockingQueue);
    }

    @Test
    void initialize()
    {
        verify(this.threadPoolProvider).initializePool();
        verify(this.blockingQueueProvider).initBlockingQueue();
        verify(this.threadPoolExecutor, Mockito.times(this.threadPoolProvider.getPoolSize()))
            .execute(any(MentionsConsumer.class));
    }

    @Test
    void execute() throws InterruptedException
    {
        this.executor.execute(new DocumentReference("xwiki", "XWiki", "Doc"),
            new DocumentReference("xwiki", "XWiki", "Author"), "1.0");

        verify(this.blockingQueue)
            .put(new MentionsData()
                     .setWikiId("xwiki")
                     .setAuthorReference("xwiki:XWiki.Author")
                     .setDocumentReference("xwiki:XWiki.Doc")
                     .setVersion("1.0"));
    }

    @Test
    void getQueueSize()
    {
        this.executor.getQueueSize();
        verify(this.blockingQueue).size();
    }

    @Test
    void clearQueue() throws AccessDeniedException
    {
        this.executor.clearQueue();
        verify(this.blockingQueue).clear();
    }
}