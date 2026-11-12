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
package org.xwiki.search.solr.internal.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrLinkExistenceDependencies}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ SolrLinkSerializer.class, DefaultSolrUtils.class })
@ReferenceComponentList
class SolrLinkExistenceDependenciesTest
{
    @InjectMockComponents
    private SolrLinkExistenceDependencies dependencies;

    @MockComponent
    private SolrInstance solrInstance;

    @MockComponent
    private ConverterManager converterManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private QueryResponse response(DocumentReference... references) throws Exception
    {
        EntityReferenceSerializer<String> parametersSerializer =
            this.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING, "withparameters");

        SolrDocumentList results = new SolrDocumentList();
        for (DocumentReference reference : references) {
            SolrDocument solrDocument = new SolrDocument();
            // Same format as the reference field set by the metadata extractors.
            solrDocument.setField(FieldUtils.REFERENCE, "document:" + parametersSerializer.serialize(reference));
            results.add(solrDocument);
        }

        QueryResponse response = mock();
        when(response.getResults()).thenReturn(results);

        return response;
    }

    @Test
    void getDependents() throws Exception
    {
        DocumentReference dependent = new DocumentReference("wiki", "Space", "Source");
        DocumentReference dependentTranslation = new DocumentReference(dependent, Locale.FRENCH);
        QueryResponse response = response(dependent, dependentTranslation);
        when(this.solrInstance.query(any(SolrParams.class))).thenReturn(response);

        assertEquals(Set.of(dependent, dependentTranslation), this.dependencies.getDependents(
            List.of(new DocumentReference("wiki", "Space", "Target", Locale.GERMAN),
                new DocumentReference("wiki", List.of("Space", "Target"), "WebHome"))));

        // The locale of the documents whose existence changed is ignored, and the translations of the dependents are
        // returned with their locale.
        ArgumentCaptor<SolrParams> paramsCaptor = ArgumentCaptor.forClass(SolrParams.class);
        verify(this.solrInstance).query(paramsCaptor.capture());
        assertEquals("links_existence:(entity\\:document\\:wiki\\:Space.Target"
            + " OR entity\\:document\\:wiki\\:Space.Target.WebHome)", paramsCaptor.getValue().get(CommonParams.Q));
    }

    @Test
    void getDependentsWithManyDocuments() throws Exception
    {
        QueryResponse response = response();
        when(this.solrInstance.query(any(SolrParams.class))).thenReturn(response);

        List<DocumentReference> documents = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            documents.add(new DocumentReference("wiki", "Space", "Page" + i));
        }

        assertEquals(Set.of(), this.dependencies.getDependents(documents));

        // The documents are split between several queries to not reach the maximum number of clauses.
        verify(this.solrInstance, times(2)).query(any(SolrParams.class));
    }

    @Test
    void getDependentsWithoutDocuments() throws Exception
    {
        assertEquals(Set.of(), this.dependencies.getDependents(List.of()));

        verify(this.solrInstance, times(0)).query(any(SolrParams.class));
    }

    @Test
    void getDependentsWhenQueryFails() throws Exception
    {
        Exception cause = new RuntimeException("Solr failure");
        when(this.solrInstance.query(any(SolrParams.class))).thenThrow(cause);

        SolrIndexerException exception = assertThrows(SolrIndexerException.class,
            () -> this.dependencies.getDependents(List.of(new DocumentReference("wiki", "Space", "Target"))));

        assertEquals("Failed to search the documents with links depending on the existence of documents",
            exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
