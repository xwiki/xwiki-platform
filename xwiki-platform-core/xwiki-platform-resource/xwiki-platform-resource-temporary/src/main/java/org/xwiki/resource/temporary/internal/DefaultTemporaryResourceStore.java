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
package org.xwiki.resource.temporary.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;

/**
 * Default {@link TemporaryResourceStore} implementation that uses the configured temporary directory.
 * 
 * @version $Id$
 * @since 7.4.6
 * @since 8.2.2
 * @since 8.3
 */
@Component
@Singleton
public class DefaultTemporaryResourceStore implements TemporaryResourceStore
{
    @Inject
    private Environment environment;

    @Override
    public File createTemporaryFile(TemporaryResourceReference reference, InputStream content) throws IOException
    {
        File temporaryFile = getTemporaryFile(reference);
        FileOutputStream fos = null;
        try {
            // Make sure the parent folders exist.
            temporaryFile.getParentFile().mkdirs();
            fos = new FileOutputStream(temporaryFile);
            IOUtils.copy(content, fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }

        return temporaryFile;
    }

    @Override
    public File getTemporaryFile(TemporaryResourceReference reference) throws IOException
    {
        List<String> segments = new ArrayList<>();

        if (reference.getOwningEntityReference() != null) {
            for (EntityReference component : reference.getOwningEntityReference().getReversedReferenceChain()) {
                segments.add(component.getName());
            }
        }
        if (!reference.getParameters().isEmpty()) {
            segments.add(String.valueOf(reference.getParameters().hashCode()));
        }

        String path = StringUtils.join(encode(segments), '/');
        String md5 = DigestUtils.md5Hex(path);
        File rootFolder = this.environment.getTemporaryDirectory();

        List<String> finalPathSegments = new ArrayList<>();

        finalPathSegments.add("tmp");
        finalPathSegments.add(reference.getModuleId());

        // Avoid having too many files in one folder because some filesystems don't perform well with large numbers of
        // files in one folder
        finalPathSegments.add(String.valueOf(md5.charAt(0)));
        finalPathSegments.add(String.valueOf(md5.charAt(1)));
        finalPathSegments.add(String.valueOf(md5.substring(2)));

        File safeFolder = new File(rootFolder, StringUtils.join(finalPathSegments, '/'));

        finalPathSegments.addAll(reference.getResourcePath());

        File temporaryFile = new File(rootFolder, StringUtils.join(finalPathSegments, '/'));

        // Make sure the resource path is not relative (e.g. "../../../") and tries to get outside of the safe folder.
        if (!temporaryFile.getAbsolutePath().startsWith(safeFolder.getAbsolutePath())) {
            throw new IOException(String.format("Resource path [%s] should be within [%s]",
                temporaryFile.getAbsolutePath(), safeFolder.getAbsolutePath()));
        }

        return temporaryFile;
    }

    private List<String> encode(List<String> path)
    {
        List<String> encodedPath = new ArrayList<>(path.size());
        for (String segment : path) {
            try {
                encodedPath.add(URLEncoder.encode(segment, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // Should never happen.
            }
        }

        return encodedPath;
    }
}
