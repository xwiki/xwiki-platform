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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.descriptor.document.DefaultWikiDescriptorDocumentHelper}.
 *
 * @version $Id$
 * @since 6.0M1
 */
@ComponentTest
class DefaultWikiDescriptorDocumentHelperTest
{
    @InjectMockComponents
    private DefaultWikiDescriptorDocumentHelper documentHelper;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    private XWikiContext context;

    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");

        this.context = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.context);
        this.xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void getDocumentReferenceFromId()
    {
        DocumentReference docRef = new DocumentReference("xwiki", XWiki.SYSTEM_SPACE, "XWikiServerWikiid1");
        assertEquals(docRef, this.documentHelper.getDocumentReferenceFromId("wikiid1"));
    }

    @Test
    void getDocumentFromWikiId() throws Exception
    {
        DocumentReference docRef = new DocumentReference("xwiki", XWiki.SYSTEM_SPACE, "XWikiServerWikiid1");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(eq(docRef), any(XWikiContext.class))).thenReturn(document);

        XWikiDocument returnedDocument = this.documentHelper.getDocumentFromWikiId("wikiid1");
        assertEquals(returnedDocument, document);
    }

    @Test
    void getWikiIdFromDocumentFullname()
    {
        String result = this.documentHelper.getWikiIdFromDocumentFullname("XWiki.XWikiServerSubwiki");
        assertEquals("subwiki", result);

        result = this.documentHelper.getWikiIdFromDocumentFullname("XWiki.XWikiServerXWikiServer");
        assertEquals("xwikiserver", result);

        result = this.documentHelper.getWikiIdFromDocumentFullname("XWiki.XWikiServerAbbc");
        assertEquals("abbc", result);
    }
}
