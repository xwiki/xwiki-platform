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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.Syntax;

/**
 * Look for all {@link MacroBlock} blocks in the passed Document and iteratively execute each Macro in the correct
 * order. Macros can:
 * <ul>
 * <li>provide a hint specifying when they should run (priority)</li>
 * <li>generate other Macros</li>
 * </ul>
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class MacroTransformation extends AbstractTransformation implements Composable
{
    /**
     * Number of macro executions allowed when rendering the current content before considering that we are in a loop.
     * Such a loop can happen if a macro generates itself for example.
     */
    private int maxMacroExecutions = 1000;

    private ComponentManager componentManager;

    private class MacroHolder implements Comparable<MacroHolder>
    {
        Macro macro;

        MacroBlock macroBlock;

        public MacroHolder(Macro macro, MacroBlock macroBlock)
        {
            this.macro = macro;
            this.macroBlock = macroBlock;
        }

        public int compareTo(MacroHolder holder)
        {
            return macro.compareTo(holder.macro);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Transformation#transform(org.xwiki.rendering.block.XDOM , org.xwiki.rendering.parser.Syntax)
     */
    public void transform(XDOM dom, Syntax syntax) throws TransformationException
    {
        // Counter to prevent infinite recursion if a macro generates the same macro for example.
        int executions = 0;
        List<MacroBlock> macroBlocks = dom.getChildrenByType(MacroBlock.class);
        while (!macroBlocks.isEmpty() && executions < this.maxMacroExecutions) {
            transformOnce(macroBlocks, dom, syntax);
            macroBlocks = dom.getChildrenByType(MacroBlock.class);
            executions++;
        }
    }

    private void transformOnce(List<MacroBlock> macroBlocks, XDOM dom, Syntax syntax) throws TransformationException
    {
        List<MacroHolder> macroHolders = new ArrayList<MacroHolder>();

        // 1) Sort the macros by priority to find the highest priority macro to execute
        for (MacroBlock macroBlock : dom.getChildrenByType(MacroBlock.class)) {
            String hintName = macroBlock.getName() + "/" + syntax.getType().toIdString();
            try {
                Macro macro = (Macro) this.componentManager.lookup(Macro.ROLE, hintName);
                macroHolders.add(new MacroHolder(macro, macroBlock));
            } catch (ComponentLookupException e) {
                // TODO: When a macro fails to be loaded replace it with an Error Block so that 1) we don't try to load
                // it again
                // and 2) the user can clearly see that it failed to be executed.
                getLogger().warn(
                    "Failed to find macro [" + macroBlock.getName() + "] for hint [" + hintName + "]. Ignoring it");
            }
        }
        // If no macros were found, return with no changes. This can happen if the macros fail to be found.
        if (macroHolders.isEmpty()) {
            return;
        }
        // Sort the Macros by priority
        Collections.sort(macroHolders);

        // 2) Execute the highest priority macro
        MacroHolder macroHolder = macroHolders.get(0);
        List<Block> newBlocks;
        try {
            newBlocks =
                macroHolder.macro.execute(macroHolder.macroBlock.getParameters(), macroHolder.macroBlock.getContent(),
                    dom);
        } catch (MacroExecutionException e) {
            throw new TransformationException("Failed to perform transformation for macro ["
                + macroHolder.macroBlock.getName() + "]", e);
        }

        // We wrap the blocks generated by the macro execution with MacroMarker blocks so that listeners/renderers
        // who wish to know the group of blocks that makes up the executed macro can. For example this is useful for
        // the XWiki Syntax renderer so that it can reconstruct the macros from the transformed XDOM.
        // Note that we only wrap if the parent block is not already a marker since there's no point of adding layers
        // of markers for nested macros since it's the outer marker that'll be used.
        List<Block> resultBlocks;
        if (MacroMarkerBlock.class.isAssignableFrom(macroHolder.macroBlock.getParent().getClass())) {
            resultBlocks = newBlocks;
        } else {
            resultBlocks =
                Collections.singletonList((Block) new MacroMarkerBlock(macroHolder.macroBlock.getName(),
                    macroHolder.macroBlock.getParameters(), macroHolder.macroBlock.getContent(), newBlocks));
        }

        // 3) Replace the MacroBlock by the Blocks generated by the execution of the Macro
        List<Block> childrenBlocks = macroHolder.macroBlock.getParent().getChildren();
        int pos = childrenBlocks.indexOf(macroHolder.macroBlock);
        for (Block block : resultBlocks) {
            block.setParent(macroHolder.macroBlock.getParent());
        }
        childrenBlocks.addAll(pos, resultBlocks);
        childrenBlocks.remove(pos + resultBlocks.size());
    }
}
