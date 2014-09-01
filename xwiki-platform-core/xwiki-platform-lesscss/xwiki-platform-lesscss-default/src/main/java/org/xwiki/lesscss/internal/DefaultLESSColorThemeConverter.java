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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.ColorTheme;
import org.xwiki.lesscss.ColorThemeCache;
import org.xwiki.lesscss.LESSColorThemeConverter;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.lesscss.LESSSkinFileCompiler;

/**
 * Default implementation of {@link org.xwiki.lesscss.LESSColorThemeConverter}.
 *
 * @since 6.1M2
 * @version $Id$
 */
@Component
public class DefaultLESSColorThemeConverter extends AbstractCachedCompiler<ColorTheme>
        implements LESSColorThemeConverter, Initializable
{
    @Inject
    private LESSSkinFileCompiler lessSkinFileCompiler;

    @Inject
    private ColorThemeCache cache;

    private final Pattern pattern = Pattern.compile("\\.colortheme-(\\w+)[\\s$]*\\{(\\w+):(#*\\w+)\\}");

    @Override
    public void initialize() throws InitializationException
    {
        super.cache = cache;
    }

    @Override
    public ColorTheme getColorThemeFromSkinFile(String fileName, boolean force) throws LESSCompilerException
    {
        return this.compileFromSkinFile(fileName, force);
    }

    @Override
    public ColorTheme getColorThemeFromSkinFile(String fileName, String skin, boolean force)
        throws LESSCompilerException
    {
        return this.compileFromSkinFile(fileName, skin, force);
    }

    /**
     * Parse a CSS and returns a Color Theme.
     * @param css code to parse
     * @return the corresponding color theme
     */
    public ColorTheme getColorThemeFromCSS(String css)
    {
        ColorTheme results = new ColorTheme();
        String cssWithoutComments = css.replaceAll("/\\*[\\u0000-\\uffff]*?\\*/", "");
        Matcher m = pattern.matcher(cssWithoutComments);
        while (m.find()) {
            String variable = m.group(1);
            String value = m.group(3);
            results.put(variable, value);
        }
        return results;
    }

    @Override
    protected ColorTheme compile(String fileName, String skin, boolean force) throws LESSCompilerException
    {
        return getColorThemeFromCSS(lessSkinFileCompiler.compileSkinFile(fileName, skin, false));
    }

}
