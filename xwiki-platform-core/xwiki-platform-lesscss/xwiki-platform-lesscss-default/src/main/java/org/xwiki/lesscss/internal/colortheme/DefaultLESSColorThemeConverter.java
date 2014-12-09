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
package org.xwiki.lesscss.internal.colortheme;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.ColorTheme;
import org.xwiki.lesscss.ColorThemeCache;
import org.xwiki.lesscss.LESSColorThemeConverter;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.lesscss.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.cache.AbstractCachedCompiler;

/**
 * Default implementation of {@link org.xwiki.lesscss.LESSColorThemeConverter}.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLESSColorThemeConverter extends AbstractCachedCompiler<ColorTheme>
        implements LESSColorThemeConverter, Initializable
{
    @Inject
    private ColorThemeCache cache;

    @Inject
    private CachedLESSColorThemeConverter cachedLESSColorThemeConverter;

    @Override
    public void initialize() throws InitializationException
    {
        super.cache = cache;
        super.compiler = cachedLESSColorThemeConverter;
    }

    @Override
    public ColorTheme getColorThemeFromSkinFile(String fileName, boolean force) throws LESSCompilerException
    {
        return super.getResult(new LESSSkinFileResourceReference(fileName), false, force);
    }

    @Override
    public ColorTheme getColorThemeFromSkinFile(String fileName, String skin, boolean force)
        throws LESSCompilerException
    {
        return super.getResult(new LESSSkinFileResourceReference(fileName), false, skin, force);
    }

}
