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

import java.util.Collections;
import java.util.List;

import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.block.Block;

/**
 * Unit tests for {@link org.xwiki.rendering.macro.XHTMLMacro}.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XHTMLMacroTest extends AbstractRenderingTestCase
{
    /**
     * Verify that XHTML entities are supported and can be parsed.
     */
    public void testMacroWithEntities() throws Exception
    {
        String html = "&nbsp;";

        String expected = "beginDocument\n"
            + "onWord: [" + ((char) 160) + "]\n"
            + "endDocument";
        
        Macro macro = (Macro) getComponentManager().lookup(XHTMLMacro.ROLE, "xhtml/xwiki");
        List<Block> blocks =
            macro.execute(macro.createMacroParameters(Collections.<String, String> emptyMap()), html,
                MacroTransformationContext.EMPTY);

        assertBlocks(expected, blocks);
    }
}
