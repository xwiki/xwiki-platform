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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.mentions.internal.jmx.JMXMentions;
import org.xwiki.mentions.internal.jmx.JMXMentionsMBean;
import org.xwiki.model.reference.DocumentReference;

import static java.util.concurrent.TimeUnit.SECONDS;
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
public class DefaultMentionsEventExecutor implements MentionsEventExecutor, Initializable, Disposable
{
    private static final String MBEAN_NAME = "name=mentions";

    private List<MentionsConsumer> consumers = new ArrayList<>();

    private BlockingQueue<MentionsData> queue;

    @Inject
    private Logger logger;

    @Inject
    private MentionsBlockingQueueProvider blockingQueueProvider;

    @Inject
    private MentionsThreadsProvider threadPoolProvider;

    @Inject
    private MentionsDataConsumer dataConsumer;

    @Inject
    private JMXBeanRegistration jmxRegistration;

    @Inject
    private ConfigurationSource configuration;

    @Override
    public void initialize()
    {
        this.queue = this.blockingQueueProvider.initBlockingQueue();
        this.consumers = new ArrayList<>();
        int nbThreads = this.configuration.getProperty("mentions.poolSize", 2);
        for (int i = 0; i < nbThreads; i++) {
            startConsumer();
        }
        JMXMentionsMBean mbean = new JMXMentions(this.queue, this::updateNbThreads, () -> this.consumers.size());
        this.jmxRegistration.registerMBean(mbean, MBEAN_NAME);
    }

    private void startConsumer()
    {
        MentionsConsumer runnable = new MentionsConsumer();
        this.consumers.add(runnable);
        this.threadPoolProvider
            .initializeThread(runnable)
            .start();
    }

    @Override
    public void dispose()
    {
        this.jmxRegistration.unregisterMBean(MBEAN_NAME);
        for (MentionsConsumer consumer : this.consumers) {
            consumer.halt();
        }
    }

    /**
     * Update the number of threads.
     *
     * If the number increases, starts new threads
     * If the number decreases, removes existing threads until the new number is reached
     * @param nbThread the new number of threads. Negative values are equivalent to 0.
     */
    public void updateNbThreads(int nbThread)
    {
        int currentNbThreads = this.consumers.size();
        if (nbThread > currentNbThreads) {
            for (int i = currentNbThreads; i < nbThread; i++) {
                startConsumer();
            }
        } else {
            for (int i = Math.max(nbThread, 0); i < currentNbThreads; i++) {
                MentionsConsumer remove = this.consumers.remove(0);
                remove.halt();
            }
        }
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

    /**
     * Consumer of the mentions analysis task queue.
     */
    public class MentionsConsumer implements Runnable
    {
        private boolean halt;

        @Override
        public void run()
        {
            while (!this.halt) {
                consume();
            }
        }

        /**
         * Consume a single queue task.
         */
        public void consume()
        {
            MentionsData data = null;
            try {
                // slightly active wait in order to let the loop be stopped when halt is set to true, once every 
                // 10 seconds.
                data = DefaultMentionsEventExecutor.this.queue.poll(10, SECONDS);
                if (data != null) {
                    DefaultMentionsEventExecutor.this.logger
                        .debug("[{}] - [{}] consumed. Queue size [{}]", data.getDocumentReference(), data.getVersion(),
                            DefaultMentionsEventExecutor.this.queue.size());

                    DefaultMentionsEventExecutor.this.dataConsumer.consume(data);
                }
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

        /**
         * Ask the consumer to stop its work.
         */
        public void halt()
        {
            this.halt = true;
        }
    }

    /**
     * 
     * @return the list of consumers
     */
    public List<MentionsConsumer> getConsumers()
    {
        return this.consumers;
    }
}
