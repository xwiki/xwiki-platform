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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.properties.internal.converter.LocaleConverter;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.test.SolrComponentList;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test the initialization of the Solr instance.
 * 
 * @version $Id$
 */
@SolrComponentList
@ComponentTest
@ComponentList({TestSolrCoreInitializer.class, LocaleConverter.class})
class DefaultSolrTest
{
    @XWikiTempDir
    private File permanentDirectory;

    private ConfigurationSource mockXWikiProperties;

    private Environment mockEnvironment;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.mockXWikiProperties =
            this.componentManager.registerMockComponent(ConfigurationSource.class, "xwikiproperties");
        this.mockEnvironment = this.componentManager.registerMockComponent(Environment.class);
        when(this.mockXWikiProperties.getProperty(anyString(), anyString())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgument(1);
            }
        });
    }

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.mockEnvironment.getPermanentDirectory()).thenReturn(this.permanentDirectory);

        FileUtils.deleteDirectory(this.permanentDirectory);
        this.permanentDirectory.mkdirs();
    }

    // Tests

    @Test
    void searchClient() throws Exception
    {
        Solr instance = this.componentManager.getInstance(Solr.class);

        SolrClient client = instance.getClient("search");

        assertNotNull(client);

        client.add(new SolrInputDocument("id", "0"));

        SolrDocument storedDocument = client.getById("0");

        assertNotNull(storedDocument);

        SolrInputDocument inputDocument = new SolrInputDocument("id", "1");

        inputDocument.setField(FieldUtils.NAME, "name");
        inputDocument.setField(FieldUtils.DOCUMENT_LOCALE, "");

        inputDocument.addField(FieldUtils.LINKS, "link1");
        inputDocument.addField(FieldUtils.LINKS, "link2");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "link1");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "link2");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "link1.1");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "link1.2");

        String titleRootLocaleField = FieldUtils.getFieldName(FieldUtils.TITLE, Locale.ROOT);
        inputDocument.setField(titleRootLocaleField, "Some title");

        client.add(inputDocument);

        storedDocument = client.getById("1");

        assertEquals("name", storedDocument.get(FieldUtils.NAME));
        assertEquals("", storedDocument.get(FieldUtils.DOCUMENT_LOCALE));
        assertEquals(Arrays.asList("link1", "link2"), storedDocument.getFieldValues(FieldUtils.LINKS));
        assertEquals(Arrays.asList("Some title"), storedDocument.get(titleRootLocaleField));
    }

    @Test
    void testClient() throws Exception
    {
        Solr solr = this.componentManager.getInstance(Solr.class);
        SolrUtils solrUtils = this.componentManager.getInstance(SolrUtils.class);

        SolrClient client = solr.getClient("test");

        assertNotNull(client);

        assertTrue(client.query(new SolrQuery()).getResults().isEmpty());

        Map<String, Object> fieldAttributes = new HashMap<>();
        fieldAttributes.put("name", "content");
        fieldAttributes.put("type", "string");
        new SchemaRequest.AddField(fieldAttributes).process(client);
        fieldAttributes.put("name", "testlocale");
        new SchemaRequest.AddField(fieldAttributes).process(client);
        fieldAttributes.put("name", "testlist");
        fieldAttributes.put("multiValued", true);
        new SchemaRequest.AddField(fieldAttributes).process(client);
        fieldAttributes.put("name", "testlocales");
        fieldAttributes.put("multiValued", true);
        new SchemaRequest.AddField(fieldAttributes).process(client);
        fieldAttributes.put("name", "text");
        fieldAttributes.put("type", "text_general");
        fieldAttributes.remove("multiValued");
        new SchemaRequest.AddField(fieldAttributes).process(client);

        SolrInputDocument inputDocument = new SolrInputDocument();

        solrUtils.setId("42", inputDocument);
        solrUtils.set("content", "content1", inputDocument);

        Map<String, Object> map = new HashMap<>();
        map.put("teststring", "testvalue");
        map.put("testint", Integer.MAX_VALUE);
        map.put("testlong", Long.MAX_VALUE);
        map.put("testfloat", Float.MAX_VALUE);
        map.put("testdouble", Double.MAX_VALUE);
        map.put("testbinary", new byte[] {1, 2, 3, 4, 5});
        map.put("teststrings", Arrays.asList("testvalue1", "testvalue2"));
        map.put("testints", new int[] {43, 44});
        solrUtils.setMap("testmap", map, inputDocument);

        solrUtils.setString("testlocale", Locale.FRANCE, inputDocument);

        solrUtils.setString("testlocales", Arrays.asList(Locale.ENGLISH, Locale.FRENCH), Locale.class, inputDocument);

        solrUtils.set("text", "two words UPPERCASE", inputDocument);

        client.add(inputDocument);
        client.commit();

        assertEquals(1, client.query(new SolrQuery()).getResults().size());

        SolrDocument storedDocument = client.getById("42");

        assertNotNull(storedDocument);

        assertEquals("42", solrUtils.getId(storedDocument));
        assertEquals("content1", solrUtils.get("content", storedDocument));
        assertEquals(Locale.FRANCE, solrUtils.get("testlocale", storedDocument, Locale.class));
        assertEquals(Arrays.asList(Locale.ENGLISH, Locale.FRENCH),
            solrUtils.getCollection("testlocales", storedDocument, Locale.class));
        assertEquals("two words UPPERCASE", solrUtils.get("text", storedDocument));

        Map<String, Object> storedMap = solrUtils.getMap("testmap", storedDocument);

        assertEquals("testvalue", storedMap.get("teststring"));
        assertEquals(Integer.MAX_VALUE, storedMap.get("testint"));
        assertEquals(Long.MAX_VALUE, storedMap.get("testlong"));
        assertEquals(Float.MAX_VALUE, storedMap.get("testfloat"));
        assertEquals(Double.MAX_VALUE, storedMap.get("testdouble"));
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, (byte[]) storedMap.get("testbinary"));
        assertEquals(Arrays.asList("testvalue1", "testvalue2"), storedMap.get("teststrings"));
        assertEquals(Arrays.asList(43, 44), storedMap.get("testints"));

        SolrQuery query = new SolrQuery("content:content1");
        SolrDocumentList documents = client.query(query).getResults();
        assertEquals(1, documents.size());
        storedDocument = documents.get(0);
        assertEquals("42", solrUtils.getId(storedDocument));

        query = new SolrQuery("content1");
        query.set(CommonParams.DF, "content");
        documents = client.query(query).getResults();
        assertEquals(1, documents.size());
        storedDocument = documents.get(0);
        assertEquals("42", solrUtils.getId(storedDocument));

        query = new SolrQuery("two");
        query.set(CommonParams.DF, "text");
        documents = client.query(query).getResults();
        assertEquals(1, documents.size());
        storedDocument = documents.get(0);
        assertEquals("42", solrUtils.getId(storedDocument));

        query = new SolrQuery("words");
        query.set("defType", "edismax");
        query.set("qf", "text");
        documents = client.query(query).getResults();
        assertEquals(1, documents.size());
        storedDocument = documents.get(0);
        assertEquals("42", solrUtils.getId(storedDocument));

        query = new SolrQuery("uppercase");
        query.set("defType", "edismax");
        query.set("qf", "text");
        documents = client.query(query).getResults();
        assertEquals(1, documents.size());
        storedDocument = documents.get(0);
        assertEquals("42", solrUtils.getId(storedDocument));

        query = new SolrQuery("UPPERCASE");
        query.set("defType", "edismax");
        query.set("qf", "text");
        documents = client.query(query).getResults();
        assertEquals(1, documents.size());
        storedDocument = documents.get(0);
        assertEquals("42", solrUtils.getId(storedDocument));

        SchemaResponse.FieldTypeResponse response = new SchemaRequest.FieldType("__xversion").process(client);
        assertEquals(String.valueOf(AbstractSolrCoreInitializer.SCHEMA_BASE_VERSION),
            response.getFieldType().getAttributes().get("defVal"));
        response = new SchemaRequest.FieldType("__cversion").process(client);
        assertEquals(String.valueOf(TestSolrCoreInitializer.VERSION),
            response.getFieldType().getAttributes().get("defVal"));

        inputDocument = new SolrInputDocument();

        solrUtils.setId("42", inputDocument);
        solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT, "testlist", "atomic", inputDocument);

        client.add(inputDocument);
        client.commit();

        assertEquals(Arrays.asList("atomic"), solrUtils.get("testlist", client.getById("42")));

        solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT, "testlist", "atomic2", inputDocument);
        client.add(inputDocument);
        client.commit();

        assertEquals(Arrays.asList("atomic", "atomic2"), solrUtils.get("testlist", client.getById("42")));

        solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT, "testlist", "atomic2", inputDocument);
        client.add(inputDocument);
        client.commit();

        assertEquals(Arrays.asList("atomic", "atomic2"), solrUtils.get("testlist", client.getById("42")));

        solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_REMOVE, "testlist", "atomic2", inputDocument);
        client.add(inputDocument);
        client.commit();

        assertEquals(Arrays.asList("atomic"), solrUtils.get("testlist", client.getById("42")));
    }
}
