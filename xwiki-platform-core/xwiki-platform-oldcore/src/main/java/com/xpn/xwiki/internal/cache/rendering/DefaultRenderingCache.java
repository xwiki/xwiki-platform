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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.cache.DocumentCache;

/**
 * Default implementation of {@link RenderingCache}.
 *
 * @version $Id$
 * @since 2.4M1
 */
@Component
@Singleton
public class DefaultRenderingCache implements RenderingCache, Initializable
{
    /**
     * UTF-8 encoding key.
     */
    private static final String UTF8 = "UTF-8";

    /**
     * Identifier of the rendering cache.
     */
    private static final String NAME = "core.renderingcache";

    /**
     * The name of the parameter used to force cache refresh.
     */
    private static final String PARAMETER_REFRESH = "refresh";

    /**
     * Configuration of the rendering cache.
     */
    @Inject
    private RenderingCacheConfiguration configuration;

    /**
     * Actually cache object.
     */
    @Inject
    private DocumentCache<String> cache;

    @Override
    public void initialize() throws InitializationException
    {
        if (this.configuration.isEnabled()) {
            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setConfigurationId(NAME);
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(this.configuration.getSize());
            lru.setTimeToLive(this.configuration.getDuration());
            cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            try {
                this.cache.create(cacheConfiguration);
            } catch (CacheException e) {
                throw new InitializationException("Failed to initialize core rendering cache", e);
            }
        }
    }

    // cache

    @Override
    public String getRenderedContent(DocumentReference documentReference, String source, XWikiContext context)
    {
        String renderedContent = null;

        if (this.configuration.isCached(documentReference)) {
            String refresh = context.getRequest() != null ? context.getRequest().getParameter(PARAMETER_REFRESH) : null;

            if (!"1".equals(refresh)) {
                renderedContent =
                        this.cache.get(documentReference, source, getAction(context), context.getLanguage(),
                                getRequestParameters(context));
            }
        }

        return renderedContent;
    }

    @Override
    public void setRenderedContent(DocumentReference documentReference, String source, String renderedContent,
            XWikiContext context)
    {
        if (this.configuration.isCached(documentReference)) {
            this.cache.set(renderedContent, documentReference, source, getAction(context), context.getLanguage(),
                    getRequestParameters(context));
        }
    }

    /**
     * Extract action information from the context.
     *
     * @param context the XWiki context
     * @return the current action
     */
    private String getAction(XWikiContext context)
    {
        return context.getAction() != null ? context.getAction() : "view";
    }

    /**
     * Extract action information from the context.
     *
     * @param context the XWiki context
     * @return the current request parameters
     */
    private String getRequestParameters(XWikiContext context)
    {
        if (context.getRequest() != null) {
            Map<String, String[]> parameters = context.getRequest().getParameterMap();

            if (parameters != null) {
                // Sort the Map so that the returned value can be used as a key that doesn't change if the servlet
                // container sends the parameter in a different order.
                SortedMap<String, String[]> sortedMap = new TreeMap<String, String[]>(parameters);
                return constructRequestString(sortedMap);
            }
        }

        return "";
    }

    /**
     * Encode the passed parameters in UTF-8 and return a String representing them.
     *
     * @param sortedMap the map representing the Request parameters
     * @return the encoded parameters as a String
     */
    private String constructRequestString(SortedMap<String, String[]> sortedMap)
    {
        StringBuilder sb = new StringBuilder();
        // TODO: Create a common class to serialize and encode parameters since this is a common need, and use
        // a Commons Collection Predicate to exclude the refresh parameter.
        for (Map.Entry<String, String[]> entry : sortedMap.entrySet()) {
            // If the parameter is the refresh parameter then ignore it
            if (!entry.getKey().equals(PARAMETER_REFRESH)) {
                for (String value : entry.getValue()) {
                    if (sb.length() > 0) {
                        sb.append('&');
                    }
                    try {
                        sb.append(URLEncoder.encode(entry.getKey(), UTF8)).append('=')
                                .append(URLEncoder.encode(value, UTF8));
                    } catch (UnsupportedEncodingException e) {
                        // That should never happen since UTF-8 is supposed to be available in any JVM.
                        throw new RuntimeException(
                            String.format("Failed to URL encode [[%s]:[%s]] parameter using UTF-8.",
                                entry.getKey(), entry.getValue()), e);
                    }
                }
            }
        }
        return sb.toString();
    }

	@Override
	public void flushCache(DocumentReference documentReference) {
		this.cache.removeAll(documentReference);
	}
}
