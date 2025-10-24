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
package org.xwiki.url.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.url.FrontendURLCheckPolicy;
import org.xwiki.url.URLConfiguration;

/**
 * Default implementation reading data from the {@code xwiki.properties} file.
 *
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Singleton
public class DefaultURLConfiguration implements URLConfiguration
{
    /**
     * Prefix for configuration keys for the Resource module.
     */
    private static final String PREFIX = "url.";

    /**
     * Defines from where to read the Resource configuration data.
     */
    @Inject
    private Provider<ConfigurationSource> configuration;

    @Override
    public String getURLFormatId()
    {
        // Note: the format corresponds to the component hint for the Resource Factory implementation to use.
        return this.configuration.get().getProperty(PREFIX + "format", "standard");
    }

    @Override
    public boolean useResourceLastModificationDate()
    {
        return this.configuration.get().getProperty(PREFIX + "useResourceLastModificationDate", true);
    }

    @Override
    public List<String> getTrustedDomains()
    {
        return this.configuration.get().getProperty(PREFIX + "trustedDomains", List.of());
    }

    @Override
    public boolean isTrustedDomainsEnabled()
    {
        return this.configuration.get().getProperty(PREFIX + "trustedDomainsEnabled", true);
    }

    @Override
    public List<String> getTrustedSchemes()
    {
        return this.configuration.get().getProperty(PREFIX + "trustedSchemes", List.of("http", "https", "ftp"));
    }

    @Override
    public FrontendURLCheckPolicy getFrontendUrlCheckPolicy()
    {
        // We use to have a property to only switch on or off the check:
        // this property should still be taken into account if the new property value is not defined.
        // The algorithm is then to check presence of the new property, and return the value based on it,
        // and if no value is defined then check presence of the old property, and only if there's no value for it,
        // fallback on the default value.
        String deprecatedProperty = PREFIX + "frontendUrlCheckEnabled";
        String newProperty = PREFIX + "frontendUrlCheckPolicy";
        ConfigurationSource configurationSource = this.configuration.get();
        FrontendURLCheckPolicy result = FrontendURLCheckPolicy.COMMENTS;
        if (configurationSource.containsKey(newProperty)) {
            String propertyString = configurationSource.getProperty(newProperty);
            try {
                result = FrontendURLCheckPolicy.valueOf(propertyString.toUpperCase());
            } catch (Throwable e) {
                result = FrontendURLCheckPolicy.COMMENTS;
            }
        } else if (configurationSource.containsKey(deprecatedProperty)) {
            result = (configurationSource.getProperty(deprecatedProperty, Boolean.class))
                ? FrontendURLCheckPolicy.ENABLED : FrontendURLCheckPolicy.DISABLED;
        }
        return result;
    }

    @Override
    public List<String> getAllowedFrontendUrls()
    {
        return this.configuration.get().getProperty(PREFIX + "allowedFrontendUrls", List.of());
    }
}
