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
package org.xwiki.extension.index.internal.listener;

import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer;
import org.xwiki.extension.index.internal.ExtensionIndexSolrUtil;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.properties.ConverterManager;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.IS_CORE_EXTENSION;
import static org.xwiki.search.solr.AbstractSolrCoreInitializer.SOLR_FIELD_ID;

/**
 * Test of {@link ExtensionIndexCleanupListener}.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@ComponentTest
@ComponentList({
    DefaultSolrUtils.class,
    ExtensionIndexSolrUtil.class
})
class ExtensionIndexCleanupListenerTest
{
    @InjectMockComponents
    private ExtensionIndexCleanupListener listener;

    @MockComponent
    private CoreExtensionRepository coreExtensionRepository;

    @MockComponent
    private InstalledExtensionRepository installedExtensionRepository;

    // For DefaultSolrUtils.
    @MockComponent
    private ConverterManager converterManager;

    // For ExtensionIndexSolrUtil.
    @MockComponent
    private ExtensionFactory extensionFactory;

    @MockComponent
    private Solr solr;

    @Mock
    private SolrClient solrClient;

    @Test
    void proceed() throws Exception
    {
        when(this.solr.getClient(ExtensionIndexSolrCoreInitializer.NAME)).thenReturn(this.solrClient);
        when(this.coreExtensionRepository.exists(new ExtensionId("coreExtension1"))).thenReturn(false);
        when(this.coreExtensionRepository.exists(new ExtensionId("coreExtension2"))).thenReturn(true);
        when(this.installedExtensionRepository.exists(new ExtensionId("installedExtension1"))).thenReturn(true);
        when(this.installedExtensionRepository.exists(new ExtensionId("installedExtension2"))).thenReturn(false);

        SolrDocument coreExtensionToRemove = new SolrDocument(Map.of(
            SOLR_FIELD_ID, "coreExtension1",
            IS_CORE_EXTENSION, true
        ));
        SolrDocument coreExtensionToKeep = new SolrDocument(Map.of(
            SOLR_FIELD_ID, "coreExtension2",
            IS_CORE_EXTENSION, true
        ));
        SolrDocument installedExtensionToKeep = new SolrDocument(Map.of(
            SOLR_FIELD_ID, "installedExtension1"
        ));
        SolrDocument installedExtensionToRemove = new SolrDocument(Map.of(
            SOLR_FIELD_ID, "installedExtension2",
            IS_CORE_EXTENSION, true
        ));
        QueryResponse responseBatch1 = mock(QueryResponse.class);
        SolrDocumentList documentList1 = new SolrDocumentList();
        documentList1.add(coreExtensionToRemove);
        documentList1.add(coreExtensionToKeep);
        when(responseBatch1.getResults()).thenReturn(documentList1);
        QueryResponse responseBatch2 = mock(QueryResponse.class);
        SolrDocumentList documentList2 = new SolrDocumentList();
        documentList2.add(installedExtensionToRemove);
        documentList2.add(installedExtensionToKeep);
        when(responseBatch2.getResults()).thenReturn(documentList2);
        // Empty result to stop the batch iteration.
        QueryResponse responseBatch3 = mock(QueryResponse.class);
        when(responseBatch3.getResults()).thenReturn(new SolrDocumentList());
        when(this.solrClient.query(any(SolrQuery.class))).thenReturn(responseBatch1, responseBatch2, responseBatch3);

        this.listener.proceed();

        verify(this.solrClient).deleteById("coreExtension1");
        verify(this.solrClient).deleteById("installedExtension2");
        verify(this.solrClient).commit();
    }
}
