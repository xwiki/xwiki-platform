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
package org.xwiki.wiki.internal.manager;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.configuration.WikiConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class DefaultWikiCreatorTest
{
    @InjectMockComponents
    private DefaultWikiCreator defaultWikiCreator;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private WikiDescriptorBuilder wikiDescriptorBuilder;

    @MockComponent
    private WikiConfiguration wikiConfiguration;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    private XWikiStoreInterface store;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        this.store = mock(XWikiStoreInterface.class);
        when(this.xwiki.getStore()).thenReturn(this.store);
    }

    @Test
    void create() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(true);

        // Other mocks
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1", "owner");
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(this.wikiDescriptorBuilder.save(eq(descriptor))).thenReturn(descriptorDocument);
        when(this.wikiDescriptorManager.getById("wikiid1")).thenReturn(descriptor);
        when(this.store.isWikiNameAvailable(any(String.class), any(XWikiContext.class))).thenReturn(true);

        // Create
        WikiDescriptor newWikiDescriptor = this.defaultWikiCreator.create("wikiid1", "wikialias1", "owner");
        assertNotNull(newWikiDescriptor);

        // Verify that the wiki descriptor is an instance of DefaultWikiDescriptor
        assertTrue(newWikiDescriptor instanceof DefaultWikiDescriptor);
        // Verify that the wiki has been created
        verify(this.store).createWiki(eq("wikiid1"), any(XWikiContext.class));
        // Verify that the wiki has been updated
        verify(this.xwiki).initializeWiki(eq("wikiid1"), eq(true), any(XWikiContext.class));
        // Verify that the descriptor document has been saved
        verify(this.wikiDescriptorBuilder).save(eq(descriptor));
        // Verify that the descriptor has been reloaded after being saved
        assertTrue(descriptor == newWikiDescriptor);
    }

    @Test
    void createWhenSkippingDatabaseCreation() throws Exception
    {
        when(this.wikiConfiguration.shouldCreateDatabase()).thenReturn(false);

        // Other mocks
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1", "owner");
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(this.wikiDescriptorBuilder.save(eq(descriptor))).thenReturn(descriptorDocument);
        when(this.wikiDescriptorManager.getById("wikiid1")).thenReturn(descriptor);
        when(this.store.isWikiNameAvailable(any(String.class), any(XWikiContext.class))).thenReturn(true);

        // Create
        WikiDescriptor newWikiDescriptor = this.defaultWikiCreator.create("wikiid1", "wikialias1", "owner");
        assertNotNull(newWikiDescriptor);

        // Verify that the wiki descriptor is an instance of DefaultWikiDescriptor
        assertInstanceOf(DefaultWikiDescriptor.class, newWikiDescriptor);
        // Verify that the DB has not been created
        verify(this.store, never()).createWiki(eq("wikiid1"), any(XWikiContext.class));
        // Verify that the wiki has been updated
        verify(this.xwiki).initializeWiki(eq("wikiid1"), eq(true), any(XWikiContext.class));
        // Verify that the descriptor document has been saved
        verify(this.wikiDescriptorBuilder).save(eq(descriptor));
        // Verify that the descriptor has been reloaded after being saved
        assertSame(descriptor, newWikiDescriptor);
    }
}
