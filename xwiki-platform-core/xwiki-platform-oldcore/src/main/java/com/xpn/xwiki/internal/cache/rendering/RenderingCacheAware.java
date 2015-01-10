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
package com.xpn.xwiki.internal.cache.rendering;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiContext;

/**
 * Specify this component needs special care when caching rendered content.
 *
 * @version $Id$
 * @since 2.4M1
 */
@Role
public interface RenderingCacheAware
{
    /**
     * Obtain needed resources for this compoment to successfully restore it from cache.
     *
     * @param context current xwiki context
     * @return resources needed for restoring this component
     */
    CachedItem.UsedExtension getCacheResources(XWikiContext context);

    /**
     * @param context current xwiki context
     * @param extension needed resources
     */
    void restoreCacheResources(XWikiContext context, CachedItem.UsedExtension extension);
}
