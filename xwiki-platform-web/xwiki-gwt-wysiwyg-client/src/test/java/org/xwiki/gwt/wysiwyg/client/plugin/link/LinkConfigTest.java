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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;

/**
 * Unit tests for the {@link LinkConfig} class.
 * 
 * @version $Id$
 */
public class LinkConfigTest extends WysiwygTestCase
{
    /**
     * The object used to create a {@link LinkConfig} from JSON.
     */
    private final LinkConfigJSONParser linkConfigJSONParser = new LinkConfigJSONParser();

    /**
     * The object used to serialize a {@link LinkConfig} instance to JSON.
     */
    private final LinkConfigJSONSerializer linkConfigJSONSerializer = new LinkConfigJSONSerializer();

    /**
     * Test for the parsing of a basic {@link LinkConfig} from a JSON String.
     */
    public void testParseSimpleJSON()
    {
        String jsonString =
            "{reference:'xwiki:Main.WebHome', url:'/xwiki/bin/view/Main/WebHome', label:'<em>foo</em>', "
                + "labelText:'foo', type:'WIKIPAGE'}";
        LinkConfig linkConfig = linkConfigJSONParser.parse(jsonString);

        assertEquals("xwiki:Main.WebHome", linkConfig.getReference());
        assertEquals("/xwiki/bin/view/Main/WebHome", linkConfig.getUrl());
        assertEquals("<em>foo</em>", linkConfig.getLabel());
        assertEquals("foo", linkConfig.getLabelText());
        assertEquals(LinkType.WIKIPAGE, linkConfig.getType());
        assertNull(linkConfig.getTooltip());
        assertFalse(linkConfig.isReadOnlyLabel());
    }

    /**
     * Test for the serialization of a basic {@link LinkConfig} to a JSON String.
     */
    public void testSerializeSimpleJSON()
    {
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.setType(LinkType.NEW_WIKIPAGE);
        linkConfig.setReference("xwiki:Main.AllDocs");
        linkConfig.setUrl("/xwiki/bin/view/Main/AllDocs");
        linkConfig.setLabel("<strong>X</strong>Wiki");
        linkConfig.setLabelText("XWiki");
        String expectedJSON =
            "{reference:'xwiki:Main.About', url:'/xwiki/bin/view/Main/AllDocs', label:'<strong>X</strong>Wiki', "
                + "labelText:'XWiki', type: 'NEW_WIKIPAGE'}";
        assertEquals(expectedJSON, linkConfigJSONSerializer.serialize(linkConfig));
    }

    /**
     * Test for the parsing and serialization of a {@link LinkConfig} with the tooltip set.
     */
    public void testTooltip()
    {
        String jsonString =
            "{reference:'xwiki:Main.Dashboard', url:'/xwiki/bin/view/Main/Dashboard', label:'x<em>y</em>z', "
                + "labelText:'xyz', type:'WIKIPAGE', tooltip:'Tooltip for xyz'}";
        LinkConfig linkConfig = linkConfigJSONParser.parse(jsonString);

        assertEquals("Tooltip for xyz", linkConfig.getTooltip());

        linkConfig = new LinkConfig();
        linkConfig.setType(LinkType.NEW_WIKIPAGE);
        linkConfig.setReference("xwiki:Main.SpaceIndex");
        linkConfig.setUrl("/xwiki/bin/view/Main/SpaceIndex");
        linkConfig.setLabel("<del>abc</del>");
        linkConfig.setLabelText("abc");
        linkConfig.setTooltip("abc rocks!");
        String expectedJSON =
            "{reference:'xwiki:Main.SpaceIndex', url:'/xwiki/bin/view/Main/SpaceIndex', label:'<del>abc</del>', "
                + "labelText:'abc', type:'NEW_WIKIPAGE', tooltip:'abc rocks!'}";
        assertEquals(expectedJSON, linkConfigJSONSerializer.serialize(linkConfig));
    }

    /**
     * Test for the parsing and serialization of a {@link LinkConfig} with the parameter set to open in a new window.
     */
    public void testNewWindowParameter()
    {
        String jsonString =
            "{reference:'xwiki:Main.Test', url:'/xwiki/bin/view/Main/Test', label:'test', "
                + "labelText:'test', type:'WIKIPAGE', openInNewWindow:'true'}";
        LinkConfig linkConfig = linkConfigJSONParser.parse(jsonString);
        assertTrue(linkConfig.isOpenInNewWindow());

        jsonString =
            "{reference:'xwiki:Main.WebPreferences', url:'/xwiki/bin/view/Main/WebPreferences', label:'prefs', "
                + "labelText:'prefs', type:'WIKIPAGE', openInNewWindow:'xyz'}";
        linkConfig = linkConfigJSONParser.parse(jsonString);
        assertFalse(linkConfig.isOpenInNewWindow());

        linkConfig = new LinkConfig();
        linkConfig.setType(LinkType.NEW_WIKIPAGE);
        linkConfig.setReference("xwiki:Main.X");
        linkConfig.setUrl("/xwiki/bin/view/Main/X");
        linkConfig.setLabel("<em>X</em> page");
        linkConfig.setLabelText("X page");
        linkConfig.setOpenInNewWindow(true);
        String expectedJSON =
            "{reference:'xwiki:Main.X', url:'/xwiki/bin/view/Main/X', label:'<em>X</em> page', "
                + "labelText:'X page', type:'NEW_WIKIPAGE', openInNewWindow:'true'}";
        assertEquals(expectedJSON, linkConfigJSONSerializer.serialize(linkConfig));
    }

    /**
     * Test for the serialization and deserialization of a {@link LinkConfig}.
     */
    public void testSerializationRoundTrip()
    {
        LinkConfig expected = new LinkConfig();
        expected.setType(LinkType.NEW_WIKIPAGE);
        expected.setReference("xwiki:Main.About");
        expected.setUrl("/xwiki/bin/view/Main/About");
        expected.setLabel("blah blah");
        expected.setReadOnlyLabel(true);
        expected.setTooltip("xwiki \"rox\"");

        LinkConfig actual = linkConfigJSONParser.parse(linkConfigJSONSerializer.serialize(expected));

        assertEquals(expected.getReference(), actual.getReference());
        assertEquals(expected.getUrl(), actual.getUrl());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getLabel(), actual.getLabel());
        assertEquals(expected.getLabelText(), actual.getLabelText());
        assertNull(actual.getLabelText());
        assertEquals(expected.isReadOnlyLabel(), actual.isReadOnlyLabel());
        assertEquals(expected.getTooltip(), actual.getTooltip());
        assertEquals(expected.isOpenInNewWindow(), actual.isOpenInNewWindow());
        assertFalse(actual.isOpenInNewWindow());
    }

    /**
     * Tests that quotes are escaped correctly in the JSON serialization.
     */
    public void testQuoteIsEscapedCorrecly()
    {
        LinkConfig expected = new LinkConfig();
        expected.setType(LinkType.NEW_WIKIPAGE);
        expected.setReference("xwiki:Main.XWiki'sPowers");
        expected.setUrl("/xwiki/bin/view/Main/XWiki'sPowers");
        expected.setLabel("xwiki <strong>rox</strong>");
        expected.setReadOnlyLabel(true);
        expected.setTooltip("xwiki's the wiki that rox");

        LinkConfig actual = linkConfigJSONParser.parse(linkConfigJSONSerializer.serialize(expected));

        assertEquals(expected.getReference(), actual.getReference());
        assertEquals(expected.getUrl(), actual.getUrl());
        assertEquals(expected.getTooltip(), actual.getTooltip());
    }
}
