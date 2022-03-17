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
package org.xwiki.container.servlet.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.internal.DefaultEnvironmentConfiguration;

/**
 * Overrides the {@link DefaultEnvironmentConfiguration} component in order to take into account the deprecated
 * permanent directory configuration property "container.persistentDirectory" and use it if the newer
 * "environment.permanentDirectory" property isn't set.
 *
 * @version $Id$
 * @since 3.5M1
 * @deprecated use the "environment.permanentDirectory" property instead
 */
@Component
@Singleton
@Deprecated(since = "3.5M1")
public class LegacyEnvironmentConfiguration extends DefaultEnvironmentConfiguration
{
    /**
     * The name of the property for configuring the permanent directory.
     */
    private static final String PROPERTY_DEPRECATED_PERMANENTDIRECTORY = "container.persistentDirectory";

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public String getPermanentDirectoryPath()
    {
        String dirPath = super.getPermanentDirectoryPath();
        if (dirPath == null) {
            // Fallback to the old deprecated permanent directory configuration property
            dirPath = getConfigurationSource().getProperty(PROPERTY_DEPRECATED_PERMANENTDIRECTORY, String.class);
            // Display a warning to the user so that he upgrades
            if (dirPath != null) {
                this.logger.warn("You're using the deprecated [{}] configuration property. You should instead use the "
                    + "newer [{}] one", PROPERTY_DEPRECATED_PERMANENTDIRECTORY, "environment.permanentDirectory");
            }
        }
        return dirPath;
    }
}
