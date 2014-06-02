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
    @Inject
    private LESSCompiler lessCompiler;

    @Inject
    private LESSSkinFileCache cache;

    @Override
    public void initialize() throws InitializationException
    {
        super.cache = cache;
    }

    @Override
    protected String compile(String fileName, boolean force) throws LESSCompilerException
    {
        // Get the XWiki object
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        try {
            // First, get the skin directory
            String path = "/skins/" + xwiki.getBaseSkin(xcontext) +  "/less";
            Path lessFilesPath = Paths.get(xwiki.getEngineContext().getRealPath(path));
            Path[] includePaths = {lessFilesPath};

            // Get the file content
            String fullFileName = path + "/" + fileName;
            InputStream is = xwiki.getEngineContext().getResourceAsStream(fullFileName);
            StringWriter source = new StringWriter();
            IOUtils.copy(is, source);

            // Parse the LESS content with Velocity
            String velocityParsedSource = xwiki.parseContent(source.toString(), xcontext);

            // Compile the LESS code
            return lessCompiler.compile(velocityParsedSource, includePaths);
        } catch (LESSCompilerException | IOException e) {
            throw new LESSCompilerException(String.format("Failed to compile the file [%s] with LESS.", fileName), e);
        }
    }

    @Override
    public String compileSkinFile(String fileName, boolean force) throws LESSCompilerException
    {
        return this.compileFromSkinFile(fileName, force);
    }

}
