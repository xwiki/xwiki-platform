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
package org.xwiki.user.internal;

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.user.UserConfiguration;

/**
 * Look for use configuration data in {@code xwiki.prorperties}, under {@code user.*} keys.
 *
 * @version $Id$
 * @since 12.2
 */
@Component
@Singleton
public class DefaultUserConfiguration implements UserConfiguration
{
    /**
     * Prefix for configuration keys for the user module.
     */
    private static final String PREFIX = "user.";

    /**
     * Defines from where to read the user configuration data.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Override
    public String getStoreHint()
    {
        return this.xwikiPropertiesSource.getProperty(PREFIX + "hint", "document");
    }

    @Override
    public Properties getSuperAdminPreferences()
    {
        return getPreferencesFor("superadmin");
    }

    @Override
    public Properties getGuestPreference()
    {
        return getPreferencesFor("guest");
    }

    @Override
    public String getUserQualifierProperty()
    {
        return this.xwikiPropertiesSource.getProperty(PREFIX + "display.qualifierProperty");
    }

    private Properties getPreferencesFor(String userName)
    {
        Properties properties = new Properties();
        // Note: We miss a CS API to get only keys matching a pattern so we have to iterate over all of them...
        for (String key : this.xwikiPropertiesSource.getKeys()) {
            String preferenceKey = StringUtils.substringAfter(key, PREFIX + "preferences." + userName + ".");
            if (!StringUtils.isEmpty(preferenceKey)) {
                properties.setProperty(preferenceKey, this.xwikiPropertiesSource.getProperty(key));
            }
        }
        return properties;
    }
}
