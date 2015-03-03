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
package org.xwiki.lesscss.internal.cache;

import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.stability.Unstable;

/**
 * A compiler that the AbstractCachedCompiler can use when the expected result of the compilation is not in the cache.
 * @param <T> class of the expected results
 *
 * @since 6.4M2
 * @version $Id$
 */
@Unstable
public interface CachedCompilerInterface<T>
{
    /**
     * Compute the compilation.
     * @param lessResourceReference reference to the LESS resource to compile
     * @param includeSkinStyle include the main LESS file of the skin in order to have variables and mix-ins
     * @param useVelocity either or not the resource be parsed by Velocity before compiling it
     * defined there
     * @param useLESS either or not the resource be compiled by the LESS compiler
     * @param skin skin in used for the compilation
     * @return the result of the compilation of the LESS Resource
     * @throws LESSCompilerException if problem occurs
     */
    T compute(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
        boolean useLESS, String skin) throws LESSCompilerException;
}
