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
package org.xwiki.lesscss.internal;

import java.nio.file.Path;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.LESSCompiler;
import org.xwiki.lesscss.LESSCompilerException;

/**
 * Implementation of {@link LESSCompiler} that uses https://github.com/SomMeri/less4j, a LESS compiler written in Java
 * for better performances.
 *
 * @since 6.3M1
 * @version $Id$
 */
@Component
@Named("less4j")
public class LESS4jCompiler implements LESSCompiler
{
    @Override
    public String compile(String lessCode) throws LESSCompilerException
    {
        return null;
    }

    @Override
    public String compile(String lessCode, Path[] includePaths) throws LESSCompilerException
    {
        return null;
    }
}
