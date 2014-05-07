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
package com.xpn.xwiki.plugin.watchlist;

import java.util.List;

import org.junit.*;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiMessageTool;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.watchlist.WatchListStore}.
 *
 * @version $Id$
 * @since 5.3M1
 */
public class WatchListStoreTest
{
    private static final String TEST_USERDOC_NAME = "userspace.userpage";

    // common test objects: the watchlist, its user and a context to fetch the user from
    private BaseObject watchListObject;
    private XWikiDocument userDocument;
    private XWikiContext xcontext;
    
    @Before
    public void setUpUserWithWatchList() throws Exception {
        watchListObject = mock(BaseObject.class);

        userDocument = mock(XWikiDocument.class);
        when(userDocument.isNew()).thenReturn(false);
        when(userDocument.getObject("XWiki.XWikiUsers")).thenReturn(mock(BaseObject.class));
        when(userDocument.getObject("XWiki.WatchListClass")).thenReturn(watchListObject);

        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xwiki.getDocument(eq(TEST_USERDOC_NAME), any(XWikiContext.class))).thenReturn(userDocument);
 
        xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getWikiId()).thenReturn("wiki");

    }
    
    @Test
    public void addWatchedElementWhenCommaInElement() throws Exception
    {
        when(watchListObject.getLargeStringValue("documents")).thenReturn("one");

        when(xcontext.getMessageTool()).thenReturn(mock(XWikiMessageTool.class));

        WatchListStore store = new WatchListStore();
        store.addWatchedElement(TEST_USERDOC_NAME, "space.element,with,comma", WatchListStore.ElementType.DOCUMENT,
            xcontext);

        // Test is here, we verify the new watched element is added properly with commas escaped.
        verify(userDocument).setLargeStringValue("XWiki.WatchListClass", "documents",
            "one,wiki:space.element\\,with\\,comma");

        // Test that the element is still in the list after another one is added
        when(watchListObject.getLargeStringValue("documents")).thenReturn("one,wiki:space.element\\,with\\,comma");
        store.addWatchedElement(TEST_USERDOC_NAME, "space.anotherPage", WatchListStore.ElementType.DOCUMENT,
                xcontext);
        verify(userDocument).setLargeStringValue("XWiki.WatchListClass", "documents",
                "one,wiki:space.element\\,with\\,comma,wiki:space.anotherPage");
    }
    
    @Test
    public void getWatchedElementsWhenCommasInElements() throws Exception
    {
        when(watchListObject.getLargeStringValue("documents")).thenReturn("space.element\\,with\\,comma,other\\,comma");

        WatchListStore store = new WatchListStore();
        List<String> elements =
            store.getWatchedElements(TEST_USERDOC_NAME, WatchListStore.ElementType.DOCUMENT, xcontext);

        assertEquals(2, elements.size());
        assertEquals("space.element,with,comma", elements.get(0));
        assertEquals("other,comma", elements.get(1));
    }
    
    @Test
    public void watchedElementsCanStartWithEmptyElement() throws Exception 
    {
        final String testDocRef = "wiki:some.other\\.doc";
        when(watchListObject.getLargeStringValue("documents")).thenReturn(","+testDocRef);

        WatchListStore store = new WatchListStore();
        List<String> elements =
            store.getWatchedElements(TEST_USERDOC_NAME, WatchListStore.ElementType.DOCUMENT, xcontext);
        assertEquals(1, elements.size());
        assertEquals(testDocRef, elements.get(0));
    }
}
