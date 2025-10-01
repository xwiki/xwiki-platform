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
package org.xwiki.rendering.internal.macro.context;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightsAnalyzer;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;

/**
 * Required rights analyzer for the context macro.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Component
@Named("context")
@Singleton
public class ContextMacroRequiredRightsAnalyzer implements MacroRequiredRightsAnalyzer
{
    @Inject
    private BeanManager beanManager;

    @Override
    public void analyze(MacroBlock macroBlock, MacroRequiredRightReporter reporter)
    {
        ContextMacroParameters parameters = new ContextMacroParameters();

        try {
            this.beanManager.populate(parameters, macroBlock.getParameters());

            // If the source parameter is set, the content is ignored, and we should analyze the source parameter
            // instead.
            if (parameters.getSource() != null) {
                String sourceType = parameters.getSource().getType();

                switch (sourceType) {
                    case MacroContentSourceReference.TYPE_STRING:
                        if (!parameters.isRestricted()) {
                            reporter.analyzeContent(macroBlock, parameters.getSource().getReference());
                        }
                        break;

                    case MacroContentSourceReference.TYPE_SCRIPT:
                        if (parameters.isRestricted()) {
                            reporter.report(macroBlock, List.of(MacroRequiredRight.SCRIPT),
                                "rendering.macro.context.requiredRights.restrictedScriptSource");
                        } else {
                            // We don't know the actual content, but at least script right is needed and the content
                            // could contain anything, so it might require programming right.
                            reporter.report(macroBlock,
                                List.of(MacroRequiredRight.SCRIPT, MacroRequiredRight.MAYBE_PROGRAM),
                                "rendering.macro.context.requiredRights.arbitraryScriptSource");
                        }
                        break;

                    default:
                        // Do nothing.
                }
            } else if (!parameters.isRestricted()) {
                reporter.analyzeContent(macroBlock, macroBlock.getContent());
            }
        } catch (PropertyException e) {
            // Ignore, the macro won't be executed when populating the parameters fails.
        }
    }
}
