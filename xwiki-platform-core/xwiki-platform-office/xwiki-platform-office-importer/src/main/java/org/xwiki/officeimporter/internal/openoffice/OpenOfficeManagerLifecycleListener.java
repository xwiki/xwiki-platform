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
package org.xwiki.officeimporter.internal.openoffice;

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
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeManagerException;

/**
 * Listens to application start and stop events in order to automatically start and stop an Open Office server instance
 * (if auto start/auto stop is configured).
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("oomanager")
@Singleton
public class OpenOfficeManagerLifecycleListener implements EventListener
{
    /**
     * The {@link OpenOfficeConfiguration} component.
     */
    @Inject
    private OpenOfficeConfiguration ooConfig;

    /**
     * The {@link OpenOfficeManager} component.
     */
    @Inject
    private OpenOfficeManager ooManager;

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
        return "oomanager";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (ApplicationStartedEvent.class.getName().equals(event.getClass().getName())) {
            startOpenOffice();
        } else if (ApplicationStoppedEvent.class.getName().equals(event.getClass().getName())) {
            stopOpenOffice();
        }
    }

    /**
     * Start Open Office if the configuration says to start it automatically.
     */
    private void startOpenOffice()
    {
        if (this.ooConfig.isAutoStart()) {
            try {
                this.ooManager.start();
            } catch (OpenOfficeManagerException ex) {
                this.logger.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Stop Open Office.
     */
    private void stopOpenOffice()
    {
        // TODO: We shouldn't stop OO if it hasn't been started automatically or if the config doesn't
        // say to stop it automatically.
        try {
            this.ooManager.stop();
        } catch (OpenOfficeManagerException ex) {
            this.logger.error(ex.getMessage(), ex);
        }
    }
}
