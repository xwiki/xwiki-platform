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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.activeinstalls.ActiveInstallsConfiguration;

import com.xpn.xwiki.util.AbstractXWikiRunnable;

/**
 * Thread that regularly sends information about the current instance (its unique id + the id and versions of all
 * installed extensions) to a central active installs Elastic Search server in order to count the number of active
 * installs of XWiki (and to know what extensions and in which versions they use).
 *
 * @version $Id$
 * @since 5.2M2
 */
public class ActiveInstallsPingThread extends AbstractXWikiRunnable
{
    /**
     * The logger to use when logging in this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveInstallsPingThread.class);

    /**
     * Once every 24 hours.
     */
    private static final long WAIT_TIME_FULL = 1000L * 60L * 60L * 24L;

    private static final long WAIT_TIME_RETRY = 1000L * 3L;

    private static final long RETRIES = 3;

    /**
     * @see #ActiveInstallsPingThread(org.xwiki.activeinstalls.ActiveInstallsConfiguration, PingSender)
     */
    private PingSender manager;

    /**
     * @see #ActiveInstallsPingThread(org.xwiki.activeinstalls.ActiveInstallsConfiguration, PingSender)
     */
    private ActiveInstallsConfiguration configuration;

    private long retryTimeout;

    /**
     * @param configuration used to nicely display the ping URL in logs if there's an error...
     * @param manager used to send the ping to the remote instance
     */
    public ActiveInstallsPingThread(ActiveInstallsConfiguration configuration, PingSender manager)
    {
        this.configuration = configuration;
        this.manager = manager;
    }

    @Override
    protected void runInternal() throws InterruptedException
    {
        while (true) {
            sendPing();
            Thread.sleep(WAIT_TIME_FULL);
        }
    }

    /**
     * Send a ping, trying several times in case of failure.
     *
     * @throws InterruptedException if the send is still failing after the retries
     * @since 11.10
     */
    void sendPing() throws InterruptedException
    {
        int count = 1;
        while (count <= RETRIES) {
            try {
                this.manager.sendPing();
                break;
            } catch (Exception e) {
                String message = String.format("Failed to send Active Installation ping to [%s] (try [%s]). "
                    + "Error = [%s].", this.configuration.getPingInstanceURL(), count,
                    ExceptionUtils.getRootCauseMessage(e));
                if (count == RETRIES) {
                    message = String.format("%s Will retry in [%s] seconds...", message, WAIT_TIME_FULL / 1000);
                }
                LOGGER.warn(message);
                // Wait a little but before retrying so that it makes a difference.
                if (count < RETRIES) {
                    Thread.sleep(getRetryTimeout());
                }
            }
            count++;
        }
    }

    /**
     * @param milliseconds the number of milliseconds to wait between retries
     * @since 11.10
     */
    void setRetryTimeout(long milliseconds)
    {
        this.retryTimeout = milliseconds;
    }

    private long getRetryTimeout()
    {
        return this.retryTimeout > -1 ? this.retryTimeout : WAIT_TIME_RETRY;
    }
}
