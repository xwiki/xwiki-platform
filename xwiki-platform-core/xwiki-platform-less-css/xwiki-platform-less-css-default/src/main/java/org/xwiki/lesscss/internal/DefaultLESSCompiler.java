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

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import javax.inject.Singleton;

import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.LESSCompiler;
import org.xwiki.lesscss.LESSCompilerException;

/**
 * Default implementation of {@link LESSCompiler}.
 *
 * @since 6.1M1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLESSCompiler implements LESSCompiler, Initializable
{
    private static final String LESS_SOURCE_FILE = "/less-rhino-1.7.0.js";

    private static final String LESSC_SOURCE_FILE = "/lessc-rhino-1.7.0.js";

    private static final String ERROR_MESSAGE = "Error during the LESS processing.";

    /**
     * The thread-safe LESS compiler.
     */
    private LessCompiler lessCompiler;

    @Override
    public void initialize() throws InitializationException
    {
        lessCompiler = new LessCompiler();
        lessCompiler.setLessJs(getClass().getResource(LESS_SOURCE_FILE));
        lessCompiler.setLesscJs(getClass().getResource(LESSC_SOURCE_FILE));
        lessCompiler.setCompress(true);
        lessCompiler.init();
    }

    @Override
    public String compile(String lessCode) throws LESSCompilerException
    {
        try {
            return lessCompiler.compile(lessCode);
        } catch (LessException e) {
            throw new LESSCompilerException(ERROR_MESSAGE, e);
        }
    }

    @Override
    public String compile(String lessCode, Path[] includePaths) throws LESSCompilerException
    {
        LessCompiler compiler = this.lessCompiler;
        if (includePaths.length > 0) {
            StringBuilder paths = new StringBuilder("--include-path=");
            for (int i = 0; i < includePaths.length; ++i) {
                paths.append(includePaths[i].toString()).append(File.pathSeparator);
            }
            // We need to create a new instance of LessCompiler until
            // https://github.com/marceloverdijk/lesscss-java/issues/49 is fixed
            compiler = new LessCompiler(Arrays.<String>asList(paths.toString()));
            compiler.setLessJs(getClass().getResource(LESS_SOURCE_FILE));
            compiler.setLesscJs(getClass().getResource(LESSC_SOURCE_FILE));
            compiler.setCompress(true);
            compiler.init();
        }
        try {
            return compiler.compile(lessCode);
        } catch (LessException e) {
            throw new LESSCompilerException(ERROR_MESSAGE, e);
        }
    }
}
