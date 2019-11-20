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
package org.xwiki.tools.jetty.listener;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Jetty lifecycle listener that prints a message to open a browser when the server is started. This is to provide
 * information to newbies so that they know what to do after the server is started.
 *
 * @version $Id$
 * @since 3.5M1
 */
public class NotifyListener extends AbstractLifeCycleListener
{
    /** Logging helper object. */
    private static final Logger LOGGER = Log.getLogger(NotifyListener.class);

    /**
     * Delimiter to print to make the message stand out in the console/logs.
     */
    private static final String DELIMITER = "----------------------------------";

    @Override
    public void lifeCycleStarted(LifeCycle event)
    {
        LOGGER.info(DELIMITER);
        try {
            String serverUrl = "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":"
                + System.getProperty("jetty.http.port", "8080") + "/";
            LOGGER.info(Messages.getString("jetty.startup.notification"), serverUrl);
        } catch (UnknownHostException ex) {
            // Shouldn't happen, localhost should be available
            LOGGER.ignore(ex);
        }
        LOGGER.info(DELIMITER);
    }

    @Override
    public void lifeCycleStopping(LifeCycle event)
    {
        LOGGER.info(DELIMITER);
        LOGGER.info(Messages.getString("jetty.stopping.notification"));
    }

    @Override
    public void lifeCycleStopped(LifeCycle event)
    {
        LOGGER.info(Messages.getString("jetty.stopped.notification"));
    }
}
