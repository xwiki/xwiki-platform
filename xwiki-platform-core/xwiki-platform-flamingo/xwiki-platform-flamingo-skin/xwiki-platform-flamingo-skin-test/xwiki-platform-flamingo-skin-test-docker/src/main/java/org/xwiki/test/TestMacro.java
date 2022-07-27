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
package org.xwiki.test;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * A macro used to test Bean Validation.
 * 
 * @version $Id$
 */
@Component
@Named("test")
@Singleton
public class TestMacro extends AbstractMacro<TestMacroParameters>
{
    /**
     * Setup the macro.
     */
    public TestMacro()
    {
        super("Test Macro", "Test Description", TestMacroParameters.class);
    }

    @Override
    public List<Block> execute(TestMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return Arrays.<Block>asList(new WordBlock(parameters.isParam() ? "testmacroOK" : "testmacroKO"));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }
}
