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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightsAnalyzer;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.context.ContextMacroParameters;

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

            // Analyze the content only when it isn't restricted.
            if (!parameters.isRestricted()) {
                reporter.analyzeContent(macroBlock, macroBlock.getContent());
            }
        } catch (PropertyException e) {
            // Ignore, the macro won't be executed when populating the parameters fails.
        }
    }
}
