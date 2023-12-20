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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatchers;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.extension.InstalledExtension.FIELD_INSTALLED_NAMESPACES;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.IS_CORE_EXTENSION;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.IS_REVIEWED_SAFE;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_CVE_ID;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_MAX_CVSS;
import static org.xwiki.extension.security.internal.livedata.ExtensionSecurityLiveDataConfigurationProvider.CVE_ID;
import static org.xwiki.extension.security.internal.livedata.ExtensionSecurityLiveDataConfigurationProvider.FIX_VERSION;
import static org.xwiki.extension.security.internal.livedata.ExtensionSecurityLiveDataConfigurationProvider.MAX_CVSS;

/**
 * Test of {@link ExtensionSecuritySolrClient}.
 *
 * @version $Id$
 */
@ComponentTest
class ExtensionSecuritySolrClientTest
{
    @InjectMockComponents
    private ExtensionSecuritySolrClient solrClient;

    @MockComponent
    private ExtensionIndexStore extensionIndexStore;

    @MockComponent
    private SolrUtils solrUtils;

    @Test
    void getExtensionsCount() throws Exception
    {
        doAnswer(invocationOnMock -> {
            invocationOnMock.<SolrQuery>getArgument(1).setFilterQueries("");
            return null;
        }).when(this.extensionIndexStore).createSolrQuery(any(), any());
        QueryResponse mock = mock(QueryResponse.class);
        SolrDocumentList documentList = new SolrDocumentList();
        documentList.setNumFound(42);
        when(mock.getResults()).thenReturn(documentList);
        when(this.extensionIndexStore.search(any(SolrQuery.class))).thenReturn(mock);

        assertEquals(42, this.solrClient.getVulnerableExtensionsCount());

        SolrQuery params = new SolrQuery();
        params.addFilterQuery(String.format("%s:{0 TO 10]", SECURITY_MAX_CVSS));
        params.addFilterQuery(getNamespaceFilterQuery());
        params.addFilterQuery(String.format("%s:false", IS_REVIEWED_SAFE));
        verify(this.extensionIndexStore)
            .search(ArgumentMatchers.<SolrQuery>argThat(
                t -> sameArraysNoOrder(t.getFilterQueries(), params.getFilterQueries())));
    }

    @Test
    void solrQuery() throws Exception
    {
        mockExtensionIndexStore();

        LiveDataQuery liveDataQuery = new LiveDataQuery();
        liveDataQuery.setLimit(10);
        liveDataQuery.setOffset(0L);
        liveDataQuery.setSort(List.of());
        liveDataQuery.setFilters(List.of(new LiveDataQuery.Filter(FIX_VERSION, "match", "15.5")));
        liveDataQuery.setSource(new LiveDataQuery.Source());
        this.solrClient.solrQuery(liveDataQuery);

        SolrQuery params = new SolrQuery();
        params.addFilterQuery(SECURITY_MAX_CVSS + ":{0 TO 10]");
        params.addFilterQuery(IS_CORE_EXTENSION + ":false");
        params.addFilterQuery(getNamespaceFilterQuery());

        verify(this.extensionIndexStore)
            .search(AdditionalMatchers.<SolrQuery>and(
                argThat(t -> sameArraysNoOrder(t.getFilterQueries(), params.getFilterQueries())),
                argThat(t -> Objects.equals(t.getSorts(), params.getSorts())))
            );
    }

    @Test
    void solrQueryIsFromEnvironment() throws Exception
    {
        mockExtensionIndexStore();

        LiveDataQuery liveDataQuery = new LiveDataQuery();
        liveDataQuery.setLimit(10);
        liveDataQuery.setOffset(0L);
        liveDataQuery.setSort(List.of());
        liveDataQuery.setFilters(List.of(new LiveDataQuery.Filter(FIX_VERSION, "match", "15.5")));
        LiveDataQuery.Source source = new LiveDataQuery.Source();
        source.getParameters().put("isFromEnvironment", "true");
        liveDataQuery.setSource(source);
        this.solrClient.solrQuery(liveDataQuery);

        SolrQuery params = new SolrQuery();
        params.addFilterQuery(SECURITY_MAX_CVSS + ":{0 TO 10]");
        params.addFilterQuery(IS_CORE_EXTENSION + ":true");
        params.addFilterQuery(getNamespaceFilterQuery());

        verify(this.extensionIndexStore)
            .search(AdditionalMatchers.<SolrQuery>and(
                argThat(t -> sameArraysNoOrder(t.getFilterQueries(), params.getFilterQueries())),
                argThat(t -> Objects.equals(t.getSorts(), params.getSorts())))
            );
    }

