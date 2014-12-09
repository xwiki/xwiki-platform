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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.LESSCompiler;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.lesscss.LESSResourceContentReader;
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.cache.CachedCompilerInterface;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Compile a LESS resource in a particular context (@see org.xwiki.lesscss.IntegratedLESSCompiler}.
 * To be used with AbstractCachedCompiler.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component(roles = CachedIntegratedLESSCompiler.class)
@Singleton
public class CachedIntegratedLESSCompiler implements CachedCompilerInterface<String>
{
    private static final String SKIN_CONTEXT_KEY = "skin";

    private static final String MAIN_SKIN_STYLE_FILENAME = "style.less.vm";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private LESSCompiler lessCompiler;

    @Inject
    private SkinDirectoryGetter skinDirectoryGetter;

    @Inject
    private LESSResourceContentReader lessResourceContentReader;

    @Override
    public String compute(LESSResourceReference lessResourceReference, boolean includeSkinStyle, String skin)
        throws LESSCompilerException
    {
        StringWriter source = new StringWriter();
        List<Path> includePaths = new ArrayList<>();
        File tempDir = null;

        try {
            if (lessResourceReference instanceof LESSSkinFileResourceReference || includeSkinStyle) {
                // Because of possible collisions between the LESS and the Velocity syntaxes, and some limitations
                // of the LESS compiler, we do not execute Velocity on included files .
                //
                // But since we want to include the main skin file (to be able to use LESS variables and mix-ins defined
                // by the skin (see http://jira.xwiki.org/browse/XWIKI-10708), we need this file to be rendered by
                // Velocity AND included by the current LESS resource.
                //
                // To do that, we create a temporary directory where we put the Velocity-rendered content of the main
                // skin file, so that the LESS resource can include it.
                //
                // This temp directory is also used to store LESS Skin files that are overwritten in the skin object.
                // (see http://jira.xwiki.org/browse/XWIKI-11394)
                // TODO: it is actually not implemented yet
                //
                // Finally, this directory is used as the main include path by LESS Compiler.
                tempDir = createTempDir(includePaths);

                // TODO: implement http://jira.xwiki.org/browse/XWIKI-11394 here

                // Get the skin directory, where LESS resources are located
                Path lessFilesPath = skinDirectoryGetter.getLESSSkinFilesDirectory(skin);
                includePaths.add(lessFilesPath);

                // Render and include the main skin file
                if (includeSkinStyle) {
                    includeMainSkinStyle(source, skin, tempDir);
                }

                // Get the content of the LESS resource
                source.write(lessResourceContentReader.getContent(lessResourceReference, skin));
            }

            // Parse the LESS content with Velocity
            String velocityParsedSource = executeVelocity(source.toString(), skin);

            // Compile the LESS code
            Path[] includePathsArray = includePaths.toArray(new Path[1]);
            return lessCompiler.compile(velocityParsedSource, includePathsArray);
        } catch (LESSCompilerException | IOException e) {
            throw new LESSCompilerException(String.format("Failed to compile the resource [%s] with LESS.",
                    lessResourceReference), e);
        } finally {
            // Delete the temp directory
            if (tempDir != null) {
                tempDir.delete();
            }
        }
    }

    private File createTempDir(List<Path> includePaths) throws IOException
    {
        File tempDir = Files.createTempDirectory("XWikiLESSCompilation").toFile();
        includePaths.add(tempDir.toPath());
        return tempDir;
    }

    private void includeMainSkinStyle(StringWriter source, String skin, File tempDir)
        throws LESSCompilerException, IOException
    {
        // Get the file content
        String mainSkinStyle = lessResourceContentReader.getContent(
            new LESSSkinFileResourceReference(MAIN_SKIN_STYLE_FILENAME), skin);
        // Execute velocity on it
        String velocityOutput = executeVelocity(mainSkinStyle, skin);
        // Write the file on the temp directory
        File mainSkinStyleFile = new File(tempDir, MAIN_SKIN_STYLE_FILENAME);
        IOUtils.copy(new StringReader(velocityOutput), new FileWriter(mainSkinStyleFile));
        // Add the import line to the LESS resource
        source.write("import (reference) \"" + MAIN_SKIN_STYLE_FILENAME + "\";\n");
    }

    private String executeVelocity(String source, String skin)
    {
        // Get the XWiki object
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        String currentSkin = xwiki.getSkin(xcontext);

        try {
            // Trick: change the current skin in order to compile the LESS file as if the specified skin
            // was the current skin
            if (!currentSkin.equals(skin)) {
                xcontext.put(SKIN_CONTEXT_KEY, skin);
            }
            return xwiki.parseContent(source, xcontext);

        } finally {
            // Reset the current skin to the old value
            if (!currentSkin.equals(skin)) {
                xcontext.put(SKIN_CONTEXT_KEY, currentSkin);
            }
        }
    }
}
