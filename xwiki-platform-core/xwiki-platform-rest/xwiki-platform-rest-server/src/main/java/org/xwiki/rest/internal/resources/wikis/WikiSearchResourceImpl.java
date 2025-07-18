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
package org.xwiki.rest.internal.resources.wikis;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.BaseSearchResult;
import org.xwiki.rest.internal.resources.KeywordSearchOptions;
import org.xwiki.rest.internal.resources.KeywordSearchScope;
import org.xwiki.rest.internal.resources.KeywordSearchSource;
import org.xwiki.rest.model.jaxb.SearchResults;
import org.xwiki.rest.resources.wikis.WikiSearchResource;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.wikis.WikiSearchResourceImpl")
public class WikiSearchResourceImpl extends BaseSearchResult implements WikiSearchResource
{
    @Inject
    private KeywordSearchSource keywordSearchSource;

    @Override
    public SearchResults search(String wikiName, String keywords, List<String> searchScopeStrings, Integer number,
        Integer start, String orderField, String order, Boolean withPrettyNames, Boolean isLocaleAware)
            throws XWikiRestException
    {
        SearchResults searchResults = objectFactory.createSearchResults();
        searchResults.setTemplate(String.format("%s?%s",
            Utils.createURI(uriInfo.getBaseUri(), WikiSearchResource.class, wikiName).toString(),
            SEARCH_TEMPLATE_INFO));

        if (wikiName != null) {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);
        }

        List<KeywordSearchScope> searchScopes = parseSearchScopeStrings(searchScopeStrings);

        KeywordSearchOptions searchOptions = KeywordSearchOptions.builder()
            .searchScopes(searchScopes)
            .wikiName(getXWikiContext().getWikiId())
            .space(null)
            .number(number)
            .start(start)
            .orderField(orderField)
            .order(order)
            .withPrettyNames(withPrettyNames)
            .isLocaleAware(isLocaleAware)
            .build();
        searchResults.getSearchResults().addAll(
            this.keywordSearchSource.search(keywords, searchOptions, this.uriInfo.getBaseUri()));

        return searchResults;
    }
}
