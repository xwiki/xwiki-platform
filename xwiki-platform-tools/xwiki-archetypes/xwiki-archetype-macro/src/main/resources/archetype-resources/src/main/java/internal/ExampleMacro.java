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
package ${packageName}.internal;

import java.util.List;
import java.util.Arrays;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import ${packageName}.ExampleMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Example Macro.
 */
@Component("example")
public class ExampleMacro extends AbstractMacro<ExampleMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Example Macro";
    
    /**
     * Create and initialize the descriptor of the macro.
     */
    public ExampleMacro()
    {
        super("Example", DESCRIPTION, ExampleMacroParameters.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(ExampleMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result;

        List<Block> wordBlockAsList = Arrays.<Block>asList(new WordBlock(parameters.getParameter()));

        // Handle both inline mode and standalone mode.
        if (context.isInline()) {
            result = wordBlockAsList;
        } else {
            // Wrap the result in a Paragraph Block since a WordBlock is an inline element and it needs to be
            // inside a standalone block.
            result = Arrays.<Block>asList(new ParagraphBlock(wordBlockAsList));
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return true;
    }
}
