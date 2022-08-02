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
package org.xwiki.rest;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link XWikiResource}.
 * 
 * @version $Id$
 */
@ComponentTest
class XWikiResourceTest
{
    @InjectMockComponents
    private XWikiResource xwikiResource;

    @Test
    void parseSpaceSegments() throws Exception
    {
        try {
            this.xwikiResource.parseSpaceSegments("");
            fail();
        } catch (XWikiRestException e) {
            assertEquals("Malformed URL: a space name cannot be empty.", e.getMessage());
        }

        try {
            this.xwikiResource.parseSpaceSegments("/one");
            fail();
        } catch (XWikiRestException e) {
            assertEquals("Malformed URL: a space name cannot be empty.", e.getMessage());
        }

        try {
            this.xwikiResource.parseSpaceSegments("/spaces");
            fail();
        } catch (XWikiRestException e) {
            assertEquals("Malformed URL: a space name cannot be empty.", e.getMessage());
        }

        try {
            this.xwikiResource.parseSpaceSegments("one/two");
            fail();
        } catch (XWikiRestException e) {
            assertEquals("Malformed URL: the spaces section is invalid.", e.getMessage());
        }

        assertEquals(Arrays.asList("one"), this.xwikiResource.parseSpaceSegments("one/"));
        assertEquals(Arrays.asList("one", "two"), this.xwikiResource.parseSpaceSegments("one/spaces/two"));
        assertEquals(Arrays.asList("spaces"), this.xwikiResource.parseSpaceSegments("spaces"));

        assertEquals(Arrays.asList("on e", "tw/o"), this.xwikiResource.parseSpaceSegments("on%20e/spaces/tw%2Fo"));

        try {
            this.xwikiResource.parseSpaceSegments("one%2");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("URLDecoder: Incomplete trailing escape (%) pattern", e.getMessage());
        }
    }
}
