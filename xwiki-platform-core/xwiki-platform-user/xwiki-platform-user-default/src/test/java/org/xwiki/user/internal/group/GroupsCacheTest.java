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
package org.xwiki.user.internal.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.internal.group.AbstractGroupCache.GroupCacheEntry;

import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Validate {@link GroupsCache}.
 * 
 * @version $Id$
 */
@ComponentTest
@ReferenceComponentList
public class GroupsCacheTest
{
    @InjectMockComponents
    private GroupsCache groupsCache;

    @MockComponent
    private CacheManager cacheManager;

    private MapCache<GroupCacheEntry> cache = new MapCache<>();

    private final static DocumentReference USER = new DocumentReference("userwiki", "userspace", "userdocument");

    private final static DocumentReference GROUP1 =
        new DocumentReference("groupwiki1", "groupspace1", "groupdocument1");

    private final static DocumentReference GROUP2 =
        new DocumentReference("groupwiki2", "groupspace2", "groupdocument2");

    private final static List<String> WIKIS = Arrays.asList("wiki1", "wiki2");

    @BeforeComponent
    public void beforeComponent() throws CacheException
    {
        when(this.cacheManager.<GroupCacheEntry>createNewCache(any())).thenReturn(this.cache);
    }

    private void fillCache()
    {
        GroupCacheEntry entry = getCacheEntry(true);

        List<DocumentReference> all = Arrays.asList(GROUP1, GROUP2);
        List<DocumentReference> direct = Arrays.asList(GROUP1);

        entry.setAll(all);
        entry.setDirect(direct);
    }

    private GroupCacheEntry getCacheEntry(boolean create)
    {
        return this.groupsCache.getCacheEntry(USER, WIKIS, create);
    }

    // Tests

    @Test
    public void getCacheEntry()
    {
        GroupCacheEntry entry = getCacheEntry(true);

        List<DocumentReference> all = Arrays.asList(GROUP1, GROUP2);
        List<DocumentReference> direct = Arrays.asList(GROUP1);

        entry.setAll(all);

        entry = getCacheEntry(true);

        assertEquals(all, new ArrayList<>(entry.getAll()));

        entry.setDirect(direct);

        entry = getCacheEntry(true);

        assertEquals(direct, new ArrayList<>(entry.getDirect()));
    }

    @Test
    public void cleanCacheReference()
    {
        // Clean user

        fillCache();

        assertNotNull(getCacheEntry(false));

        this.groupsCache.cleanCache(USER);

        assertNull(getCacheEntry(false));

        // Clean direct group

        fillCache();

        assertNotNull(getCacheEntry(false));

        this.groupsCache.cleanCache(GROUP1);

        assertNull(getCacheEntry(false));

        // Clean all group

        fillCache();

        assertNotNull(getCacheEntry(false));

        this.groupsCache.cleanCache(GROUP2);

        assertNull(getCacheEntry(false));
    }

    @Test
    public void cleanCacheWiki()
    {
        // Clean user wiki

        fillCache();

        assertNotNull(getCacheEntry(false));

        this.groupsCache.cleanCache(USER.getWikiReference().getName());

        assertNull(getCacheEntry(false));

        // Clean user wiki

        fillCache();

        assertNotNull(getCacheEntry(false));

        this.groupsCache.cleanCache(GROUP1.getWikiReference().getName());

        assertNull(getCacheEntry(false));

        // Clean user wiki

        fillCache();

        assertNotNull(getCacheEntry(false));

        this.groupsCache.cleanCache(GROUP2.getWikiReference().getName());

        assertNull(getCacheEntry(false));

        // Clean other wiki

        fillCache();

        assertNotNull(getCacheEntry(false));

        this.groupsCache.cleanCache("otherwiki");

        assertNotNull(getCacheEntry(false));
    }
}
