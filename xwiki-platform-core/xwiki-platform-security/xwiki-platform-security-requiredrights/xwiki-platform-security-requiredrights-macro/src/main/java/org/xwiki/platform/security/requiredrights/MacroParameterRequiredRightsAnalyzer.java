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
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.stability.Unstable;

/**
 * An analyzer for a macro parameter.
 *
 * @param <T> the type of the macro parameter.
 * @version $Id$
 * @since 17.1.0RC1
 * @since 16.10.4
 * @since 16.4.7
 */
// The type parameter isn't used in the interface itself, but it is used to differentiate component implementations
// for different parameter types so it is definitely used.
@SuppressWarnings("unused")
@Role
@Unstable
public interface MacroParameterRequiredRightsAnalyzer<T>
{
    /**
     * Analyzes the given parameter value in the given macro block and reports the required rights to the given
     * reporter.
     *
     * @param macroBlock the macro block to analyze
     * @param parameterDescriptor the descriptor of the parameter being analyzed
     * @param value the value of the parameter being analyzed
     * @param reporter the reporter to report the required rights to
     */
    void analyze(MacroBlock macroBlock, ParameterDescriptor parameterDescriptor,
        String value, MacroRequiredRightReporter reporter);
}
