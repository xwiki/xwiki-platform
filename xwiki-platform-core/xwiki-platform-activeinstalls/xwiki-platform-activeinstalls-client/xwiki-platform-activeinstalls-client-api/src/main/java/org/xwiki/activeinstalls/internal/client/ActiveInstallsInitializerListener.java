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

import org.xwiki.activeinstalls.client.InstanceIdManager;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

@Component
@Singleton
@Named("ActiveInstallsInitializerListener")
public class ActiveInstallsInitializerListener implements EventListener
{
    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = new ArrayList<Event>(Arrays.asList(new ApplicationReadyEvent()));

    @Inject
    private Provider<InstanceIdManager> managerProvider;

    @Inject
    private InstalledExtensionRepository extensionRepository;

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
        // Verify if the the instance id is saved in the Database and if not create a unique id and save it.
        InstanceIdManager idManager = this.managerProvider.get();
        idManager.initializeInstanceId();

        // Start a thread to regularly send pings to the active installs server.
        Thread pingThread = new ActiveInstallsPingThread(idManager.getInstanceId(), this.extensionRepository);
        pingThread.setName("Active Installs Ping Thread");
        pingThread.start();
    }
}
