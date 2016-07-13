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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cached item including any extensions.
 *
 * @version $Id$
 * @since 6.2
 */
public class CachedItem
{
    /**
     * Rendered content.
     */
    public String rendered;

    /**
     * Map containing all extensions used in cached item.
     */
    public Map<RenderingCacheAware, UsedExtension> extensions = new HashMap<RenderingCacheAware,
        CachedItem.UsedExtension>();

    /**
     * Extension used in cached item.
     *
     * @version $Id$
     * @since 6.2
     */
    public static class UsedExtension
    {
        /**
         * Needed resources to rebuild extension.
         */
        public Set<String> resources;

        /**
         * Extension parameters.
         */
        public Map<String, Map<String, Object>> parameters;

        /**
         * @param resources needed resources
         * @param parameters extension parameters
         */
        public UsedExtension(Set<String> resources, Map<String, Map<String, Object>> parameters)
        {
            this.resources = resources;
            this.parameters = parameters;
        }
    }
}
