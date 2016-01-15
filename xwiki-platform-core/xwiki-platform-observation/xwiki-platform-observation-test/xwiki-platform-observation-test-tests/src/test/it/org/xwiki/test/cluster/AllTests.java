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
package org.xwiki.test.cluster;

import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.runner.RunWith;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiExecutorSuite;

/**
 * Runs all functional tests found in the classpath and start/stop XWiki before/after the tests (only once).
 * 
 * @version $Id$
 */
@RunWith(XWikiExecutorSuite.class)
@XWikiExecutorSuite.Executors(2)
public class AllTests
{
    @XWikiExecutorSuite.PreStart
    public void preInitialize(List<XWikiExecutor> executors) throws Exception
    {
        initChannel(executors.get(0), "tcp");
        initChannel(executors.get(1), "tcp");
    }

    private void initChannel(XWikiExecutor executor, String channelName) throws Exception
    {
        PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();
        properties.setProperty("observation.remote.enabled", "true");
        properties.setProperty("observation.remote.channels", channelName);
        executor.saveXWikiProperties(properties);

        // Force bind_addr since tcp jgroups configuration expect cluster members to listen localhost by default
        executor.setXWikiOpts("-Djgroups.bind_addr=localhost -Xmx512m -XX:MaxPermSize=128m");
    }
}
