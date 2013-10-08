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
package org.xwiki.wiki.descriptor.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.xwiki.wiki.Wiki;
import org.xwiki.wiki.descriptor.internal.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.descriptor.internal.manager.DefaultWikiManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.wiki.descriptor.internal.manager.DefaultWikiManager}.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class DefaultWikiManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiManager> mocker =
        new MockitoComponentMockingRule(DefaultWikiManager.class);

    @Test
    public void createWiki() throws Exception
    {


        //this.mocker.getComponentUnderTest().create("wikiid", "wikialias");

    }

    @Test
    public void getByWikiAliasWhenNotInCacheButExists() throws Exception
    {
        // Wiki alias is not in cache...
        Cache<Wiki> wikiAliasCache = mock(Cache.class);
        Cache<Wiki> wikiIdCache = mock(Cache.class);
        CacheFactory cacheFactory = this.mocker.getInstance(CacheFactory.class);
        when(cacheFactory.<Wiki>newCache(any(CacheConfiguration.class))).thenReturn(wikiAliasCache,
            wikiIdCache);

        // Return "space.page" document name when querying the DB for a XWiki.XWikiServerClass matching the alias
        QueryManager queryManager = this.mocker.getInstance(QueryManager.class);
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(queryManager.createQuery("where doc.object(XWiki.XWikiServerClass).server = :wikiAlias and "
            + "doc.name like 'XWikiServer%'", Query.XWQL)).thenReturn(
            new DefaultQuery("statement", "language", queryExecutor));
        when(queryExecutor.<String>execute(any(Query.class))).thenReturn(Arrays.asList("space.page"));

        // Convert the returned document name represented as a String into a Document Reference
        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        DocumentReference reference = new DocumentReference("xwiki", "space", "page");
        when(resolver.resolve("space.page")).thenReturn(reference);

        // Get the XWikiDocument for the Document Reference
        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(reference), any(XWikiContext.class))).thenReturn(document);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        Wiki descriptor = new Wiki("wikiid", "wikialias");
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class),
            any(XWikiContext.class))).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByAlias("wikialias"));

        // Verify that calling getByAlias() also sets the wiki id in the cache.
        verify(wikiIdCache).set("wikiid", descriptor);
    }

    @Test
    public void getByWikiAliasWhenInCache() throws Exception
    {
        // Wiki alias is in cache...
        Cache<Wiki> wikiAliasCache = mock(Cache.class);
        Cache<Wiki> wikiIdCache = mock(Cache.class);
        CacheFactory cacheFactory = this.mocker.getInstance(CacheFactory.class);
        when(cacheFactory.<Wiki>newCache(any(CacheConfiguration.class))).thenReturn(wikiAliasCache,
            wikiIdCache);
        Wiki descriptor = new Wiki("wikiid", "wikialias");
        when(wikiAliasCache.get("wikialias")).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByAlias("wikialias"));
    }

    @Test
    public void getByWikiAliasWhenNotInCacheAndItDoesntExist() throws Exception
    {
        // Wiki alias is not in cache...
        Cache<Wiki> wikiAliasCache = mock(Cache.class);
        Cache<Wiki> wikiIdCache = mock(Cache.class);
        CacheFactory cacheFactory = this.mocker.getInstance(CacheFactory.class);
        when(cacheFactory.<Wiki>newCache(any(CacheConfiguration.class))).thenReturn(wikiAliasCache,
            wikiIdCache);

        // No result when querying the DB for a XWiki.XWikiServerClass matching the alias
        QueryManager queryManager = this.mocker.getInstance(QueryManager.class);
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(queryManager.createQuery("where doc.object(XWiki.XWikiServerClass).server = :wikiAlias and "
            + "doc.name like 'XWikiServer%'", Query.XWQL)).thenReturn(
            new DefaultQuery("statement", "language", queryExecutor));
        when(queryExecutor.<String>execute(any(Query.class))).thenReturn(Collections.EMPTY_LIST);

        // Get the main wiki
        /*
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);

        assertNull(this.mocker.getComponentUnderTest().getByAlias("wikialias"));*/
    }

    @Test
    public void getByWikiIdWhenNotInCacheButExists() throws Exception
    {
        // Wiki id is not in cache...
        Cache<Wiki> wikiAliasCache = mock(Cache.class);
        Cache<Wiki> wikiIdCache = mock(Cache.class);
        CacheFactory cacheFactory = this.mocker.getInstance(CacheFactory.class);
        when(cacheFactory.<Wiki>newCache(any(CacheConfiguration.class))).thenReturn(wikiAliasCache,
            wikiIdCache);

        // Get the XWikiDocument for the Document Reference
        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference reference = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid");
        when(xwiki.getDocument(eq(reference), any(XWikiContext.class))).thenReturn(document);
        when(document.isNew()).thenReturn(false);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        Wiki descriptor = new Wiki("wikiid", "wikialias");
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class),
            any(XWikiContext.class))).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getById("wikiid"));

        // Verify that calling getById() also sets the wiki alias in the cache.
        verify(wikiAliasCache).set("wikialias", descriptor);
    }

    @Test
    public void getByWikiIdWhenInCache() throws Exception
    {
        // Wiki id is in cache...
        Cache<Wiki> wikiAliasCache = mock(Cache.class);
        Cache<Wiki> wikiIdCache = mock(Cache.class);
        CacheFactory cacheFactory = this.mocker.getInstance(CacheFactory.class);
        when(cacheFactory.<Wiki>newCache(any(CacheConfiguration.class))).thenReturn(wikiAliasCache,
            wikiIdCache);
        Wiki descriptor = new Wiki("wikiid", "wikialias");
        when(wikiIdCache.get("wikiid")).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getById("wikiid"));
    }

    @Test
    public void getByWikiIdWhenNotInCacheAndItDoesntExist() throws Exception
    {
        // Wiki id is not in cache...
        Cache<Wiki> wikiAliasCache = mock(Cache.class);
        Cache<Wiki> wikiIdCache = mock(Cache.class);
        CacheFactory cacheFactory = this.mocker.getInstance(CacheFactory.class);
        when(cacheFactory.<Wiki>newCache(any(CacheConfiguration.class))).thenReturn(wikiAliasCache,
            wikiIdCache);

        // Get the XWikiDocument for the Document Reference but mark it as new (meaning that it doesn't exist)
        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference reference = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid");
        when(xwiki.getDocument(eq(reference), any(XWikiContext.class))).thenReturn(document);
        when(document.isNew()).thenReturn(true);

        assertNull(this.mocker.getComponentUnderTest().getById("wikiid"));
    }

    @Test
    public void getAll() throws Exception
    {
        // Cache setup
        Cache<Wiki> wikiAliasCache = mock(Cache.class);
        Cache<Wiki> wikiIdCache = mock(Cache.class);
        CacheFactory cacheFactory = this.mocker.getInstance(CacheFactory.class);
        when(cacheFactory.<Wiki>newCache(any(CacheConfiguration.class))).thenReturn(wikiAliasCache,
            wikiIdCache);

        QueryManager queryManager = this.mocker.getInstance(QueryManager.class);
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(queryManager.createQuery("from doc.object(XWiki.XWikiServerClass) as descriptor where "
            + "doc.name like 'XWikiServer%'", Query.XWQL)).thenReturn(
            new DefaultQuery("statement", "language", queryExecutor));
        when(queryExecutor.<String>execute(any(Query.class))).thenReturn(Arrays.asList("space1.page1", "space2.page2"));

        // Convert the returned document names represented as Strings into Document References
        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        DocumentReference reference1 = new DocumentReference("xwiki", "space1", "page1");
        when(resolver.resolve("space1.page1")).thenReturn(reference1);
        DocumentReference reference2 = new DocumentReference("xwiki", "space2", "page2");
        when(resolver.resolve("space2.page2")).thenReturn(reference2);

        // Get the XWikiDocuments for the Document References
        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);
        XWikiDocument document1 = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(reference1), any(XWikiContext.class))).thenReturn(document1);
        XWikiDocument document2 = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(reference2), any(XWikiContext.class))).thenReturn(document2);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document1.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);
        when(document2.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        Wiki descriptor1 = new Wiki("wikiid1", "wikialias1");
        Wiki descriptor2 = new Wiki("wikiid2", "wikialias2");
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class),
            any(XWikiContext.class))).thenReturn(descriptor1, descriptor2);

        Collection<Wiki> descriptors = this.mocker.getComponentUnderTest().getAll();
        assertEquals(2, descriptors.size());

        // Verify all descriptors were put in cache
        verify(wikiAliasCache).set("wikialias1", descriptor1);
        verify(wikiAliasCache).set("wikialias2", descriptor2);
        verify(wikiIdCache).set("wikiid1", descriptor1);
        verify(wikiIdCache).set("wikiid2", descriptor2);
    }
}
