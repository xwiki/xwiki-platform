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
package org.xwiki.whatsnew;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link NewsSourceDescriptor}.
 *
 * @version $Id$
 */
class NewsSourceDescriptorTest
{
    @Test
    void equality()
    {
        NewsSourceDescriptor d1 = new NewsSourceDescriptor("id", "hint", Collections.emptyMap());
        NewsSourceDescriptor d2 = new NewsSourceDescriptor("id", "hint", Collections.emptyMap());
        NewsSourceDescriptor d3 = new NewsSourceDescriptor("id", "otherhint", Collections.emptyMap());
        NewsSourceDescriptor d4 = new NewsSourceDescriptor("otherid", "hint", Collections.emptyMap());
        NewsSourceDescriptor d5 = new NewsSourceDescriptor("id", "hint", Collections.singletonMap("key", "value"));
        NewsSourceDescriptor d6 = new NewsSourceDescriptor("id", "hint", Collections.singletonMap("key", "value"));
        NewsSourceDescriptor d7 = new NewsSourceDescriptor("id", "hint",
            Collections.singletonMap("otherkey", "value"));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", "value");
        parameters.put("otherkey", "othervalue");
        NewsSourceDescriptor d8 = new NewsSourceDescriptor("id", "hint", parameters);

        assertEquals(d1, d2);
        assertNotEquals(d1, d3);
        assertNotEquals(d1, d4);
        assertNotEquals(d1, d5);
        assertEquals(d5, d6);
        assertNotEquals(d5, d7);
        assertNotEquals(d5, d8);
    }

    @Test
    void string()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", "value");
        parameters.put("otherkey", "othervalue");
        NewsSourceDescriptor d = new NewsSourceDescriptor("id", "hint", parameters);

        assertEquals("id = [id], sourceTypeHint = [hint], parameters = [[otherkey] = [othervalue], [key] = [value]]",
            d.toString());
    }
}
