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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test the initialization of the Solr instance.
 * 
 * @version $Id$
 */
@AllComponents
@ComponentTest
public class DefaultSolrTest
{
    @XWikiTempDir
    private File permanentDirectory;

    private ConfigurationSource mockXWikiProperties;

    private Environment mockEnvironment;

    @MockComponent
    @Named("test")
    private SolrCoreInitializer testCore;

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

        when(this.testCore.getCoreName()).thenReturn("test");
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
    public void getXWikiClient() throws Exception
    {
        Solr instance = this.componentManager.getInstance(Solr.class);

        SolrClient client = instance.getClient("search");

        assertNotNull(client);

        client.add(new SolrInputDocument("id", "42"));

        SolrDocument storedDocument = client.getById("42");

        assertNotNull(storedDocument);
    }

    @Test
    public void getTestClient() throws Exception
    {
        Solr instance = this.componentManager.getInstance(Solr.class);

        SolrClient client = instance.getClient("test");

        Map<String, Object> fieldAttributes = new HashMap<>();
        fieldAttributes.put("name", "content");
        fieldAttributes.put("type", "string");
        new SchemaRequest.AddField(fieldAttributes).process(client);

        assertNotNull(client);

        client.add(new SolrInputDocument("id", "42", "content", "content1"));

        client.commit();

        SolrDocument storedDocument = client.getById("42");

        assertEquals("42", storedDocument.get("id"));
        assertEquals("content1", storedDocument.get("content"));
        
        assertNotNull(storedDocument);
    }
}
