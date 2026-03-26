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

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.cache.AbstractCachedCompiler;
import org.xwiki.lesscss.internal.cache.LESSResourcesCache;
import org.xwiki.lesscss.resources.LESSResourceReference;

/**
 * Default implementation for {@link org.xwiki.lesscss.compiler.LESSCompiler}. It uses the CachedIntegratedLESSCompiler
 * through the AbstractCachedCompiler to cache the result of the compilation.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLESSCompiler extends AbstractCachedCompiler<String> implements LESSCompiler,
    Initializable
{
    @Inject
    private LESSResourcesCache lessResourcesCache;

    @Inject
    private CachedLESSCompiler cachedLESSCompiler;

    @Override
    public void initialize() throws InitializationException
    {
        super.cache = this.lessResourcesCache;
        super.compiler = this.cachedLESSCompiler;
    }

    @Override
    public String compile(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
                          boolean force) throws LESSCompilerException
    {
        return super.getResult(lessResourceReference, includeSkinStyle, useVelocity, force);
    }

    @Override
    public String compile(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
                         String skin, boolean force) throws LESSCompilerException
    {
        return super.getResult(lessResourceReference, includeSkinStyle, useVelocity, skin, force);
    }

    @Override
    protected String cloneResult(String toClone)
    {
        return new String(toClone);
    }

    @Override
    protected String exceptionAsResult(LESSCompilerException exception)
    {
        StringWriter serializedException = new StringWriter();
        exception.printStackTrace(new PrintWriter(serializedException));
        return String.format("/* %s */", serializedException.toString());
    }
}
