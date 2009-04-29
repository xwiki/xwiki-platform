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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroFactory;
import org.xwiki.rendering.macro.MacroNotFoundException;
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
    private MacroFactory macroFactory;

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
     *      org.xwiki.rendering.parser.Syntax)
     */
    public void transform(XDOM dom, Syntax syntax) throws TransformationException
    {
        // Create a macro execution context with all the information required for macros.
        MacroTransformationContext context = new MacroTransformationContext();
        context.setXDOM(dom);
        context.setMacroTransformation(this);
        context.setSyntax(syntax);

        // Counter to prevent infinite recursion if a macro generates the same macro for example.
        int executions = 0;
        List<MacroBlock> macroBlocks = dom.getChildrenByType(MacroBlock.class, true);
        while (!macroBlocks.isEmpty() && executions < this.maxMacroExecutions) {
            if (!transformOnce(context, syntax)) {
                break;
            }
            // TODO: Make this less inefficient by caching the blocks list.
            macroBlocks = dom.getChildrenByType(MacroBlock.class, true);
            executions++;
        }
    }

    private boolean transformOnce(MacroTransformationContext context, Syntax syntax) throws TransformationException
    {
        // 1) Get highest priority macro to execute
        MacroHolder macroHolder = getHighestPriorityMacro(context.getXDOM(), syntax);
        if (macroHolder == null) {
            return false;
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
                getLogger().debug("The [" + macroHolder.macroBlock.getName() + "] macro doesn't support inline mode.");
                return false;
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
                // TODO: BeanUtils shouldn't be exposed to users of the Macro
                BeanUtils.populate(macroParameters, macroHolder.macroBlock.getParameters());
            } catch (Exception e) {
                // One macro parameter was invalid.
                // The macro will not be executed and we generate an error message instead of the macro
                // execution result.
                generateError(macroHolder.macroBlock, "Invalid macro parameters used for macro: "
                    + macroHolder.macroBlock.getName(), e);
                getLogger().debug(
                    "Invalid macro parameter for macro [" + macroHolder.macroBlock.getName() + "]. Internal error: ["
                        + e.getMessage() + "]");
                return false;
            }

            newBlocks = ((Macro<Object>) macroHolder.macro).execute(macroParameters, 
                normalizeContent(macroHolder.macroBlock.getContent()), context);
        } catch (Exception e) {
            // The Macro failed to execute.
            // The macro will not be executed and we generate an error message instead of the macro
            // execution result.
            // Note: We catch any Exception because we want to never break the whole rendering.
            generateError(macroHolder.macroBlock, "Failed to execute macro: " + macroHolder.macroBlock.getName(), e);
            getLogger().debug(
                "Failed to execute macro [" + macroHolder.macroBlock.getName() + "]. Internal error [" + e.getMessage()
                    + "]");
            return false;
        }

        // We wrap the blocks generated by the macro execution with MacroMarker blocks so that listeners/renderers
        // who wish to know the group of blocks that makes up the executed macro can. For example this is useful for
        // the XWiki Syntax renderer so that it can reconstruct the macros from the transformed XDOM.
        List<Block> resultBlocks = wrapInMacroMarker(macroHolder.macroBlock, newBlocks);

        // 4) Replace the MacroBlock by the Blocks generated by the execution of the Macro
        macroHolder.macroBlock.replace(resultBlocks);

        return true;
    }

    /**
     * Strip a single leading and trailing new line if any. We do this since we want that:
     * <pre><code>{{macro}}
     * content
     * {{/macro}}</code></pre>
     * be equivalent to:
     * <pre><code>{{macro}}content{{/macro}}</code></pre>
     * This is to make it easier for the user to not introduce significant new lines by error.
     * 
     * @param originalContent the content to normalize
     * @return the normalized string
     * @todo move this method in a generic xwiki-text or xwiki-string module
     */
    private String normalizeContent(String originalContent)
    {
        String normalizedContent = originalContent;
        if (normalizedContent != null && normalizedContent.length() > 0) {

            // Remove leading New Line
            if (normalizedContent.charAt(0) == '\n') {
                normalizedContent = normalizedContent.substring(1);
            } else if (normalizedContent.length() > 1 && normalizedContent.charAt(0) == '\r' 
                && normalizedContent.charAt(1) == '\n') 
            {
                normalizedContent = normalizedContent.substring(2);
            } else if (normalizedContent.charAt(0) == '\r') {
                normalizedContent = normalizedContent.substring(1);
            }
            
            // Remove trailing New Line
            normalizedContent = StringUtils.chomp(normalizedContent);
        }
        
        return normalizedContent;
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
                Macro< ? > macro = this.macroFactory.getMacro(macroBlock.getName(), syntax);
                macroHolders.add(new MacroHolder(macro, macroBlock));
            } catch (MacroNotFoundException e) {
                // Macro cannot be found. Generate an error message instead of the macro execution result.
                // TODO: make it internationalized
                generateError(macroBlock, "Unknown macro: " + macroBlock.getName(), "The \"" + macroBlock.getName()
                    + "\" macro is not in the list of registered macros. Verify the "
                    + "spelling or contact your administrator.");
                getLogger().debug("Failed to locate macro [" + macroBlock.getName() + "]. Ignoring it.");
            }
        }

        // Sort the Macros by priority
        Collections.sort(macroHolders);

        return macroHolders.size() > 0 ? macroHolders.get(0) : null;
    }

    private List<Block> wrapInMacroMarker(MacroBlock macroBlockToWrap, List<Block> newBlocks)
    {
        return Collections.singletonList((Block) new MacroMarkerBlock(macroBlockToWrap.getName(), macroBlockToWrap
            .getParameters(), macroBlockToWrap.getContent(), newBlocks, macroBlockToWrap.isInline()));
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
            errorBlocks.add(new FormatBlock(Arrays.asList(descriptionBlock), Format.NONE, 
                errorDescriptionBlockParams));
        } else {
            errorBlocks.add(new GroupBlock(Arrays.<Block> asList(new WordBlock(message)), errorBlockParams));
            errorBlocks.add(new GroupBlock(Arrays.asList(descriptionBlock), errorDescriptionBlockParams));
        }

        macroToReplace.replace(wrapInMacroMarker(macroToReplace, errorBlocks));
    }

    private void generateError(MacroBlock macroToReplace, String message, Throwable throwable)
    {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        generateError(macroToReplace, message, writer.getBuffer().toString());
    }
}
