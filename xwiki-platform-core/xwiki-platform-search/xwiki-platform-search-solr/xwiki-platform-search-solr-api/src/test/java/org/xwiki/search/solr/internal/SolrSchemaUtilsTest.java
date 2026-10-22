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
package org.xwiki.search.solr.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.InputStreamResponseParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link SolrSchemaUtils}.
 *
 * @version $Id$
 */
@ComponentTest
class SolrSchemaUtilsTest
{
    @InjectMockComponents
    private SolrSchemaUtils solrSchema;

    @Mock
    private XWikiSolrCore core;

    @Mock
    private SolrClient client;

    @BeforeEach
    void beforeEach()
    {
        when(this.core.getClient()).thenReturn(this.client);
        when(this.core.getSolrName()).thenReturn("xwiki_search_9");
    }

    private void mockSegmentsResponse(int status, String json) throws Exception
    {
        when(this.client.request(any())).thenReturn(InputStreamResponseParser.createInputStreamNamedList(status,
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void getNonCompliantFields() throws Exception
    {
        mockSegmentsResponse(200, """
            {
              "responseHeader": {"status": 0, "QTime": 1},
              "info": {"numSegments": 2, "userData": {"commitTimeMSec": "1"}},
              "segments": {
                "_0": {
                  "name": "_0",
                  "fields": {
                    "id": {"flags": "I-S-----OF-----", "schemaType": "string"},
                    "hidden": {
                      "flags": "I-------OF-----",
                      "schemaType": "boolean",
                      "nonCompliant": {"docValues": "schema=SORTED, segment=false", "schemaField": "hidden"}
                    }
                  },
                  "largestFiles": {"_0.fdt": "1 KB"}
                },
                "_1": {
                  "name": "_1",
                  "diagnostics": {"source": "flush"},
                  "fields": {
                    "title_en": {"flags": "IT---------P--", "nonCompliant": null},
                    "property.Space.Class.prop_boolean": {
                      "nonCompliant": {"docValues": "schema=SORTED, segment=false"}
                    }
                  }
                }
              },
              "fieldInfoLegend": ["I - Indexed", "D - DocValues"]
            }""");

        assertEquals(Set.of("hidden", "property.Space.Class.prop_boolean"),
            this.solrSchema.getNonCompliantFields(this.core));

        // The response is streamed instead of being loaded in memory
        ArgumentCaptor<SolrRequest<?>> requestCaptor = ArgumentCaptor.forClass(SolrRequest.class);
        verify(this.client).request(requestCaptor.capture());
        SolrRequest<?> request = requestCaptor.getValue();
        assertEquals("/admin/segments", request.getPath());
        assertEquals("true", request.getParams().get("fieldInfo"));
        assertInstanceOf(InputStreamResponseParser.class, request.getResponseParser());
        assertEquals("json", request.getResponseParser().getWriterType());
    }

    @Test
    void getNonCompliantFieldsWithEmptyIndex() throws Exception
    {
        mockSegmentsResponse(200, "{\"info\": {\"numSegments\": 0}, \"segments\": {}}");

        assertEquals(Set.of(), this.solrSchema.getNonCompliantFields(this.core));
    }

    @Test
    void getNonCompliantFieldsWithErrorStatus() throws Exception
    {
        mockSegmentsResponse(403, "{\"error\": {\"msg\": \"Forbidden\"}}");

        SolrException exception =
            assertThrows(SolrException.class, () -> this.solrSchema.getNonCompliantFields(this.core));
        assertEquals("Failed to get the segments of the Solr core [xwiki_search_9]: status [403]",
            exception.getMessage());
    }

    @Test
    void getNonCompliantFieldsWithInvalidResponse() throws Exception
    {
        mockSegmentsResponse(200, "{\"segments\": {\"_0\": ");

        SolrException exception =
            assertThrows(SolrException.class, () -> this.solrSchema.getNonCompliantFields(this.core));
        assertEquals("Failed to parse the segments of the Solr core [xwiki_search_9]", exception.getMessage());
    }

    @Test
    void getNonCompliantFieldsWhenRequestFails() throws Exception
    {
        IOException cause = new IOException("Connection refused");
        when(this.client.request(any())).thenThrow(cause);

        SolrException exception =
            assertThrows(SolrException.class, () -> this.solrSchema.getNonCompliantFields(this.core));
        assertEquals("Failed to get the segments of the Solr core [xwiki_search_9]", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
