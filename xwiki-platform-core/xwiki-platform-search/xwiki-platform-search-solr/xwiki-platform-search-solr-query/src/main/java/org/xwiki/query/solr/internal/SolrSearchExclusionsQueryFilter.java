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
package org.xwiki.query.solr.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.search.SearchConfiguration;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 * @since 17.8.0RC1
 */
@Component
@Singleton
@Named("searchExclusions/solr")
public class SolrSearchExclusionsQueryFilter implements QueryFilter
{
    /**
     * The name of the parameter used to pass filter queries to Solr.
     */
    private static final String FILTER_QUERY = "fq";

    @Inject
    private Logger logger;

    @Inject
    private SearchConfiguration searchConfiguration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public String filterStatement(String statement, String language)
    {
        return statement;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List filterResults(List results)
    {
        return results;
    }

    @Override
    public Query filterQuery(Query query)
    {
        List<String> exclusionsFilterQuery = getExclusionsFilterQuery(getExclusions());
        if (!exclusionsFilterQuery.isEmpty()) {
            List<String> filterQueryValues = new ArrayList<>(exclusionsFilterQuery);
            Object filterQuery = query.getNamedParameters().get(FILTER_QUERY);
            if (filterQuery instanceof Iterable<?> filterQueryIterable) {
                for (Object filterQueryValue : filterQueryIterable) {
                    filterQueryValues.add((String) filterQueryValue);
                }
            } else if (filterQuery instanceof String filterQueryValue) {
                filterQueryValues.add(filterQueryValue);
            }
            query.bindValue(FILTER_QUERY, filterQueryValues);
        }

        return query;
    }

    private Set<EntityReference> getExclusions()
    {
        try {
            // Solr searches are executed across all wikis, so we need to get the exclusions for all wikis.
            return this.wikiDescriptorManager.getAllIds().stream().map(this::getExclusions).flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (WikiManagerException e) {
            this.logger.warn("Failed to get the search exclusions. Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Full stack trace: ", e);
            return Set.of();
        }
    }

    private Collection<EntityReference> getExclusions(String wikiId)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        String currentWikiId = xcontext.getWikiId();
        try {
            xcontext.setWikiId(wikiId);
            // TODO: We should resolve the exclusions coming from sub-wikis in order to make then absolute so that they
            // don't affect the results from other wikis. We can allow exclusions from the main wiki to remain relative.
            return this.searchConfiguration.getExclusions();
        } finally {
            xcontext.setWikiId(currentWikiId);
        }
    }

    private List<String> getExclusionsFilterQuery(Set<EntityReference> exclusions)
    {
        return exclusions.stream().map(this::getExclusionFilterQuery).toList();
    }

    private String getExclusionFilterQuery(EntityReference entityReference)
    {
        EntityReference wikiReference = entityReference.extractReference(EntityType.WIKI);
        switch (entityReference.getType()) {
            case DOCUMENT:
                // TODO: Add support for relative / local document references (when specified by the main wiki).
                return String.format("-(wiki:%s AND space_exact:%s AND name_exact:%s)",
                    this.solrUtils.toFilterQueryString(wikiReference.getName()),
                    this.solrUtils.toFilterQueryString(
                        this.localEntityReferenceSerializer.serialize(entityReference.getParent())),
                    this.solrUtils.toFilterQueryString(entityReference.getName()));
            case SPACE:
                // TODO: Add support for relative / local space references (when specified by the main wiki).
                return String.format("-(wiki:%s AND space_prefix:%s)",
                    this.solrUtils.toFilterQueryString(wikiReference.getName()),
                    this.solrUtils.toFilterQueryString(this.localEntityReferenceSerializer.serialize(entityReference)));
            default:
                return "";
        }
    }
}
