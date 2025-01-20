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
package org.xwiki.rendering.internal.macro.code;

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
import org.xwiki.rendering.macro.code.CodeMacroParameters;

import static org.xwiki.rendering.macro.source.MacroContentSourceReference.TYPE_SCRIPT;

/**
 * Required rights analyzer for the code macro.
 *
 * @version $Id$
 * @since 16.4.7
 * @since 16.10.3
 * @since 17.0.0
 */
@Component
@Singleton
@Named("code")
public class CodeMacroRequiredRightsAnalyzer implements MacroRequiredRightsAnalyzer
{
    @Inject
    private BeanManager beanManager;

    @Override
    public void analyze(MacroBlock macroBlock, MacroRequiredRightReporter reporter)
    {
        CodeMacroParameters parameters = new CodeMacroParameters();

        try {
            this.beanManager.populate(parameters, macroBlock.getParameters());

            if (parameters.getSource() != null && TYPE_SCRIPT.equals(parameters.getSource().getType())) {
                reporter.report(macroBlock, List.of(MacroRequiredRight.SCRIPT),
                    "rendering.macro.code.requiredRights.scriptSource");
            }
        } catch (PropertyException e) {
            // Ignore, the macro won't be executed when populating the parameters fails.
        }
    }
}
