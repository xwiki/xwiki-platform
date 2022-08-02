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
 * @deprecated use the notion of Environment instead
 */
@Deprecated(since = "3.5M1")
public abstract class AbstractApplicationContext implements ApplicationContext
{
    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApplicationContext.class);

    /**
     * The name of the property for configuring the persistent directory path.
     */
    private static final String PROPERTY_PERSISTENTDIRECTORY = "container.persistentDirectory";

    /**
     * Use to lookup {@link ConfigurationSource} component.
     */
    private final ComponentManager componentManager;

    /**
     * @see #getPermanentDirectory()
     */
    private File permanentDirectory;

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
    public File getPermanentDirectory()
    {
        if (this.permanentDirectory == null) {
            try {
                this.permanentDirectory = getConfiguredPermanentDirectory();
            } catch (Exception e) {
                LOGGER.error("Failed to get configured permanent directory", e);
            }

            if (this.permanentDirectory == null) {
                // Only choice left
                this.permanentDirectory = getTemporaryDirectory();
            }
        }

        return this.permanentDirectory;
    }

    /**
     * @return the directory indicated in configuration, null if none is configured or if it's invalid
     * @throws ComponentLookupException error when trying to lookup {@link ConfigurationSource} component
     */
    private File getConfiguredPermanentDirectory() throws ComponentLookupException
    {
        File directory = null;

        ConfigurationSource source = this.componentManager.getInstance(ConfigurationSource.class, "xwikiproperties");
        String directoryName = source.getProperty(PROPERTY_PERSISTENTDIRECTORY);
        if (directoryName != null) {
            directory = new File(directoryName);
            if (directory.exists()) {
                if (!directory.isDirectory()) {
                    LOGGER.error("Configured permanent storage directory [{}] is not a directory",
                        directory.getAbsolutePath());
                    directory = null;
                } else if (!directory.canWrite()) {
                    LOGGER.error("Configured permanent storage directory [{}] is not writable",
                        directory.getAbsolutePath());
                    directory = null;
                }
            } else {
                directory.mkdirs();
            }
        }

        return directory;
    }
}
