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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultWikiCreatorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiCreator> mocker =
            new MockitoComponentMockingRule(DefaultWikiCreator.class);

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    private XWikiStoreInterface store;

    private WikiDescriptorBuilder wikiDescriptorBuilder;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        store = mock(XWikiStoreInterface.class);
        when(xwiki.getStore()).thenReturn(store);
        wikiDescriptorBuilder = mocker.getInstance(WikiDescriptorBuilder.class);
    }

    @Test
    public void create() throws Exception
    {
        // Other mocks
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid1", "wikialias1");
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(wikiDescriptorBuilder.save(eq(descriptor))).thenReturn(descriptorDocument);
        when(wikiDescriptorManager.getById("wikiid1")).thenReturn(descriptor);
        when(store.isWikiNameAvailable(any(String.class), any(XWikiContext.class))).thenReturn(true);

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
        verify(wikiDescriptorBuilder).save(eq(descriptor));
        // Verify that the descriptor has been reloaded after being saved
        assertTrue(descriptor == newWikiDescriptor);
    }
}
