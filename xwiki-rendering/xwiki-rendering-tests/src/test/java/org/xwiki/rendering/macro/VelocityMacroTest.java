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
package org.xwiki.rendering.macro;

import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;

import java.util.List;
import java.util.Collections;

/**
 * Unit tests for {@link org.xwiki.rendering.macro.VelocityMacro}.
 *
 * @version $Id$
 * @since 1.5M2
 */
public class VelocityMacroTest extends AbstractRenderingTestCase
{
    public void testMacro() throws Exception
    {
        String content = "#set ($list = ['one', 'two'])\n"
            + "#foreach ($item in $list)\n"
            + "* $item\n"
            + "#end";
        
        String expected = "beginDocument\n"
        	+ "beginList: [BULLETED]\n"
            + "beginListItem\n"
            + "onWord: [one]\n"
            + "endListItem\n"
            + "beginListItem\n"
            + "onWord: [two]\n"
            + "endListItem\n"
            + "endList: [BULLETED]\n"
            + "endDocument\n";

        Macro macro = (Macro) getComponentManager().lookup(VelocityMacro.ROLE, "velocity/xwiki");
        List<Block> blocks = macro.execute(Collections.EMPTY_MAP, content,
            new XDOM(Collections.EMPTY_LIST));

        assertBlocks(expected, blocks);
    }
}
