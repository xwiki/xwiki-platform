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
package org.xwiki.rendering.internal.transformation;

import java.util.Arrays;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.macro.AbstractNoParameterMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Test macro that generates an raw xhtml block with only spaces inside.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component("testspacesxhtmlrawblock")
public class TestSpacesXHTMLRawBlockMacro extends AbstractNoParameterMacro
{
    /**
     * Default constructor.
     */
    public TestSpacesXHTMLRawBlockMacro()
    {
        super("Macro that produces a raw XHTML block with only spaces inside.");
    }

    @Override
    public List<Block> execute(Object arg0, String arg1, MacroTransformationContext arg2)
        throws MacroExecutionException
    {
        return Arrays.<Block> asList(new RawBlock("    ", Syntax.XHTML_1_0));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }
}
