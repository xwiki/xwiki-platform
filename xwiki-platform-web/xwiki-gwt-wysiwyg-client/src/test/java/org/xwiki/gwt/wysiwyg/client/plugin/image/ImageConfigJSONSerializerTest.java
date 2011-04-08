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
package org.xwiki.gwt.wysiwyg.client.plugin.image;

import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;

/**
 * Unit tests for {@link ImageConfigJSONSerializer} and {@link ImageConfigJSONParser}.
 * 
 * @version $Id$
 */
public class ImageConfigJSONSerializerTest extends WysiwygTestCase
{
    /**
     * The object used to parse image configuration from JSON.
     */
    private final ImageConfigJSONParser imageConfigJSONParser = new ImageConfigJSONParser();

    /**
     * The object used to serialize image configuration to JSON.
     */
    private final ImageConfigJSONSerializer imageConfigJSONSerializer = new ImageConfigJSONSerializer();

    /**
     * Tests if {@link ImageConfigJSONParser} and {@link ImageConfigJSONSerializer} are compatible.
     */
    public void testParseAndSerialize()
    {
        String imageJSON = "{reference:\"1\",url:\"2\",width:\"3\",height:\"4\",alttext:\"5\",alignment:\"MIDDLE\"}";
        assertEquals(imageJSON, imageConfigJSONSerializer.serialize(imageConfigJSONParser.parse(imageJSON)));
    }

    /**
     * Checks if {@code null} values are serialized.
     */
    public void testNullValuesAreNotSerialized()
    {
        assertEquals("{}", imageConfigJSONSerializer.serialize(imageConfigJSONParser.parse("{url:null}")));
    }

    /**
     * Tests that strings are properly escaped.
     */
    public void testStringsAreProperlyEscaped()
    {
        ImageConfig before = new ImageConfig();
        before.setAltText("x\ny\\");
        ImageConfig after = imageConfigJSONParser.parse(imageConfigJSONSerializer.serialize(before));
        assertEquals(before.getAltText(), after.getAltText());
    }
}
