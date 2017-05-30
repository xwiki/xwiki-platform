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
package org.xwiki.configuration.internal.cache;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.cache.internal.DefaultCacheManagerConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Override {@link DefaultCacheManagerConfiguration} to avoid cross dependency between cache and configuration
 * components (wiki based configuration component cache the properties).
 * 
 * @version $Id$
 */
@Component
@Singleton
public class XWikiCacheManagerConfiguration extends DefaultCacheManagerConfiguration
{
    /**
     * We read the cache configuration data only from the XWiki configuration file. We don't look for cache
     * configuration in other sources (such as in the XWikiPreferences page for example) since the cache configuration
     * is farm wide and shouldn't be overridden in wikis. In addition that would cause some cyclic dependency since the
     * configuration source would look for config data in wiki pages thus calling the cache store which in turn would
     * call this class again.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    protected ConfigurationSource getConfigurationSource()
    {
        return this.configuration;
    }
}
