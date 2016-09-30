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
package org.xwiki.activeinstalls.internal.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.ActiveInstallsConfiguration;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
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
@Named("ActiveInstallsInitializerListener")
public class ActiveInstallsInitializerListener implements EventListener
{
    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = new ArrayList<Event>(Arrays.asList(new ApplicationReadyEvent()));

    /**
     * Used to initialize the unique XWiki instance id (to be removed in the future, see the source documentation
     * below).
     * <p>
     * Note that we use a Provider since the Observation Manager will register listeners very early in the
     * initialization process and some of the components injected transitively by the {@link InstanceIdManager}
     * implementation have initialization code that require an Execution Context to be available, which is not the case
     * early one in XWiki's initialization since no HTTP request has been made yet...
     */
    @Inject
    private Provider<InstanceIdManager> instanceIdManagerProvider;

    /**
     * Used to send the ping to the remote instance.
     * <p>
     * Note that we use a Provider since the Observation Manager will register listeners very early in the
     * initialization process and some of the components injected transitively by the {@link InstanceIdManager}
     * implementation have initialization code that require an Execution Context to be available, which is not the case
     * early one in XWiki's initialization since no HTTP request has been made yet...
     */
    @Inject
    private Provider<PingSender> pingSenderProvider;

    /**
     * Used to display the remote URL being hit in the logs if the ping fails...
     */
    @Inject
    private ActiveInstallsConfiguration configuration;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "ActiveInstallsInitializerListener";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Ensure that the instance id is initialized and available.
        // TODO: In the future introduce an EventListener in the instance module and have this listener execute *after*
        this.instanceIdManagerProvider.get().initializeInstanceId();

        // Start a thread to regularly send pings to the active installs server.
        Thread pingThread = new Thread(new ActiveInstallsPingThread(this.configuration, this.pingSenderProvider.get()));
        pingThread.setName("Active Installs Ping Thread");
        pingThread.start();
    }
}
