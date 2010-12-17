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
package org.xwiki.rendering.internal.transformation.macro;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * Unit tests for {@link MacroTransformationContext}.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class MacroTransformationContextTest
{
    @Test
    public void testClone()
    {
        MacroTransformationContext context = new MacroTransformationContext();
        context.setId("id");
        context.setInline(true);
        context.setSyntax(Syntax.XWIKI_2_0);

        XDOM xdom = new XDOM(Arrays.<Block>asList(new WordBlock("test1")));
        context.setXDOM(xdom);

        MacroBlock macroBlock = new MacroBlock("testmacro", Collections.<String, String>emptyMap(), null, false);
        context.setCurrentMacroBlock(macroBlock);

        Transformation transformation = new Transformation()
        {
            public int getPriority()
            {
                throw new RuntimeException("dummy");
            }

            public void transform(XDOM dom, Syntax syntax) throws TransformationException
            {
                throw new RuntimeException("dummy");
            }

            public void transform(Block block, TransformationContext context) throws TransformationException
            {
                throw new RuntimeException("dummy");
            }

            public int compareTo(Transformation transformation)
            {
                throw new RuntimeException("dummy");
            }
        };
        context.setTransformation(transformation);

        MacroTransformationContext newContext = context.clone();
        Assert.assertNotSame(context, newContext);
        Assert.assertEquals("id", newContext.getId());
        Assert.assertEquals(true, newContext.isInline());
        Assert.assertEquals(Syntax.XWIKI_2_0, newContext.getSyntax());
        Assert.assertEquals(xdom, newContext.getXDOM());
        Assert.assertEquals(macroBlock, newContext.getCurrentMacroBlock());
        Assert.assertEquals(transformation, newContext.getTransformation());
    }
}
