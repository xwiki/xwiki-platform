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
package org.xwiki.store.filesystem.internal;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Disposable;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.store.blob.BlobStoreException;

/**
 * Deletes the temporary attachments older than {@code store.temporaryAttachments.maxAge}, whatever happened to the
 * editing session that uploaded them (closed browser, expired session, crashed cluster member).
 * <p>
 * The check runs every hour, starting at a random delay after the application is ready so that the cluster members do
 * not all list the store at the same time. Every member runs it: the deletions are idempotent, a member deleting an
 * upload that another member already deleted is not an error.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named(TemporaryAttachmentsCleaner.NAME)
@Singleton
public class TemporaryAttachmentsCleaner extends AbstractEventListener implements Disposable
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.store.filesystem.internal.TemporaryAttachmentsCleaner";

    private static final long PERIOD_MINUTES = 60;

    @Inject
    private TemporaryAttachmentStore store;

    @Inject
    private TemporaryAttachmentsConfiguration configuration;

    @Inject
    private Logger logger;

    private ScheduledExecutorService executor;

    /**
     * Default constructor.
     */
    public TemporaryAttachmentsCleaner()
    {
        super(NAME, new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        start();
    }

    /**
     * Starts the periodic cleanup, if not already started.
     */
    synchronized void start()
    {
        if (this.executor == null) {
            this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "XWiki temporary attachments cleaner");
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            });
            long initialDelay = 1 + ThreadLocalRandom.current().nextLong(PERIOD_MINUTES);
            this.executor.scheduleWithFixedDelay(this::run, initialDelay, PERIOD_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * Deletes the expired temporary attachments now.
     *
     * @return the number of deleted uploads
     * @throws BlobStoreException if the store cannot be listed
     */
    public int clean() throws BlobStoreException
    {
        Duration maxAge = this.configuration.getMaxAge();
        int count = this.store.deleteExpired(System.currentTimeMillis(), maxAge);
        if (count > 0) {
            this.logger.info("Deleted [{}] temporary attachments older than [{}] hours.", count, maxAge.toHours());
        } else {
            this.logger.debug("No temporary attachment older than [{}] hours.", maxAge.toHours());
        }

        return count;
    }

    /**
     * Runs one cleanup, logging failures so that the next runs are still scheduled.
     */
    void run()
    {
        try {
            clean();
        } catch (Exception e) {
            // Catch everything so that the next runs are still scheduled.
            this.logger.warn("Failed to delete the expired temporary attachments: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Override
    public synchronized void dispose()
    {
        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
    }
}
