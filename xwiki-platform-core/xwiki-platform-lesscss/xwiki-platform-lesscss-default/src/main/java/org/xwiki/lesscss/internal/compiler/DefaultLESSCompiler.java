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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;

/**
 * Default implementation for {@link LESSCompiler}. Actually use the good implementation depending on the configuration.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLESSCompiler implements LESSCompiler
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private ConfigurationSource configurationSource;

    private LESSCompiler getLessCompiler() throws LESSCompilerException
    {
        String compilerName = configurationSource.getProperty("less.compiler", "less4j");
        try {
            return componentManager.getInstance(LESSCompiler.class, compilerName);
        } catch (ComponentLookupException e) {
            throw new LESSCompilerException(String.format("Unable to get the LESS Compiler component [%s].",
                    compilerName), e);
        }
    }

    @Override
    public String compile(String lessCode) throws LESSCompilerException
    {
        LESSCompiler lessCompiler = getLessCompiler();
        return lessCompiler.compile(lessCode);
    }

    @Override
    public String compile(String lessCode, Path[] includePaths) throws LESSCompilerException
    {
        LESSCompiler lessCompiler = getLessCompiler();
        return lessCompiler.compile(lessCode, includePaths);
    }
}
