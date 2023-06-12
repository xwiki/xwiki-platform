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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import org.xwiki.velocity.tools.EscapeTool;

import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_CVSS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_ID;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_LINK;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_MAX_CVSS;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.CVE_ID;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.EXTENSION_ID;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.MAX_CVSS;
import static org.xwiki.search.solr.AbstractSolrCoreInitializer.SOLR_FIELD_ID;

/**
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(ExtensionSecurityLiveDataSource.ID)
public class ExtensionSecurityLiveDataEntryStore implements LiveDataEntryStore
{
    private static final Map<String, String> LD_TO_SOLR = Map.of(
        EXTENSION_ID, SOLR_FIELD_ID,
        MAX_CVSS, SECURITY_MAX_CVSS
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

            initSort(liveDataQuery, solrQuery);

            initFilter(liveDataQuery, solrQuery);

            QueryResponse searchResults = this.extensionIndexStore.search(solrQuery);

            LiveData liveData = new LiveData();
            liveData.setCount(searchResults.getResults().getNumFound());
            for (SolrDocument solrDoc : searchResults.getResults()) {

                List<Object> cveIds = new ArrayList<>(solrDoc.getFieldValues(SECURITY_CVE_ID));
                List<Object> cveLinks = new ArrayList<>(solrDoc.getFieldValues(SECURITY_CVE_LINK));
                List<Object> cveCvss = new ArrayList<>(solrDoc.getFieldValues(SECURITY_CVE_CVSS));
                EscapeTool escapeTool = new EscapeTool();
                Stream<String> objectStream = IntStream.range(0, cveIds.size())
                    .mapToObj(value -> String.format("<a href='%s'>%s</a> (%s)", escapeTool.xml(cveLinks.get(value)),
                        escapeTool.xml(cveIds.get(value)), escapeTool.xml(cveCvss.get(value))));

                liveData.getEntries().add(Map.of(
                    EXTENSION_ID, solrDoc.get(SOLR_FIELD_ID),
                    MAX_CVSS, solrDoc.get(SECURITY_MAX_CVSS),
                    CVE_ID, objectStream.collect(Collectors.joining("<br/>"))
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

    private void initFilter(LiveDataQuery liveDataQuery, SolrQuery solrQuery)
    {
        for (String filterQuery : solrQuery.getFilterQueries()) {
            solrQuery.removeFilterQuery(filterQuery);
        }
        solrQuery.addFilterQuery("security_maxCVSS:[0 TO 10]");
        for (Filter filter : liveDataQuery.getFilters()) {
            String field = mapPropertyToField(filter.getProperty());
            for (Constraint constraint : filter.getConstraints()) {
                if (Objects.equals(field, SECURITY_MAX_CVSS)
                    && Objects.equals(constraint.getOperator(), "contains"))
                {
                    // TODO: refine the filters.
                    solrQuery.addFilterQuery(field + ":[" + constraint.getValue() + " TO 10]");
                } else {
                    solrQuery.addFilterQuery(
                        String.format("%s:*%s*", field, this.solrUtils.toFilterQueryString(constraint.getValue())));
                }
            }
        }
    }

    private static void initSort(LiveDataQuery liveDataQuery, SolrQuery solrQuery)
    {
        solrQuery.clearSorts();
        for (SortEntry sortEntry : liveDataQuery.getSort()) {
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
