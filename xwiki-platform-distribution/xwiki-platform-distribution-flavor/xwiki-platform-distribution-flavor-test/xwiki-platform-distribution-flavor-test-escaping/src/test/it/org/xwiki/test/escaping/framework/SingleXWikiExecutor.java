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
package org.xwiki.test.escaping.framework;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiWatchdog;

/**
 * Starts and stops exactly one XWiki instance. The methods {@link #start()} and {@link #stop()}
 * allow to call them multiple times, starting and stopping the server only on the first and
 * last call respectively.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public final class SingleXWikiExecutor extends XWikiExecutor
{
    /** Singleton instance. */
    private static SingleXWikiExecutor executor = null;

    /** Call counter. */
    private static int counter = 0;

    private XWikiWatchdog watchdog = new XWikiWatchdog();

    /**
     * Create new SingleXWikiExecutor.
     */
    private SingleXWikiExecutor()
    {
        super(0);
    }

    /**
     * Get the executor instance.
     * 
     * @return XWiki server executor
     */
    public static synchronized SingleXWikiExecutor getExecutor()
    {
        if (SingleXWikiExecutor.executor == null) {
            SingleXWikiExecutor.executor = new SingleXWikiExecutor();
        }
        return SingleXWikiExecutor.executor;
    }

    /**
     * {@inheritDoc}
     * 
     * Starts the server on the first call, subsequent calls only increase the internal counter by one.
     */
    @Override
    public synchronized void start() throws Exception
    {
        if (counter == 0) {
            if (!VERIFY_RUNNING_XWIKI_AT_START.equals("true") || this.watchdog.isXWikiStarted(getURL(), 15).timedOut) {
                // Disable extensions manager external repositories
                PropertiesConfiguration properties = loadXWikiPropertiesConfiguration();
                if (!properties.containsKey("extension.repositories")) {
                    properties.setProperty("extension.repositories", "");
                }
                saveXWikiProperties();
            }

            super.start();
        }
        counter++;
    }

    /**
     * {@inheritDoc}
     * 
     * Decreases the internal counter, stops the server when it reaches 0.
     */
    @Override
    public synchronized void stop() throws Exception
    {
        if (counter == 1) {
            super.stop();
        }
        counter--;
    }
}
