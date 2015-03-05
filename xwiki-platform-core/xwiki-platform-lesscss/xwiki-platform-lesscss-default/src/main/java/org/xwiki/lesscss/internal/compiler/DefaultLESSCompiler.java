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

import java.nio.file.Path;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.compiler.less4j.FileLESSSource;

import com.github.sommeri.less4j.Less4jException;
import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.core.DefaultLessCompiler;
import com.github.sommeri.less4j.core.problems.BugHappened;

/**
 * Implementation of {@link LESSCompiler} that uses https://github.com/SomMeri/less4j, a LESS compiler written in Java
 * for better performances.
 *
 * @since 7.0RC1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLESSCompiler implements LESSCompiler
{
    private static final String ERROR_MESSAGE = "Failed to compile the LESS code: [%s]";

    @Override
    public String compile(String lessCode) throws LESSCompilerException
    {
        LessCompiler lessCompiler = new DefaultLessCompiler();
        LessCompiler.Configuration options = new LessCompiler.Configuration();
        options.setCompressing(true);
        try {
            LessCompiler.CompilationResult result = lessCompiler.compile(lessCode, options);
            return result.getCss();
        } catch (Less4jException e) {
            throw new LESSCompilerException(ERROR_MESSAGE, e);
        }
    }

    @Override
    public String compile(String lessCode, Path[] includePaths) throws LESSCompilerException
    {
        LessCompiler lessCompiler = new DefaultLessCompiler();
        LessCompiler.Configuration options = new LessCompiler.Configuration();
        options.setCompressing(true);
        try {
            FileLESSSource source = new FileLESSSource(lessCode, includePaths);
            LessCompiler.CompilationResult result = lessCompiler.compile(source, options);
            return result.getCss();
        } catch (Less4jException | BugHappened e) {
            throw new LESSCompilerException(String.format(ERROR_MESSAGE, e.getMessage()), e);
        }
    }


}
