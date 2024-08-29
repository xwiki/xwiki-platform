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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Default implementation of {@link MacroRequiredRightReporter}.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Component(roles = DefaultMacroRequiredRightReporter.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultMacroRequiredRightReporter extends AbstractMacroBlockRequiredRightAnalyzer
    implements MacroRequiredRightReporter
{
    private final List<RequiredRightAnalysisResult> results = new ArrayList<>();

    @Override
    public void report(MacroBlock macroBlock, List<MacroRequiredRight> requiredRights, String summaryTranslationKey,
        Object... translationParameters)
    {
        this.results.add(new RequiredRightAnalysisResult(
            extractSourceReference(macroBlock),
            this.translationBlockSupplierProvider.get(summaryTranslationKey, translationParameters),
            this.macroBlockBlockSupplierProvider.get(macroBlock),
            requiredRights.stream().map(this::translateMacroRequiredRight).collect(Collectors.toList())
        ));
    }

    private RequiredRight translateMacroRequiredRight(MacroRequiredRight macroRequiredRight)
    {
        RequiredRight requiredRight;

        switch (macroRequiredRight) {
            case PROGRAM:
                requiredRight = RequiredRight.PROGRAM;
                break;
            case MAYBE_PROGRAM:
                requiredRight = RequiredRight.MAYBE_PROGRAM;
                break;
            case SCRIPT:
                requiredRight = RequiredRight.SCRIPT;
                break;
            case MAYBE_SCRIPT:
                requiredRight = RequiredRight.MAYBE_SCRIPT;
                break;
            default:
                requiredRight = null;
        }

        return requiredRight;
    }

    @Override
    public void analyzeContent(MacroBlock macroBlock, String content)
    {
        analyzeContent(macroBlock, content, null);
    }

    @Override
    public void analyzeContent(MacroBlock macroBlock, String content, Syntax syntax)
    {
        if (StringUtils.isNotBlank(content)) {
            try {
                this.results.addAll(analyzeMacroContent(macroBlock, content, syntax));
            } catch (RequiredRightsException e) {
                this.results.add(reportAnalysisError(macroBlock, e));
            }
        }
    }

    /**
     * @return the collected results
     */
    public List<RequiredRightAnalysisResult> getResults()
    {
        return this.results;
    }
}
