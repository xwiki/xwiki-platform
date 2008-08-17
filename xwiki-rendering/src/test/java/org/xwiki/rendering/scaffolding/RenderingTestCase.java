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
package org.xwiki.rendering.scaffolding;

import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.renderer.PrintRenderer;

import java.io.StringReader;

/**
 * @version $Id: $
 * @since 1.6M1
 */
public class RenderingTestCase extends AbstractRenderingTestCase
{
    private String input;
    private String expected;
    private Parser parser;
    private PrintRenderer renderer;
    private boolean runTransformations;

    public RenderingTestCase(String testName, String input, String expected, Parser parser,
        PrintRenderer renderer, boolean runTransformations)
    {
        super(testName);
        this.input = input;
        this.expected = expected;
        this.parser = parser;
        this.renderer = renderer;
        this.runTransformations = runTransformations;
    }

    @Override
    protected void runTest() throws Throwable
    {
        XDOM dom = this.parser.parse(new StringReader(this.input));

        if (this.runTransformations) {
            TransformationManager transformationManager =
                (TransformationManager) getComponentManager().lookup(TransformationManager.ROLE);
            transformationManager.performTransformations(dom, this.parser.getSyntax());
        }

        dom.traverse(this.renderer);

        assertEquals(this.expected, this.renderer.getPrinter().toString());
    }
}
