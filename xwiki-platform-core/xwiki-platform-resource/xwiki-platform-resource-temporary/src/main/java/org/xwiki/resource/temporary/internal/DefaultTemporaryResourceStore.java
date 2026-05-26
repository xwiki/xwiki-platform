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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
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
public class DefaultTemporaryResourceStore implements TemporaryResourceStore, Initializable
{
    @Inject
    private Environment environment;

    private Path rootPath;

    @Override
    public void initialize() throws InitializationException
    {
        this.rootPath = this.environment.getTemporaryDirectory().toPath().resolve("tmp").toAbsolutePath();
    }

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

        List<String> finalPathSegments = new ArrayList<>();

        finalPathSegments.add(reference.getModuleId());

        // Avoid having too many files in one folder because some filesystems don't perform well with large numbers of
        // files in one folder
        finalPathSegments.add(String.valueOf(md5.charAt(0)));
        finalPathSegments.add(String.valueOf(md5.charAt(1)));
        finalPathSegments.add(String.valueOf(md5.substring(2)));

        // Get the path of the folder in which the temporary file is supposed to be stored
        Path modulePath = this.rootPath.resolve(StringUtils.join(finalPathSegments, '/'));

        // Make sure the resource folder does not try go outside the module folder.
        if (!modulePath.startsWith(this.rootPath)) {
            throw new IOException(
                String.format("The module path [%s] should be within [%s]", modulePath, this.rootPath));
        }

        finalPathSegments.addAll(reference.getResourcePath());

        // Get the sub path in which to store the temporary file
        Path temporaryPath = modulePath.resolve(StringUtils.join(reference.getResourcePath(), '/')).normalize();

        // Make sure the resource path does not try go outside the safe folder.
        if (!temporaryPath.startsWith(modulePath) || !temporaryPath.startsWith(this.rootPath)) {
            throw new IOException(
                String.format("Resource path [%s] should be within [%s]", temporaryPath, modulePath));
        }

        return temporaryPath.toFile();
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
