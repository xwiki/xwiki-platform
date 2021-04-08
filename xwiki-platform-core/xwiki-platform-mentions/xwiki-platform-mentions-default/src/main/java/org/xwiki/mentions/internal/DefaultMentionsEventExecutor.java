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
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.mentions.internal.jmx.JMXMentions;
import org.xwiki.mentions.internal.jmx.JMXMentionsMBean;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Default implementation of {@link MentionsEventExecutor}.
 *
 * This class is in charge of the management of mentions task analysis.
 * First, when {@link DefaultMentionsEventExecutor#execute(DocumentReference, DocumentReference, String)} is called,
 * a {@link MentionsData} is added to a queue.
 * Then, a pool of {@link MentionsConsumer} workers is consuming the queue and delegate the actual analysis to
 * {@link MentionsDataConsumer#consume(MentionsData)} which deals with actual implementation of the user mentions 
 * analysis.  
 *
 * @version $Id$
 * @since 12.6
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

    @Inject
    private EntityReferenceSerializer<String> serializer;

    private boolean threadStarted;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.queue = this.blockingQueueProvider.initBlockingQueue();
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize the queue", e);
        }
        this.consumers = new ArrayList<>();
        JMXMentionsMBean mbean = new JMXMentions(this::getQueueSize, this::clearQueue, () -> this.consumers.size());
        this.jmxRegistration.registerMBean(mbean, MBEAN_NAME);
    }

    private void clearQueue()
    {
        this.queue.clear();
    }

    @Override
    public void startThreads()
    {
        synchronized (this) {
            if (!this.threadStarted) {
                int nbThreads = this.configuration.getProperty("mentions.poolSize", 1);
                for (int i = 0; i < nbThreads; i++) {
                    startConsumer();
                }
                this.threadStarted = true;
            }
        }
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
            // We ask the consumers to stop directly with halt, in case their is many elements in the queue.
            // Additionally we fill the queue with stop element in order to wake up consumers and notify them
            // to stop if the queue is empty.
            consumer.halt();
            this.queue.add(MentionsData.STOP);
        }
        this.blockingQueueProvider.closeQueue();
    }

    @Override
    public void execute(DocumentReference documentReference, DocumentReference authorReference, String version)
    {
        MentionsData mentionsData = new MentionsData()
                                        .setDocumentReference(this.serializer.serialize(documentReference));
        if (authorReference != null) {
            mentionsData = mentionsData
                               .setAuthorReference(this.serializer.serialize(authorReference));
        }

        MentionsData data = mentionsData
                                .setVersion(version)
                                .setWikiId(documentReference.getWikiReference().getName());
        try {
            this.queue.put(data);
        } catch (InterruptedException e) {
            this.logger
                .warn("Error while adding the task [{}] to the mentions analysis queue. Cause [{}]", data,
                    getRootCauseMessage(e));
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
                data = DefaultMentionsEventExecutor.this.queue.take();
                if (data != null) {
                    data.increaseAttempts();
                    if (data.isStop()) {
                        this.halt = true;
                    } else if (!data.isDeprecated()) {
                        DefaultMentionsEventExecutor.this.logger
                            .debug("[{}] - [{}] consumed. Queue size [{}]", data.getDocumentReference(),
                                data.getVersion(), getQueueSize());

                        DefaultMentionsEventExecutor.this.dataConsumer.consume(data);
                    }
                }
            } catch (Exception e) {
                DefaultMentionsEventExecutor.this.logger
                    .warn("Error during mention analysis of task [{}]. Cause [{}].", data, getRootCauseMessage(e));
                if (data != null) {
                    if (!data.tooManyAttempts()) {
                        // push back the failed task at the beginning of the queue.
                        try {
                            DefaultMentionsEventExecutor.this.queue.put(data);
                        } catch (InterruptedException interruptedException) {
                            DefaultMentionsEventExecutor.this.logger
                                .error("Error when adding back a fail failed task [{}]. Cause [{}].", data,
                                    getRootCauseMessage(e));
                        }
                    } else {
                        DefaultMentionsEventExecutor.this.logger
                            .error("[{}] abandoned because it has failed to many times.", data);
                    }
                }
            }
        }

        /**
         * Ask for the thread to stop.
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
