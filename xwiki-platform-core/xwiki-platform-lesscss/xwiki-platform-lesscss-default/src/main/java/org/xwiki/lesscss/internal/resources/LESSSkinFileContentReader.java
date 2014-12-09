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
package org.xwiki.lesscss.internal.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.lesscss.LESSResourceContentReader;
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.compiler.SkinDirectoryGetter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Component to get the content of a LESS skin file resource.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Named("org.xwiki.lesscss.LESSSkinFileResourceReference")
@Singleton
public class LESSSkinFileContentReader implements LESSResourceContentReader
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private SkinDirectoryGetter skinDirectoryGetter;

    @Override
    public String getContent(LESSResourceReference lessResourceReference, String skin) throws LESSCompilerException
    {
        if (!(lessResourceReference instanceof LESSSkinFileResourceReference)) {
            throw new LESSCompilerException("Invalid LESS resource type.");
        }

        LESSSkinFileResourceReference lessSkinFileResourceReference =
                (LESSSkinFileResourceReference) lessResourceReference;
        return getSkinFileContent(skinDirectoryGetter.getSkinDirectory(skin) + "/less/",
                lessSkinFileResourceReference.getFileName());

    }

    private String getSkinFileContent(String directory, String fileName) throws LESSCompilerException
    {
        // Get the XWiki object
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // Get the file
        String fullFileName = directory + fileName;
        File lessFile = new File(xwiki.getEngineContext().getRealPath(fullFileName));
        if (!lessFile.exists() || !lessFile.isFile()) {
            throw new LESSCompilerException(String.format("The path [%s] is not a file or does not exists.",
                    fullFileName));
        }

        // Get the file content
        try {
            InputStream is = xwiki.getEngineContext().getResourceAsStream(fullFileName);
            StringWriter content = new StringWriter();
            IOUtils.copy(is, content);
            return content.toString();
        } catch (IOException e) {
            throw new LESSCompilerException(String.format("Error while reading the file [%s].",
                    fullFileName));
        }
    }
}
