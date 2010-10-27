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
package org.xwiki.rendering.internal.transformation.wikiword;

import java.io.StringReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.transformation.wikiword.WikiWordTransformation}.
 *
 * @version $Id$
 * @since 2.6RC1
 */
public class WikiWordTransformationTest extends AbstractComponentTestCase
{
    private Transformation wikiWordTransformation;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        this.wikiWordTransformation = getComponentManager().lookup(Transformation.class, "wikiword");
    }

    @Test
    public void testWikiWordTransformation() throws Exception
    {
        XDOM xdom = getComponentManager().lookup(Parser.class, "xwiki/2.1").parse(new StringReader(
            "This is a WikiWord, AnotherOne"));
        this.wikiWordTransformation.transform(xdom, new TransformationContext());
        WikiPrinter printer = new DefaultWikiPrinter();
        getComponentManager().lookup(BlockRenderer.class, "xwiki/2.1").render(xdom, printer);
        Assert.assertEquals("This is a [[doc:WikiWord]], [[doc:AnotherOne]]", printer.toString());
    }
}
