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
package org.xwiki.notifications.filters;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test of {@link NotificationFilterPreference}.
 *
 * @version $Id$
 * @since 14.5
 * @since 14.4.1
 * @since 13.10.7
 */
@ComponentTest
class NotificationFilterPreferenceTest
{
    private NotificationFilterPreference notificationFilterPreference;

    @BeforeEach
    void setUp()
    {
        this.notificationFilterPreference = spy(NotificationFilterPreference.class);
    }

    @Test
    void isFromWiki()
    {
        assertFalse(this.notificationFilterPreference.isFromWiki("wiki"));
    }

    @Test
    void isFromWikiWikiDefined()
    {
        when(this.notificationFilterPreference.getWiki()).thenReturn("wikiid");
        assertTrue(this.notificationFilterPreference.isFromWiki("wikiid"));
    }

    @Test
    void isFromWikiUserDefined()
    {
        when(this.notificationFilterPreference.getUser()).thenReturn("wikiid:XWiki.User");
        assertTrue(this.notificationFilterPreference.isFromWiki("wikiid"));
    }

    @Test
    void isFromWikiPageDefined()
    {
        when(this.notificationFilterPreference.getPage()).thenReturn("wikiid:XWiki.Page");
        assertTrue(this.notificationFilterPreference.isFromWiki("wikiid"));
    }

    @Test
    void isFromWikiPageOnlyDefined()
    {
        when(this.notificationFilterPreference.getPageOnly()).thenReturn("wikiid:XWiki.Page");
        assertTrue(this.notificationFilterPreference.isFromWiki("wikiid"));
    }

    @Test
    void getWikIdWikiDefined()
    {
        when(this.notificationFilterPreference.getWiki()).thenReturn("wikiid");
        assertEquals(Optional.of("wikiid"), this.notificationFilterPreference.getWikiId());
    }

    @Test
    void getWikIdUserDefined()
    {
        when(this.notificationFilterPreference.getUser()).thenReturn("wikiid:XWiki.User");
        assertEquals(Optional.of("wikiid"), this.notificationFilterPreference.getWikiId());
    }

    @Test
    void getWikIdPageDefined()
    {
        when(this.notificationFilterPreference.getPage()).thenReturn("wikiid:XWiki.Page");
        assertEquals(Optional.of("wikiid"), this.notificationFilterPreference.getWikiId());
    }

    @Test
    void getWikIdPageOnlyDefined()
    {
        when(this.notificationFilterPreference.getPageOnly()).thenReturn("wikiid:XWiki.Page");
        assertEquals(Optional.of("wikiid"), this.notificationFilterPreference.getWikiId());
    }

    @Test
    void getWikIdUnknownWikiId()
    {
        assertEquals(Optional.empty(), this.notificationFilterPreference.getWikiId());
    }
}
