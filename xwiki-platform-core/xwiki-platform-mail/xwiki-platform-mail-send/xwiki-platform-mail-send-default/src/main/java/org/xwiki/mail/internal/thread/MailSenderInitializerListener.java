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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Used to trigger the mails Thread used to prepare and send mails. Note that we want application to be ready before
 * starting the mail threads since sending mail require access to the configuration which is usually defined in wiki
 * pages.
 *
 * @version $Id$
 * @since 7.4.5
 */
@Component
@Named(MailSenderInitializerListener.LISTENER_NAME)
@Singleton
public class MailSenderInitializerListener implements EventListener, Disposable
{
    /**
     * The name of the listener.
     */
    public static final String LISTENER_NAME = "MailSenderInitializationEventListener";

    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = new ArrayList<Event>(Arrays.asList(new ApplicationReadyEvent()));

    /**
     * Logger to use to log shutdown information (opposite of initialization).
     */
    private static final Logger SHUTDOWN_LOGGER = LoggerFactory.getLogger("org.xwiki.shutdown");

    @Inject
    @Named("prepare")
    private MailRunnable prepareMailRunnable;

    @Inject
    @Named("send")
    private MailRunnable sendMailRunnable;

    private Thread prepareMailThread;

    private Thread sendMailThread;

    @Override
    public String getName()
    {
        return LISTENER_NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object o, Object o1)
    {
        // Step 1: Start the Mail Prepare Thread
        this.prepareMailThread = new Thread(this.prepareMailRunnable);
        this.prepareMailThread.setName("Mail Prepare Thread");
        this.prepareMailThread.setDaemon(true);
        this.prepareMailThread.start();

        // Step 2: Start the Mail Sender Thread
        this.sendMailThread = new Thread(this.sendMailRunnable);
        this.sendMailThread.setName("Mail Sender Thread");
        this.sendMailThread.setDaemon(true);
        this.sendMailThread.start();

    }

    /**
     * Stops the Mail Prepare and Sender threads. Should be called when the application is stopped for a clean shutdown.
     *
     * @throws InterruptedException if a thread fails to be stopped
     */
    private void stopMailThreads() throws InterruptedException
    {
        // Step 1: Stop the Mail Sender Thread

        if (this.sendMailThread != null) {
            this.sendMailRunnable.stopProcessing();
            // Make sure the Thread goes out of sleep if it's sleeping so that it stops immediately.
            this.sendMailThread.interrupt();
            // Wait till the thread goes away
            this.sendMailThread.join();
            SHUTDOWN_LOGGER.debug(String.format("Mail Prepare Thread has been stopped"));
        }

        // Step 2: Stop the Mail Prepare Thread

        if (this.prepareMailThread != null) {
            this.prepareMailRunnable.stopProcessing();
            // Make sure the Thread goes out of sleep if it's sleeping so that it stops immediately.
            this.prepareMailThread.interrupt();
            // Wait till the thread goes away
            this.prepareMailThread.join();
            SHUTDOWN_LOGGER.debug(String.format("Mail Sender Thread has been stopped"));
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        try {
            stopMailThreads();
        } catch (InterruptedException e) {
            SHUTDOWN_LOGGER.debug("Mail threads shutdown has been interruped", e);
        }
    }
}
