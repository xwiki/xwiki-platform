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
package com.xpn.xwiki.content.parsers;

import junit.framework.TestCase;
import com.xpn.xwiki.content.Link;

/**
 * Unit tests for {@link com.xpn.xwiki.content.parsers.LinkParser}.
 *
 * @version $Id$
 */
public class LinkParserTest extends TestCase
{
    public void testParseAliasWhenOnlyReferenceIsSpecified()
    {
        LinkParser parser = new LinkParser();
        Link link = new Link();
        StringBuffer sb = new StringBuffer("reference");
        parser.parseAlias(sb, link);

        assertNull(link.getAlias());
        assertFalse(link.isUsingPipeDelimiter());
        assertEquals("reference", sb.toString());
    }

    public void testParseAliasWhenValidAliasSpecified()
    {
        LinkParser parser = new LinkParser();
        Link link = new Link();
        StringBuffer sb = new StringBuffer("alias|reference");
        parser.parseAlias(sb, link);

        assertEquals("alias", link.getAlias());
        assertEquals("reference", sb.toString());
        assertTrue(link.isUsingPipeDelimiter());

        link = new Link();
        sb = new StringBuffer("alias>reference");
        parser.parseAlias(sb, link);

        assertEquals("alias", link.getAlias());
        assertEquals("reference", sb.toString());
        assertFalse(link.isUsingPipeDelimiter());
    }

    public void testParseAliasWhenTargetSpecified()
    {
        LinkParser parser = new LinkParser();
        Link link = new Link();
        StringBuffer sb = new StringBuffer("reference|_target");
        parser.parseAlias(sb, link);

        assertNull(link.getAlias());
        assertEquals("reference|_target", sb.toString());
        assertTrue(link.isUsingPipeDelimiter());
    }

    public void testParseTargetWhenNoTargetSpecified() throws Exception
    {
        LinkParser parser = new LinkParser();
        Link link = new Link();
        StringBuffer sb = new StringBuffer("reference");
        parser.parseTarget(sb, link);

        assertNull(link.getTarget());
        assertEquals("reference", sb.toString());
        assertFalse(link.isUsingPipeDelimiter());
    }

    public void testParseTargetWithValidTarget() throws Exception
    {
        LinkParser parser = new LinkParser();
        Link link = new Link();
        StringBuffer sb = new StringBuffer("reference|_target");
        parser.parseTarget(sb, link);

        assertEquals("_target", link.getTarget());
        assertEquals("reference", sb.toString());
        assertTrue(link.isUsingPipeDelimiter());

        link = new Link();
        sb = new StringBuffer("reference>_target");
        parser.parseTarget(sb, link);

        assertEquals("_target", link.getTarget());
        assertEquals("reference", sb.toString());
        assertFalse(link.isUsingPipeDelimiter());
    }

    public void testParseTargetWithInvalidTarget()
    {
        LinkParser parser = new LinkParser();
        Link link = new Link();
        StringBuffer sb = new StringBuffer("reference|target");
        try {
            parser.parseTarget(sb, link);
            fail("Should have thrown an exception here");
        } catch (ContentParserException expected) {
            assertEquals("Error number 22000 in 17: Invalid link format. The target element must "
                + "start with an underscore, got [target]", expected.getMessage());
        }
    }

    public void testParseURIWhenMailtoProtocolSpecified() throws Exception
    {
        LinkParser parser = new LinkParser();

        StringBuffer sb = new StringBuffer("mailto:john@smith.com");
        assertEquals("mailto:john@smith.com", parser.parseURI(sb).toString());
        assertEquals("", sb.toString());
    }

    public void testParseURIWhenURLSpecified() throws Exception
    {
        LinkParser parser = new LinkParser();

        StringBuffer sb = new StringBuffer("http://xwiki.org");
        assertEquals("http://xwiki.org", parser.parseURI(sb).toString());
        assertEquals("", sb.toString());
    }

    public void testParseURIWhenInvalidProtocolSpecified()
    {
        LinkParser parser = new LinkParser();

        StringBuffer sb = new StringBuffer("mywiki:http://xwiki.org");
        try {
            parser.parseURI(sb);
            fail("Exception shoud have been thrown here");
        } catch (ContentParserException expected) {
            assertEquals("Error number 22001 in 17: Invalid URL format "
                + "[mywiki:http://xwiki.org]\nWrapped Exception: unknown protocol: mywiki",
                expected.getMessage());
        }
    }

