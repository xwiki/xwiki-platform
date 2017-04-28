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

import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TODO DOCUMENT ME!
 * 
 * @version $Id$
 */
@ReferenceComponentList
public class DefaultSolrIndexerTest
{
    @Rule
    public final MockitoComponentMockingRule<SolrIndexer> mocker =
        new MockitoComponentMockingRule<SolrIndexer>(DefaultSolrIndexer.class);

    private XWikiContext xcontext;

    private XWiki xwiki;

    private SolrConfiguration mockConfig;

    @Before
    public void configure() throws Exception
    {
        Utils.setComponentManager(mocker);

        // XWiki

        this.xwiki = mock(XWiki.class);

        // XWikiContext

        this.xcontext = new XWikiContext();
        this.xcontext.setWikiId("xwiki");
        this.xcontext.setWiki(this.xwiki);

        // Solr configuration

        URL url = this.getClass().getClassLoader().getResource("solrhome");
        this.mockConfig = this.mocker.getInstance(SolrConfiguration.class);
        when(this.mockConfig.getInstanceConfiguration(eq(EmbeddedSolrInstance.TYPE), eq("home"), any()))
            .thenReturn(url.getPath());
    }

    @Test
    public void testIndexEmptyWiki() throws Exception
    {

        // Mock solr wrapper
        // Add expectations

        WikiReference wikiReference = new WikiReference("xwiki");

        // index.index(wikiReference);
    }
}
