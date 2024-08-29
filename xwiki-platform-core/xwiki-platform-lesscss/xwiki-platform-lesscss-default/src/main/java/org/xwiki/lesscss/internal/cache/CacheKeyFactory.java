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
package org.xwiki.lesscss.internal.cache;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReference;
import org.xwiki.lesscss.internal.skin.SkinReference;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;

/**
 * Factory to create a cache key.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component(roles = CacheKeyFactory.class)
@Singleton
public class CacheKeyFactory
{
    private static final String CACHE_KEY_SEPARATOR = "_";

    @Inject
    private XWikiContextCacheKeyFactory xcontextCacheKeyFactory;

    @Inject
    private Container container;

    /**
     * Get the cache key corresponding to the given LESS resource and context.
     *
     * @param lessResourceReference the reference to the LESS resource that have been compiled
     * @param skinReference skin for which the resource have been compiled
     * @param colorThemeReference color theme for which the resource have been compiled.
     * @param withContext whether or not the current XWikiContext must be handled by the cache.
     *
     * @return the corresponding cache key.
     */
    public String getCacheKey(LESSResourceReference lessResourceReference, SkinReference skinReference,
            ColorThemeReference colorThemeReference, boolean withContext)
    {
        String lessResource  = lessResourceReference.serialize();
        String skin          = skinReference.serialize();
        String colorTheme    = colorThemeReference.serialize();
        
        String result = lessResource.length()  + CACHE_KEY_SEPARATOR + lessResource + CACHE_KEY_SEPARATOR
                     +  skin.length()          + CACHE_KEY_SEPARATOR + skin         + CACHE_KEY_SEPARATOR
                     +  colorTheme.length()    + CACHE_KEY_SEPARATOR + colorTheme;

        if (withContext) {
            /** Also take into account the request parameters, if any, except parameters which are already
             * taken into account or that are irrelevant. */
            Request request = container.getRequest();
            List<String> excludes = Arrays.asList("skin", "colorTheme", "colorThemeVersion", "language", "docVersion",
                XWiki.CACHE_VERSION);
            if (request instanceof ServletRequest) {
                Map<String, String[]> parameters = ((ServletRequest) request).getHttpServletRequest().getParameterMap();
                for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                    if (!excludes.contains(entry.getKey())) {
                        String[] values = entry.getValue();
                        result += CACHE_KEY_SEPARATOR + entry.getKey() + ":" + StringUtils.join(values, "|");
                    }
                }
            }

            String xcontext = xcontextCacheKeyFactory.getCacheKey();
            result += CACHE_KEY_SEPARATOR + xcontext.length() + CACHE_KEY_SEPARATOR + xcontext;
        }

        return result;
    }
}
