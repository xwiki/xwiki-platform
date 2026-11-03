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
package org.xwiki.rest.jersey.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.rest.model.jaxb.Page;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RestletJacksonContextResolver}.
 *
 * @version $Id$
 */
class RestletJacksonContextResolverTest
{
    private final ObjectMapper objectMapper = new RestletJacksonContextResolver().getContext(Page.class);

    @Test
    void readPageWithoutHidden() throws Exception
    {
        Page page = this.objectMapper.readValue("{\"content\":\"content\"}", Page.class);

        assertEquals("content", page.getContent());
        assertNull(page.isHiddenValue());
        assertFalse(page.isHidden());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void readPageWithHidden(boolean hidden) throws Exception
    {
        Page page = this.objectMapper.readValue("{\"hidden\":" + hidden + "}", Page.class);

        assertEquals(hidden, page.isHiddenValue());
        assertEquals(hidden, page.isHidden());
    }

    @Test
    void writePageWithHidden()
    {
        JsonNode json = this.objectMapper.valueToTree(new Page().withHidden(true));

        assertTrue(json.get("hidden").booleanValue());
        assertFalse(json.has("hiddenValue"));
    }
}
