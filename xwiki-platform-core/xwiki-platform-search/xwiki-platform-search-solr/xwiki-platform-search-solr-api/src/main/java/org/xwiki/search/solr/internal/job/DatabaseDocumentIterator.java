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
package org.xwiki.search.solr.internal.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Iterates the documents from the XWiki database.
 * 
 * @version $Id$
 * @since 5.4.5
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named("database")
public class DatabaseDocumentIterator extends AbstractDocumentIterator<String>
{
    /**
     * The current index in the list of {@link #results}.
     */
    private int index;

    /**
     * A 'page' of results taken from the database.
     */
    private List<Object[]> results = Collections.emptyList();

    /**
     * Used to get the list of available wikis.
     */
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * Iterates over the available wikis.
     */
    private Iterator<String> wikisIterator;

    /**
     * The wiki that is currently being iterated.
     */
    private String wiki;

    /**
     * The offset in the current wiki.
     */
    private int offset;

    /**
     * Used to query the underlying storage.
     */
    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> explicitEntityReferenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * The query used to fetch the documents from the database.
     */
    private Query query;

    /**
     * The query used to count the documents from the database.
     */
    private Query countQuery;

    /**
     * The query filter used to count the documents from the database.
     */
    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Override
    public boolean hasNext()
    {
        return getResults().size() > index;
    }

    @Override
    public Pair<DocumentReference, String> next()
    {
        Object[] result = getResults().get(index++);
        String localSpaceReference = (String) result[0];
        String name = (String) result[1];
        String locale = (String) result[2];
        String version = (String) result[3];
        SpaceReference spaceReference = new SpaceReference(this.explicitEntityReferenceResolver
            .resolve(localSpaceReference, EntityType.SPACE, new WikiReference(wiki)));
        DocumentReference documentReference = new DocumentReference(name, spaceReference);
        if (!StringUtils.isEmpty(locale)) {
            documentReference = new DocumentReference(documentReference, LocaleUtils.toLocale(locale));
        }
        return new ImmutablePair<DocumentReference, String>(documentReference, version);
    }

    @Override
    public long size()
    {
        long size = 0;

        try {
            getQuery();

            for (String wikiName : getWikis()) {
                size += (long) countQuery.setWiki(wikiName).execute().get(0);
            }
        } catch (QueryException e) {
            throw new IllegalStateException("Failed to count the documents.", e);
        }

        return size;
    }

    /**
     * The current 'page' of results. If the current page has been fully iterated then a new page is fetched
     * automatically.
     * 
     * @return the current 'page' of results taken from the database
     */
    private List<Object[]> getResults()
    {
        if (index >= results.size()) {
            if (wiki == null) {
                wiki = getNextWiki();
            }
            while (wiki != null) {
                fetchNextResults();
                if (results.size() > 0) {
                    break;
                }
                wiki = getNextWiki();
                offset = 0;
            }
            index = 0;
        }
        return results;
    }

    /**
     * Fetches the next 'page' of results from the database.
     */
    private void fetchNextResults()
    {
        try {
            // We use basic pagination (absolute offset) because we don't expect the database to change too much while
            // the synchronization takes place. Also, the database is used as the reference store, meaning that we
            // update the Solr index to match the database, not the other way around.
            results = getQuery().setWiki(wiki).setOffset(offset).execute();
            offset += LIMIT;
        } catch (QueryException e) {
            throw new IllegalStateException("Failed to query the database.", e);
        }
    }

    /**
     * @return the query used to fetch the documents from the database
     * @throws QueryException if creating the query fails
     */
    private Query getQuery() throws QueryException
    {
        if (query == null) {
            // This iterator must have the same order as the SolrDocumentIterator, otherwise the synchronization fails.
            String select = "select doc.space, doc.name, doc.language, doc.version from XWikiDocument doc";
            String orderBy = " order by doc.space, doc.name, doc.language nulls first";

            EntityReference spaceReference = null;
            EntityReference documentReference = null;
            if (rootReference != null) {
                spaceReference = rootReference.extractReference(EntityType.SPACE);
                documentReference = rootReference.extractReference(EntityType.DOCUMENT);
            }

            String whereClause = "";
            if (spaceReference != null) {
                whereClause += " where doc.space = :space";
                if (documentReference != null) {
                    whereClause += " and doc.name = :name";
                }
            }

            query = queryManager.createQuery(select + whereClause + orderBy, Query.HQL).setLimit(LIMIT);
            countQuery = queryManager.createQuery(whereClause, Query.HQL).addFilter(countFilter);

            if (spaceReference != null) {
                query.bindValue("space", this.localEntityReferenceSerializer.serialize(spaceReference));
                if (documentReference != null) {
                    query.bindValue("name", documentReference.getName());
                }
            }

            for (Map.Entry<String, Object> parameter : query.getNamedParameters().entrySet()) {
                countQuery.bindValue(parameter.getKey(), parameter.getValue());
            }
        }
        return query;
    }

    /**
     * @return the next wiki, in alphabetical order
     */
    private String getNextWiki()
    {
        if (wikisIterator == null) {
            List<String> wikis = getWikis();
            Collections.sort(wikis);
            wikisIterator = wikis.iterator();
        }
        return wikisIterator.hasNext() ? wikisIterator.next() : null;
    }

    /**
     * If the root entity is not specified then all the available wikis are returned. Otherwise only the wiki
     * corresponding to the root entity is returned.
     * 
     * @return the list of wikis to iterate
     */
    private List<String> getWikis()
    {
        if (rootReference == null) {
            List<String> wikis;
            try {
                wikis = new ArrayList<String>(wikiDescriptorManager.getAllIds());
            } catch (WikiManagerException e) {
                throw new IllegalStateException("Failed to get the list of available wikis.", e);
            }

            return wikis;
        } else {
            return Arrays.asList(rootReference.extractReference(EntityType.WIKI).getName());
        }
    }
}
