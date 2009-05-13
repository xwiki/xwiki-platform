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
package org.xwiki.rendering.macro.box;

import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

@Component("testbox")
public class TestBoxMacro extends AbstractBoxMacro<BoxMacroParameters>
{
    public TestBoxMacro()
    {
        super(new DefaultMacroDescriptor("TestBoxMacro", BoxMacroParameters.class));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.box.AbstractBoxMacro#parseContent(org.xwiki.rendering.macro.box.BoxMacroParameters,
     *      java.lang.String, org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    @Override
    protected List<Block> parseContent(BoxMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return Collections.<Block> singletonList(new VerbatimBlock(content, context.isInline()));
    }
}
