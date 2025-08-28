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
package org.xwiki.rendering.internal.macro;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightsAnalyzer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Required rights analyzer for content macro.
 *
 * @version $Id$
 * @since 16.4.7
 * @since 16.10.3
 * @since 17.0.0
 */
@Component
@Singleton
@Named("content")
public class ContentMacroRequiredRightsAnalyzer implements MacroRequiredRightsAnalyzer
{
    @Inject
    private ConverterManager converterManager;

    @Override
    public void analyze(MacroBlock macroBlock, MacroRequiredRightReporter reporter)
    {
        List<Syntax> contentSyntaxes = getParameterValues(macroBlock, Syntax.class, "syntax");
        List<MacroContentSourceReference> sources = getParameterValues(macroBlock, MacroContentSourceReference.class,
            "source");

        if (!sources.isEmpty()) {
            // If there are several sources, we don't know which one will win - just analyze all, having more than
            // one source isn't a real use case.
            for (MacroContentSourceReference source : sources) {
                if (MacroContentSourceReference.TYPE_SCRIPT.equals(source.getType())) {
                    reporter.report(macroBlock, List.of(MacroRequiredRight.SCRIPT, MacroRequiredRight.MAYBE_PROGRAM),
                        "rendering.macro.content.requiredRights.scriptSource");
                } else if (MacroContentSourceReference.TYPE_STRING.equals(source.getType())) {
                    analyzeContentWithSyntaxes(macroBlock, reporter, contentSyntaxes, source.getReference());
                }
            }
        } else {
            analyzeContentWithSyntaxes(macroBlock, reporter, contentSyntaxes, macroBlock.getContent());
        }
    }

    private static void analyzeContentWithSyntaxes(MacroBlock macroBlock, MacroRequiredRightReporter reporter,
        List<Syntax> contentSyntaxes, String content)
    {
        if (contentSyntaxes.isEmpty()) {
            reporter.analyzeContent(macroBlock, content);
        } else {
            // If there are several syntax parameters, we don't know which one will really be used, so analyze
            // with all to catch dangerous content in all syntaxes. In practice, there should be at most a single syntax
            // parameter.
            contentSyntaxes.forEach(syntax -> reporter.analyzeContent(macroBlock, content, syntax));
        }
    }

    private <T> List<T> getParameterValues(MacroBlock macroBlock, Class<T> tClass, String parameterName)
    {
        return macroBlock.getParameters().entrySet().stream()
            .filter(entry -> parameterName.equalsIgnoreCase(entry.getKey()))
            .map(entry -> {
                try {
                    return this.converterManager.<T>convert(tClass, entry.getValue());
                } catch (Exception e) {
                    // Ignore invalid values.
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
