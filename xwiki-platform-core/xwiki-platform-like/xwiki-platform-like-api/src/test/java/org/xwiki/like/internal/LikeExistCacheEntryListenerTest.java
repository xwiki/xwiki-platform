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
package org.xwiki.like.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.model.reference.EntityReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link org.xwiki.like.internal.LikeManagerCacheHelper.LikeExistCacheEntryListener}.
 *
 * @version $Id$
 */
public class LikeExistCacheEntryListenerTest
{
    private LikeManagerCacheHelper.LikeExistCacheEntryListener cacheEntryListener;

    class TestCacheEntryEvent<T> implements CacheEntryEvent<T>
    {
        final String key;
        final T entry;

        TestCacheEntryEvent(String key, T entry)
        {
            this.key = key;
            this.entry = entry;
        }

        @Override
        public CacheEntry<T> getEntry()
        {
            return new CacheEntry<T>()
            {
                @Override
                public Cache<T> getCache()
                {
                    return null;
                }

                @Override
                public String getKey()
                {
                    return key;
                }

                @Override
                public T getValue()
                {
                    return entry;
                }
            };
        }

        @Override public Cache<T> getCache()
        {
            return null;
        }
    }

    @BeforeEach
    void setup()
    {
        cacheEntryListener = new LikeManagerCacheHelper.LikeExistCacheEntryListener();
    }

    @Test
    void cacheEntryAdded()
    {
        assertTrue(this.cacheEntryListener.getReferenceKeyMapping().isEmpty());

        EntityReference entityReference1 = mock(EntityReference.class);
        this.cacheEntryListener.cacheEntryAdded(new TestCacheEntryEvent<>("firstKey", Pair.of(entityReference1, true)));

        Map<EntityReference, List<String>> referenceKeyMapping = this.cacheEntryListener.getReferenceKeyMapping();
        assertEquals(1, referenceKeyMapping.size());
        assertTrue(referenceKeyMapping.containsKey(entityReference1));
        assertEquals(Collections.singletonList("firstKey"), referenceKeyMapping.get(entityReference1));

        this.cacheEntryListener.cacheEntryAdded(
            new TestCacheEntryEvent<>("secondKey", Pair.of(entityReference1, false)));
        EntityReference entityReference2 = mock(EntityReference.class);
        this.cacheEntryListener.cacheEntryAdded(
            new TestCacheEntryEvent<>("thirdKey", Pair.of(entityReference2, false)));

        referenceKeyMapping = this.cacheEntryListener.getReferenceKeyMapping();
        assertEquals(2, referenceKeyMapping.size());
        assertTrue(referenceKeyMapping.containsKey(entityReference1));
        assertEquals(Arrays.asList("firstKey", "secondKey"), referenceKeyMapping.get(entityReference1));
        assertTrue(referenceKeyMapping.containsKey(entityReference2));
        assertEquals(Collections.singletonList("thirdKey"), referenceKeyMapping.get(entityReference2));
    }

    @Test
    void cacheEntryRemoved()
    {
        EntityReference entityReference1 = mock(EntityReference.class);
        EntityReference entityReference2 = mock(EntityReference.class);
        this.cacheEntryListener.cacheEntryAdded(new TestCacheEntryEvent<>("firstKey", Pair.of(entityReference1, true)));
        this.cacheEntryListener.cacheEntryAdded(
            new TestCacheEntryEvent<>("secondKey", Pair.of(entityReference1, false)));
        this.cacheEntryListener.cacheEntryAdded(
            new TestCacheEntryEvent<>("thirdKey", Pair.of(entityReference2, false)));

        Map<EntityReference, List<String>> referenceKeyMapping = this.cacheEntryListener.getReferenceKeyMapping();
        assertEquals(2, referenceKeyMapping.size());
        assertTrue(referenceKeyMapping.containsKey(entityReference1));
        assertEquals(Arrays.asList("firstKey", "secondKey"), referenceKeyMapping.get(entityReference1));
        assertTrue(referenceKeyMapping.containsKey(entityReference2));
        assertEquals(Collections.singletonList("thirdKey"), referenceKeyMapping.get(entityReference2));

        this.cacheEntryListener.cacheEntryRemoved(
            new TestCacheEntryEvent<>("secondKey", Pair.of(entityReference1, true)));
        this.cacheEntryListener.cacheEntryRemoved(
            new TestCacheEntryEvent<>("thirdKey", Pair.of(entityReference2, true)));
        this.cacheEntryListener.cacheEntryRemoved(
            new TestCacheEntryEvent<>("otherKey", Pair.of(entityReference1, true)));

        referenceKeyMapping = this.cacheEntryListener.getReferenceKeyMapping();
        assertEquals(1, referenceKeyMapping.size());
        assertTrue(referenceKeyMapping.containsKey(entityReference1));
        assertEquals(Collections.singletonList("firstKey"), referenceKeyMapping.get(entityReference1));
        assertFalse(referenceKeyMapping.containsKey(entityReference2));
    }
}
