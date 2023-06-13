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
import java.nio.charset.Charset;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Constraint;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;

import com.xpn.xwiki.web.ViewAction;

import static org.apache.commons.lang.StringEscapeUtils.escapeXml;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_CVSS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_ID;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_LINK;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_FIX_VERSION;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_MAX_CVSS;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.ADVICE;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.CVE_ID;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.EXTENSION_ID;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.FIX_VERSION;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.MAX_CVSS;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.NAME;
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
        MAX_CVSS, SECURITY_MAX_CVSS,
        FIX_VERSION, SECURITY_FIX_VERSION
    );

    @Inject
    private ExtensionIndexStore extensionIndexStore;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

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
                Stream<String> objectStream = IntStream.range(0, cveIds.size())
                    .mapToObj(value -> String.format("<a href='%s'>%s</a> (%s)",
                        escapeXml(String.valueOf(cveLinks.get(value))), escapeXml(String.valueOf(cveIds.get(value))),
                        escapeXml(String.valueOf(cveCvss.get(value)))));

                liveData.getEntries().add(Map.of(
                    NAME, buildExtensionName(solrDoc),
                    EXTENSION_ID, solrDoc.get(SOLR_FIELD_ID),
                    MAX_CVSS, solrDoc.get(SECURITY_MAX_CVSS),
                    CVE_ID, objectStream.collect(Collectors.joining("<br/>")),
                    FIX_VERSION, this.solrUtils.get(SECURITY_FIX_VERSION, solrDoc),
                    ADVICE, ""
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

    private String buildExtensionName(SolrDocument solrDoc)
    {
        String extensionName;
        // If the extension does not have a name and version indexed, we fallback to an extensionId parsing.
        // Note: this is not supposed to happen in practice.
        ExtensionId extensionId = this.extensionIndexStore.getExtensionId(solrDoc);
        List<BasicNameValuePair> parameters = buildParameter(extensionId.getId(), extensionId.getVersion().getValue());
        if (solrDoc.get(FieldUtils.NAME) != null) {
            extensionName = this.solrUtils.get(FieldUtils.NAME, solrDoc);
        } else {
            String[] versionId = this.solrUtils.getId(solrDoc).split("/");
            extensionName = versionId[0];
        }
        String url = this.documentAccessBridge.getDocumentURL(new LocalDocumentReference("XWiki", "Extensions"),
            ViewAction.VIEW_ACTION, URLEncodedUtils.format(parameters, Charset.defaultCharset()), null);

        return String.format("<a href='%s'>%s</a>", escapeXml(url), escapeXml(String.valueOf(extensionName)));
    }

    private static List<BasicNameValuePair> buildParameter(String extensionId, String extensionVersion)
    {
        return List.of(
            new BasicNameValuePair("section", "XWiki.Extensions"),
            new BasicNameValuePair("extensionId", extensionId),
            new BasicNameValuePair("extensionVersion", extensionVersion)
        );
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
                String filterQueryString = this.solrUtils.toFilterQueryString(constraint.getValue());
                if (StringUtils.isEmpty(filterQueryString)) {
                    continue;
                }
                if (Objects.equals(field, SECURITY_MAX_CVSS)
                    && Objects.equals(constraint.getOperator(), "contains"))
                {
                    // TODO: refine the filters.
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
