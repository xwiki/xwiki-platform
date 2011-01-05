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
package org.xwiki.gwt.wysiwyg.client.plugin.style;

import java.util.List;

import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;

/**
 * Unit tests for {@link StyleDescriptorJSONParser}.
 * 
 * @version $Id$
 */
public class StyleDescriptorJSONParserTest extends WysiwygTestCase
{
    /**
     * The object being tested.
     */
    private final StyleDescriptorJSONParser parser = new StyleDescriptorJSONParser();

    /**
     * Unit test for {@link StyleDescriptorJSONParser#parse(String)}.
     */
    public void testParse()
    {
        List<StyleDescriptor> descriptors =
            parser.parse("[{}, {\"name\": \"\"}, {\"name\": \"note\"}, {\"name\": \"todo\", \"label\": \"TODO\"}, "
                + "{\"name\": \"secret\", \"label\": \"Top Secret\", \"inline\": false}]");

        assertNotNull(descriptors);
        assertEquals(3, descriptors.size());

        assertEquals("note", descriptors.get(0).getLabel());
        assertTrue(descriptors.get(0).isInline());

        assertEquals("todo", descriptors.get(1).getName());

        assertEquals("Top Secret", descriptors.get(2).getLabel());
        assertFalse(descriptors.get(2).isInline());
    }
}
