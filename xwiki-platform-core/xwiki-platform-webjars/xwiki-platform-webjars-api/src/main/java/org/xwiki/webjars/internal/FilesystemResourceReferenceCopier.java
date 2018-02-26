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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.url.filesystem.FilesystemExportContext;

/**
 * Saves WebJar resources to the file system.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public class FilesystemResourceReferenceCopier
{
    private static final Logger LOGGER  = LoggerFactory.getLogger(FilesystemResourceReferenceCopier.class);

    /**
     * Matches {@code url("whatever")} or {@code ur('whatever')}.
     */
    private static final Pattern URL_PATTERN = Pattern.compile("url\\(['\"](.*?)['\"]\\)");

    private static final String CONCAT_PATH_FORMAT = "%s/%s";

    private File getJARFile(String resourceName) throws IOException
    {
        // Get the JAR URL by looking up the passed resource name to extract the location of the JAR
        URL resourceURL = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        // The resourceName can contain invalid resource pointers. For example if a CSS file contains
        // "url($!services.webjars.url(...))". In this case ignore it and don't copy the resource to the file system.
        if (resourceURL != null) {
            JarURLConnection connection = (JarURLConnection) resourceURL.openConnection();
            URL jarURL = connection.getJarFileURL();

            File file;
            try {
                file = new File(jarURL.toURI());
            } catch (URISyntaxException e) {
                file = new File(jarURL.getPath());
            }

            return file;
        } else {
            LOGGER.debug("Cannot construct JAR File for resource [{}] which couldn't be found in the context "
                + "ClassLoader.", resourceName);
            return null;
        }
    }

    void copyResourceFromJAR(String resourcePrefix, String resourceName, String targetPrefix,
        FilesystemExportContext exportContext) throws IOException
    {
        // Note that we cannot use ClassLoader.getResource() to access the resource since the resourcePath may point
        // to a directory inside the JAR, and in this case we need to copy all resources inside that directory in the
        // JAR!

        String resourcePath = String.format(CONCAT_PATH_FORMAT, resourcePrefix, resourceName);
        File jarFile = getJARFile(resourcePath);
        if (jarFile == null) {
            // Ignore errors
            return;
        }
        JarFile jar = new JarFile(jarFile);
        try {
            for (Enumeration<JarEntry> enumeration = jar.entries(); enumeration.hasMoreElements();) {
                JarEntry entry = enumeration.nextElement();
                if (entry.getName().startsWith(resourcePath) && !entry.isDirectory()) {
                    // Copy the resource!
                    // TODO: Won't this cause collisions if the same resource is available on several subwikis
                    // for example?
                    String targetPath = targetPrefix + entry.getName().substring(resourcePrefix.length());
                    File targetLocation = new File(exportContext.getExportDir(), targetPath);
                    if (!targetLocation.exists()) {
                        targetLocation.getParentFile().mkdirs();
                        InputStream is = jar.getInputStream(entry);
                        try (FileOutputStream fos = new FileOutputStream(targetLocation)) {
                            IOUtils.copy(is, fos);
                        }
                        is.close();
                    }
                }
            }
        } finally {
            jar.close();
        }
    }

    void processCSS(String resourcePrefix, String resourceName, String targetPrefix,
        FilesystemExportContext exportContext) throws Exception
    {
        String resourcePath = String.format(CONCAT_PATH_FORMAT, resourcePrefix, resourceName);
        File jarFile = getJARFile(resourcePath);
        if (jarFile == null) {
            // Ignore errors
            return;
        }
        JarFile jar = new JarFile(jarFile);
        try {
            for (Enumeration<JarEntry> enumeration = jar.entries(); enumeration.hasMoreElements();) {
                JarEntry entry = enumeration.nextElement();
                if (entry.getName().equals(resourcePath)) {
                    // Read the CSS and look for url() entries...
                    processCSSfile(resourcePrefix, targetPrefix, entry, jar, exportContext);
                    break;
                }
            }
        } finally {
            jar.close();
        }
    }

    private void processCSSfile(String resourcePrefix, String targetPrefix, JarEntry entry, JarFile jar,
        FilesystemExportContext exportContext) throws Exception
    {
        // Limitation: we only support url() constructs located on a single line
        try (BufferedReader br = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = URL_PATTERN.matcher(line);
                while (matcher.find()) {
                    String url = matcher.group(1);
                    // Determine if URL is relative
                    if (isRelativeURL(url)) {
                        // Remove any query string part and any fragment part too
                        url = StringUtils.substringBefore(url, "?");
                        url = StringUtils.substringBefore(url, "#");
                        // Normalize paths
                        String resourceName = String.format(CONCAT_PATH_FORMAT,
                            StringUtils.substringBeforeLast(entry.getName(), "/"), url);
                        resourceName = new URI(resourceName).normalize().getPath();
                        resourceName = resourceName.substring(resourcePrefix.length() + 1);
                        // Copy to filesystem
                        copyResourceFromJAR(resourcePrefix, resourceName, targetPrefix, exportContext);
                    }
                }
            }
        }
    }

    private boolean isRelativeURL(String url)
    {
        try {
            return !new URI(url).isAbsolute();
        } catch (URISyntaxException e) {
            LOGGER.debug("Failed to find if URL is relative or not for [{}]. Don't copy it to the filesystem. "
                + "Error: [{}]", url, ExceptionUtils.getRootCauseMessage(e));
            return false;
        }
    }
}
