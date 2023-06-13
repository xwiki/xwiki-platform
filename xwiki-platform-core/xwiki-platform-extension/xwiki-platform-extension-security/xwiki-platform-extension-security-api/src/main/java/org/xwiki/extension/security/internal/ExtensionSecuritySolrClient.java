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
package org.xwiki.extension.security.internal;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.search.solr.SolrUtils;

import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_FIX_VERSION;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_MAX_CVSS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.EXTENSION_ID;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.FIX_VERSION;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.MAX_CVSS;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.NAME;
import static org.xwiki.search.solr.AbstractSolrCoreInitializer.SOLR_FIELD_ID;

/**
 * Provides the operations to interact with solr and {@link ExtensionIndexStore}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component(roles = ExtensionSecuritySolrClient.class)
@Singleton
public class ExtensionSecuritySolrClient
{
    private static final Map<String, String> LD_TO_SOLR = Map.of(
        EXTENSION_ID, SOLR_FIELD_ID,
        MAX_CVSS, SECURITY_MAX_CVSS,
        FIX_VERSION, SECURITY_FIX_VERSION
    );

    @Inject
    private ExtensionIndexStore extensionIndexStore;

    @Inject
    private SolrUtils solrUtils;

    /**
     * Perform a solr query returning the count of extensions with at least one known vulnerability.
     *
     * @return the query response of the query
     * @throws IOException If there is a low-level I/O error
     * @throws SolrServerException if there is an error on the server
     */
    public long getExtensionsCount() throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();
        this.extensionIndexStore.createSolrQuery(new ExtensionQuery(), solrQuery);

        initFilter(solrQuery);
        QueryResponse search = this.extensionIndexStore.search(solrQuery);
        return search.getResults().getNumFound();
    }

    /**
     * Perform a solr query to obtain a list of extension with known vulnerabilities, according to a given live data
     * query.
     *
     * @param liveDataQuery the live data query to use for the filters, sorts, and pagination
     * @return the query response of the query
     * @throws IOException If there is a low-level I/O error
     * @throws SolrServerException if there is an error on the server
     */
    public QueryResponse solrQuery(LiveDataQuery liveDataQuery) throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();
        this.extensionIndexStore.createSolrQuery(new ExtensionQuery(), solrQuery);
        solrQuery.setRows(liveDataQuery.getLimit());
        solrQuery.setStart(Math.toIntExact(liveDataQuery.getOffset()));

        initSort(liveDataQuery, solrQuery);

        initFilter(liveDataQuery, solrQuery);

        return this.extensionIndexStore.search(solrQuery);
    }

    private void initFilter(LiveDataQuery liveDataQuery, SolrQuery solrQuery)
    {
        initFilter(solrQuery);
        for (LiveDataQuery.Filter filter : liveDataQuery.getFilters()) {
            String field = mapPropertyToField(filter.getProperty());
            for (LiveDataQuery.Constraint constraint : filter.getConstraints()) {
                String filterQueryString = this.solrUtils.toFilterQueryString(constraint.getValue());
                if (StringUtils.isEmpty(filterQueryString)) {
                    continue;
                }
                if (Objects.equals(field, SECURITY_MAX_CVSS)
                    && Objects.equals(constraint.getOperator(), "contains"))
                {
                    solrQuery.addFilterQuery(field + ":[" + filterQueryString + " TO 10]");
                } else if (Objects.equals(field, NAME)) {
                    solrQuery.addFilterQuery(
                        String.format("(%s:*%s* OR %s:*%s*)", field, filterQueryString, SOLR_FIELD_EXTENSIONID,
                            filterQueryString));
                } else {
                    solrQuery.addFilterQuery(String.format("%s:*%s*", field, filterQueryString));
                }
            }
        }
    }

    private static void initFilter(SolrQuery solrQuery)
    {
        for (String filterQuery : solrQuery.getFilterQueries()) {
            solrQuery.removeFilterQuery(filterQuery);
        }
        solrQuery.addFilterQuery("security_maxCVSS:[0 TO 10]");
    }

    private static void initSort(LiveDataQuery liveDataQuery, SolrQuery solrQuery)
    {
        solrQuery.clearSorts();
        for (LiveDataQuery.SortEntry sortEntry : liveDataQuery.getSort()) {
            String property = sortEntry.getProperty();
            String field = mapPropertyToField(property);
            solrQuery.addSort(field, sortEntry.isDescending() ? SolrQuery.ORDER.desc : SolrQuery.ORDER.asc);
        }
    }

    private static String mapPropertyToField(String property)
    {
        return LD_TO_SOLR.getOrDefault(property, property);
    }
}
