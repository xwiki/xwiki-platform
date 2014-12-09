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
package org.xwiki.lesscss.internal.compiler;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.IntegratedLESSCompiler;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.lesscss.LESSSkinFileCompiler;
import org.xwiki.lesscss.LESSSkinFileResourceReference;

/**
 * Default implementation for {@link org.xwiki.lesscss.LESSSkinFileCompiler}.
 *
 * @since 6.4M2
 * @version $Id$
 * @deprecated use {@link }DefaultIntegratedLESSCompiler} instead
 */
@Component
@Singleton
@Deprecated
public class DefaultLESSSkinFileCompiler implements LESSSkinFileCompiler
{
    @Inject
    private IntegratedLESSCompiler integratedLESSCompiler;

    @Override
    public String compileSkinFile(String fileName, boolean force) throws LESSCompilerException
    {
        return integratedLESSCompiler.compile(new LESSSkinFileResourceReference(fileName), false, force);
    }

    @Override
    public String compileSkinFile(String fileName, String skin, boolean force) throws LESSCompilerException
    {
        return integratedLESSCompiler.compile(new LESSSkinFileResourceReference(fileName), false, skin, force);
    }
}
