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
package org.xwiki.extension.security.internal.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.extension.security.ExtensionSecurityConfiguration;

/**
 * Default implementation of {@link ExtensionSecurityConfiguration}. Fetches the configurations from
 * {@code xwiki.properties}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
public class DefaultExtensionSecurityConfiguration implements ExtensionSecurityConfiguration
{
    /**
     * Scan enabled/disabled configuration key.
     */
    public static final String SCAN_ENABLED = "scanEnabled";

    /**
     * Scan delay configuration key.
     */
    public static final String SCAN_DELAY = "scanDelay";

    /**
     * Scan URL configuration key.
     */
    public static final String SCAN_URL = "scanURL";

    /**
     * The URL where the reviews are fetched.
     *
     * @since 15.6RC1
     */
    public static final String REVIEWS_URL = "reviewsURL";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesConfigurationSource;

    @Inject
    @Named(DocConfigurationSource.ID)
    private ConfigurationSource xObjectConfigurationSource;

    @Override
    public boolean isSecurityScanEnabled()
    {
        return getWithFallback(SCAN_ENABLED, "extension.security.scan.enabled", true);
    }

    @Override
    public int getScanDelay()
    {
        return getWithFallback(SCAN_DELAY, "extension.security.scan.delay", 24);
    }

    @Override
    public String getScanURL()
    {
        return getWithFallback(SCAN_URL, "extension.security.scan.url", "https://api.osv.dev/v1/query");
    }

    @Override
    public String getReviewsURL()
    {
        return getWithFallback(REVIEWS_URL, "extension.security.reviews.url",
            "https://extensions.xwiki.org/xwiki/bin/view/Extension/Extension/Security/Code/Reviews");
    }

    private <T> T getWithFallback(String classKey, String propertiesKey, T fallbackValue)
    {
        Class<T> aClass = (Class<T>) fallbackValue.getClass();
        if (this.xObjectConfigurationSource.containsKey(classKey)) {
            return this.xObjectConfigurationSource.getProperty(classKey, aClass);
        } else {
            return this.xwikiPropertiesConfigurationSource.getProperty(propertiesKey, aClass, fallbackValue);
        }
    }
}
