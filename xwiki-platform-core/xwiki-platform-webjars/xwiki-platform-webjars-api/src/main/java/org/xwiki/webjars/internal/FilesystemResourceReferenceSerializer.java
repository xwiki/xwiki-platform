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
package org.xwiki.webjars.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.filesystem.FilesystemExportContext;
import org.xwiki.url.internal.RelativeExtendedURL;

/**
 * Converts a {@link WebJarsResourceReference} into a {@link ExtendedURL} (with the Context Path added) that points
 * to a absolute URL pointing to a file on the local filesystem. This can be used when exporting to HTML for example.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named("filesystem")
@Singleton
public class FilesystemResourceReferenceSerializer
    implements ResourceReferenceSerializer<WebJarsResourceReference, ExtendedURL>
{
    /**
     * Prefix for locating resource files (JavaScript, CSS) in the classloader.
     */
    private static final String WEBJARS_RESOURCE_PREFIX = "META-INF/resources/webjars";

    private static final String WEBJAR_PATH = "webjars";

    @Inject
    private Provider<FilesystemExportContext> exportContextProvider;

    @Override
    public ExtendedURL serialize(WebJarsResourceReference reference)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        // Copy the resource from the webjar to the filesystem
        FilesystemExportContext exportContext = this.exportContextProvider.get();
        try {
            copyResourceFromJAR(WEBJARS_RESOURCE_PREFIX, reference.getResourceName(), WEBJAR_PATH, exportContext);
        } catch (IOException e) {
            throw new SerializeResourceReferenceException(
                String.format("Failed to extract and copy WebJAR resource [%s]", reference.getResourceName()), e);
        }

        List<String> pathSegments = new ArrayList<>();

        // Adjust path for links inside CSS files (since they need to be relative to the CSS file they're in).
        for (int i = 0; i < exportContext.getCSSParentLevel(); i++) {
            pathSegments.add("..");
        }

        pathSegments.add(WEBJAR_PATH);
        for (String resourceSegment : StringUtils.split(reference.getResourceName(), '/')) {
            pathSegments.add(resourceSegment);
        }

        return new RelativeExtendedURL(pathSegments);
    }

    private File getJARFile(String resourceName) throws IOException
    {
        // Get the JAR URL by looking up the passed resource name to extract the location of the JAR
        URL resourceURL = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        JarURLConnection connection = (JarURLConnection) resourceURL.openConnection();
        URL jarURL = connection.getJarFileURL();

        File file;
        try {
            file = new File(jarURL.toURI());
        } catch (URISyntaxException e) {
            file = new File(jarURL.getPath());
        }

        return file;
    }

    private void copyResourceFromJAR(String resourcePrefix, String resourceName, String targetPrefix,
        FilesystemExportContext exportContext) throws IOException
    {
        // Note that we cannot use ClassLoader.getResource() to access the resource since the resourcePath may point
        // to a directory inside the JAR, and in this case we need to copy all resources inside that directory in the
        // JAR!

        String resourcePath = String.format("%s/%s", resourcePrefix, resourceName);
        JarFile jar = new JarFile(getJARFile(resourcePath));
        for (Enumeration<JarEntry> enumeration = jar.entries(); enumeration.hasMoreElements();) {
            JarEntry entry = enumeration.nextElement();
            if (entry.getName().startsWith(resourcePath) && !entry.isDirectory()) {
                // Copy the resource!
                // TODO: Won't this cause collisions if the same resource is available on several subwikis for example?
                String targetPath = targetPrefix + entry.getName().substring(resourcePrefix.length());
                File targetLocation = new File(exportContext.getExportDir(), targetPath);
                if (!targetLocation.exists()) {
                    targetLocation.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(targetLocation);
                    InputStream is = jar.getInputStream(entry);
                    IOUtils.copy(is, fos);
                    fos.close();
                    is.close();
                }
            }
        }
        jar.close();
    }
}
