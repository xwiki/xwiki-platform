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
package org.xwiki.test.page;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.environment.Environment;
import org.xwiki.test.XWikiTempDirUtil;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Environment implement that look for resources in the {@code target/classes} directory. The permanent and temporary
 * directories are set under Maven's {@code target} directory.
 *
 * @version $Id$
 * @since 13.8RC1
 * @since 13.7.1
 * @since 13.4.4
 */
public class PageTestEnvironment implements Environment
{
    private final File temporaryDirectory = XWikiTempDirUtil.createTemporaryDirectory();

    private final File permanentDirectory = XWikiTempDirUtil.createTemporaryDirectory();

    @Inject
    private Logger logger;

    @Override
    public File getTemporaryDirectory()
    {
        return this.temporaryDirectory;
    }

    @Override
    public File getPermanentDirectory()
    {
        return this.permanentDirectory;
    }

    @Override
    public URL getResource(String resourceName)
    {
        // When testing resource, for instance using PageTest, we only want to load resource by their path if they 
        // are local to the test. Other resources required to run the test are declared in the tested 
        // module dependencies and should be resolved using the classloader instead.
        try {
            // Get the test-classes directory.
            URL resource = getClass().getResource(File.separator);
            if (resource == null) {
                return null;
            }
            // Resolve the classes directory relatively to the test-classes directory.
            Path classesDirectory = Paths.get(resource.toURI())
                .getParent()
                .resolve("classes");
            // Update the absolute path to a relative one to be able to resolve it relatively to the classes directory.
            String cleanResourceName = resourceName;
            if (resourceName.startsWith(File.separator)) {
                cleanResourceName = resourceName.substring(1);
            }
            URI resourceURI = classesDirectory
                .resolve(cleanResourceName)
                .toUri();
            // Checks if the resolved URI correspond to a resource in the test directory.
            if (Paths.get(resourceURI).toFile().exists()) {
                return resourceURI.toURL();
            } else {
                return null;
            }
        } catch (URISyntaxException | MalformedURLException e) {
            this.logger.warn("Failed to resolve the resourceName [{}]. Cause: [{}]", resourceName,
                getRootCauseMessage(e));
        }
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String resourceName)
    {
        return getClass().getResourceAsStream(resourceName);
    }
}

