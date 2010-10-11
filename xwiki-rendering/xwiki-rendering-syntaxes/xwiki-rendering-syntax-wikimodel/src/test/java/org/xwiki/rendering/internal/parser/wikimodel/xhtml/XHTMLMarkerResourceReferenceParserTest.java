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
package org.xwiki.rendering.internal.parser.wikimodel.xhtml;

import org.junit.*;
import org.xwiki.rendering.internal.renderer.ParametersPrinter;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.parser.reference.XWiki20ImageReferenceParser}.
 *
 * @version $Id$
 * @since 2.5RC1
 */
public class XHTMLMarkerResourceReferenceParserTest extends AbstractComponentTestCase
{
    private  ResourceReferenceParser parser;

    @Override
    protected void registerComponents() throws Exception
    {
        this.parser = getComponentManager().lookup(ResourceReferenceParser.class, "xhtmlmarker");
    }

    @Test
    public void testParse()
    {
        ResourceReference expected = new ResourceReference("reference", ResourceType.DOCUMENT);
        expected.setParameter("param1", "value1");
        expected.setParameter("param2", "value2");

        Assert.assertEquals(expected, this.parser.parse("true|-|doc|-|reference|-|param1=value1 param2=value2"));
    }

    @Test
    public void testParseWithEscapesInParameterValues()
    {
        ResourceReference expected = new ResourceReference("reference", ResourceType.DOCUMENT);
        expected.setParameter("param", "va\"l\\=ue");

        ParametersPrinter printer = new ParametersPrinter();
        Assert.assertEquals(expected, this.parser.parse("true|-|doc|-|reference|-|"
            + printer.print("param", "va\"l\\=ue", '\\')));
    }
}
