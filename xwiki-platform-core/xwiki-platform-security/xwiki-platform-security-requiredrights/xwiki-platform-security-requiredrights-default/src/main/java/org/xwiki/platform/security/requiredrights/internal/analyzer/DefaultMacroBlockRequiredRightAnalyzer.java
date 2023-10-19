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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.script.ScriptMacro;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Default analyzer for macro blocks. Recurses into the macro content if it is wiki syntax.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
public class DefaultMacroBlockRequiredRightAnalyzer extends AbstractMacroBlockRequiredRightAnalyzer
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named(ScriptMacroAnalyzer.ID)
    private RequiredRightAnalyzer<MacroBlock> scriptMacroAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(MacroBlock macroBlock) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> result;

        try {
            // Check if there is an analyzer specifically for this macro, if yes, use it.
            RequiredRightAnalyzer<MacroBlock> specificAnalyzer =
                this.componentManagerProvider.get().getInstance(
                    new DefaultParameterizedType(null, RequiredRightAnalyzer.class, MacroBlock.class),
                    macroBlock.getId()
                );
            result = specificAnalyzer.analyze(macroBlock);
        } catch (ComponentLookupException e) {
            // No specific analyzer found, get more information about the macro.
            MacroTransformationContext transformationContext = getTransformationContext(macroBlock);
            Macro<?> macro = getMacro(macroBlock, transformationContext);

            if (macro instanceof ScriptMacro) {
                result = this.scriptMacroAnalyzer.analyze(macroBlock);
            } else if (macro != null && this.shouldMacroContentBeParsed(macro)) {
                try {
                    result = analyzeMacroContent(macroBlock, transformationContext);
                } catch (MacroExecutionException e1) {
                    // TODO: should we really throw an exception here? Or just log a warning or add an analysis
                    //  result if the author doesn't have programming right?
                    throw new RequiredRightsException("Error while parsing macro content for finding nested macros",
                        e1);
                }
            } else {
                result = List.of();
            }
        }

        return result;
    }

    private boolean shouldMacroContentBeParsed(Macro<?> macro)
    {
        ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
        return contentDescriptor != null && Block.LIST_BLOCK_TYPE.equals(contentDescriptor.getType());
    }
}
