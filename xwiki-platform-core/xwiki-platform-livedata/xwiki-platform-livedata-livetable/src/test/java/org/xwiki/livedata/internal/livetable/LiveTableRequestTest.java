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
package org.xwiki.livedata.internal.livetable;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link LiveTableRequest}.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
class LiveTableRequestTest
{
    private XWikiRequest xwikiRequest = mock(XWikiRequest.class);

    private Map<String, String[]> parameters = new HashMap<>();

    private LiveTableRequest liveTableRequest = new LiveTableRequest(this.xwikiRequest, this.parameters);

    @Test
    void getParameter()
    {
        this.parameters.put("alice", new String[] {"one", "two"});
        this.parameters.put("bob", new String[] {});

        assertNull(this.liveTableRequest.getParameter("carol"));
        assertNull(this.liveTableRequest.get("carol"));

        assertEquals("one", this.liveTableRequest.getParameter("alice"));
        assertEquals("one", this.liveTableRequest.get("alice"));

        try {
            this.liveTableRequest.getParameter("bob");
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            // This is expected, because the parameters map should either don't have an entry or have an entry with at
            // least one value.
        }
    }
}
