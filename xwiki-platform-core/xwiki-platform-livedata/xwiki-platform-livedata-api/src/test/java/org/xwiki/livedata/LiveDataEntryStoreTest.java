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
 * @since 13.2RC1
 * @since 12.10.6
 */
class LiveDataEntryStoreTest
{
    @Test
    void getEmpty() throws Exception
    {
        Optional<Map<String, Object>> empty = Optional.empty();
        LiveDataEntryStore liveDataEntryStore = initLiveDataEntryStore(empty);
        Optional<Object> o = liveDataEntryStore.get("entryIdTest", "propertyTest");
        assertFalse(o.isPresent());
    }

    @Test
    void getMissingProperty() throws Exception
    {
        Map<String, Object> value = new HashMap<>();
        value.put("key1", 1);
        value.put("key2", 2);
        Optional<Map<String, Object>> value1 = Optional.of(value);
        LiveDataEntryStore liveDataEntryStore = initLiveDataEntryStore(value1);
        Optional<Object> o = liveDataEntryStore.get("entryIdTest", "propertyTest");
        assertFalse(o.isPresent());
    }

    @Test
    void get() throws Exception
    {
        Map<String, Object> value = new HashMap<>();
        value.put("key1", 1);
        value.put("propertyTest", 2);
        Optional<Map<String, Object>> value1 = Optional.of(value);
        LiveDataEntryStore liveDataEntryStore = initLiveDataEntryStore(value1);
        Optional<Object> o = liveDataEntryStore.get("entryIdTest", "propertyTest");
        assertTrue(o.isPresent());
        assertEquals(2, o.get());
    }

    @Test
    void updatePreviousValueNull() throws Exception
    {
        LiveDataEntryStore liveDataEntryStore = new LiveDataEntryStore()
        {
            @Override
            public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
            {
                Map<String, Object> value = new HashMap<>();
                return Optional.of(value);
            }

            @Override
            public LiveData get(LiveDataQuery query) throws LiveDataException
            {
                return null;
            }

            @Override
            public Optional<Object> save(Map<String, Object> entry) throws LiveDataException
            {
                return Optional.of("identifierTest");
            }
        };

        Optional<Object> update = liveDataEntryStore.update("entryIdTest", "propertyTest", 1);
        assertFalse(update.isPresent());
    }

    @Test
    void updateSaveEmpty() throws Exception
    {
        LiveDataEntryStore liveDataEntryStore = new LiveDataEntryStore()
        {
            @Override
            public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
            {
                Map<String, Object> value = new HashMap<>();
                value.put("propertyTest", "oldValue");
                return Optional.of(value);
            }

            @Override
            public LiveData get(LiveDataQuery query) throws LiveDataException
            {
                return null;
            }

            @Override
            public Optional<Object> save(Map<String, Object> entry) throws LiveDataException
            {
                return Optional.empty();
            }
        };

        Optional<Object> update = liveDataEntryStore.update("entryIdTest", "propertyTest", 1);
        assertFalse(update.isPresent());
    }

    @Test
    void update() throws Exception
    {
        LiveDataEntryStore liveDataEntryStore = new LiveDataEntryStore()
        {
            @Override
            public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
            {
                Map<String, Object> value = new HashMap<>();
                value.put("propertyTest", "oldValue");
                return Optional.of(value);
            }

            @Override
            public LiveData get(LiveDataQuery query) throws LiveDataException
            {
                return null;
            }

            @Override
            public Optional<Object> save(Map<String, Object> entry) throws LiveDataException
            {
                return Optional.of("entryIdTest");
            }
        };

        Optional<Object> update = liveDataEntryStore.update("entryIdTest", "propertyTest", 1);
        assertTrue(update.isPresent());
        assertEquals("oldValue", update.get());
    }

    private LiveDataEntryStore initLiveDataEntryStore(Optional<Map<String, Object>> value1)
    {
        return new LiveDataEntryStore()
        {
            @Override
            public Optional<Map<String, Object>> get(Object entryId)
            {

                return value1;
            }

            @Override
            public LiveData get(LiveDataQuery query)
            {
                return null;
            }
        };
    }
}
