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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
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
        return this.configuration.get().getProperty(PREFIX + "trustedDomains", Collections.emptyList());
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
}
