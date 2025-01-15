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
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightsAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.script.ScriptMacro;

/**
 * Default analyzer for macro blocks. Recurses into the macro content if it is wiki syntax.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
public class DefaultMacroBlockRequiredRightAnalyzer extends AbstractMacroBlockRequiredRightAnalyzer
    implements RequiredRightAnalyzer<MacroBlock>
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named(ScriptMacroAnalyzer.ID)
    private RequiredRightAnalyzer<MacroBlock> scriptMacroAnalyzer;

    @Inject
    private Logger logger;

    @Inject
    private Provider<DefaultMacroRequiredRightReporter> macroRequiredRightReporterProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(MacroBlock macroBlock)
    {
        List<RequiredRightAnalysisResult> result;

        try {
            result = analyzeWithExceptions(macroBlock);
        } catch (Exception e) {
            result = List.of(reportAnalysisError(macroBlock, e));
        }

        return result;
    }

    private List<RequiredRightAnalysisResult> analyzeWithExceptions(MacroBlock macroBlock)
        throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> result;

        Optional<RequiredRightAnalyzer<MacroBlock>>
            specificAnalyzer = getMacroBlockRequiredRightAnalyzer(macroBlock);

        if (specificAnalyzer.isPresent()) {
            result = specificAnalyzer.get().analyze(macroBlock);
        } else {
            // Check if there is a macro analyzer for this macro, if yes, use it.
            Optional<MacroRequiredRightsAnalyzer> macroAnalyzer = getMacroAnalyzer(macroBlock.getId());

            if (macroAnalyzer.isPresent()) {
                DefaultMacroRequiredRightReporter reporter = this.macroRequiredRightReporterProvider.get();
                macroAnalyzer.get().analyze(macroBlock, reporter);
                result = reporter.getResults();
            } else {
                // No specific analyzer found, get more information about the macro.
                Macro<?> macro = getMacro(macroBlock);

                if (macro instanceof ScriptMacro) {
                    result = this.scriptMacroAnalyzer.analyze(macroBlock);
                } else if (macro != null && this.shouldMacroContentBeParsed(macro)) {
                    result = analyzeMacroContent(macroBlock, macroBlock.getContent());
                } else {
                    result = List.of();
                }
            }
        }

        return result;
    }

    private Optional<RequiredRightAnalyzer<MacroBlock>> getMacroBlockRequiredRightAnalyzer(MacroBlock macroBlock)
    {
        String macroId = macroBlock.getId();
        DefaultParameterizedType roleType =
            new DefaultParameterizedType(null, RequiredRightAnalyzer.class, MacroBlock.class);
        ComponentManager componentManager = this.componentManagerProvider.get();

        try {
            // Check if there is an analyzer specifically for this macro, if yes, use it.
            // Don't try loading the "default" analyzer as this would cause an endless recursion.
            // For the "default" macro, a MacroRequiredRightsAnalyzer component should be created if needed.
            if (!"default".equals(macroId) && componentManager.hasComponent(roleType, macroId)) {
                return Optional.of(componentManager.getInstance(roleType, macroId));
            }
        } catch (ComponentLookupException e) {
            this.logger.warn(
                "The macro block required rights analyzer for macro [{}] failed to be initialized, root cause: [{}]",
                macroId, ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Full exception trace: ", e);
        }

        return Optional.empty();
    }

    private Optional<MacroRequiredRightsAnalyzer> getMacroAnalyzer(String id)
    {
        try {
            return Optional.of(this.componentManagerProvider.get().getInstance(MacroRequiredRightsAnalyzer.class, id));
        } catch (ComponentLookupException e) {
            return Optional.empty();
        }
    }

    private boolean shouldMacroContentBeParsed(Macro<?> macro)
    {
        ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
        return contentDescriptor != null && Block.LIST_BLOCK_TYPE.equals(contentDescriptor.getType());
    }
}
