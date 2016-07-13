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
package org.xwiki.lesscss.internal.colortheme.converter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.cache.AbstractCachedCompiler;
import org.xwiki.lesscss.internal.cache.ColorThemeCache;
import org.xwiki.lesscss.internal.colortheme.ColorTheme;
import org.xwiki.lesscss.internal.colortheme.LESSColorThemeConverter;
import org.xwiki.lesscss.resources.LESSResourceReferenceFactory;

/**
 * Default implementation of {@link org.xwiki.lesscss.internal.colortheme.LESSColorThemeConverter}.
 *
 * @since 7.0RC1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLESSColorThemeConverter extends AbstractCachedCompiler<ColorTheme>
        implements LESSColorThemeConverter, Initializable
{
    private static final ColorTheme EMPTY_COLOR_THEME = new ColorTheme();
    
    @Inject
    private ColorThemeCache cache;

    @Inject
    private CachedLESSColorThemeConverter cachedLESSColorThemeConverter;
    
    @Inject
    private LESSResourceReferenceFactory lessResourceReferenceFactory;

    @Override
    public void initialize() throws InitializationException
    {
        super.cache = cache;
        super.compiler = cachedLESSColorThemeConverter;
    }

    @Override
    public ColorTheme getColorThemeFromSkinFile(String fileName, boolean force) throws LESSCompilerException
    {
        return super.getResult(lessResourceReferenceFactory.createReferenceForSkinFile(fileName), false, true, force);
    }

    @Override
    public ColorTheme getColorThemeFromSkinFile(String fileName, String skin, boolean force)
        throws LESSCompilerException
    {
        return super.getResult(lessResourceReferenceFactory.createReferenceForSkinFile(fileName), false, true, skin,
            force);
    }

    @Override
    protected ColorTheme cloneResult(ColorTheme toClone)
    {
        return new ColorTheme(toClone);
    }

    @Override
    protected ColorTheme exceptionAsResult(LESSCompilerException exception)
    {
        return EMPTY_COLOR_THEME;
    }
}
