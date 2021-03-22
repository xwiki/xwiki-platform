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
package org.xwiki.like.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.like.LikeConfiguration;

/**
 * Default configuration for Like module.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Component
@Singleton
public class DefaultLikeConfiguration implements LikeConfiguration
{
    @Inject
    @Named("like")
    private ConfigurationSource configurationSource;

    @Override
    public boolean alwaysDisplayButton()
    {
        return this.configurationSource.getProperty("alwaysDisplayButton", false);
    }

    @Override
    public int getLikeCacheCapacity()
    {
        return this.configurationSource.getProperty("cacheCapacity", 500);
    }

    @Override
    public boolean isEnabled()
    {
        return this.configurationSource.getProperty("enabled", true);
    }
}
