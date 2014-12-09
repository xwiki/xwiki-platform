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
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.IntegratedLESSCompiler;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.LESSSkinFileCache;
import org.xwiki.lesscss.internal.cache.AbstractCachedCompiler;

/**
 * Default implementation for {@link org.xwiki.lesscss.IntegratedLESSCompiler}. It uses the CachedIntegratedLESSCompiler
 * through the AbstractCachedCompiler to cache the result of the compilation.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultIntegratedLESSCompiler extends AbstractCachedCompiler<String> implements IntegratedLESSCompiler,
        Initializable
{
    @Inject
    private LESSSkinFileCache cache;

    @Inject
    private CachedIntegratedLESSCompiler cachedIntegratedLESSCompiler;

    @Override
    public void initialize() throws InitializationException
    {
        super.cache = cache;
        super.compiler = cachedIntegratedLESSCompiler;
    }

    @Override
    public String compile(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean force)
        throws LESSCompilerException
    {
        return super.getResult(lessResourceReference, includeSkinStyle, force);
    }

    @Override
    public String compile(LESSResourceReference lessResourceReference, boolean includeSkinStyle, String skin,
            boolean force) throws LESSCompilerException
    {
        return super.getResult(lessResourceReference, includeSkinStyle, skin, force);
    }
}
