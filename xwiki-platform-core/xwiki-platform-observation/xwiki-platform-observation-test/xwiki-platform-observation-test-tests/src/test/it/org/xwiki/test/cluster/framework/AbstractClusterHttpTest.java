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
package org.xwiki.test.cluster.framework;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hsqldb.Server;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.extension.test.ExtensionTestUtils;
import org.xwiki.test.cluster.AllTests;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.AbstractTest;

/**
 * Base class for REST based clustering integration test.
 * 
 * @version $Id$
 */
public abstract class AbstractClusterHttpTest extends AbstractTest
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractClusterHttpTest.class);

    @BeforeClass
    public static void init() throws Exception
    {
        if (context == null) {
            // Start HSQLDB server if not already running
            try (Socket ignored = new Socket("localhost", 9001)) {
                LOGGER.info("HSQLDB server is already running");
            } catch (IOException ignored) {
                LOGGER.info("HSQLDB server is not running. Starting one...");

                Server server = new Server();

                server.setSilent(true);

                File testDirectory = new File("target/test-" + new Date().getTime()).getAbsoluteFile();
                File databaseDirectory = new File(testDirectory, "database");
                server.setDatabaseName(0, "xwiki_db");
                server.setDatabasePath(0, databaseDirectory.toString());
                server.setDaemon(true);
                server.start();

                LOGGER.info("HSQLDB server started");
            }

            List<XWikiExecutor> executors = new ArrayList<>(2);

            executors.add(createExecutor(0));
            executors.add(createExecutor(1));

            // Require some AbstractTest initialization
            AbstractTest.init(executors);
        }
    }

    protected static XWikiExecutor createExecutor(int index)
    {
        XWikiExecutor executor = new XWikiExecutor(index);

        AllTests.setupExecutor(executor);

        return executor;
    }

    protected static ExtensionTestUtils getExtensionTestUtils()
    {
        return (ExtensionTestUtils) context.getProperties().get(ExtensionTestUtils.PROPERTY_KEY);
    }
}
