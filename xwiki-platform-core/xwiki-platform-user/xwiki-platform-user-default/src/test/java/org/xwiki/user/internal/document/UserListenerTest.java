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
package org.xwiki.user.internal.document;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Validate {@link UserListener}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList(UserCache.class)
class UserListenerTest
{
    private static final WikiReference WIKI = new WikiReference("wiki");

    @InjectMockComponents
    private UserListener listener;

    @InjectMockComponents
    private UserCache userCache;

    @MockComponent
    private CacheManager cacheManager;

    private MapCache<Boolean> cache = new MapCache<>();

    @BeforeComponent
    public void beforeComponent() throws Exception
    {
        when(this.cacheManager.<Boolean>createNewCache(any())).thenReturn(this.cache);
    }

    @Test
    void userDeleted()
    {
        assertNull(this.cache.get(WIKI.getName()));

        this.userCache.computeIfAbsent(WIKI, w -> true);

        assertTrue(this.cache.get(WIKI.getName()));

        this.listener.onEvent(new XObjectDeletedEvent(),
            new XWikiDocument(new DocumentReference("wiki2", "space", "page")), null);

        assertTrue(this.cache.get(WIKI.getName()));

        this.listener.onEvent(new XObjectDeletedEvent(),
            new XWikiDocument(new DocumentReference("wiki", "space", "page")), null);

        assertNull(this.cache.get(WIKI.getName()));
    }

    @Test
    void userAdded()
    {
        assertNull(this.cache.get(WIKI.getName()));

        this.listener.onEvent(new XObjectAddedEvent(),
            new XWikiDocument(new DocumentReference("wiki2", "space", "page")), null);

        assertNull(this.cache.get(WIKI.getName()));

        this.listener.onEvent(new XObjectAddedEvent(),
            new XWikiDocument(new DocumentReference("wiki", "space", "page")), null);

        assertTrue(this.cache.get(WIKI.getName()));
    }

    @Test
    void wikiDeleted()
    {
        this.userCache.computeIfAbsent(WIKI, w -> true);

        assertTrue(this.cache.get(WIKI.getName()));

        this.listener.onEvent(new WikiDeletedEvent(WIKI.getName()), null, null);

        assertNull(this.cache.get(WIKI.getName()));
    }
}
