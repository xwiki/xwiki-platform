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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.ExportURLFactoryActionHandler;
import com.xpn.xwiki.web.ExportURLFactoryContext;

/**
 * Handles exporting content having WebJARS URLs, by extracting resources from JARs pointed to by the WebJARs URLs.
 *
 * @version $Id$
 * @since 6.2RC1
 */
@Component
@Named("webjars")
@Singleton
public class WebJarsExportURLFactoryActionHandler implements ExportURLFactoryActionHandler
{
    /**
     * Prefix for locating resource files (JavaScript, CSS) in the classloader.
     */
    private static final String WEBJARS_RESOURCE_PREFIX = "META-INF/resources/webjars/";

    private static final String WEBJAR_PATH = "webjars/";

    @Override
    public URL createURL(String web, String name, String querystring, String anchor, String wikiId,
        XWikiContext context, ExportURLFactoryContext factoryContext) throws Exception
    {
        // Example of URL:
        // /xwiki/bin/webjars/resources/path?value=bootstrap%2F3.2.0%2Ffonts/glyphicons-halflings-regular.eot
        // where:
        // - web = resources
        // - name = path
        // - action = webjars
        // - querystring = value=bootstrap%2F3.2.0%2Ffonts/glyphicons-halflings-regular.eot

        // Copy the resources found in JARs on the filesystem

        // We need to decode the passed Query String because the query string passed to ExportURLFactory are always
        // encoded. See XWikiURLFactory#createURL()'s javadoc for more details on why the query string is passed
        // encoded.
        String resourceName = URLDecoder.decode(StringUtils.substringAfter(querystring, "value="), "UTF-8");

        String resourcePath = String.format("%s%s", WEBJARS_RESOURCE_PREFIX, resourceName);

        copyResourceFromJAR(resourcePath, WEBJAR_PATH, factoryContext);

        StringBuffer path = new StringBuffer("file://");

        // Adjust path for links inside CSS files (since they need to be relative to the CSS file they're in).
        factoryContext.adjustCSSPath(path);

        path.append(WEBJAR_PATH);
        path.append(resourcePath.replace(" ", "%20"));
        return new URI(path.toString()).toURL();
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

    private void copyResourceFromJAR(String resourcePath, String prefix, ExportURLFactoryContext factoryContext)
        throws IOException
    {
        // Note that we cannot use ClassLoader.getResource() to access the resource since the resourcePath may point
        // to a directory inside the JAR, and in this case we need to copy all resources inside that directory in the
        // JAR!

        JarFile jar = new JarFile(getJARFile(resourcePath));
        for (Enumeration<JarEntry> enumeration = jar.entries(); enumeration.hasMoreElements();) {
            JarEntry entry = enumeration.nextElement();
            if (entry.getName().startsWith(resourcePath) && !entry.isDirectory()) {
                // Copy the resource!
                String targetPath = prefix + entry.getName();
                File targetLocation = new File(factoryContext.getExportDir(), targetPath);
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
