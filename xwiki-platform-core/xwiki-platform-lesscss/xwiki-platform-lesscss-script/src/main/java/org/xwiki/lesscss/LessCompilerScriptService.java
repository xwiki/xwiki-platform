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
package org.xwiki.lesscss;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.cache.ColorThemeCache;
import org.xwiki.lesscss.cache.LESSResourcesCache;
import org.xwiki.lesscss.colortheme.ColorTheme;
import org.xwiki.lesscss.colortheme.ColorThemeReference;
import org.xwiki.lesscss.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.colortheme.LESSColorThemeConverter;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.compiler.LESSSkinFileCompiler;
import org.xwiki.lesscss.skin.SkinReference;
import org.xwiki.lesscss.skin.SkinReferenceFactory;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * This script service provides a LESS preprocessor (http://lesscss.org/) for CSS generation.
 *
 * @since 6.1M1
 * @version $Id$
 */
@Component
@Named("lesscss")
@Singleton
@Unstable
public class LessCompilerScriptService implements ScriptService
{
    @Inject
    private LESSSkinFileCompiler lessCompiler;

    @Inject
    private LESSResourcesCache lessCache;

    @Inject
    private ColorThemeCache colorThemeCache;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private LESSColorThemeConverter lessColorThemeConverter;

    @Inject
    private SkinReferenceFactory skinReferenceFactory;

    @Inject
    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Compile a LESS file located in the "less" directory of the current skin directory.
     * Velocity will also be parsed on the file, but not on the files included via the @import directive.
     * The result is cached by XWiki until the skin or the color theme is changed.
     *
     * @param fileName name of the file to compile
     * @return the generated CSS, or an error message if some problem occurs
     */
    public String compileSkinFile(String fileName)
    {
        return compileSkinFile(fileName, false);
    }

    /**
     * Compile a LESS file located in the "less" directory of the current skin directory.
     * Velocity will also be parsed on the file, but not on the files included via the @import directive.
     * The result is cached by XWiki until the skin or the color theme is changed.
     *
     * @param fileName name of the file to compile
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the generated CSS, or an error message if some problem occurs
     */
    public String compileSkinFile(String fileName, boolean force)
    {
        try {
            return lessCompiler.compileSkinFile(fileName, force);
        } catch (LESSCompilerException e) {
            return ExceptionUtils.getRootCauseMessage(e);
        }
    }

    /**
     * Compile a LESS file located in the "less" directory of the specified skin directory.
     * Velocity will also be parsed on the file, but not on the files included via the @import directive.
     * The result is cached by XWiki until the skin or the color theme is changed.
     *
     * @param fileName name of the file to compile
     * @param skin name of the skin where the LESS file is located
     * @return the generated CSS, or an error message if some problem occurs
     */
    public String compileSkinFile(String fileName, String skin)
    {
        return compileSkinFile(fileName, skin, false);
    }

    /**
     * Compile a LESS file located in the "less" directory of the specified skin directory.
     * Velocity will also be parsed on the file, but not on the files included via the @import directive.
     * The result is cached by XWiki until the skin or the color theme is changed.
     *
     * @param fileName name of the file to compile
     * @param skin name of the skin where the LESS file is located
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the generated CSS, or an error message if some problem occurs
     */
    public String compileSkinFile(String fileName, String skin, boolean force)
    {
        try {
            return lessCompiler.compileSkinFile(fileName, skin, force);
        } catch (LESSCompilerException e) {
            return ExceptionUtils.getRootCauseMessage(e);
        }
    }

    /**
     * Return a color theme from a LESS file located in the "less" directory of the current skin.
     *
     * @param filename name of the LESS file
     * @return the corresponding Color Theme.
     */
    public ColorTheme getColorThemeFromSkinFile(String filename)
    {
        try {
            return lessColorThemeConverter.getColorThemeFromSkinFile(filename, false);
        } catch (LESSCompilerException e) {
            return new ColorTheme();
        }
    }

    /**
     * Return a color theme from a LESS file located in the "less" directory of the specified skin.
     *
     * @param filename name of the LESS file
     * @param skin name of the skin where the LESS file is located
     * @return the corresponding Color Theme.
     */
    public ColorTheme getColorThemeFromSkinFile(String filename, String skin)
    {
        try {
            return lessColorThemeConverter.getColorThemeFromSkinFile(filename, skin, false);
        } catch (LESSCompilerException e) {
            return new ColorTheme();
        }
    }

    /**
     * Remove every generated files from the XWiki cache. The script calling this method needs the programming rights.
     * @return true if the operation succeed
     */
    public boolean clearCache()
    {
        XWikiContext xcontext = xcontextProvider.get();

        // Check if the current script has the programing rights
        if (!authorizationManager.hasAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
            xcontext.getDoc().getDocumentReference())) {
            return false;
        }

        lessCache.clear();
        colorThemeCache.clear();
        return true;
    }

    /**
     * Remove every generated files corresponding to a color theme.
     * The script calling this method needs the programming rights.
     * @param colorTheme fullname of the color theme
     * @return true if the operation succeed
     */
    public boolean clearCacheFromColorTheme(String colorTheme)
    {
        XWikiContext xcontext = xcontextProvider.get();

        // Check if the current script has the programing rights
        if (!authorizationManager.hasAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                xcontext.getDoc().getDocumentReference())) {
            return false;
        }

        try {
            ColorThemeReference colorThemeReference = colorThemeReferenceFactory.createReference(colorTheme);
            lessCache.clearFromColorTheme(colorThemeReference);
            colorThemeCache.clearFromColorTheme(colorThemeReference);
            return true;
        } catch (LESSCompilerException e) {
            return false;
        }
    }

    /**
     * Remove every generated files corresponding to a filesystem skin.
     * The script calling this method needs the programming rights.
     * @param skin name of the filesystem skin
     * @return true if the operation succeed
     *
     * @since 6.4M2
     */
    public boolean clearCacheFromSkin(String skin)
    {
        XWikiContext xcontext = xcontextProvider.get();

        // Check if the current script has the programing rights
        if (!authorizationManager.hasAccess(Right.PROGRAM, xcontext.getDoc().getAuthorReference(),
                xcontext.getDoc().getDocumentReference())) {
            return false;
        }

        try {
            SkinReference skinReference = skinReferenceFactory.createReference(skin);
            lessCache.clearFromSkin(skinReference);
            colorThemeCache.clearFromSkin(skinReference);
            return true;
        } catch (LESSCompilerException e) {
            return false;
        }
    }
}
