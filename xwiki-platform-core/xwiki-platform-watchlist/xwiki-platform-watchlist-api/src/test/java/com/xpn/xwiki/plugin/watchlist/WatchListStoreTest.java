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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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
    public void setUpUserWithWatchList() throws Exception
    {
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

    // Removed deprecated escaping tests from when the watched entities were stored in a string instead of a list.
    // TODO: Add relevant tests.
}
