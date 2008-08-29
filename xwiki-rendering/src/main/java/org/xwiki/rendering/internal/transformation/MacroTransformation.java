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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.transformation.AbstractTransformation;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Look for all {@link org.xwiki.rendering.block.MacroBlock} blocks in the passed Document and iteratively execute each
 * Macro in the correct order. Macros can:
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
        Macro< ? > macro;

        MacroBlock macroBlock;

        public MacroHolder(Macro< ? > macro, MacroBlock macroBlock)
        {
            this.macro = macro;
            this.macroBlock = macroBlock;
            
            //ConvertUtils.register(new EnumConverter(), Enum.class);
        }

        public int compareTo(MacroHolder holder)
        {
            return this.macro.compareTo(holder.macro);
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
     * @see org.xwiki.rendering.transformation.Transformation#transform(org.xwiki.rendering.block.XDOM , org.xwiki.rendering.parser.Syntax)
     */
    public void transform(XDOM dom, Syntax syntax) throws TransformationException
    {
        MacroTransformationContext context = new MacroTransformationContext();

        context.setXDOM(dom);

        // Counter to prevent infinite recursion if a macro generates the same macro for example.
        int executions = 0;
        List<MacroBlock> macroBlocks = dom.getChildrenByType(MacroBlock.class, true);
        while (!macroBlocks.isEmpty() && executions < this.maxMacroExecutions) {
            if (!transformOnce(macroBlocks, context, syntax)) {
                break;
            }
            // TODO: Make this less inefficient by caching the blocks list.
            macroBlocks = dom.getChildrenByType(MacroBlock.class, true);
            executions++;
        }
    }

    private boolean transformOnce(List<MacroBlock> macroBlocks, MacroTransformationContext context, Syntax syntax)
        throws TransformationException
    {
        List<MacroHolder> macroHolders = new ArrayList<MacroHolder>();

        // 1) Sort the macros by priority to find the highest priority macro to execute
        for (MacroBlock macroBlock : context.getXDOM().getChildrenByType(MacroBlock.class, true)) {
            String hintName = macroBlock.getName() + "/" + syntax.getType().toIdString();
            try {
                Macro< ? > macro = (Macro< ? >) this.componentManager.lookup(Macro.ROLE, hintName);
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
            return false;
        }
        // Sort the Macros by priority
        Collections.sort(macroHolders);

        // 2) Execute the highest priority macro
        MacroHolder macroHolder = macroHolders.get(0);
        List<Block> newBlocks;
        try {
            context.setCurrentMacroBlock(macroHolder.macroBlock);

            Object macroParameters;
            try {
                macroParameters = macroHolder.macro.getDescriptor().getParametersBeanClass().newInstance();
                BeanUtils.populate(macroParameters, macroHolder.macroBlock.getParameters());
            } catch (Exception e) {
                throw new MacroParameterException("Failed to parse parameters", e);
            }

            newBlocks =
                ((Macro<Object>) macroHolder.macro).execute(macroParameters, macroHolder.macroBlock.getContent(),
                    context);
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
        return true;
    }
}
