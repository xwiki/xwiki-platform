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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightsAnalyzer;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.SyntaxRegistry;
import org.xwiki.rendering.syntax.SyntaxType;

/**
 * Analyzes the required rights of the raw macro and reports script right for HTML content.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Component
@Singleton
@Named("raw")
public class RawMacroRequiredRightsAnalyzer implements MacroRequiredRightsAnalyzer
{
    @Inject
    private SyntaxRegistry syntaxRegistry;

    @Override
    public void analyze(MacroBlock macroBlock, MacroRequiredRightReporter reporter)
    {
        try {
            SyntaxType syntax = this.syntaxRegistry.resolveSyntax(macroBlock.getParameter("syntax")).getType();
            if (SyntaxType.HTML_FAMILY_TYPES.contains(syntax)) {
                reporter.report(macroBlock, List.of(MacroRequiredRight.SCRIPT),
                    "rendering.macro.rawMacroRequiredRights");
            }
        } catch (ParseException e) {
            // Ignore, this should fail the macro or at least won't produce HTML output.
        }
    }
}
