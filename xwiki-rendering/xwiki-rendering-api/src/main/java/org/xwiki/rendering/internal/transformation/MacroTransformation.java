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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.AbstractTransformation;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

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
@Component("macro")
public class MacroTransformation extends AbstractTransformation
{
    /**
     * Number of macro executions allowed when rendering the current content before considering that we are in a loop.
     * Such a loop can happen if a macro generates itself for example.
     */
    private int maxMacroExecutions = 1000;

    /**
     * Handles macro registration and macro lookups. Injected by the Component Manager.
     */
    @Requirement
    private MacroManager macroManager;

    /**
     * Used to populate automatically macros parameters classes with parameters specified in the Macro Block.
     */
    @Requirement
    private BeanManager beanManager;

    private class MacroHolder implements Comparable<MacroHolder>
    {
        Macro< ? > macro;

        MacroBlock macroBlock;

        public MacroHolder(Macro< ? > macro, MacroBlock macroBlock)
        {
            this.macro = macro;
            this.macroBlock = macroBlock;
        }

        public int compareTo(MacroHolder holder)
        {
            return this.macro.compareTo(holder.macro);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.transformation.Transformation#transform(org.xwiki.rendering.block.XDOM ,
     *      org.xwiki.rendering.syntax.Syntax)
     */
    public void transform(XDOM dom, Syntax syntax) throws TransformationException
    {
        // Create a macro execution context with all the information required for macros.
        MacroTransformationContext context = new MacroTransformationContext();
        context.setXDOM(dom);
        context.setTransformation(this);
        context.setSyntax(syntax);

        // Counter to prevent infinite recursion if a macro generates the same macro for example.
        int executions = 0;
        List<MacroBlock> macroBlocks = dom.getChildrenByType(MacroBlock.class, true);
        while (!macroBlocks.isEmpty() && executions < this.maxMacroExecutions) {
            transformOnce(context, syntax);

            // TODO: Make this less inefficient by caching the blocks list.
            macroBlocks = dom.getChildrenByType(MacroBlock.class, true);
            executions++;
        }
    }

    private void transformOnce(MacroTransformationContext context, Syntax syntax)
    {
        // 1) Get highest priority macro to execute
        MacroHolder macroHolder = getHighestPriorityMacro(context.getXDOM(), syntax);
        if (macroHolder == null) {
            return;
        }

        // 2) Verify if we're in macro inline mode and if the macro supports it. If not, send an error.
        if (macroHolder.macroBlock.isInline()) {
            context.setInline(true);
            if (!macroHolder.macro.supportsInlineMode()) {
                // The macro doesn't support inline mode, raise a warning but continue.
                // The macro will not be executed and we generate an error message instead of the macro
                // execution result.
                generateError(macroHolder.macroBlock, "Not an inline macro",
                    "This macro can only be used by itself on a new line");
                getLogger().debug("The [" + macroHolder.macroBlock.getId() + "] macro doesn't support inline mode.");
                return;
            }
        } else {
            context.setInline(false);
        }

        // 3) Execute the highest priority macro
        List<Block> newBlocks;
        try {
            context.setCurrentMacroBlock(macroHolder.macroBlock);

            // Populate and validate macro parameters.
            Object macroParameters;
            try {
                macroParameters = macroHolder.macro.getDescriptor().getParametersBeanClass().newInstance();
                this.beanManager.populate(macroParameters, macroHolder.macroBlock.getParameters());
            } catch (Throwable e) {
                // One macro parameter was invalid.
                // The macro will not be executed and we generate an error message instead of the macro
                // execution result.
                generateError(macroHolder.macroBlock, "Invalid macro parameters used for the \""
                    + macroHolder.macroBlock.getId() + "\" macro", e);
                getLogger().debug(
                    "Invalid macro parameter for the [" + macroHolder.macroBlock.getId() + "] macro. Internal error: ["
                        + e.getMessage() + "]");

                return;
            }

            newBlocks =
                    ((Macro<Object>) macroHolder.macro).execute(macroParameters, macroHolder.macroBlock.getContent(),
                        context);
        } catch (Throwable e) {
            // The Macro failed to execute.
            // The macro will not be executed and we generate an error message instead of the macro
            // execution result.
            // Note: We catch any Exception because we want to never break the whole rendering.
            generateError(macroHolder.macroBlock, "Failed to execute the [" + macroHolder.macroBlock.getId()
                + "] macro", e);
            getLogger().debug(
                "Failed to execute the [" + macroHolder.macroBlock.getId() + "]macro. Internal error ["
                    + e.getMessage() + "]");

            return;
        }

        // We wrap the blocks generated by the macro execution with MacroMarker blocks so that listeners/renderers
        // who wish to know the group of blocks that makes up the executed macro can. For example this is useful for
        // the XWiki Syntax renderer so that it can reconstruct the macros from the transformed XDOM.
        Block resultBlock = wrapInMacroMarker(macroHolder.macroBlock, newBlocks);

        // 4) Replace the MacroBlock by the Blocks generated by the execution of the Macro
        macroHolder.macroBlock.getParent().replaceChild(resultBlock, macroHolder.macroBlock);
    }

