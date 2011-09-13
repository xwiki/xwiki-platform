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
 *
 */
package org.xwiki.container;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Base class for {@link ApplicationContext} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractApplicationContext implements ApplicationContext
{
    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApplicationContext.class);

    /**
     * The name of the property where to find work directory path.
     */
    private static final String PROPERTY_WORKDIRECTORY = "container.workDirectory";

    /**
     * Use to lookup {@link ConfigurationSource} component.
     */
    private final ComponentManager componentManager;

    /**
     * @see #getWorkDirectory()
     */
    private File workDirectory;

    /**
     * @param componentManager use to lookup {@link ConfigurationSource} component. Can't directly get the
     *            {@link ConfigurationSource} because it depends on {@link ApplicationContext} to actually get the
     *            configuration
     */
    public AbstractApplicationContext(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    @Override
    public File getWorkDirectory()
    {
        if (this.workDirectory == null) {
            try {
                this.workDirectory = getConfiguredWorkDirectory();
            } catch (Exception e) {
                LOGGER.error("Failed to get configured work directory", e);
            }

            if (this.workDirectory == null) {
                // Only choice left
                this.workDirectory = getTemporaryDirectory();
            }
        }

        return this.workDirectory;
    }

    /**
     * @return the work directory indicated in configuration, null if none is configured or if it's invalid
     * @throws ComponentLookupException error when trying to lookup {@link ConfigurationSource} component
     */
    private File getConfiguredWorkDirectory() throws ComponentLookupException
    {
        File directory = null;

        String workDirectoryName =
            this.componentManager.lookup(ConfigurationSource.class, "xwikiproperties").getProperty(
                PROPERTY_WORKDIRECTORY);
        if (workDirectoryName != null) {
            directory = new File(workDirectoryName);
            if (directory.exists()) {
                if (!directory.isDirectory()) {
                    LOGGER.error("{}: not a directory", directory.getAbsolutePath());

                    directory = null;
                } else if (!directory.canWrite()) {
                    LOGGER.error("{}: no write permission", directory.getAbsolutePath());

                    directory = null;
                }
            } else {
                directory.mkdirs();
            }
        }

        return directory;
    }
}
