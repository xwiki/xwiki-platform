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
package org.xwiki.watchlist.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.watchlist.internal.api.WatchListNotifier;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Unit tests for {@link org.xwiki.watchlist.internal.DefaultWatchListNotifier}.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultWatchListNotifierTest
{
    private XWikiContext xcontext;

    private XWiki xwiki;

    private BaseObject userObject;

    @Rule
    public MockitoComponentMockingRule<WatchListNotifier> mocker = new MockitoComponentMockingRule<WatchListNotifier>(
        DefaultWatchListNotifier.class);

    @Before
    public void setUp() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);

        Provider<XWikiContext> contextProvider =
            mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        when(contextProvider.get()).thenReturn(this.xcontext);

        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        XWikiDocument subscriberDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument("subscriber", this.xcontext)).thenReturn(subscriberDocument);

        this.userObject = mock(BaseObject.class);
        when(subscriberDocument.getObject("XWiki.XWikiUsers")).thenReturn(this.userObject);
    }

    @Test
    public void sendEmailNotificationDoNothingIfEmailAddressDoesntContainAtSymbol() throws Exception
    {
        when(this.userObject.getStringValue(DefaultWatchListNotifier.XWIKI_USER_CLASS_EMAIL_PROP)).thenReturn(
            "invalidemail");

        mocker.getComponentUnderTest().sendNotification("subscriber", Collections.EMPTY_LIST, "emailtemplate",
            new Date());

        verify(this.xwiki, never()).getPlugin("mailsender", this.xcontext);
    }
}
