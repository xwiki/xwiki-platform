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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.activeinstalls2.ActiveInstallsConfiguration;

import com.xpn.xwiki.util.AbstractXWikiRunnable;

/**
 * Runnable that regularly sends information about the current instance (its unique id + the id and versions of all
 * installed extensions) to a central active installs Elastic Search server in order to count the number of active
 * installs of XWiki (and to know what extensions and in which versions they use).
 *
 * @version $Id$
 * @since 5.2M2
 */
public class ActiveInstallsPingRunnable extends AbstractXWikiRunnable
{
    /**
     * The logger to use when logging in this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveInstallsPingRunnable.class);

    private static final long WAIT_TIME_RETRY = 1000L * 3L;

    private static final long RETRIES = 3;

    private PingSender manager;

    private ActiveInstallsConfiguration configuration;

    private long retryTimeout;

    private long period;

    private TimeUnit timeUnit;

    /**
     * @param configuration used to nicely display the ping URL in logs if there's an error...
     * @param manager used to send the ping to the remote instance
     * @param period the number of time units between each ping
     * @param timeUnit the time unit for the period
     */
    public ActiveInstallsPingRunnable(ActiveInstallsConfiguration configuration, PingSender manager, long period,
        TimeUnit timeUnit)
    {
        this.configuration = configuration;
        this.manager = manager;
        this.period = period;
        this.timeUnit = timeUnit;
    }

    @Override
    protected void runInternal() throws InterruptedException
    {
        sendPing();
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
                String message = String.format(
                    "Failed to send Active Installation ping to [%s] (try [%s]). Error = [%s].",
                    this.configuration.getPingInstanceURL(), count, ExceptionUtils.getRootCauseMessage(e));
                if (count == RETRIES) {
                    message = String.format("%s Will retry in [%s %s]...", message, this.period,
                        this.timeUnit.toString().toLowerCase(Locale.ROOT));
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
