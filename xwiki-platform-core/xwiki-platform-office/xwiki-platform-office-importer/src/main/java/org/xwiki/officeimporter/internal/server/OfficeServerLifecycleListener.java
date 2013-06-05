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
package org.xwiki.officeimporter.internal.server;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.officeimporter.server.OfficeServerConfiguration;
import org.xwiki.officeimporter.server.OfficeServerException;

/**
 * Listens to application start and stop events in order to automatically start and stop an office server instance (if
 * auto start/auto stop is configured).
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("OfficeServerLifecycleListener")
@Singleton
public class OfficeServerLifecycleListener implements EventListener
{
    /**
     * The configuration component.
     */
    @Inject
    private OfficeServerConfiguration officeServerConfig;

    /**
     * The office server component.
     */
    @Inject
    private OfficeServer officeServer;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.asList(new ApplicationStartedEvent(), new ApplicationStoppedEvent());
    }

    @Override
    public String getName()
    {
        return "OfficeServerLifecycleListener";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (ApplicationStartedEvent.class.getName().equals(event.getClass().getName())) {
            startOfficeServer();
        } else if (ApplicationStoppedEvent.class.getName().equals(event.getClass().getName())) {
            stopOfficeServer();
        }
    }

    /**
     * Start the office server if the configuration says to start it automatically.
     */
    private void startOfficeServer()
    {
        if (this.officeServerConfig.isAutoStart()) {
            try {
                this.officeServer.start();
            } catch (OfficeServerException ex) {
                this.logger.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Stop the office server.
     */
    private void stopOfficeServer()
    {
        // TODO: We shouldn't stop the office server if it hasn't been started automatically or if the configuration
        // doesn't say to stop it automatically.
        try {
            this.officeServer.stop();
        } catch (OfficeServerException ex) {
            this.logger.error(ex.getMessage(), ex);
        }
    }
}
