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
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.DocumentManager;
import org.xwiki.velocity.VelocityManager;
import org.jmock.Mock;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.StringWriter;

/**
 * Unit tests for {@link IncludeMacro}.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class IncludeMacroTest extends AbstractRenderingTestCase
{
    public void testIncludeMacroWithCurrentContext() throws Exception
    {
        String expected = "beginDocument\n"
            + "onMacro: [someMacro] [] []\n"
            + "endDocument";

        IncludeMacro macro = (IncludeMacro) getComponentManager().lookup(VelocityMacro.ROLE, "include/xwiki");
        Mock mockDocumentManager = mock(DocumentManager.class);
        mockDocumentManager.expects(once()).method("getDocumentContent").will(returnValue("{{someMacro/}}"));
        macro.setDocumentManager((DocumentManager) mockDocumentManager.proxy());

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("document", "wiki:Space.Page");
        properties.put("context", "current");

        List<Block> blocks =
            macro.execute(macro.getMacroDescriptor().createMacroParameters(properties), null,
                MacroTransformationContext.EMPTY);

        assertBlocks(expected, blocks);
    }

    public void testIncludeMacroWithNewContext() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMacroMarker: [velocity] [] [$myvar]\n"
            + "beginParagraph\n"
            + "onSpecialSymbol: [$]\n" + "onWord: [myvar]\n"
            + "endParagraph\n"
            + "endMacroMarker: [velocity] [] [$myvar]\n" + "endDocument";

        // Since it's not in the same context, we verify that a Velocity variable set in the including page is not
        // seen in the included page.
        VelocityManager velocityManager = (VelocityManager) getComponentManager().lookup(VelocityManager.ROLE);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer, "template",
            "#set ($myvar = 'hello')");

        IncludeMacro macro = (IncludeMacro) getComponentManager().lookup(VelocityMacro.ROLE, "include/xwiki");
        Mock mockDocumentManager = mock(DocumentManager.class);
        mockDocumentManager.expects(once()).method("getDocumentContent").will(
            returnValue("{{velocity}}$myvar{{/velocity}}"));
        macro.setDocumentManager((DocumentManager) mockDocumentManager.proxy());

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("document", "wiki:Space.Page");
        properties.put("context", "new");

        List<Block> blocks =
            macro.execute(macro.getMacroDescriptor().createMacroParameters(properties), null,
                MacroTransformationContext.EMPTY);

        assertBlocks(expected, blocks);
    }
}
