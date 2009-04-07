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
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Test case for {@link PageIndexNamingCriterion}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class PageIndexNamingCriterionTest extends AbstractXWikiComponentTestCase
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
     * {@inheritDoc}
     */
    protected void setUp() throws Exception
    {
        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
        super.setUp();
        xwikiParser = (Parser) getComponentManager().lookup(Parser.class, "xwiki/2.0");
        docBridge = (DocumentAccessBridge) getComponentManager().lookup(DocumentAccessBridge.class, "default");
    }

    /**
     * Tests document names generated.
     * 
     * @throws Exception
     */
    public void testDocumentNamesGeneration() throws Exception
    {
        XDOM xdom = xwikiParser.parse(new StringReader("=Test="));
        NamingCriterion namingCriterion = new PageIndexNamingCriterion("Main.Test", docBridge);
        for (int i = 1; i < 10; i++) {
            assertEquals("Main.Test-" + i, namingCriterion.getDocumentName(xdom));
        }
    }
}
