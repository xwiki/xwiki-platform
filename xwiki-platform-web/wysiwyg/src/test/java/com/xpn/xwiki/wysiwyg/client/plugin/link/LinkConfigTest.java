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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig.LinkType;

/**
 * Unit tests for the {@link LinkConfig} class.
 * 
 * @version $Id$
 */
public class LinkConfigTest extends AbstractWysiwygClientTest
{
    /**
     * Test for the parsing of a basic {@link LinkConfig} from a JSON String.
     */
    public void testParseSimpleJSON()
    {
        String jsonString =
            "{ reference: 'xwiki:Main.WebHome', url: '/xwiki/bin/view/Main/WebHome', label: 'foo', "
                + "labeltext: 'foo', type: 'WIKIPAGE' }";
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.fromJSON(jsonString);

        assertEquals("xwiki:Main.WebHome", linkConfig.getReference());
        assertEquals("/xwiki/bin/view/Main/WebHome", linkConfig.getUrl());
        assertEquals("foo", linkConfig.getLabel());
        assertEquals("foo", linkConfig.getLabelText());
        assertEquals(LinkType.WIKIPAGE, linkConfig.getType());
        assertNull(linkConfig.getTooltip());
        assertFalse(linkConfig.isReadOnlyLabel());
        // assert wiki, space and page are not set
        assertNull(linkConfig.getWiki());
        assertNull(linkConfig.getSpace());
        assertNull(linkConfig.getPage());
    }

    /**
     * Test for the serialization of a basic {@link LinkConfig} to a JSON String.
     */
    public void testSerializeSimpleJSON()
    {
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.setType(LinkType.NEW_WIKIPAGE);
        linkConfig.setReference("xwiki:Main.About");
        linkConfig.setUrl("/xwiki/bin/view/Main/About");
        // set some parameters that we don't expect serialized
        linkConfig.setWiki("xwiki");
        linkConfig.setSpace("Main");
        linkConfig.setPage("WebHome");
        linkConfig.setLabel("xwiki <strong>rox</strong>");
        linkConfig.setLabelText("xwiki rox");
        linkConfig.setReadOnlyLabel(false);
        String expectedJSON =
            "{ reference: 'xwiki:Main.About', url: '/xwiki/bin/view/Main/About', label: 'xwiki <strong>rox</strong>', "
                + "labeltext: 'xwiki rox', type: 'NEW_WIKIPAGE' }";
        assertEquals(expectedJSON, linkConfig.toJSON());
    }

    /**
     * Test for the parsing and serialization of a {@link LinkConfig} with the tooltip parameter set.
     */
    public void testTooltipParameter()
    {
        String jsonString =
            "{ reference: 'xwiki:Main.WebHome', url: '/xwiki/bin/view/Main/WebHome', label: 'foo', "
                + "labeltext: 'foo', type: 'WIKIPAGE', _xtitle: 'This is a foo' }";
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.fromJSON(jsonString);

        assertEquals("This is a foo", linkConfig.getTooltip());

        linkConfig = new LinkConfig();
        linkConfig.setType(LinkType.NEW_WIKIPAGE);
        linkConfig.setReference("xwiki:Main.About");
        linkConfig.setUrl("/xwiki/bin/view/Main/About");
        linkConfig.setLabel("xwiki <strong>rox</strong>");
        linkConfig.setLabelText("xwiki rox");
        linkConfig.setReadOnlyLabel(false);
        linkConfig.setTooltip("It really rocks");
        String expectedJSON =
            "{ reference: 'xwiki:Main.About', url: '/xwiki/bin/view/Main/About', label: 'xwiki <strong>rox</strong>', "
                + "labeltext: 'xwiki rox', type: 'NEW_WIKIPAGE', _xtitle: 'It really rocks' }";
        assertEquals(expectedJSON, linkConfig.toJSON());
    }

    /**
     * Test for the parsing and serialization of a {@link LinkConfig} with the parameter set to open in a new window.
     */
    public void testNewWindowParameter()
    {
        String jsonString =
            "{ reference: 'xwiki:Main.WebHome', url: '/xwiki/bin/view/Main/WebHome', label: 'foo', "
                + "labeltext: 'foo', type: 'WIKIPAGE', _xrel: '__blank' }";
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.fromJSON(jsonString);

        assertTrue(linkConfig.isOpenInNewWindow());

        jsonString =
            "{ reference: 'xwiki:Main.WebHome', url: '/xwiki/bin/view/Main/WebHome', label: 'foo', "
                + "labeltext: 'foo', type: 'WIKIPAGE', _xrel: 'else' }";
        linkConfig = new LinkConfig();
        linkConfig.fromJSON(jsonString);

        assertFalse(linkConfig.isOpenInNewWindow());

        linkConfig = new LinkConfig();
        linkConfig.setType(LinkType.NEW_WIKIPAGE);
        linkConfig.setReference("xwiki:Main.About");
        linkConfig.setUrl("/xwiki/bin/view/Main/About");
        linkConfig.setLabel("xwiki <strong>rox</strong>");
        linkConfig.setLabelText("xwiki rox");
        linkConfig.setReadOnlyLabel(false);
        linkConfig.setOpenInNewWindow(true);
        String expectedJSON =
            "{ reference: 'xwiki:Main.About', url: '/xwiki/bin/view/Main/About', label: 'xwiki <strong>rox</strong>', "
                + "labeltext: 'xwiki rox', type: 'NEW_WIKIPAGE', _xrel: '__blank' }";
        assertEquals(expectedJSON, linkConfig.toJSON());
    }

