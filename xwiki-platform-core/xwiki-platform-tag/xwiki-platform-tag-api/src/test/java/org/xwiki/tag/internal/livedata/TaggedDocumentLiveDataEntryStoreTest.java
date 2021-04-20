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
package org.xwiki.tag.internal.livedata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.livedata.LiveDataQuery.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.tag.internal.TagQueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Test of {@link TaggedDocumentLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@ComponentTest
class TaggedDocumentLiveDataEntryStoreTest
{
    @InjectMockComponents
    private TaggedDocumentLiveDataEntryStore taggedDocumentLiveDataEntryStore;

    @MockComponent
    private TagQueryManager tagQueryManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki xWiki;

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xWikiContext);
        when(this.xWikiContext.getWiki()).thenReturn(this.xWiki);
    }

    @Test
    void get() throws Exception
    {
        LiveData expected = new LiveData();
        expected.setCount(4);
        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("page", "Title1");
        entry1.put("page_link", "http://xwiki.test/P1");
        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("page", "Title2");
        entry2.put("page_link", "http://xwiki.test/P2");
        expected.getEntries().addAll(Arrays.asList(entry1, entry2));
        DocumentReference page1 = new DocumentReference("xwiki", "XWiki", "D1");
        DocumentReference page2 = new DocumentReference("xwiki", "XWiki", "D2");
        XWikiDocument document1 = mock(XWikiDocument.class);
        XWikiDocument document2 = mock(XWikiDocument.class);

        when(this.tagQueryManager.countPages("t1")).thenReturn(4L);
        when(this.tagQueryManager.getPages("t1", 2, 1)).thenReturn(Arrays.asList(
            page1,
            page2
        ));
        when(this.xWiki.getDocument(page1, this.xWikiContext)).thenReturn(document1);
        when(this.xWiki.getDocument(page2, this.xWikiContext)).thenReturn(document2);
        when(document1.getRenderedTitle(this.xWikiContext)).thenReturn("Title1");
        when(document2.getRenderedTitle(this.xWikiContext)).thenReturn("Title2");
        when(this.xWiki.getURL(page1, this.xWikiContext)).thenReturn("http://xwiki.test/P1");
        when(this.xWiki.getURL(page2, this.xWikiContext)).thenReturn("http://xwiki.test/P2");

        LiveDataQuery query = new LiveDataQuery();
        Source source = new Source();
        source.setParameter("tag", "t1");
        query.setSource(source);
        query.setLimit(2);
        query.setOffset(1L);
        LiveData liveData = this.taggedDocumentLiveDataEntryStore.get(query);

        assertEquals(expected, liveData);
    }
}