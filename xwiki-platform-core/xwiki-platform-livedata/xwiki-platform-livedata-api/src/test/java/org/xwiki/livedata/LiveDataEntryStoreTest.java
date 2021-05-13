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
package org.xwiki.livedata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link LiveDataEntryStore}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
class LiveDataEntryStoreTest
{
    @Test
    void getEmpty() throws Exception
    {
        LiveDataEntryStore liveDataEntryStore = initLiveDataEntryStore(Optional.empty());
        Optional<Object> propertyValue = liveDataEntryStore.get("entryIdTest", "propertyTest");
        assertFalse(propertyValue.isPresent());
    }

    @Test
    void getMissingProperty() throws Exception
    {
        Map<String, Object> entry = new HashMap<>();
        entry.put("key1", 1);
        entry.put("key2", 2);
        LiveDataEntryStore liveDataEntryStore = initLiveDataEntryStore(Optional.of(entry));
        Optional<Object> propertyValue = liveDataEntryStore.get("entryIdTest", "propertyTest");
        assertFalse(propertyValue.isPresent());
    }

    @Test
    void get() throws Exception
    {
        Map<String, Object> entry = new HashMap<>();
        entry.put("key1", 1);
        entry.put("propertyTest", 2);
        LiveDataEntryStore liveDataEntryStore = initLiveDataEntryStore(Optional.of(entry));
        Optional<Object> propertyValue = liveDataEntryStore.get("entryIdTest", "propertyTest");
        assertTrue(propertyValue.isPresent());
        assertEquals(2, propertyValue.get());
    }

    @Test
    void updatePreviousValueNull() throws Exception
    {
        LiveDataEntryStore liveDataEntryStore = new LiveDataEntryStore()
        {
            @Override
            public Optional<Map<String, Object>> get(Object entryId)
            {
                return Optional.of(new HashMap<>());
            }

            @Override
            public LiveData get(LiveDataQuery query)
            {
                return null;
            }

            @Override
            public Optional<Object> save(Map<String, Object> entry)
            {
                return Optional.of("identifierTest");
            }
        };

        Optional<Object> previousValue = liveDataEntryStore.update("entryIdTest", "propertyTest", 1);
        assertFalse(previousValue.isPresent());
    }

    @Test
    void updateSaveEmpty() throws Exception
    {
        LiveDataEntryStore liveDataEntryStore = new LiveDataEntryStore()
        {
            @Override
            public Optional<Map<String, Object>> get(Object entryId)
            {
                Map<String, Object> entry = new HashMap<>();
                entry.put("propertyTest", "oldValue");
                return Optional.of(entry);
            }

            @Override
            public LiveData get(LiveDataQuery query)
            {
                return null;
            }

            @Override
            public Optional<Object> save(Map<String, Object> entry)
            {
                return Optional.empty();
            }
        };

        Optional<Object> previousValue = liveDataEntryStore.update("entryIdTest", "propertyTest", 1);
        assertFalse(previousValue.isPresent());
    }

    @Test
    void update() throws Exception
    {
        LiveDataEntryStore liveDataEntryStore = new LiveDataEntryStore()
        {
            @Override
            public Optional<Map<String, Object>> get(Object entryId)
            {
                Map<String, Object> entry = new HashMap<>();
                entry.put("propertyTest", "oldValue");
                return Optional.of(entry);
            }

            @Override
            public LiveData get(LiveDataQuery query)
            {
                return null;
            }

            @Override
            public Optional<Object> save(Map<String, Object> entry)
            {
                return Optional.of("entryIdTest");
            }
        };

        Optional<Object> previousValue = liveDataEntryStore.update("entryIdTest", "propertyTest", 1);
        assertTrue(previousValue.isPresent());
        assertEquals("oldValue", previousValue.get());
    }

    private LiveDataEntryStore initLiveDataEntryStore(Optional<Map<String, Object>> entry)
    {
        return new LiveDataEntryStore()
        {
            @Override
            public Optional<Map<String, Object>> get(Object entryId)
            {

                return entry;
            }

            @Override
            public LiveData get(LiveDataQuery query)
            {
                return null;
            }
        };
    }
}
