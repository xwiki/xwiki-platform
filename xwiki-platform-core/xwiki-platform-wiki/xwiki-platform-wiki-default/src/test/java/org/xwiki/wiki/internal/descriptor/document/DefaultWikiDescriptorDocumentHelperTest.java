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
package org.xwiki.wiki.internal.descriptor.document;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.descriptor.document.DefaultWikiDescriptorDocumentHelper}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class DefaultWikiDescriptorDocumentHelperTest
{
    @Rule
    public org.xwiki.test.mockito.MockitoComponentMockingRule<DefaultWikiDescriptorDocumentHelper> mocker =
            new MockitoComponentMockingRule<>(DefaultWikiDescriptorDocumentHelper.class);

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private QueryManager queryManager;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private XWikiContext context;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.registerMockComponent(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");

        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        context = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(context);
        xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);

        queryManager = mocker.getInstance(QueryManager.class);
        documentReferenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
    }

    @Test
    public void getDocumentReferenceFromId() throws Exception
    {
        DocumentReference docRef = new DocumentReference("xwiki", XWiki.SYSTEM_SPACE, "XWikiServerWikiid1");
        assertEquals(docRef, this.mocker.getComponentUnderTest().getDocumentReferenceFromId("wikiid1"));
    }

    @Test
    public void getDocumentFromWikiId() throws Exception
    {
        DocumentReference docRef = new DocumentReference("xwiki", XWiki.SYSTEM_SPACE, "XWikiServerWikiid1");
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(docRef), any(XWikiContext.class))).thenReturn(document);

        XWikiDocument returnedDocument = this.mocker.getComponentUnderTest().getDocumentFromWikiId("wikiid1");
        assertEquals(returnedDocument, document);
    }

    @Test
    public void getWikiIdFromDocumentFullname() throws Exception
    {
        String result = mocker.getComponentUnderTest().getWikiIdFromDocumentFullname("XWiki.XWikiServerSubwiki");
        assertEquals("subwiki", result);

        result = mocker.getComponentUnderTest().getWikiIdFromDocumentFullname("XWiki.XWikiServerXWikiServer");
        assertEquals("xwikiserver", result);

        result = mocker.getComponentUnderTest().getWikiIdFromDocumentFullname("XWiki.XWikiServerAbbc");
        assertEquals("abbc", result);
    }

}
