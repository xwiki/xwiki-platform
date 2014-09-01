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
package org.xwiki.lesscss;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * This component provides a LESS preprocessor for the generation of CSS files from LESS sources located in the current
 * skin directory.
 *
 * This component must cache the outputs of compilation in an instance of {@link org.xwiki.lesscss.LESSSkinFileCache}.
 *
 * @since 6.1M1
 * @version $Id$
 */
@Role
@Unstable
public interface LESSSkinFileCompiler
{
    /**
     * Compile a LESS file located in the "less" directory of the current skin directory.
     * Velocity will also be parsed on the file, but not on the files included via the @import directive.
     * Since the result is cached, do not put velocity code that needs to be always executed, unless you use the force
     * parameter.
     *
     * @param fileName name of the file to compile
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the generated CSS
     * @throws LESSCompilerException if problems occur
     */
    String compileSkinFile(String fileName, boolean force) throws LESSCompilerException;

    /**
     * Compile a LESS file located in the "less" directory of the specified skin directory.
     * Velocity will also be parsed on the file, but not on the files included via the @import directive.
     * Since the result is cached, do not put velocity code that needs to be always executed, unless you use the force
     * parameter.
     *
     * @param fileName name of the file to compile
     * @param skin name of the skin where the LESS file is located
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the generated CSS
     * @throws LESSCompilerException if problems occur
     */
    String compileSkinFile(String fileName, String skin, boolean force) throws LESSCompilerException;
}
