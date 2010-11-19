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
package org.xwiki.rendering.internal.block;

import java.util.Arrays;
import java.util.Collections;

import org.junit.*;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.block.ProtectedBlockFilter}.
 *
 * @version $Id$
 * @since 2.6
 */
public class ProtectedBlockFilterTest
{
    @Test
    public void testGetNextSibling()
    {
        ProtectedBlockFilter pbf = new ProtectedBlockFilter();
        Block b1 = new ParagraphBlock(Collections.<Block>emptyList());
        Block b2 = new MacroMarkerBlock("code", Collections.<String, String>emptyMap(), Collections.<Block>emptyList(),
            false);
        Block b3 = new ParagraphBlock(Collections.<Block>emptyList());
        XDOM xdom = new XDOM(Arrays.asList(b1, b2, b3));
        Assert.assertEquals(b3, pbf.getNextSibling(b1));
    }
}
