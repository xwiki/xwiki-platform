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
package org.xwiki.refactoring.splitter.criterion.naming;

import java.io.StringReader;

import org.xwiki.refactoring.internal.AbstractRefactoringTestCase;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for {@link HeadingNameNamingCriterion}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class HeadingNameNamingCriterionTest extends AbstractRefactoringTestCase
{
    /**
     * Tests document names generated.
     * 
     * @throws Exception
     */
    @Test
    public void testDocumentNamesGeneration() throws Exception
    {
        XDOM xdom = xwikiParser.parse(new StringReader("=Heading="));
        BlockRenderer plainSyntaxBlockRenderer =
            getComponentManager().getInstance(BlockRenderer.class, Syntax.PLAIN_1_0.toIdString());
        NamingCriterion namingCriterion =
            new HeadingNameNamingCriterion("Test.Test", docBridge, plainSyntaxBlockRenderer, false);
        Block sectionBlock = xdom.getChildren().get(0);
        // Test normal heading-name naming
        Assert.assertEquals("Test.Heading", namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())));
        // Test name clash resolution
        Assert.assertEquals("Test.Heading-1", namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())));
        // Test heading text cleaning (replacing)
        xdom = xwikiParser.parse(new StringReader("= This-Very.Weird:Heading! ="));
        sectionBlock = xdom.getChildren().get(0);
        Assert.assertEquals("Test.This-Very-Weird-Heading!",
            namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())));
        // Test heading text cleaning (stripping) 
        xdom = xwikiParser.parse(new StringReader("= This?Is@A/Very#Weird~Heading ="));
        sectionBlock = xdom.getChildren().get(0);
        Assert.assertEquals("Test.ThisIsAVeryWeirdHeading",
            namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())));
        // Test page name truncation.
        xdom = xwikiParser.parse(new StringReader("=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa="));
        sectionBlock = xdom.getChildren().get(0);
        Assert.assertEquals(255, namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())).length());
        // Test fallback operation
        Assert.assertEquals("Test.Test-1", namingCriterion.getDocumentName(xdom));
        // Test fallback operation under empty heading names
        xdom = xwikiParser.parse(new StringReader("=   ="));
        sectionBlock = xdom.getChildren().get(0);
        Assert.assertEquals("Test.Test-2", namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())));
    }
}
