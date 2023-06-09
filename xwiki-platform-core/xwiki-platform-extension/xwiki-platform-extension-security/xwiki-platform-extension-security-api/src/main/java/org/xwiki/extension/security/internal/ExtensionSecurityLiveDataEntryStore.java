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
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Constraint;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.search.solr.SolrUtils;

/**
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(ExtensionSecurityLiveDataSource.ID)
public class ExtensionSecurityLiveDataEntryStore implements LiveDataEntryStore
{
    private static final String EXTENSION_ID_LD_PROPERTY = "extensionId";

    private static final String MAX_CVSS_LD_PROPERTY = "maxCVSS";

    private static final String ID_SOLR_FIELD = "id";

    private static final String SECURITY_MAX_CCSV_SOLR_FIELD = "security_maxCCSV";

    private static final Map<String, String> LD_TO_SOLR = Map.of(
        EXTENSION_ID_LD_PROPERTY, ID_SOLR_FIELD,
        MAX_CVSS_LD_PROPERTY, SECURITY_MAX_CCSV_SOLR_FIELD
    );

    @Inject
    private ExtensionIndexStore extensionIndexStore;

    @Inject
    private SolrUtils solrUtils;

    @Override
    public Optional<Map<String, Object>> get(Object entryId)
    {
        return Optional.empty();
    }

    @Override
    public LiveData get(LiveDataQuery liveDataQuery) throws LiveDataException
    {
        try {
            SolrQuery solrQuery = new SolrQuery();
            this.extensionIndexStore.createSolrQuery(new ExtensionQuery(), solrQuery);
            solrQuery.setRows(liveDataQuery.getLimit());
            solrQuery.setStart(Math.toIntExact(liveDataQuery.getOffset()));

            solrQuery.clearSorts();
            for (SortEntry sortEntry : liveDataQuery.getSort()) {
                String property = sortEntry.getProperty();
                String field = mapPropertyToField(property);
                solrQuery.addSort(field, sortEntry.isDescending() ? SolrQuery.ORDER.desc : SolrQuery.ORDER.asc);
            }

            for (String filterQuery : solrQuery.getFilterQueries()) {
                solrQuery.removeFilterQuery(filterQuery);
            }
            solrQuery.addFilterQuery("security_maxCCSV:[0 TO 10]");
            for (Filter filter : liveDataQuery.getFilters()) {
                String field = mapPropertyToField(filter.getProperty());
                for (Constraint constraint : filter.getConstraints()) {
                    if (Objects.equals(field, SECURITY_MAX_CCSV_SOLR_FIELD)
                        && Objects.equals(constraint.getOperator(), "match"))
                    {
                        // TODO: refine the filters.
                        solrQuery.addFilterQuery(field + ":[" + constraint.getValue() + " TO 10]");
                    } else {
                        solrQuery.addFilterQuery(
                            field + ":" + this.solrUtils.toFilterQueryString(constraint.getValue()));
                    }
                }
            }

            QueryResponse searchResults = this.extensionIndexStore.search(solrQuery);

            LiveData liveData = new LiveData();
            liveData.setCount(searchResults.getResults().getNumFound());
            for (SolrDocument solrDoc : searchResults.getResults()) {

                liveData.getEntries().add(Map.of(
                    // TODO: extract to constant.
                    EXTENSION_ID_LD_PROPERTY, solrDoc.get(ID_SOLR_FIELD),
                    MAX_CVSS_LD_PROPERTY, solrDoc.get(SECURITY_MAX_CCSV_SOLR_FIELD)
                ));
            }
            return liveData;
        } catch (ArithmeticException e) {
            throw new LiveDataException("Failed to convert the limit for solr", e);
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ExtensionQuery.COMPARISON mapOperatorToComparison(String operator)
    {
        if (operator.equals("match")) {
            return ExtensionQuery.COMPARISON.MATCH;
        } else {
            // TODO: should throw an exception or return an optional + a log...
            return ExtensionQuery.COMPARISON.EQUAL;
        }
    }

    private static String mapPropertyToField(String property)
    {
        return LD_TO_SOLR.getOrDefault(property, property);
    }
}
