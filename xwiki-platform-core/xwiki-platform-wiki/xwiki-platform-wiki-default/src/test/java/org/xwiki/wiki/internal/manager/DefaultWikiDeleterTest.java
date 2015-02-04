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
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultWikiDeleterTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiDeleter> mocker =
            new MockitoComponentMockingRule(DefaultWikiDeleter.class);

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private WikiDescriptorDocumentHelper descriptorDocumentHelper;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    private XWikiStoreInterface store;

    @Before
    public void setUp() throws Exception
    {
        // Injection
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        descriptorDocumentHelper = mocker.getInstance(WikiDescriptorDocumentHelper.class);

        // Frequent uses
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        store = mock(XWikiStoreInterface.class);
        when(xwiki.getStore()).thenReturn(store);
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
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        descriptor.addAlias("wikialias2");
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(descriptorDocumentHelper.getDocumentFromWikiId("wikiid")).thenReturn(descriptorDocument);

        // Delete
        this.mocker.getComponentUnderTest().delete("wikiid");

        // Verify that the database has been deleted
        verify(store).deleteWiki(eq("wikiid"), any(XWikiContext.class));
        // Verify that the descriptor document has been deleted
        verify(xwiki).deleteDocument(eq(descriptorDocument), any(XWikiContext.class));
    }
}
