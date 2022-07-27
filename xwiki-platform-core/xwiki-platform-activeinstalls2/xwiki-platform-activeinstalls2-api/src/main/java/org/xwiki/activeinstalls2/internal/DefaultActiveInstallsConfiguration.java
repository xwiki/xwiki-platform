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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.activeinstalls2.ActiveInstallsConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * All configuration options for the Active Installs module.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class DefaultActiveInstallsConfiguration implements ActiveInstallsConfiguration
{
    /**
     * Prefix for configuration keys for the Active Installs module.
     */
    private static final String PREFIX = "activeinstalls2.";

    /**
     * @see #getPingInstanceURL()
     */
    private static final String DEFAULT_PING_URL = "https://extensions.xwiki.org/activeinstalls2";

    private static final String DEFAULT_USER_AGENT = "XWikiActiveInstalls2";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Inject
    private ConfigurationSource configuration;

    @Override
    public String getPingInstanceURL()
    {
        return getProperty("pingURL", DEFAULT_PING_URL);
    }

    @Override
    public String getUserAgent()
    {
        return getProperty("userAgent", DEFAULT_USER_AGENT);
    }

    private String getProperty(String shortPropertyName, String defaultValue)
    {
        return this.configuration.getProperty(PREFIX + shortPropertyName, defaultValue);
    }
}
