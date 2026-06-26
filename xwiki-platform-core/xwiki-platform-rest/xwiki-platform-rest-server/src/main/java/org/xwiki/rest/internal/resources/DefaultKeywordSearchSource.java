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
package org.xwiki.rest.internal.resources;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.SearchResult;

/**
 * Default implementation of {@link KeywordSearchSource} that uses the configured source for search.
 *
 * @version $Id$
 * @since 17.5.0
 */
@Component
@Singleton
public class DefaultKeywordSearchSource implements KeywordSearchSource
{
    /**
     * Hint of the fallback search source that is always available as this module contains the implementation.
     */
    private static final String FALLBACK_SEARCH_SOURCE = "database";

    @Inject
    @Named(FALLBACK_SEARCH_SOURCE)
    private KeywordSearchSource databaseKeywordSearchSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    @Override
    public List<SearchResult> search(String keywords, KeywordSearchOptions options, URI baseURI)
        throws XWikiRestException
    {
        String hint =
            this.configurationSource.getProperty("rest.keywordSearchSource", getDefaultKeywordSearchSourceHint());

        KeywordSearchSource searchSource = this.databaseKeywordSearchSource;

        if (!FALLBACK_SEARCH_SOURCE.equals(hint)) {
            try {
                searchSource = this.componentManagerProvider.get().getInstance(KeywordSearchSource.class, hint);
            } catch (ComponentLookupException e) {
                this.logger.error("Error loading the configured keyword search source with hint [{}]."
                    + " Falling back to database search.", hint, e);
            }
        }

        return searchSource.search(keywords, options, baseURI);
    }

    /**
     * @return the hint of the keyword search implementation that should be used by default
     */
    protected String getDefaultKeywordSearchSourceHint()
    {
        return FALLBACK_SEARCH_SOURCE;
    }
}
