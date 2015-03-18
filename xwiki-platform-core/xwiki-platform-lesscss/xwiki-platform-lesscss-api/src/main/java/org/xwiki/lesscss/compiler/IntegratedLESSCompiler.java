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
package org.xwiki.lesscss.compiler;

import org.xwiki.component.annotation.Role;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.stability.Unstable;

/**
 * The Integrated LESS Compiler is a LESS compiler that take care of the wiki context (skin and color themes) to
 * generate the appropriate CSS output.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Role
@Unstable
public interface IntegratedLESSCompiler
{
    /**
     * Compile a LESS resource.
     * Velocity will also be parsed on the resource, but not on the files included via the @import directive.
     * Since the result is cached, do not put velocity code that needs to be always executed, unless you use the force
     * parameter.
     *
     * @param lessResourceReference reference of the LESS resource to compile
     * @param includeSkinStyle include the main LESS file of the skin in order to have variables and mix-ins
     * defined there
     * @param useVelocity either or not the resource be parsed by Velocity before compiling it
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the generated CSS
     * @throws LESSCompilerException if problems occur
     */
    String compile(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
        boolean force) throws LESSCompilerException;

    /**
     * Compile a LESS resource.
     * Velocity will also be parsed on the resource, but not on the files included via the @import directive.
     * Since the result is cached, do not put velocity code that needs to be always executed, unless you use the force
     * parameter.
     *
     * @param lessResourceReference reference of the LESS resource to compile
     * @param includeSkinStyle include the main LESS file of the skin in order to have variables and mix-ins
     * defined there
     * @param skin name of the skin where the LESS file is located
     * @param useVelocity either or not the resource be parsed by Velocity before compiling it
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the generated CSS
     * @throws LESSCompilerException if problems occur
     */
    String compile(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
        String skin, boolean force) throws LESSCompilerException;
}
