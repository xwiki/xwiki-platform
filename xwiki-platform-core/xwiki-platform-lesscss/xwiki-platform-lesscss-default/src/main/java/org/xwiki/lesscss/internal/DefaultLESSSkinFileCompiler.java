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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.lesscss.LESSCompiler;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.lesscss.LESSSkinFileCache;
import org.xwiki.lesscss.LESSSkinFileCompiler;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation for {@link org.xwiki.lesscss.LESSSkinFileCompiler}.
 *
 * @since 6.1M1
 * @version $Id$
 */
@Component
public class DefaultLESSSkinFileCompiler extends AbstractCachedCompiler<String> implements LESSSkinFileCompiler,
        Initializable
{
    private static final String SKIN_CONTEXT_KEY = "skin";

    @Inject
    private LESSCompiler lessCompiler;

    @Inject
    private LESSSkinFileCache cache;

    @Inject
    private SkinDirectoryGetter skinDirectoryGetter;

    @Override
    public void initialize() throws InitializationException
    {
        super.cache = cache;
    }

    @Override
    protected String compile(String fileName, String skin, boolean force) throws LESSCompilerException
    {
        // Get the XWiki object
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        String currentSkin = xwiki.getSkin(xcontext);

        try {
            // First, get the skin directory
            String path = getSkinDirectory(skin) +  "/less";
            String realPath = xwiki.getEngineContext().getRealPath(path);
            File lessDirectory = new File(realPath);
            if (!lessDirectory.exists() || !lessDirectory.isDirectory()) {
                throw new LESSCompilerException(String.format("The path [%s] is not a directory or does not exists.",
                        path));
            }
            Path lessFilesPath = Paths.get(xwiki.getEngineContext().getRealPath(path));
            Path[] includePaths = {lessFilesPath};

            // Get the file content
            String fullFileName = path + "/" + fileName;
            File lessFile = new File(xwiki.getEngineContext().getRealPath(fullFileName));
            if (!lessFile.exists() || !lessFile.isFile()) {
                throw new LESSCompilerException(String.format("The path [%s] is not a file or does not exists.",
                        fullFileName));
            }
            InputStream is = xwiki.getEngineContext().getResourceAsStream(fullFileName);
            StringWriter source = new StringWriter();
            IOUtils.copy(is, source);

            // Trick: change the current skin in order to compile the LESS file as if the specified skin
            // was the current skin
            if (!currentSkin.equals(skin)) {
                xcontext.put(SKIN_CONTEXT_KEY, skin);
            }

            // Parse the LESS content with Velocity
            String velocityParsedSource = xwiki.parseContent(source.toString(), xcontext);

            // Do not compile the LESS code if the result is already in the cache and we are performing an HTML export
            // (quick backport of http://jira.xwiki.org/browse/XWIKI-11731)
            if (lessContext.isHtmlExport()
                    && cache.get(fileName, skin, currentColorThemeGetter.getCurrentColorTheme("default")) != null) {
                return velocityParsedSource;
            } else {
                return lessCompiler.compile(velocityParsedSource, includePaths);
            }
        } catch (LESSCompilerException | IOException e) {
            throw new LESSCompilerException(String.format("Failed to compile the file [%s] with LESS.", fileName), e);
        } finally {
            // Reset the current skin to the old value
            if (!currentSkin.equals(skin)) {
                xcontext.put(SKIN_CONTEXT_KEY, currentSkin);
            }
        }
    }

    @Override
    public String compileSkinFile(String fileName, boolean force) throws LESSCompilerException
    {
        return this.compileFromSkinFile(fileName, force);
    }

    @Override
    public String compileSkinFile(String fileName, String skin, boolean force) throws LESSCompilerException
    {
        return this.compileFromSkinFile(fileName, skin, force);
    }

    private String getSkinDirectory(String skin) throws LESSCompilerException
    {
        return skinDirectoryGetter.getSkinDirectory(skin);
    }

}