    public void testParseElementBeforeStringWhenFound()
    {
        LinkParser parser = new LinkParser();
        StringBuffer sb = new StringBuffer("Space.WebHome");
        assertEquals("Space", parser.parseElementBeforeString(sb, "."));
        assertEquals("WebHome", sb.toString());
    }

    public void testParseElementBeforeStringWhenNotFound()
    {
        LinkParser parser = new LinkParser();
        StringBuffer sb = new StringBuffer("WebHome");
        assertNull(parser.parseElementBeforeString(sb, "."));
        assertEquals("WebHome", sb.toString());
    }

    public void testParseElementAfterStringWhenFound()
    {
        LinkParser parser = new LinkParser();
        StringBuffer sb = new StringBuffer("WebHome#anchor");
        assertEquals("anchor", parser.parseElementAfterString(sb, "#"));
        assertEquals("WebHome", sb.toString());
    }

    public void testParseElementAfterStringWhenNotFound()
    {
        LinkParser parser = new LinkParser();
        StringBuffer sb = new StringBuffer("WebHome");
        assertNull(parser.parseElementAfterString(sb, "#"));
        assertEquals("WebHome", sb.toString());
    }

    public void testParse() throws Exception
    {
        LinkParser parser = new LinkParser();

        Link link = parser.parse("");
        assertNull(link.getAlias());
        assertNull(link.getPage());
        assertNull(link.getURI());
        assertFalse(link.isExternal());
        assertEquals("", link.toString());

        link = parser.parse("Hello World");
        assertNull(link.getAlias());
        assertEquals("Hello World", link.getPage());
        assertEquals("Hello World", link.toString());

        link = parser.parse("Hello World>HelloWorld");
        assertEquals("Hello World", link.getAlias());
        assertEquals("HelloWorld", link.getPage());
        assertEquals("Hello World>HelloWorld", link.toString());

        link = parser.parse("Hello World>HelloWorld>_target");
        assertEquals("Hello World", link.getAlias());
        assertEquals("HelloWorld", link.getPage());
        assertEquals("_target", link.getTarget());
        assertEquals("Hello World>HelloWorld>_target", link.toString());

        link = parser.parse("HelloWorld#anchor?param1=1&param2=2@wikipedia");
        assertEquals("HelloWorld", link.getPage());
        assertEquals("anchor", link.getAnchor());
        assertEquals("param1=1&param2=2", link.getQueryString());
        assertEquals("wikipedia", link.getInterWikiAlias());
        assertEquals("HelloWorld#anchor?param1=1&param2=2@wikipedia", link.toString());
        assertTrue(link.isExternal());

        link = parser.parse("Hello World?xredirect=../whatever");
        assertEquals("Hello World", link.getPage());
        assertEquals("xredirect=../whatever", link.getQueryString());
        assertEquals("Hello World?xredirect=../whatever", link.toString());
        assertFalse(link.isExternal());

        link = parser.parse("Hello World>http://xwiki.org");
        assertEquals("Hello World", link.getAlias());
        assertNull(link.getPage());
        assertEquals("http://xwiki.org", link.getURI().toString());
        assertEquals("Hello World>http://xwiki.org", link.toString());
        assertTrue(link.isExternal());

        link = parser.parse("Hello World>HelloWorld?xredirect=http://xwiki.org");
        assertEquals("Hello World", link.getAlias());
        assertEquals("HelloWorld", link.getPage());
        assertEquals("xredirect=http://xwiki.org", link.getQueryString());
        assertNull(link.getURI());
        assertEquals("Hello World>HelloWorld?xredirect=http://xwiki.org", link.toString());

        link = parser.parse("http://xwiki.org");
        assertEquals("http://xwiki.org", link.getURI().toString());
        assertEquals("http://xwiki.org", link.toString());
        assertTrue(link.isExternal());

        link = parser.parse("#anchor");
        assertEquals("anchor", link.getAnchor());
        assertEquals("#anchor", link.toString());
        assertFalse(link.isExternal());
    }
}
