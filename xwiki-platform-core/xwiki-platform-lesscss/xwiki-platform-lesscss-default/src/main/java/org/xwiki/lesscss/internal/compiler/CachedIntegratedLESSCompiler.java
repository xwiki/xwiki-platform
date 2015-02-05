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
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReader;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.cache.CachedCompilerInterface;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Compile a LESS resource in a particular context (@see org.xwiki.lesscss.compiler.IntegratedLESSCompiler}.
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

    private static final String LESS_INCLUDE_SEPARATOR = ".realStartOfXWikiSSX{color:#000}";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private LESSCompiler lessCompiler;

    @Inject
    private SkinDirectoryGetter skinDirectoryGetter;

    @Inject
    private LESSResourceReader lessResourceReader;

    @Override
    public String compute(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
        boolean useLESS, String skin) throws LESSCompilerException
    {
        StringWriter source = new StringWriter();
        List<Path> includePaths = new ArrayList<>();
        List<File> tempFilesToDelete = new ArrayList<>();
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
                tempDir = createTempDir(includePaths, tempFilesToDelete);

                // TODO: implement http://jira.xwiki.org/browse/XWIKI-11394 here

                // Get the skin directory, where LESS resources are located
                Path lessFilesPath = skinDirectoryGetter.getLESSSkinFilesDirectory(skin);
                includePaths.add(lessFilesPath);

                // Render and include the main skin file
                if (includeSkinStyle) {
                    importMainSkinStyle(source, skin, tempDir, tempFilesToDelete);
                }

                // Get the content of the LESS resource
                source.write(lessResourceReader.getContent(lessResourceReference, skin));
            }

            // Parse the LESS content with Velocity
            String lessCode = source.toString();
            if (useVelocity) {
                lessCode = executeVelocity(lessCode, skin);
            }

            String result;
            // Compile the LESS code
            if (useLESS) {
                Path[] includePathsArray = includePaths.toArray(new Path[1]);
                result = lessCompiler.compile(lessCode, includePathsArray);

                // Remove some useless code
                if (includeSkinStyle) {
                    result = removeMainSkinStyleUndesiredOutput(result);
                }
            } else {
                result = lessCode;
            }

            // End
            return result;
        } catch (LESSCompilerException | IOException e) {
            throw new LESSCompilerException(String.format("Failed to compile the resource [%s] with LESS.",
                    lessResourceReference), e);
        } finally {
            deleteTempFiles(tempFilesToDelete);
        }
    }

    private File createTempDir(List<Path> includePaths, List<File> tempFilesToDelete) throws IOException
    {
        File tempDir = Files.createTempDirectory("XWikiLESSCompilation").toFile();
        includePaths.add(tempDir.toPath());
        // Don't forget to delete this file later
        tempFilesToDelete.add(tempDir);
        // Be sure it's done even if the JVM is stopped
        tempDir.deleteOnExit();
        return tempDir;
    }

    private void importMainSkinStyle(StringWriter source, String skin, File tempDir, List<File> tempFilesToDelete)
        throws LESSCompilerException, IOException
    {
        // Get the file content
        String mainSkinStyle = lessResourceReader.getContent(
            new LESSSkinFileResourceReference(MAIN_SKIN_STYLE_FILENAME), skin);
        // Execute velocity on it
        String velocityOutput = executeVelocity(mainSkinStyle, skin);
        // Write the file on the temp directory
        File mainSkinStyleFile = new File(tempDir, MAIN_SKIN_STYLE_FILENAME);
        FileWriter fileWriter = new FileWriter(mainSkinStyleFile);
        IOUtils.copy(new StringReader(velocityOutput), fileWriter);
        fileWriter.close();
        // Don't forget to delete this file later
        tempFilesToDelete.add(mainSkinStyleFile);
        // Be sure it's done even if the JVM is stopped
        mainSkinStyleFile.deleteOnExit();
        // Add the import line to the LESS resource.
        // We import this file to be able to use variables and mix-ins defined in it/
        // But we don't want it in the output.
        source.write("@import (reference) \"" + MAIN_SKIN_STYLE_FILENAME + "\";\n");
        // See removeMainSkinStyleUndesiredOutput()
        source.write(LESS_INCLUDE_SEPARATOR);
    }

    private String removeMainSkinStyleUndesiredOutput(String cssCode) {
        // Because of a bug in the "@import" function of the LESS compiler, we manually remove all the content that
        // have been imported, thanks to LESS_INCLUDE_SEPARATOR which is a marker to know where the interesting
        // content really start.
        // See: https://github.com/less/less.js/issues/1968 and https://github.com/less/less.js/issues/1878
        int contentToRemoveIndex = cssCode.indexOf(LESS_INCLUDE_SEPARATOR) + LESS_INCLUDE_SEPARATOR.length();
        return cssCode.substring(contentToRemoveIndex);
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

    private void deleteTempFiles(List<File> tempFilesToDelete)
    {
        // Delete files from the last to the first, because the first could be a directory that contains all the others
        for (int i = tempFilesToDelete.size() - 1; i >= 0; --i) {
            File tempFile = tempFilesToDelete.get(i);
            tempFile.delete();
        }
        tempFilesToDelete.clear();
    }
}
