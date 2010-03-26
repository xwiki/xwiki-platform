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
package org.xwiki.rendering.internal.parser;

import org.jmock.Mockery;
import org.junit.*;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class XWikiLinkParserTest extends AbstractComponentTestCase
{
    private LinkParser parser;

    private Mockery mockery = new Mockery();

    @Override
    protected void registerComponents() throws Exception
    {
        // Create a Mock WikiModel implementation so that the link parser works in wiki mode
        WikiModel mockWikiModel = this.mockery.mock(WikiModel.class); 

        DefaultComponentDescriptor<WikiModel> componentDescriptor = new DefaultComponentDescriptor<WikiModel>();
        componentDescriptor.setRole(WikiModel.class);
        componentDescriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        componentDescriptor.setImplementation(null);

        getComponentManager().registerComponent(componentDescriptor, mockWikiModel);
        this.parser = getComponentManager().lookup(LinkParser.class, "xwiki/2.0");
    }

    @Test
    public void testParseLinksWhenInWikiMode() throws Exception
    {

        Link link = parser.parse("");
        Assert.assertEquals("", link.getReference());
        Assert.assertEquals("Reference = []", link.toString());

        link = parser.parse("Hello World");
        Assert.assertEquals("Hello World", link.getReference());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Reference = [Hello World]", link.toString());

        link = parser.parse("HelloWorld#anchor?param1=1&param2=2@wikipedia");
        Assert.assertEquals("HelloWorld", link.getReference());
        Assert.assertEquals("anchor", link.getAnchor());
        Assert.assertEquals("param1=1&param2=2", link.getQueryString());
        Assert.assertEquals("wikipedia", link.getInterWikiAlias());
        Assert.assertEquals("Reference = [HelloWorld] QueryString = [param1=1&param2=2] "
            + "Anchor = [anchor] InterWikiAlias = [wikipedia]", link.toString());

        link = parser.parse("Hello World?xredirect=../whatever");
        Assert.assertEquals("Hello World", link.getReference());
        Assert.assertEquals("xredirect=../whatever", link.getQueryString());
        Assert.assertEquals("Reference = [Hello World] QueryString = [xredirect=../whatever]", link.toString());

        link = parser.parse("HelloWorld?xredirect=http://xwiki.org");
        Assert.assertEquals("HelloWorld", link.getReference());
        Assert.assertEquals("xredirect=http://xwiki.org", link.getQueryString());
        Assert.assertEquals("Reference = [HelloWorld] QueryString = [xredirect=http://xwiki.org]", link.toString());

        link = parser.parse("http://xwiki.org");
        Assert.assertEquals("http://xwiki.org", link.getReference());
        Assert.assertEquals(LinkType.URI, link.getType());
        Assert.assertEquals("Reference = [http://xwiki.org]", link.toString());

        link = parser.parse("#anchor");
        Assert.assertEquals("anchor", link.getAnchor());
        Assert.assertEquals("Reference = [] Anchor = [anchor]", link.toString());

        link = parser.parse("Hello#anchor");
        Assert.assertEquals("Hello", link.getReference());
        Assert.assertEquals("anchor", link.getAnchor());
        Assert.assertEquals("Reference = [Hello] Anchor = [anchor]", link.toString());

        // Verify mailto: URI is recognized
        link = parser.parse("mailto:john@smith.com");
        Assert.assertEquals("mailto:john@smith.com", link.getReference());
        Assert.assertEquals(LinkType.URI, link.getType());
        Assert.assertEquals("Reference = [mailto:john@smith.com]", link.toString());

        // Verify image: URI is recognized
        link = parser.parse("image:some:content");
        Assert.assertEquals("image:some:content", link.getReference());
        Assert.assertEquals(LinkType.URI, link.getType());
        Assert.assertEquals("Reference = [image:some:content]", link.toString());

        // Verify attach: URI is recognized
        link = parser.parse("attach:some:content");
        Assert.assertEquals("attach:some:content", link.getReference());
        Assert.assertEquals(LinkType.URI, link.getType());
        Assert.assertEquals("Reference = [attach:some:content]", link.toString());

        // Verify that unknown URIs are ignored
        // Note: We consider that myxwiki is the wiki name and http://xwiki.org is the page name
        link = parser.parse("mywiki:http://xwiki.org");
        Assert.assertEquals("mywiki:http://xwiki.org", link.getReference());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Reference = [mywiki:http://xwiki.org]", link.toString());

    }
}
