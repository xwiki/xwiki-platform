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
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.WikiDescriptor;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.internal.descriptor.document.DefaultWikiDescriptorDocumentHelper;
import org.xwiki.wiki.internal.manager.DefaultWikiManager;
import org.xwiki.wiki.internal.manager.WikiDescriptorCache;
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
 * Unit tests for {@link org.xwiki.wiki.internal.manager.DefaultWikiManager}.
 *
 * @version $Id$
 * @since 5.3M1
 */
public class DefaultWikiManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiManager> mocker =
            new MockitoComponentMockingRule(DefaultWikiManager.class);

    private Provider<XWikiContext> xcontextProvider;

    private ContextualLocalizationManager localizationManager;

    private ObservationManager observationManager;

    private Logger logger;

    private WikiDescriptorBuilder wikiDescriptorBuilder;

    private DefaultWikiDescriptorDocumentHelper descriptorDocumentHelper;

    private WikiDescriptorCache cache;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        // Injection
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        localizationManager = mocker.getInstance(ContextualLocalizationManager.class);
        observationManager = mocker.getInstance(ObservationManager.class);
        //logger = mocker.getInstance(Logger.class);
        wikiDescriptorBuilder = mocker.getInstance(WikiDescriptorBuilder.class);
        cache = this.mocker.getInstance(WikiDescriptorCache.class);
        descriptorDocumentHelper = mocker.getInstance(DefaultWikiDescriptorDocumentHelper.class);

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
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid")).thenReturn(document);
        when(document.isNew()).thenReturn(false);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(wikiDescriptorBuilder.buildDescriptorObject(anyListOf(BaseObject.class), any(XWikiDocument.class))).
                thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getById("wikiid"));

        // Verify that calling getById() also sets the descriptor in the cache.
        verify(cache).add(descriptor);
    }

    @Test
    public void getByWikiIdWhenInCache() throws Exception
    {
        // Wiki id is in cache...
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(cache.getFromId("wikiid")).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getById("wikiid"));
    }

    @Test
    public void getByWikiIdWhenNotInCacheAndItDoesntExist() throws Exception
    {
        // Get the XWikiDocument for the Document Reference but mark it as new (meaning that it doesn't exist)
        XWikiDocument document = mock(XWikiDocument.class);
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid")).thenReturn(document);
        when(document.isNew()).thenReturn(true);

        assertNull(this.mocker.getComponentUnderTest().getById("wikiid"));
    }

    @Test
    public void getByAliasWhenNotInCacheButExists() throws Exception
    {
        DocumentReference reference = new DocumentReference("xwiki", "space", "page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(descriptorDocumentHelper.findXWikiServerClassDocument("wikialias")).thenReturn(document);
        when(document.isNew()).thenReturn(false);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias", reference);
        when(wikiDescriptorBuilder.buildDescriptorObject(anyListOf(BaseObject.class), any(XWikiDocument.class))).
                thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByAlias("wikialias"));

        // Verify that calling getByAlias() also sets the descriptor in the cache.
        verify(cache).add(descriptor);
    }

    @Test
    public void getByAliasWhenInCache() throws Exception
    {
        // Wiki alias is in cache...
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(cache.getFromAlias("wikialias")).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByAlias("wikialias"));
    }

    @Test
    public void getByAliasWhenNotInCacheAndItDoesntExist() throws Exception
    {
        assertNull(this.mocker.getComponentUnderTest().getByAlias("wikialias"));
    }

    @Test
    public void getAll() throws Exception
    {
        DocumentReference reference1 = new DocumentReference("xwiki", "space1", "page1");
        DocumentReference reference2 = new DocumentReference("xwiki", "space2", "page2");

        // Get the XWikiDocuments for the Document References
        XWikiDocument document1 = mock(XWikiDocument.class);
        XWikiDocument document2 = mock(XWikiDocument.class);

        // Get documents
        when(descriptorDocumentHelper.getAllXWikiServerClassDocument()).thenReturn(Arrays.asList(document1, document2));

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document1.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);
        when(document2.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        DefaultWikiDescriptor descriptor1 = new DefaultWikiDescriptor("wikiid1", "wikialias1", reference1);
        DefaultWikiDescriptor descriptor2 = new DefaultWikiDescriptor("wikiid2", "wikialias2", reference2);
        when(wikiDescriptorBuilder.buildDescriptorObject(anyListOf(BaseObject.class), any(XWikiDocument.class))).
                thenReturn(descriptor1, descriptor2);

        Collection<WikiDescriptor> descriptors = this.mocker.getComponentUnderTest().getAll();
        assertEquals(2, descriptors.size());

        // Verify all descriptors were put in cache
        verify(cache).add(descriptor1);
        verify(cache).add(descriptor2);
    }

    @Test
    public void exists() throws Exception
    {
        // When the wiki exists
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1");
        when(cache.getFromId("wikiid1")).thenReturn(descriptor);

        assertTrue(mocker.getComponentUnderTest().exists("wikiid1"));

        // When the wiki does not exists
        XWikiDocument document = mock(XWikiDocument.class);
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid2")).thenReturn(document);
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
        when(cache.getFromId("xwiki")).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getMainWikiDescriptor());
    }

    @Test
    public void idAvailable() throws Exception
    {
        // Forbidden list
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden,wikiid3,toto");

        // When the wiki already exists
        when(cache.getFromId("wikiid1")).thenReturn(new DefaultWikiDescriptor("wikiid1", "wikialias1"));
        assertFalse(this.mocker.getComponentUnderTest().idAvailable("wikiid1"));

        // When the wiki does not already exists
        XWikiDocument document2 = mock(XWikiDocument.class);
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid2")).thenReturn(document2);
        when(document2.isNew()).thenReturn(true);

        assertTrue(this.mocker.getComponentUnderTest().idAvailable("wikiid2"));

        // When the wiki does not already exists but the id is forbidden
        XWikiDocument document3 = mock(XWikiDocument.class);
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid3")).thenReturn(document3);
        when(document3.isNew()).thenReturn(true);

        assertFalse(this.mocker.getComponentUnderTest().idAvailable("wikiid3"));
    }

    @Test
    public void createWhenWikiExists() throws Exception
    {
        // When the wiki already exists
        boolean exceptionCaught = false;
        when(cache.getFromId("wikiid1")).thenReturn(new DefaultWikiDescriptor("wikiid1", "wikialias1"));
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
        XWikiDocument document1 = mock(XWikiDocument.class);
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid1")).thenReturn(document1);
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
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid1")).thenReturn(descriptorDocument);
        when(descriptorDocument.isNew()).thenReturn(true);

        // The wiki id is valid
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden");

        // Other mocks
        XWikiStoreInterface store = mock(XWikiStoreInterface.class);
        when(xwiki.getStore()).thenReturn(store);
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1");
        when(wikiDescriptorBuilder.buildDescriptorDocument(eq(descriptor))).thenReturn(descriptorDocument);
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
        verify(cache).add(eq((DefaultWikiDescriptor) newWikiDescriptor));
    }

    @Test
    public void deleteTheMainWiki() throws Exception
    {
        boolean exceptionCaught = false;
        try {
            this.mocker.getComponentUnderTest().delete("xwiki");
        } catch (WikiManagerException e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

    @Test
    public void deleteWiki() throws Exception
    {
        // Mock
        XWikiStoreInterface store = mock(XWikiStoreInterface.class);
        when(xwiki.getStore()).thenReturn(store);
        DocumentReference documentReference = new DocumentReference("xwiki", "space", "page");
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias", documentReference);
        descriptor.addAlias("wikialias2");
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(document);
        when(cache.getFromId("wikiid")).thenReturn(descriptor);

        // Delete
        this.mocker.getComponentUnderTest().delete("wikiid");

        // Verify that the database has been deleted
        verify(store).deleteWiki(eq("wikiid"), any(XWikiContext.class));
        // Verify that the descriptor document has been deleted
        verify(xwiki).deleteDocument(eq(document), any(XWikiContext.class));
        // Verify that the descriptor has been removed from caches
        verify(cache).remove(eq(descriptor));
        // Verify that an event has been sent
        verify(observationManager).notify(eq(new WikiDeletedEvent("wikiid")), eq(descriptor));
    }

    @Test
    public void copyWhenWikiAlreadyExists() throws Exception
    {
        // The wiki already exist
        when(cache.getFromId("existingid")).thenReturn(new DefaultWikiDescriptor("exisitingid", "alias"));
        boolean exceptionCaught = false;
        try {
            this.mocker.getComponentUnderTest().copy("wikiid", "existingid", "newwikialias", true, true);
        } catch (WikiManagerException e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

    @Test
    public void copyWhenWikiAvailable() throws Exception
    {
        // The wiki does not already exist
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid1");
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid1")).thenReturn(descriptorDocument);
        when(descriptorDocument.isNew()).thenReturn(true);

        // The new id is valid
        when(xwiki.Param("xwiki.virtual.reserved_wikis")).thenReturn("forbidden");

        // Other mocks
        XWikiStoreInterface store = mock(XWikiStoreInterface.class);
        when(xwiki.getStore()).thenReturn(store);
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1");
        when(wikiDescriptorBuilder.buildDescriptorDocument(eq(descriptor))).thenReturn(descriptorDocument);
        when(descriptorDocument.getDocumentReference()).thenReturn(documentReference);

        // Create
        WikiDescriptor newWikiDescriptor = this.mocker.getComponentUnderTest().copy("wikiid", "wikiid1",
                "wikialias1", true, true);
        assertNotNull(newWikiDescriptor);

        // Verify that the wiki descriptor is an instance of DefaultWikiDescriptor
        assertTrue(newWikiDescriptor instanceof DefaultWikiDescriptor);
        // Verify that the wiki has been copied
        verify(xwiki).copyWiki(eq("wikiid"), eq("wikiid1"), any(String.class), any(XWikiContext.class));
        // Verify that the descriptor document has been saved
        verify(store).saveXWikiDoc(eq(descriptorDocument), any(XWikiContext.class));
        // Verify that the descriptor has the good document reference
        assertEquals(documentReference, ((DefaultWikiDescriptor) newWikiDescriptor).getDocumentReference());
        // Verify that the descriptor has been added to the caches
        verify(cache).add(eq((DefaultWikiDescriptor) newWikiDescriptor));
    }
}

