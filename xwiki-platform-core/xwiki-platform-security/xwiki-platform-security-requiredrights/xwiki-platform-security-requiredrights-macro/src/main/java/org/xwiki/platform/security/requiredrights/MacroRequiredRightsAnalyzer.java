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
package org.xwiki.platform.security.requiredrights;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.MacroBlock;

/**
 * Analyzes a macro to determine if it requires specific rights.
 * <p>
 * This is a simplified version of {@code RequiredRightAnalyzer} that only analyzes macro blocks with the aim of
 * making it easy to write analyzers for macros that don't have any dependencies apart from the rendering API and
 * this module.
 * </p>
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Role
public interface MacroRequiredRightsAnalyzer
{
    /**
     * Analyzes the given macro block and reports the required rights to the given reporter.
     *
     * @param macroBlock the macro block to analyze
     * @param reporter the reporter to report the required rights to
     */
    void analyze(MacroBlock macroBlock, MacroRequiredRightReporter reporter);
}
