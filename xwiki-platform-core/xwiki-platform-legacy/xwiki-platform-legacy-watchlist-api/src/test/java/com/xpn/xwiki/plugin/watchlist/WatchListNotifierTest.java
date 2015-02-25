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

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.watchlist.WatchListNotifier}.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class WatchListNotifierTest
{
    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    private com.xpn.xwiki.api.Object userObject;

    private WatchListNotifier notifier = new WatchListNotifier();

    @Before
    public void setUp() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);
        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        XWikiDocument xwikiDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument("subscriber", this.xcontext)).thenReturn(xwikiDocument);
        Document subscriberDocument = mock(Document.class);
        when(xwikiDocument.newDocument(this.xcontext)).thenReturn(subscriberDocument);
        this.userObject = mock(com.xpn.xwiki.api.Object.class);
        when(subscriberDocument.getObject("XWiki.XWikiUsers")).thenReturn(this.userObject);
    }

    @Test
    public void sendEmailNotificationDoNothingIfEmailAddressDoesntContainAtSymbol() throws Exception
    {
        Property emailProperty = mock(Property.class);
        when(this.userObject.getProperty("email")).thenReturn(emailProperty);
        when(emailProperty.getValue()).thenReturn("invalidemail");

        this.notifier.sendEmailNotification("subscriber", Collections.EMPTY_LIST, "emailtemplate", new Date(),
            this.xcontext);

        verify(this.xwiki, never()).getPlugin("mailsender", this.xcontext);
    }
}
