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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.SolrIndex;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.web.Utils;

/**
 * TODO DOCUMENT ME!
 * 
 * @version $Id$
 */
@ComponentList({DefaultModelContext.class, DefaultModelConfiguration.class, LocalStringEntityReferenceSerializer.class,
RelativeStringEntityReferenceResolver.class, CurrentReferenceDocumentReferenceResolver.class,
CurrentReferenceEntityReferenceResolver.class, CurrentEntityReferenceValueProvider.class,
CurrentMixedStringDocumentReferenceResolver.class, CurrentMixedEntityReferenceValueProvider.class,
DefaultEntityReferenceValueProvider.class, CompactWikiStringEntityReferenceSerializer.class})
public class DefaultSolrIndexTest
{
    @Rule
    public final MockitoComponentMockingRule<SolrIndex> mocker = new MockitoComponentMockingRule<SolrIndex>(
        DefaultSolrIndex.class);

    private XWikiContext xwikiContext;

    private XWiki xwiki;

    @Before
    public void configure() throws Exception
    {
        Utils.setComponentManager(mocker);

        final Execution execution = this.mocker.getInstance(Execution.class);
        final ExecutionContext executionContext = new ExecutionContext();

        this.xwiki = mock(XWiki.class);

        this.xwikiContext = new XWikiContext();
        this.xwikiContext.setDatabase("xwiki");
        this.xwikiContext.setWiki(this.xwiki);

        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xwikiContext);

        when(execution.getContext()).thenReturn(executionContext);

        URL url = this.getClass().getClassLoader().getResource("solrhome");
        System.setProperty(EmbeddedSolrInstance.SOLR_HOME_SYSTEM_PROPERTY, url.getPath());
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
