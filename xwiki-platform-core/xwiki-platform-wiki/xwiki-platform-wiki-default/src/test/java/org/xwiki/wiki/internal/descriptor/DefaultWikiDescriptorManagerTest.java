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
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.internal.manager.WikiDescriptorCache;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.properties.WikiPropertyGroup;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
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
@OldcoreTest
class DefaultWikiDescriptorManagerTest
{
    @InjectMockComponents
    private DefaultWikiDescriptorManager descriptorManager;

    @MockComponent
    private WikiDescriptorCache cache;

    @MockComponent
    private WikiDescriptorDocumentHelper descriptorDocumentHelper;

    @MockComponent
    private WikiDescriptorBuilder wikiDescriptorBuilder;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @BeforeEach
    void beforeEach()
    {
        // Cache is supposed to return null and not empty list by default
        when(this.cache.getWikiIds()).thenReturn(null);
    }

    @Test
    void getByIdWhenNotInCacheButExists() throws Exception
    {
        // Not in cache
        when(this.cache.getFromId("wikiid")).thenReturn(null);

        // But exists
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.descriptorDocumentHelper.getDocumentFromWikiId("wikiid")).thenReturn(document);
        when(document.isNew()).thenReturn(false);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(this.wikiDescriptorBuilder.buildDescriptorObject(anyList(), any(XWikiDocument.class)))
            .thenReturn(descriptor);

        assertEquals(descriptor, this.descriptorManager.getById("wikiid"));

