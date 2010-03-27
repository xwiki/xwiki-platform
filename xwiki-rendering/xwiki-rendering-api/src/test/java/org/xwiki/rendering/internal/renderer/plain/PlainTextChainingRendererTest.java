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
package org.xwiki.rendering.internal.renderer.plain;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link PlainTextChainingRenderer} that cannot easily be performed using the Renderng Test framework.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class PlainTextChainingRendererTest extends AbstractComponentTestCase
{
    @Test
    public void testBeginLinkWhenLinkLabelGeneratorIsNull() throws Exception
    {
        // Use the constructor that sets the Link Label Generator to null
        PlainTextRenderer renderer = new PlainTextRenderer();
        renderer.initialize();
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        renderer.setPrinter(printer);

        Link link = new Link();
        link.setAnchor("anchor");
        link.setQueryString("param=value");
        link.setReference("reference");
        link.setType(LinkType.DOCUMENT);

        renderer.beginLink(link, false, Collections.<String, String> emptyMap());
        renderer.endLink(link, false, Collections.<String, String> emptyMap());

        Assert.assertEquals("reference#anchor?param=value", printer.toString());
    }

    @Test
    public void testBeginLinkWhenExternalLink() throws Exception
    {
        // Use the constructor that sets the Link Label Generator to null
        PlainTextRenderer renderer = new PlainTextRenderer();
        renderer.initialize();
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        renderer.setPrinter(printer);

        Link link = new Link();
        link.setReference("reference#anchor?param=value");
        link.setType(LinkType.URI);

        renderer.beginLink(link, false, Collections.<String, String> emptyMap());
        renderer.endLink(link, false, Collections.<String, String> emptyMap());

        Assert.assertEquals("reference#anchor?param=value", printer.toString());
    }
}
