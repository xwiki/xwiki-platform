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
package org.xwiki.activeinstalls.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.ActiveInstallsConfiguration;
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
    private static final String PREFIX = "activeinstalls.";

    /**
     * @see #getPingInstanceURL()
     */
    private static final String DEFAULT_PING_URL = "http://extensions.xwiki.org/activeinstalls";

    private static final String DEFAULT_USER_AGENT = "XWikiActiveInstalls";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Inject
    private ConfigurationSource configuration;

    @Override
    public String getPingInstanceURL()
    {
        return this.configuration.getProperty(PREFIX + "pingURL", DEFAULT_PING_URL);
    }

    @Override
    public String getUserAgent()
    {
        return this.configuration.getProperty(PREFIX + "userAgent", DEFAULT_USER_AGENT);
    }
}
