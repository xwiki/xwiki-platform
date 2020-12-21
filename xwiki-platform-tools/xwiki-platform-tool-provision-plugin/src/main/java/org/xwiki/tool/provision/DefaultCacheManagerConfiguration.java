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
package org.xwiki.tool.provision;

import javax.inject.Singleton;

import org.xwiki.cache.CacheManagerConfiguration;
import org.xwiki.component.annotation.Component;

/**
 * Implements a cache configuration overriding the default, that will force
 * any component relying on a cache to use the "default" cache, that does nothing.
 *
 * @version $Id$
 * @since 12.10.1
 * @since 13.0-rc1
 */
@Component
@Singleton
public class DefaultCacheManagerConfiguration implements CacheManagerConfiguration
{
    private static final String DEFAULT = "default";

    @Override
    public String getDefaultCache()
    {
        return DEFAULT;
    }

    @Override
    public String getDefaultLocalCache()
    {
        return DEFAULT;
    }
}
