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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.WikiDescriptor;
import org.xwiki.wiki.descriptor.internal.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.descriptor.internal.manager.DefaultWikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private Provider<XWikiContext> xcontextProvider;

    private QueryManager queryManager;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private ContextualLocalizationManager localizationManager;

    private ObservationManager observationManager;

    private Logger logger;

    private WikiDescriptorBuilder wikiDescriptorBuilder;

    private Cache<DefaultWikiDescriptor> wikiAliasCache;

    private Cache<DefaultWikiDescriptor> wikiIdCache;

    private CacheFactory cacheFactory;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        // Injection
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        queryManager = mocker.getInstance(QueryManager.class);
        documentReferenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        localizationManager = mocker.getInstance(ContextualLocalizationManager.class);
        observationManager = mocker.getInstance(ObservationManager.class);
        //logger = mocker.getInstance(Logger.class);
        wikiDescriptorBuilder = mocker.getInstance(WikiDescriptorBuilder.class);
        cacheFactory = this.mocker.getInstance(CacheFactory.class);

        // Fields
        wikiAliasCache = mock(Cache.class);
        wikiIdCache = mock(Cache.class);

        // Init
        when(cacheFactory.<DefaultWikiDescriptor>newCache(any(CacheConfiguration.class))).thenReturn(wikiAliasCache,
                wikiIdCache);

        // Frequent uses
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
    }

    @Test
    public void getByIdWhenNotInCacheButExists() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);

        DocumentReference reference = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid");
        when(xwiki.getDocument(eq(reference), any(XWikiContext.class))).thenReturn(document);
        when(document.isNew()).thenReturn(false);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class))).
                thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getById("wikiid"));

        // Verify that calling getById() also sets the wiki id in the cache.
        verify(wikiIdCache).set("wikiid", descriptor);

        // Verify that calling getById() also sets the wiki alias in the cache.
        verify(wikiAliasCache).set("wikialias", descriptor);
    }

    @Test
    public void getByWikiIdWhenInCache() throws Exception
    {
        // Wiki id is in cache...
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(wikiIdCache.get("wikiid")).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getById("wikiid"));
    }

    @Test
    public void getByWikiIdWhenNotInCacheAndItDoesntExist() throws Exception
    {
        // Get the XWikiDocument for the Document Reference but mark it as new (meaning that it doesn't exist)
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference reference = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid");
        when(xwiki.getDocument(eq(reference), any(XWikiContext.class))).thenReturn(document);
        when(document.isNew()).thenReturn(true);

        assertNull(this.mocker.getComponentUnderTest().getById("wikiid"));
    }

    @Test
    public void getByAliasWhenNotInCacheButExists() throws Exception
    {
        // Return "space.page" document name when querying the DB for a XWiki.XWikiServerClass matching the alias
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
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(reference), any(XWikiContext.class))).thenReturn(document);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias", reference);
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class))).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByAlias("wikialias"));

        // Verify that calling getByAlias() also sets the wiki id in the cache.
        verify(wikiIdCache).set("wikiid", descriptor);

        // Verify that calling getById() also sets the wiki alias in the cache.
        verify(wikiAliasCache).set("wikialias", descriptor);
    }

    @Test
    public void getByAliasWhenInCache() throws Exception
    {
        // Wiki alias is in cache...
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(wikiAliasCache.get("wikialias")).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByAlias("wikialias"));
    }

    @Test
    public void getByAliasWhenNotInCacheAndItDoesntExist() throws Exception
    {
        // No result when querying the DB for a XWiki.XWikiServerClass matching the alias
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(queryManager.createQuery("where doc.object(XWiki.XWikiServerClass).server = :wikiAlias and "
                + "doc.name like 'XWikiServer%'", Query.XWQL)).thenReturn(
                new DefaultQuery("statement", "language", queryExecutor));
        when(queryExecutor.<String>execute(any(Query.class))).thenReturn(Collections.EMPTY_LIST);

        assertNull(this.mocker.getComponentUnderTest().getByAlias("wikialias"));
    }

    @Test
    public void getAll() throws Exception
    {
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(queryManager.createQuery("from doc.object(XWiki.XWikiServerClass) as descriptor where "
                + "doc.name like 'XWikiServer%'", Query.XWQL)).thenReturn(
                new DefaultQuery("statement", "language", queryExecutor));
        when(queryExecutor.<String>execute(any(Query.class))).thenReturn(Arrays.asList("space1.page1", "space2.page2"));

        // Convert the returned document names represented as Strings into Document References
        DocumentReference reference1 = new DocumentReference("xwiki", "space1", "page1");
        when(documentReferenceResolver.resolve("space1.page1")).thenReturn(reference1);
        DocumentReference reference2 = new DocumentReference("xwiki", "space2", "page2");
        when(documentReferenceResolver.resolve("space2.page2")).thenReturn(reference2);

        // Get the XWikiDocuments for the Document References
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
        DefaultWikiDescriptor descriptor1 = new DefaultWikiDescriptor("wikiid1", "wikialias1", reference1);
        DefaultWikiDescriptor descriptor2 = new DefaultWikiDescriptor("wikiid2", "wikialias2", reference2);
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class))).
                thenReturn(descriptor1, descriptor2);

        Collection<WikiDescriptor> descriptors = this.mocker.getComponentUnderTest().getAll();
        assertEquals(2, descriptors.size());

        // Verify all descriptors were put in cache
        verify(wikiAliasCache).set("wikialias1", descriptor1);
        verify(wikiAliasCache).set("wikialias2", descriptor2);
        verify(wikiIdCache).set("wikiid1", descriptor1);
        verify(wikiIdCache).set("wikiid2", descriptor2);
    }

    @Test
    public void exists() throws Exception
    {
        // When the wiki exists
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1");
        when(wikiIdCache.get("wikiid1")).thenReturn(descriptor);

        assertTrue(mocker.getComponentUnderTest().exists("wikiid1"));

        // When the wiki does not exists
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid2");
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(document);
        when(document.isNew()).thenReturn(true);

        assertFalse(mocker.getComponentUnderTest().exists("wikiid2"));
    }

    @Test
    public void getMainWikiId() throws Exception
    {
        assertEquals("xwiki", this.mocker.getComponentUnderTest().getMainWikiId());
    }

    @Test
    public void getMainWikiDescriptor() throws Exception
    {
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("xwiki", "xwiki");
        when(wikiIdCache.get("xwiki")).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getMainWikiDescriptor());
    }

    @Test
    public void idAvailable() throws Exception
    {
        // Forbidden list
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden,wikiid3,toto");

        // When the wiki already exists
        when(wikiIdCache.get("wikiid1")).thenReturn(new DefaultWikiDescriptor("wikiid1", "wikialias1"));
        assertFalse(this.mocker.getComponentUnderTest().idAvailable("wikiid1"));

        // When the wiki does not already exists
        DocumentReference documentReference2 = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid2");
        XWikiDocument document2 = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(documentReference2), any(XWikiContext.class))).thenReturn(document2);
        when(document2.isNew()).thenReturn(true);

        assertTrue(this.mocker.getComponentUnderTest().idAvailable("wikiid2"));

        // When the wiki does not already exists but the id is forbidden
        DocumentReference documentReference3 = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid3");
        XWikiDocument document3 = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(documentReference3), any(XWikiContext.class))).thenReturn(document3);
        when(document3.isNew()).thenReturn(true);

        assertFalse(this.mocker.getComponentUnderTest().idAvailable("wikiid3"));
    }

    @Test
    public void createWhenWikiExists() throws Exception
    {
        // When the wiki already exists
        boolean exceptionCaught = false;
        when(wikiIdCache.get("wikiid1")).thenReturn(new DefaultWikiDescriptor("wikiid1", "wikialias1"));
        try {
            this.mocker.getComponentUnderTest().create("wikiid1", "wikialias1");
        } catch (WikiManagerException e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

    @Test
    public void createWhenWikiIdIsForbidden() throws Exception
    {
        // The wiki does not already exist
        DocumentReference documentReference1 = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid1");
        XWikiDocument document1 = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(documentReference1), any(XWikiContext.class))).thenReturn(document1);
        when(document1.isNew()).thenReturn(true);

        // Forbidden list
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden,wikiid1");

        boolean exceptionCaught = false;
        try {
            this.mocker.getComponentUnderTest().create("wikiid1", "wikialias1");
        } catch (WikiManagerException e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

    @Test
    public void createWhenWikiIdIsValid() throws Exception
    {
        // The wiki does not already exist
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid1");
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(descriptorDocument);
        when(descriptorDocument.isNew()).thenReturn(true);
        // The wiki id is valid
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden");
        // Other mocks
        XWikiStoreInterface store = mock(XWikiStoreInterface.class);
        when(xwiki.getStore()).thenReturn(store);
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1");
        when(wikiDescriptorBuilder.build(eq(descriptor))).thenReturn(descriptorDocument);
        when(descriptorDocument.getDocumentReference()).thenReturn(documentReference);

        // Create
        WikiDescriptor newWikiDescriptor = this.mocker.getComponentUnderTest().create("wikiid1", "wikialias1");
        assertNotNull(newWikiDescriptor);
        // Verify that the wiki descriptor is an instance of DefaultWikiDescriptor
        assertTrue(newWikiDescriptor instanceof DefaultWikiDescriptor);
        // Verify that the wiki has been created
        verify(store).createWiki(eq("wikiid1"), any(XWikiContext.class));
        // Verify that the wiki has been updated
        verify(xwiki).updateDatabase(eq("wikiid1"), eq(true), eq(true), any(XWikiContext.class));
        // Verify that the descriptor document has been saved
        verify(store).saveXWikiDoc(eq(descriptorDocument), any(XWikiContext.class));
        // Verify that the descriptor has the good document reference
        assertEquals(documentReference, ((DefaultWikiDescriptor) newWikiDescriptor).getDocumentReference());
        // Verify that the descriptor has been added to the caches
        verify(wikiIdCache).set("wikiid1", (DefaultWikiDescriptor) newWikiDescriptor);
        verify(wikiAliasCache).set("wikialias1", (DefaultWikiDescriptor) newWikiDescriptor);
    }
}