    /**
     * Test for the parsing and serialization of a {@link LinkConfig} with some custom parameters set.
     */
    public void testCustomParameters()
    {
        String jsonString =
            "{ reference: 'xwiki:Main.WebHome', url: '/xwiki/bin/view/Main/WebHome', label: 'foo', "
                + "labeltext: 'foo', type: 'WIKIPAGE', _xrel: '__blank', _xstyle: 'color: red;', _xcustom: 'foobar' }";
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.fromJSON(jsonString);

        assertEquals("foobar", linkConfig.getParameter("custom"));
        assertEquals("color: red;", linkConfig.getParameter("style"));

        linkConfig = new LinkConfig();
        linkConfig.setType(LinkType.NEW_WIKIPAGE);
        linkConfig.setReference("xwiki:Main.About");
        linkConfig.setUrl("/xwiki/bin/view/Main/About");
        linkConfig.setLabel("xwiki <strong>rox</strong>");
        linkConfig.setLabelText("xwiki rox");
        linkConfig.setReadOnlyLabel(false);
        linkConfig.setParameter("custom", "xwiki");
        linkConfig.setParameter("align", "center");
        String expectedJSON =
            "{ reference: 'xwiki:Main.About', url: '/xwiki/bin/view/Main/About', label: 'xwiki <strong>rox</strong>', "
                + "labeltext: 'xwiki rox', type: 'NEW_WIKIPAGE', _xcustom: 'xwiki', _xalign: 'center' }";
        assertEquals(expectedJSON, linkConfig.toJSON());
    }

    /**
     * Test for the parsing and serialization of a {@link LinkConfig} with the class custom parameters set.
     */
    public void testClassCustomParameter()
    {
        String jsonString =
            "{ reference: 'xwiki:Main.WebHome', url: '/xwiki/bin/view/Main/WebHome', label: 'foo', "
                + "labeltext: 'foo', type: 'WIKIPAGE', _xrel: '__blank', _xclass: 'foobar' }";
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.fromJSON(jsonString);

        assertEquals("foobar", linkConfig.getParameter("class"));

        linkConfig = new LinkConfig();
        linkConfig.setType(LinkType.NEW_WIKIPAGE);
        linkConfig.setReference("xwiki:Main.About");
        linkConfig.setUrl("/xwiki/bin/view/Main/About");
        linkConfig.setLabel("xwiki <strong>rox</strong>");
        linkConfig.setLabelText("xwiki rox");
        linkConfig.setReadOnlyLabel(false);
        linkConfig.setParameter("class", "foobar");
        String expectedJSON =
            "{ reference: 'xwiki:Main.About', url: '/xwiki/bin/view/Main/About', label: 'xwiki <strong>rox</strong>', "
                + "labeltext: 'xwiki rox', type: 'NEW_WIKIPAGE', _xclass: 'foobar' }";
        assertEquals(expectedJSON, linkConfig.toJSON());
    }

    /**
     * Test for the serialization and deserialization of a {@link LinkConfig}.
     */
    public void testSerializationRoundTrip()
    {
        LinkConfig before = new LinkConfig();
        before.setType(LinkType.NEW_WIKIPAGE);
        before.setReference("xwiki:Main.About");
        before.setUrl("/xwiki/bin/view/Main/About");
        before.setLabel("xwiki <strong>rox</strong>");
        before.setReadOnlyLabel(true);
        before.setTooltip("xwiki \"rox\"");
        before.setParameter("style", "color: pink;");

        String serialization = before.toJSON();

        LinkConfig after = new LinkConfig();
        after.fromJSON(serialization);

        assertEquals(before.getReference(), after.getReference());
        assertEquals(before.getUrl(), after.getUrl());
        assertEquals(before.getWiki(), after.getWiki());
        assertNull(after.getWiki());
        assertEquals(before.getSpace(), after.getSpace());
        assertNull(after.getSpace());
        assertEquals(before.getPage(), after.getPage());
        assertNull(after.getPage());
        assertEquals(before.getType(), after.getType());
        assertEquals(before.getLabel(), after.getLabel());
        assertEquals(before.getLabelText(), after.getLabelText());
        assertNull(after.getLabelText());
        assertEquals(before.isReadOnlyLabel(), after.isReadOnlyLabel());
        assertEquals(before.getTooltip(), after.getTooltip());
        assertEquals("xwiki \"rox\"", after.getTooltip());
        assertEquals(before.isOpenInNewWindow(), after.isOpenInNewWindow());
        assertFalse(after.isOpenInNewWindow());
        assertEquals(before.getParameter("style"), after.getParameter("style"));
        assertEquals("color: pink;", after.getParameter("style"));
    }

    public void testQuoteIsEscapedCorrecly()
    {
        LinkConfig before = new LinkConfig();
        before.setType(LinkType.NEW_WIKIPAGE);
        before.setReference("xwiki:Main.XWiki'sPowers");
        before.setUrl("/xwiki/bin/view/Main/XWiki'sPowers");
        before.setLabel("xwiki <strong>rox</strong>");
        before.setReadOnlyLabel(true);
        before.setTooltip("xwiki's the wiki that rox");

        String serialization = before.toJSON();

        LinkConfig after = new LinkConfig();
        after.fromJSON(serialization);

        assertEquals(before.getReference(), after.getReference());
        assertEquals("xwiki:Main.XWiki'sPowers", after.getReference());
        assertEquals(before.getUrl(), after.getUrl());
        assertEquals("/xwiki/bin/view/Main/XWiki'sPowers", after.getUrl());
        assertEquals(before.getTooltip(), after.getTooltip());
        assertEquals("xwiki's the wiki that rox", after.getTooltip());
    }
}
