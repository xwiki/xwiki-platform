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
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class DefaultWikiDeleterTest
{
    @InjectMockComponents
    private DefaultWikiDeleter defaultWikiDeleter;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private WikiDescriptorDocumentHelper descriptorDocumentHelper;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    private XWikiStoreInterface store;

    @BeforeEach
    void setUp()
    {
        // Frequent uses
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        this.store = mock(XWikiStoreInterface.class);
        when(this.xwiki.getStore()).thenReturn(this.store);
    }

    @Test
    void deleteTheMainWiki()
    {
        assertThrows(WikiManagerException.class, () -> this.defaultWikiDeleter.delete("xwiki"));
    }

    @Test
    void deleteWiki() throws Exception
    {
        // Mock
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        descriptor.addAlias("wikialias2");
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(this.descriptorDocumentHelper.getDocumentFromWikiId("wikiid")).thenReturn(descriptorDocument);

        // Delete
        this.defaultWikiDeleter.delete("wikiid");

        // Verify that the database has been deleted
        verify(this.store).deleteWiki(eq("wikiid"), any(XWikiContext.class));
        // Verify that the descriptor document has been deleted
        verify(this.xwiki).deleteDocument(eq(descriptorDocument), any(XWikiContext.class));
    }
}
