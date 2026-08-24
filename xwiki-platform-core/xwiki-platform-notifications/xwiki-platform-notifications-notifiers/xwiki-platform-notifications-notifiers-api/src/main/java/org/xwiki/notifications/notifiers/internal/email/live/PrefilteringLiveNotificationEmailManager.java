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
package org.xwiki.notifications.notifiers.internal.email.live;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.concurrent.ExecutionContextRunnable;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.internal.DefaultEntityEvent;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.notifiers.internal.email.IntervalUsersManager;
import org.xwiki.notifications.preferences.NotificationEmailInterval;

/**
 * Dispatch events to users with live email notifications enabled.
 * 
 * @version $Id$
 * @since 12.6
 */
@Component(roles = PrefilteringLiveNotificationEmailManager.class)
@Singleton
public class PrefilteringLiveNotificationEmailManager implements Initializable, Disposable
{
    private static final EntityEvent STOP = new DefaultEntityEvent(null, null);

    @Inject
    private PrefilteringLiveNotificationEmailDispatcher dispatcher;

    @Inject
    private JobExecutor executor;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private IntervalUsersManager intervals;

    @Inject
    private Logger logger;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    private final BlockingQueue<EntityEvent> preQueue = new LinkedBlockingQueue<>();

    private boolean disposed;

    @Override
    public void initialize() throws InitializationException
    {
        // Start the pre queue thread
        Thread optimizeThreadthread = new Thread(new ExecutionContextRunnable(this::prepare, this.componentManager));
        optimizeThreadthread.setName("Pre filtering Live mail notification optimizer");
        optimizeThreadthread.setPriority(Thread.NORM_PRIORITY - 1);
        optimizeThreadthread.setDaemon(true);
        optimizeThreadthread.start();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.disposed = true;
        this.preQueue.clear();
        this.preQueue.add(STOP);
    }

    private void prepare()
    {
        while (!this.disposed) {
            EntityEvent event;
            try {
                event = this.preQueue.take();
            } catch (InterruptedException e) {
                this.logger.warn("The thread handling live event optimization has been interrupted", e);

                Thread.currentThread().interrupt();
                break;
            }

            if (event != STOP) {
                DocumentReference userDocumentReference =
                    this.resolver.resolve(event.getEntityId(), event.getEvent().getWiki());

                // Check if live mail is enabled for this user
                NotificationEmailInterval interval = this.intervals.getInterval(userDocumentReference);
                if (interval == NotificationEmailInterval.LIVE) {
                    this.dispatcher.addEvent(event.getEvent(), userDocumentReference);
                }
            }
        }
    }

    /**
     * @param entityEvent an event associated to a specific user
     * @throws InterruptedException if interrupted while waiting
     */
    public void addEvent(EntityEvent entityEvent)
    {
        if (!this.preQueue.add(entityEvent)) {
            this.logger.warn(
                "The event [{}] could not be added to the queue of live notification mails. "
                    + "It generally means the queue was already full because more event are produced than sent.",
                entityEvent);
        }
    }

    /**
     * @param wikiId the identifier of the wiki in which to search for user with enabled live notifications mails
     * @since 12.6
     */
    public void addEvents(String wikiId)
    {
        try {
            this.executor.execute(MissingLiveNotificationMailsJob.JOBTYPE,
                new MissingLiveNotificationMailsRequest(wikiId));
        } catch (JobException e) {
            this.logger.error("Failed to start the processing of missed live notification events", e);
        }
    }
}
