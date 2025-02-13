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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.MacroParameterRequiredRightsAnalyzer;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;

/**
 * Required rights analyzer for wiki syntax parameters.
 *
 * @version $Id$
 * @since 17.1.0RC1
 * @since 16.10.4
 * @since 16.4.7
 */
@Component
@Singleton
public class WikiSyntaxMacroParameterRequiredRightsAnalyzer implements MacroParameterRequiredRightsAnalyzer<List<Block>>
{
    @Override
    public void analyze(MacroBlock macroBlock, ParameterDescriptor parameterDescriptor, String value,
        MacroRequiredRightReporter reporter)
    {
        reporter.analyzeContent(macroBlock, value);
    }
}