    private static String getNamespaceFilterQuery()
    {
        return String.format("(%s:[* TO *] OR %s:true)", FIELD_INSTALLED_NAMESPACES, IS_CORE_EXTENSION);
    }

    @Test
    void filterByCVEId() throws Exception
    {
        mockExtensionIndexStore();

        when(this.solrUtils.toFilterQueryString(any(), eq(String.class))).thenAnswer(it -> it.getArgument(0));

        LiveDataQuery liveDataQuery = new LiveDataQuery();
        liveDataQuery.initialize();
        liveDataQuery.setFilters(List.of(new LiveDataQuery.Filter(CVE_ID, "match", "test")));
        this.solrClient.solrQuery(liveDataQuery);

        SolrQuery expectedSolrQuery = new SolrQuery();
        expectedSolrQuery.addFilterQuery(String.format("%s:*test*", SECURITY_CVE_ID));
        expectedSolrQuery.addFilterQuery(SECURITY_MAX_CVSS + ":{0 TO 10]");
        expectedSolrQuery.addFilterQuery(getNamespaceFilterQuery());
        expectedSolrQuery.addFilterQuery(IS_CORE_EXTENSION + ":false");

        verify(this.extensionIndexStore).search(argThat((SolrQuery solrQuery) ->
            sameArraysNoOrder(solrQuery.getFilterQueries(), expectedSolrQuery.getFilterQueries())));
    }

    @ParameterizedTest
    @CsvSource({
        "6.3,6.3",
        "abcd,0.0",
    })
    void filterByMaxCVSS(String value, String convertedValue) throws Exception
    {
        mockExtensionIndexStore();

        when(this.solrUtils.toFilterQueryString(any(), eq(Double.class))).thenAnswer(it -> {
            try {
                return String.valueOf(Double.parseDouble(it.getArgument(0)));
            } catch (NumberFormatException e) {
                return "0.0";
            }
        });

        LiveDataQuery liveDataQuery = new LiveDataQuery();
        liveDataQuery.initialize();
        liveDataQuery.setFilters(List.of(new LiveDataQuery.Filter(MAX_CVSS, "contains", value)));
        this.solrClient.solrQuery(liveDataQuery);

        SolrQuery expectedSolrQuery = new SolrQuery();
        expectedSolrQuery.addFilterQuery(String.format("%s:[%s TO 10]", SECURITY_MAX_CVSS, convertedValue));
        expectedSolrQuery.addFilterQuery(SECURITY_MAX_CVSS + ":{0 TO 10]");
        expectedSolrQuery.addFilterQuery(getNamespaceFilterQuery());
        expectedSolrQuery.addFilterQuery(IS_CORE_EXTENSION + ":false");

        verify(this.extensionIndexStore).search(argThat((SolrQuery solrQuery) ->
            sameArraysNoOrder(solrQuery.getFilterQueries(), expectedSolrQuery.getFilterQueries())));
    }

    private void mockExtensionIndexStore()
    {
        doAnswer(invocationOnMock -> {
            SolrQuery solrQuery = invocationOnMock.getArgument(1);
            solrQuery.setFilterQueries("");
            solrQuery.setSort("fake", SolrQuery.ORDER.asc);
            return null;
        }).when(this.extensionIndexStore).createSolrQuery(any(), any());
    }

    private static boolean sameArraysNoOrder(String[] a1, String[] a2)
    {
        List<String> sl1 = Arrays.stream(a1).sorted().collect(Collectors.toList());
        List<String> sl2 = Arrays.stream(a2).sorted().collect(Collectors.toList());
        return Objects.equals(sl1, sl2);
    }
}
