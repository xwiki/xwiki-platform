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
package org.xwiki.activeinstalls2.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.xwiki.activeinstalls2.ActiveInstallsConfiguration;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.context.Execution;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Used to trigger a Thread to periodically send a ping to the remote instance that stores it.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
@Named("ActiveInstalls2InitializerListener")
public class ActiveInstallsInitializerListener implements EventListener, Initializable, Disposable
{
    private static final long PERIOD = 1;

    private static final TimeUnit TIME_UNIT = TimeUnit.DAYS;

    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = new ArrayList<>(Arrays.asList(
        // Triggered when XWiki is started and ready (i.e. DB can be used)
        new ApplicationReadyEvent()
    ));

    /**
     * Used to send the ping to the remote instance.
     * <p>
     * Note that we use a Provider since the Observation Manager will register listeners very early in the
     * initialization process and some of the components injected transitively by the {@link InstanceIdManager}
     * implementation have initialization code that require an Execution Context to be available, which is not the case
     * early on in XWiki's initialization since no HTTP request has been made yet...
     */
    @Inject
    private Provider<PingSender> pingSenderProvider;

    /**
     * Used to display the remote URL being hit in the logs if the ping fails...
     */
    @Inject
    private ActiveInstallsConfiguration configuration;

    @Inject
    private Execution execution;

    private ScheduledExecutorService executorService;

    @Override
    public void initialize()
    {
        // Two cases:
        // - called when XWiki starts and this code is part of XWiki core. In this case we don't want to execute the
        //   ping thread since the DB is no ready yet. It'll be started when the ApplicationReadyEvent event is
        //   received. To recognize this case we check if there's an ExecutionContext defined already or not, since, at
        //   XWiki start, that's not the case.
        // - called when this listener is installed as an Extension. In this case, the Observation Manager is listening
        //   to ComponentDescriptorAddedEvent to automatically register any new EventListener.
        if (this.execution.getContext() != null) {
            startPingThread();
        }
    }

    @Override
    public void dispose()
    {
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "ActiveInstalls2InitializerListener";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        startPingThread();
    }

    private synchronized void startPingThread()
    {
        // Make sure we always have at least one ping thread.
        if (this.executorService == null) {
            // Start a thread to regularly send pings to the active installs server.
            BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("Active Installs 2 Ping Thread")
                .daemon(true)
                .priority(Thread.MIN_PRIORITY)
                .build();
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(factory);
            Runnable runnable = new ActiveInstallsPingRunnable(this.configuration, this.pingSenderProvider.get(),
                PERIOD, TIME_UNIT);
            // One ping every day
            service.scheduleAtFixedRate(runnable, 0, PERIOD, TIME_UNIT);
            this.executorService = service;
        }
    }
}
