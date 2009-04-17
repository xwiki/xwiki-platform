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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.refactoring.internal.MockDocumentAccessBridge;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Test case for {@link HeadingNameNamingCriterion}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class HeadingNameNamingCriterionTest extends AbstractXWikiComponentTestCase
{
    /**
     * The {@link Parser} component.
     */
    private Parser xwikiParser;

    /**
     * The {@link DocumentAccessBridge} component.
     */
    private DocumentAccessBridge docBridge;
    
    /**
     * Factory to get various syntax renderers.
     */
    private PrintRendererFactory rendererFactory;
    
    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception
    {
        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
        super.setUp();
        xwikiParser = (Parser) getComponentManager().lookup(Parser.class, "xwiki/2.0");
        docBridge = (DocumentAccessBridge) getComponentManager().lookup(DocumentAccessBridge.class, "default");
        rendererFactory = (PrintRendererFactory) getComponentManager().lookup(PrintRendererFactory.class, "default");
    }
    
    /**
     * Tests document names generated.
     * 
     * @throws Exception
     */
    public void testDocumentNamesGeneration() throws Exception
    {
        XDOM xdom = xwikiParser.parse(new StringReader("=Heading="));
        NamingCriterion namingCriterion = new HeadingNameNamingCriterion("Test.Test", docBridge, rendererFactory);
        Block sectionBlock = xdom.getChildren().get(0);
        // Test normal heading-name naming
        assertEquals("Test.Heading", namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())));
        // Test name clash resolution
        assertEquals("Test.Heading-1", namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())));
        // Test heading text cleaning
        xdom = xwikiParser.parse(new StringReader("= Very.Weird:Heading! ="));
        sectionBlock = xdom.getChildren().get(0);
        assertEquals("Test.VeryWeirdHeading", namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())));
        // Test fallback operation
        assertEquals("Test.Test-1", namingCriterion.getDocumentName(xdom));
        // Test fallback operation under empty heading names
        xdom = xwikiParser.parse(new StringReader("=   ="));
        sectionBlock = xdom.getChildren().get(0);
        assertEquals("Test.Test-2", namingCriterion.getDocumentName(new XDOM(sectionBlock.getChildren())));
    }
}
