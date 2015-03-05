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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.colortheme.ColorTheme;
import org.xwiki.lesscss.compiler.IntegratedLESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.internal.cache.CachedCompilerInterface;

/**
 * Computes a color theme corresponding to a LESS file. To be used with AbstractCachedCompiler.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component(roles = CachedLESSColorThemeConverter.class)
@Singleton
public class CachedLESSColorThemeConverter implements CachedCompilerInterface<ColorTheme>
{
    @Inject
    private IntegratedLESSCompiler lessCompiler;

    private final Pattern pattern = Pattern.compile("\\.colortheme-(\\w+)[\\s$]*\\{(\\w+):(#*\\w+)\\}");

    @Override
    public ColorTheme compute(LESSResourceReference lessResourceReference, boolean includeSkinStyle,
        boolean useVelocity, boolean useLESS, String skin)
        throws LESSCompilerException
    {
        return getColorThemeFromCSS(lessCompiler.compile(lessResourceReference, false, useVelocity, skin, false));
    }

    /**
     * Parse a CSS and returns a Color Theme.
     * @param css code to parse
     * @return the corresponding color theme
     */
    private ColorTheme getColorThemeFromCSS(String css)
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
}
