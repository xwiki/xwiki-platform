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
package org.xwiki.wiki.internal;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.WikiDescriptor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultWikiDescriptorManager}.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class DefaultWikiDescriptorManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiDescriptorManager> mocker =
        new MockitoComponentMockingRule(DefaultWikiDescriptorManager.class);

    @Test
    public void getByWikiAliasWhenNotInCacheButExists() throws Exception
    {
        Cache<WikiDescriptor> cache = mock(Cache.class);
        when(cache.get("wikialias")).thenReturn(null);

        CacheFactory cacheFactory = this.mocker.getInstance(CacheFactory.class);
        when(cacheFactory.<WikiDescriptor>newCache(any(CacheConfiguration.class))).thenReturn(cache);

        QueryManager queryManager = this.mocker.getInstance(QueryManager.class);
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(
            new DefaultQuery("statement", "language", queryExecutor));
        when(queryExecutor.<String>execute(any(Query.class))).thenReturn(Arrays.asList("space.page"));

        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        when(resolver.resolve("space.page")).thenReturn(reference);

        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);

        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq((EntityReference) reference), any(XWikiContext.class))).thenReturn(document);

        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        WikiDescriptor descriptor = new WikiDescriptor("wikiid", "wikialias");
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class), any(XWikiContext.class))).thenReturn(
            descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByWikiAlias("wikialias"));
    }
}
