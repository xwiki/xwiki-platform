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
package org.xwiki.rendering.internal.macro;

import java.util.List;

import org.jmock.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.internal.macro.include.IncludeMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;
import org.xwiki.rendering.transformation.MacroTransformationContext;

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
            + "onMacroStandalone [someMacro] [] []\n"
            + "endDocument";

        IncludeMacro macro = (IncludeMacro) getComponentManager().lookup(Macro.ROLE, "include");
        Mock mockDocumentAccessBridge = mock(DocumentAccessBridge.class);
        mockDocumentAccessBridge.expects(once()).method("isDocumentViewable").will(returnValue(true));
        mockDocumentAccessBridge.expects(once()).method("getDocumentContent").will(returnValue("{{someMacro/}}"));
        macro.setDocumentAccessBridge((DocumentAccessBridge) mockDocumentAccessBridge.proxy());

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("wiki:Space.Page");
        parameters.setContext(Context.CURRENT);

        List<Block> blocks = macro.execute(parameters, null, new MacroTransformationContext());

        assertBlocks(expected, blocks);
    }
}
