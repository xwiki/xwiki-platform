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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.activeinstalls.ActiveInstallsConfiguration;

/**
 * Thread that regularly sends information about the current instance (its unique id + the id and versions of all
 * installed extensions) to a central active installs Elastic Search server in order to count the number of active
 * installs of XWiki (and to know what extensions and in which versions they use).
 *
 * @version $Id$
 * @since 5.2M2
 */
public class ActiveInstallsPingThread extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveInstallsPingThread.class);

    /**
     * Once every 24 hours.
     */
    private static final long WAIT_TIME = 1000L * 60L * 60L * 24L;

    private PingSender manager;

    private ActiveInstallsConfiguration configuration;

    public ActiveInstallsPingThread(ActiveInstallsConfiguration configuration, PingSender manager)
    {
        this.configuration = configuration;
        this.manager = manager;
    }

    @Override
    public void run()
    {
        while (true) {
            try {
                this.manager.sendPing();
            } catch (Exception e) {
                // Failed to connect or send the ping to the remote Elastic Search instance, will try again after the
                // sleep.
                LOGGER.warn(
                    "Failed to send Active Installation ping to [{}]. Error = [{}]. Will retry in [{}] seconds...",
                    this.configuration.getPingInstanceURL(), ExceptionUtils.getRootCauseMessage(e), WAIT_TIME / 1000);
            }
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