        // Verify that calling getById() also sets the descriptor in the cache.
        verify(this.cache).add(descriptor);
    }

    @Test
    void getByIdWhenInCache() throws Exception
    {
        // Wiki id is in cache...
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(this.cache.getFromId("wikiid")).thenReturn(descriptor);

        assertEquals(descriptor, this.descriptorManager.getById("wikiid"));
    }

    @Test
    void getByIdWhenNotInCacheAndItDoesntExist() throws Exception
    {
        // Get the XWikiDocument for the Document Reference but mark it as new (meaning that it doesn't exist)
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.descriptorDocumentHelper.getDocumentFromWikiId("wikiid")).thenReturn(document);
        when(document.isNew()).thenReturn(true);

        assertNull(this.descriptorManager.getById("wikiid"));

        verify(this.cache).addFromId(eq("wikiid"), same(DefaultWikiDescriptor.VOID));
    }

    @Test
    void getByAliasWhenNotInCacheButExists() throws Exception
    {
        // Not in cache
        when(this.cache.getFromId("wikiid")).thenReturn(null);

        // But exists
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.descriptorDocumentHelper.findXWikiServerClassDocument("wikialias")).thenReturn(document);
        when(document.isNew()).thenReturn(false);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(this.wikiDescriptorBuilder.buildDescriptorObject(anyList(), any(XWikiDocument.class)))
            .thenReturn(descriptor);

        assertEquals(descriptor, this.descriptorManager.getByAlias("wikialias"));

        // Verify that calling getByAlias() also sets the descriptor in the cache.
        verify(this.cache).add(descriptor);
    }

    @Test
    void getByAliasWhenInCache() throws Exception
    {
        // Wiki alias is in cache...
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        when(this.cache.getFromAlias("wikialias")).thenReturn(descriptor);

        assertEquals(descriptor, this.descriptorManager.getByAlias("wikialias"));
    }

    @Test
    void getByAliasWhenNotInCacheAndItDoesntExist() throws Exception
    {
        assertNull(this.descriptorManager.getByAlias("wikialias"));

        verify(this.cache).addFromAlias(eq("wikialias"), same(DefaultWikiDescriptor.VOID));
    }

    @Test
    void getAll() throws Exception
    {
        // Get the XWikiDocuments for the Document References
        XWikiDocument document1 = mock(XWikiDocument.class);
        XWikiDocument document2 = mock(XWikiDocument.class);
        XWikiDocument maindocument = mock(XWikiDocument.class);

        DefaultWikiDescriptor descriptor3 = new DefaultWikiDescriptor("wikiid3", "wikialias3");
        descriptor3.setPrettyName("alice");

        // Get documents
        when(this.descriptorDocumentHelper.getAllXWikiServerClassDocumentNames()).thenReturn(
            Arrays.asList("XWiki.XWikiServerWikiid1", "XWiki.XWikiServerWikiid2", "XWiki.XWikiServerWikiid3"));
        when(this.descriptorDocumentHelper.getWikiIdFromDocumentFullname("XWiki.XWikiServerWikiid1"))
            .thenReturn("dev_wikiid1");
        when(this.descriptorDocumentHelper.getWikiIdFromDocumentFullname("XWiki.XWikiServerWikiid2"))
            .thenReturn("wikiid2");
        when(this.descriptorDocumentHelper.getWikiIdFromDocumentFullname("XWiki.XWikiServerWikiid3"))
            .thenReturn("wikiid3");
        when(this.cache.getFromId("wikiid3")).thenReturn(descriptor3);
        when(this.descriptorDocumentHelper.getDocumentFromWikiId("dev_wikiid1")).thenReturn(document1);
        when(this.descriptorDocumentHelper.getDocumentFromWikiId("wikiid2")).thenReturn(document2);
        when(this.descriptorDocumentHelper.getDocumentFromWikiId("xwiki")).thenReturn(maindocument);

        when(maindocument.isNew()).thenReturn(true);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document1.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);
        when(document2.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a Wiki from the Wiki Descriptor Builder
        DefaultWikiDescriptor descriptor1 = new DefaultWikiDescriptor("dev_wikiid1", "wikialias1");
        DefaultWikiDescriptor descriptor2 = new DefaultWikiDescriptor("wikiid2", "wikialias2");
        descriptor1.setPrettyName("John");
        when(this.wikiDescriptorBuilder.buildDescriptorObject(anyList(), any(XWikiDocument.class)))
            .thenReturn(descriptor1, descriptor2);

        Collection<WikiDescriptor> descriptors = this.descriptorManager.getAll();
        assertEquals(4, descriptors.size());

        // The descriptors should be sorted by pretty name (with a fallback to wiki id).
        assertEquals(Arrays.asList("wikiid3", "dev_wikiid1", "wikiid2", "xwiki"),
            descriptors.stream().map(WikiDescriptor::getId).collect(Collectors.toList()));

        // Verify that XWiki.XWikiServerWikiid3 has not be loaded
        verify(this.descriptorDocumentHelper, never()).getDocumentFromWikiId("wikiid3");

        // Verify all descriptors were put in cache except those which was already there
        verify(this.cache).add(descriptor1);
        verify(this.cache).add(descriptor2);
        verify(this.cache, never()).add(descriptor3);
    }

    @Test
    void exists() throws Exception
    {
        when(this.cache.getWikiIds()).thenReturn(Arrays.asList("wikiid1"));

        // When the wiki exists
        assertTrue(this.descriptorManager.exists("wikiid1"));

        // When the wiki does not exist
        assertFalse(this.descriptorManager.exists("wikiid2"));
    }

    @Test
    void getMainWikiId()
    {
        assertEquals("xwiki", this.descriptorManager.getMainWikiId());
    }

    @Test
    void isMainWiki()
    {
        assertTrue(this.descriptorManager.isMainWiki(null));
        assertTrue(this.descriptorManager.isMainWiki(""));
        assertTrue(this.descriptorManager.isMainWiki("xwiki"));
        assertFalse(this.descriptorManager.isMainWiki("notmainwiki"));
    }

    @Test
    void getMainWikiDescriptor() throws Exception
    {
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("xwiki", "xwiki");
        when(this.cache.getFromId("xwiki")).thenReturn(descriptor);

        assertEquals(descriptor, this.descriptorManager.getMainWikiDescriptor());
    }

    @Test
    void cacheProtection() throws WikiManagerException
    {
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("xwiki", "xwiki");
        descriptor.setPrettyName("pretty name");
        WikiPropertyGroup propertyGroup = new WikiPropertyGroup("group");
        propertyGroup.set("property", "value");
        descriptor.addPropertyGroup(propertyGroup);
        when(this.cache.getFromId("xwiki")).thenReturn(descriptor);
        when(this.cache.getFromAlias("xwiki")).thenReturn(descriptor);

        WikiDescriptorManager wikiDescriptorManager = this.descriptorManager;

        // Modify the descriptor without saving it
        wikiDescriptorManager.getById("xwiki").setPrettyName("changed pretty name");
        assertEquals("pretty name", wikiDescriptorManager.getById("xwiki").getPrettyName());
        wikiDescriptorManager.getById("xwiki").getPropertyGroup("group").set("property", "modified value");
        assertEquals("value", wikiDescriptorManager.getById("xwiki").getPropertyGroup("group").get("property"));

        // Modify the descriptor without saving it
        wikiDescriptorManager.getByAlias("xwiki").setPrettyName("changed pretty name");
        assertEquals("pretty name", wikiDescriptorManager.getByAlias("xwiki").getPrettyName());
        wikiDescriptorManager.getByAlias("xwiki").getPropertyGroup("group").set("property", "modified value");
        assertEquals("value", wikiDescriptorManager.getByAlias("xwiki").getPropertyGroup("group").get("property"));

        // Modify the descriptor without saving it
        wikiDescriptorManager.getMainWikiDescriptor().setPrettyName("changed pretty name");
        assertEquals("pretty name", wikiDescriptorManager.getMainWikiDescriptor().getPrettyName());
        wikiDescriptorManager.getMainWikiDescriptor().getPropertyGroup("group").set("property", "modified value");
        assertEquals("value", wikiDescriptorManager.getMainWikiDescriptor().getPropertyGroup("group").get("property"));
    }

    @Test
    void getCurrentWikiId()
    {
        this.oldcore.getXWikiContext().setWikiId("wiki");

        assertEquals("wiki", this.descriptorManager.getCurrentWikiId());
    }

    @Test
    void getCurrentWikiDescriptor() throws WikiManagerException
    {
        this.oldcore.getXWikiContext().setWikiId("wiki");
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wiki", "wikialias");
        when(this.cache.getFromId("wiki")).thenReturn(descriptor);

        assertEquals(descriptor, this.descriptorManager.getCurrentWikiDescriptor());
    }
}
