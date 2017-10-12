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
package org.xwiki.extension.versioncheck.internal;

import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.extension.versioncheck.ExtensionVersionCheckConfiguration;
import org.xwiki.text.StringUtils;

/**
 * Default implementation of {@link ExtensionVersionCheckConfiguration}.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component
@Singleton
public class DefaultVersionCheckConfiguration implements ExtensionVersionCheckConfiguration
{
    /**
     * The prefix that should be used by the configuration variables.
     */
    private static final String CONFIGURATION_PREFIX = "extension.versioncheck.";

    private static final String ENVIRONMENT_CONFIGURATION_PREFIX = CONFIGURATION_PREFIX + "environment.";

    @Inject
    private ConfigurationSource configurationSource;

    @Override
    public boolean isEnvironmentCheckEnabled()
    {
        return configurationSource.getProperty(ENVIRONMENT_CONFIGURATION_PREFIX + "enabled", true);
    }

    @Override
    public long environmentCheckInterval()
    {
        return configurationSource.getProperty(ENVIRONMENT_CONFIGURATION_PREFIX + "interval", 3600);
    }

    @Override
    public Pattern allowedEnvironmentVersions()
    {
        return Pattern.compile(configurationSource.getProperty(ENVIRONMENT_CONFIGURATION_PREFIX + "allowedVersions",
                StringUtils.EMPTY));
    }
}
