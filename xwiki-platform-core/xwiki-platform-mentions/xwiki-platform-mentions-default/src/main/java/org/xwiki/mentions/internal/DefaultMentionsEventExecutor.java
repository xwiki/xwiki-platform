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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Default implementation of {@link MentionsEventExecutor}.
 *
 * This class is in charge of the management of mentions task analysis.
 * First, when {@link DefaultMentionsEventExecutor#execute(DocumentReference, DocumentReference, String)} is called,
 * a {@link MentionsData} is added to a queue.
 * Then, a pool of {@link MentionsConsumer} workers is consuming the queue and delegate the actual analyis to
 * {@link MentionsDataConsumer#consume(MentionsData)} which deals with actual implementation of the user mentions 
 * analysis.  
 *
 * @version $Id$
 * @since 12.6RC1
 */
@Component
@Singleton
public class DefaultMentionsEventExecutor implements MentionsEventExecutor, Initializable
{
    private ThreadPoolExecutor executor;

    private BlockingQueue<MentionsData> queue;

    @Inject
    private Logger logger;

    @Inject
    private MentionsBlockingQueueProvider blockingQueueProvider;

    @Inject
    private MentionsThreadPoolProvider threadPoolProvider;

    @Inject
    private MentionsDataConsumer dataConsumer;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Override
    public void initialize()
    {
        this.executor = this.threadPoolProvider.initializePool();
        this.queue = this.blockingQueueProvider.initBlockingQueue();
        IntStream
            .range(0, this.threadPoolProvider.getPoolSize())
            .forEach(i -> this.executor.execute(new MentionsConsumer()));
    }

    @Override
    public void execute(DocumentReference documentReference, DocumentReference authorReference, String version)
    {
        try {
            this.queue.put(new MentionsData()
                               .setDocumentReference(documentReference.toString())
                               .setAuthorReference(authorReference.toString())
                               .setVersion(version)
                               .setWikiId(documentReference.getWikiReference().getName())
            );
        } catch (InterruptedException e) {
            this.logger
                .warn("Error while adding a task to the mentions analysis queue. Cause [{}]", getRootCauseMessage(e));
        }
    }

    @Override
    public long getQueueSize()
    {
        return this.queue.size();
    }

    @Override
    public void clearQueue() throws AccessDeniedException
    {
        this.authorizationManager.checkAccess(Right.ADMIN);
        this.queue.clear();
    }

    /**
     * Consumer of the mentions analysis task queue.
     */
    public class MentionsConsumer implements Runnable
    {
        @Override
        public void run()
        {
            while (true) {
                runOnce();
            }
        }

        /**
         * Consume a single queue task.
         */
        public void runOnce()
        {
            MentionsData data = null;
            try {
                data = DefaultMentionsEventExecutor.this.queue.take();
                DefaultMentionsEventExecutor.this.logger
                    .debug("[{}] - [{}] consumed. Queue size [{}]", data.getDocumentReference(), data.getVersion(),
                        DefaultMentionsEventExecutor.this.queue.size());

                DefaultMentionsEventExecutor.this.dataConsumer.consume(data);
            } catch (Exception e) {
                DefaultMentionsEventExecutor.this.logger
                    .warn("Error during mention analysis of task [{}]. Cause [{}].", data, getRootCauseMessage(e));
                if (data != null) {
                    // push back the failed task at the beginning of the queue.
                    try {
                        DefaultMentionsEventExecutor.this.queue.put(data);
                    } catch (InterruptedException interruptedException) {
                        DefaultMentionsEventExecutor.this.logger
                            .error("Error when adding back a fail failed task [{}]. Cause [{}].", data,
                                getRootCauseMessage(e));
                    }
                }
            }
        }
    }
}
