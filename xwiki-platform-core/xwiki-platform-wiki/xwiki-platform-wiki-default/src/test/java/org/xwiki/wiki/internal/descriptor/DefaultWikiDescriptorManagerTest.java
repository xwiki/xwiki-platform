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
package org.xwiki.wiki.internal.descriptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.internal.manager.WikiDescriptorCache;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptorManager}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class DefaultWikiDescriptorManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiDescriptorManager> mocker =
            new MockitoComponentMockingRule(DefaultWikiDescriptorManager.class);

    private Provider<XWikiContext> xcontextProvider;

    private WikiDescriptorCache cache;

    private WikiDescriptorDocumentHelper descriptorDocumentHelper;

    private WikiDescriptorBuilder wikiDescriptorBuilder;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        // Injection
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        wikiDescriptorBuilder = mocker.getInstance(WikiDescriptorBuilder.class);
        cache = this.mocker.getInstance(WikiDescriptorCache.class);
        descriptorDocumentHelper = mocker.getInstance(WikiDescriptorDocumentHelper.class);

        // Cache is supposed to return null and nul empty list by default
        when(cache.getWikiIds()).thenReturn(null);

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
        // Not in cache
        when(cache.getFromId("wikiid")).thenReturn(null);

        // But exists
        XWikiDocument document = mock(XWikiDocument.class);
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

        verify(cache).addFromId(eq("wikiid"), same(DefaultWikiDescriptor.VOID));
    }

    @Test
    public void getByAliasWhenNotInCacheButExists() throws Exception
    {
        // Not in cache
        when(cache.getFromId("wikiid")).thenReturn(null);

        // But exists
        DocumentReference reference = new DocumentReference("xwiki", "space", "page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(descriptorDocumentHelper.findXWikiServerClassDocument("wikialias")).thenReturn(document);
        when(document.isNew()).thenReturn(false);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
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

        verify(cache).addFromAlias(eq("wikialias"), same(DefaultWikiDescriptor.VOID));
    }

    @Test
    public void getAll() throws Exception
    {
        // Get the XWikiDocuments for the Document References
        XWikiDocument document1 = mock(XWikiDocument.class);
        XWikiDocument document2 = mock(XWikiDocument.class);
        XWikiDocument maindocument = mock(XWikiDocument.class);

        DefaultWikiDescriptor descriptor3 = new DefaultWikiDescriptor("wikiid3", "wikialias3");

        // Get documents
        when(descriptorDocumentHelper.getAllXWikiServerClassDocumentNames()).thenReturn(
                Arrays.asList("XWiki.XWikiServerWikiid1", "XWiki.XWikiServerWikiid2", "XWiki.XWikiServerWikiid3"));
        when(descriptorDocumentHelper.getWikiIdFromDocumentFullname("XWiki.XWikiServerWikiid1")).thenReturn("wikiid1");
        when(descriptorDocumentHelper.getWikiIdFromDocumentFullname("XWiki.XWikiServerWikiid2")).thenReturn("wikiid2");
        when(descriptorDocumentHelper.getWikiIdFromDocumentFullname("XWiki.XWikiServerWikiid3")).thenReturn("wikiid3");
        when(cache.getFromId("wikiid3")).thenReturn(descriptor3);
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid1")).thenReturn(document1);
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid2")).thenReturn(document2);
        when(descriptorDocumentHelper.getDocumentFromWikiId("xwiki")).thenReturn(maindocument);

        when(maindocument.isNew()).thenReturn(true);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document1.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);
        when(document2.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        DefaultWikiDescriptor descriptor1 = new DefaultWikiDescriptor("wikiid1", "wikialias1");
        DefaultWikiDescriptor descriptor2 = new DefaultWikiDescriptor("wikiid2", "wikialias2");
        when(wikiDescriptorBuilder.buildDescriptorObject(anyListOf(BaseObject.class), any(XWikiDocument.class))).
                thenReturn(descriptor1, descriptor2);

        Collection<WikiDescriptor> descriptors = this.mocker.getComponentUnderTest().getAll();
        assertEquals(4, descriptors.size());

        // Verify that XWiki.XWikiServerWikiid3 has not be loaded
        verify(descriptorDocumentHelper, never()).getDocumentFromWikiId("wikiid3");

        // Verify all descriptors were put in cache except those which was already there
        verify(cache).add(descriptor1);
        verify(cache).add(descriptor2);
        verify(cache, never()).add(descriptor3);
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
}
