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
package org.xwiki.security.authentication.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.security.authentication.api.AuthenticationConfiguration;

/**
 * Default implementation for {@link AuthenticationConfiguration}.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Component
@Singleton
public class DefaultAuthenticationConfiguration implements AuthenticationConfiguration
{
    /**
     * Defines from where to read the Resource configuration data.
     */
    @Inject
    @Named("authentication")
    private ConfigurationSource configuration;

    @Override
    public int getMaxAuthorizedAttempts()
    {
        return configuration.getProperty("maxAuthorizedAttempts", 3);
    }

    @Override
    public int getTimeWindow()
    {
        return configuration.getProperty("timeWindowAttempts", 300);
    }

    @Override
    public String[] getFailureStrategies()
    {
        String strategies = configuration.getProperty("failureStrategy", "");
        if (!StringUtils.isEmpty(strategies)) {
            return strategies.split(",");
        } else {
            return new String[0];
        }
    }
}
