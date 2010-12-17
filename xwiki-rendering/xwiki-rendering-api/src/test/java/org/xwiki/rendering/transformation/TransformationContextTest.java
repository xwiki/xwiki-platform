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
package org.xwiki.rendering.transformation;

import java.util.Arrays;

import org.junit.Test;
import org.junit.Assert;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Unit tests for {@link TransformationContext}.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class TransformationContextTest
{
    @Test
    public void testClone()
    {
        TransformationContext context = new TransformationContext();
        context.setId("id");
        context.setSyntax(Syntax.XWIKI_2_0);
        XDOM xdom = new XDOM(Arrays.<Block>asList(new WordBlock("test")));
        context.setXDOM(xdom);

        TransformationContext newContext = context.clone();
        Assert.assertNotSame(context, newContext);
        Assert.assertEquals("id", newContext.getId());
        Assert.assertEquals(Syntax.XWIKI_2_0, newContext.getSyntax());
        Assert.assertEquals(xdom, newContext.getXDOM());
    }
}
