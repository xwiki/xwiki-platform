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
        // Tests the following at once:
        // - that a wiki word is recognized
        // - that several wiki words in a row are recognized
        // - that wiki words with non ASCII chars are recognized (accented chars)
        // - that two uppercase letters following each other (as in "XWiki") are not considered a wiki word
        // - that several uppercases chars followed by lowercases and then one uppercase and lowercase chars is
        //   recognized as a wiki word (eg "XWikiEnterprise")
        String testInput = "This is a WikiWord, Another\u00D9ne, XWikiEnterprise, not one: XWiki";

        XDOM xdom = getComponentManager().lookup(Parser.class, "xwiki/2.1").parse(new StringReader(testInput));
        this.wikiWordTransformation.transform(xdom, new TransformationContext());
        WikiPrinter printer = new DefaultWikiPrinter();
        getComponentManager().lookup(BlockRenderer.class, "xwiki/2.1").render(xdom, printer);
        Assert.assertEquals("This is a [[doc:WikiWord]], [[doc:Another\u00D9ne]], [[doc:XWikiEnterprise]], "
            + "not one: XWiki", printer.toString());
    }
}