    /**
     * @return the macro with the highest priority for the passed syntax or null if no macro is found
     */
    private MacroHolder getHighestPriorityMacro(XDOM xdom, Syntax syntax)
    {
        List<MacroHolder> macroHolders = new ArrayList<MacroHolder>();

        // 1) Sort the macros by priority to find the highest priority macro to execute
        for (MacroBlock macroBlock : xdom.getChildrenByType(MacroBlock.class, true)) {
            try {
                Macro< ? > macro = this.macroManager.getMacro(new MacroId(macroBlock.getId(), syntax));
                macroHolders.add(new MacroHolder(macro, macroBlock));
            } catch (MacroLookupException e) {
                // Macro cannot be found. Generate an error message instead of the macro execution result.
                // TODO: make it internationalized
                generateError(macroBlock, "Unknown macro: " + macroBlock.getId(), "The \"" + macroBlock.getId()
                    + "\" macro is not in the list of registered macros. Verify the "
                    + "spelling or contact your administrator.");
                getLogger().debug("Failed to locate the [" + macroBlock.getId() + "] macro. Ignoring it.");
            }
        }

        // Sort the Macros by priority
        Collections.sort(macroHolders);

        return macroHolders.size() > 0 ? macroHolders.get(0) : null;
    }

    private Block wrapInMacroMarker(MacroBlock macroBlockToWrap, List<Block> newBlocks)
    {
        return new MacroMarkerBlock(macroBlockToWrap.getId(), macroBlockToWrap.getParameters(),
            macroBlockToWrap.getContent(), newBlocks, macroBlockToWrap.isInline());
    }

    private void generateError(MacroBlock macroToReplace, String message, String description)
    {
        List<Block> errorBlocks = new ArrayList<Block>();

        Map<String, String> errorBlockParams = Collections.singletonMap("class", "xwikirenderingerror");
        Map<String, String> errorDescriptionBlockParams =
                Collections.singletonMap("class", "xwikirenderingerrordescription hidden");

        Block descriptionBlock = new VerbatimBlock(description, macroToReplace.isInline());

        if (macroToReplace.isInline()) {
            errorBlocks.add(new FormatBlock(Arrays.<Block> asList(new WordBlock(message)), Format.NONE,
                errorBlockParams));
            errorBlocks.add(new FormatBlock(Arrays.asList(descriptionBlock), Format.NONE, errorDescriptionBlockParams));
        } else {
            errorBlocks.add(new GroupBlock(Arrays.<Block> asList(new WordBlock(message)), errorBlockParams));
            errorBlocks.add(new GroupBlock(Arrays.asList(descriptionBlock), errorDescriptionBlockParams));
        }

        macroToReplace.getParent().replaceChild(wrapInMacroMarker(macroToReplace, errorBlocks), macroToReplace);
    }

    private void generateError(MacroBlock macroToReplace, String message, Throwable throwable)
    {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        generateError(macroToReplace, message, writer.getBuffer().toString());
    }
}
