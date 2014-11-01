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
package org.xwiki.webjars.script;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.script.service.ScriptService;
import org.xwiki.velocity.tools.EscapeTool;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Make it easy to use WebJars in scripts. For example it can compute an XWiki WebJars URL.
 *
 * @version $Id$
 * @since 6.0M1
 */
@Component
@Named("webjars")
@Singleton
public class WebJarsScriptService implements ScriptService
{
    /**
     * The name of the parameter that specifies the WebJar version.
     */
    private static final String VERSION = "version";

    @Inject
    private Logger logger;

    /**
     * @todo finish implementing URL serialization in the URL module to avoid this dependency on oldcore.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to check if the WebJar is a core extension and to get its version.
     */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * Used to check if the WebJar is an installed extension and to get its version.
     */
    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    /**
     * Used to serialize a {@link Map} as a URL query string.
     */
    private EscapeTool escapeTool = new EscapeTool();

    /**
     * Compute an XWiki WebJar URL of the form {@code http://server/bin/webjars/resource/path?value=(resource name)}.
     *
     * @param resourceName the resource asked (e.g. {@code angular/2.1.11/angular.js"})
     * @return the computed URL
     */
    public String url(String resourceName)
    {
        if (StringUtils.isEmpty(resourceName)) {
            return null;
        }

        String[] parts = resourceName.split("/", 3);
        if (parts.length < 3) {
            logger.warn("Invalid webjar resource name [{}]. Expected format is 'webjarId/version/path'", resourceName);
            return null;
        }

        return url(parts[0], parts[2], Collections.singletonMap(VERSION, parts[1]));
    }

    /**
     * Creates an URL that can be used to load a resource (JavaScript, CSS, etc.) from a WebJar.
     *
     * @param webjarId the id of the WebJar that contains the resource; the format of the WebJar id is
     *            "groupId:artifactId" (e.g. org.xwiki.platform:xwiki-platform-job-webjar), where the groupId can be
     *            omitted if it is 'org.webjars' (i.e. "angular" translates to "org.webjars:angular")
     * @param path the path within the WebJar, starting from the version folder (e.g. you should pass just "angular.js"
     *            if the actual path is "META-INF/resources/webjars/angular/2.1.11/angular.js")
     * @return the URL to load the WebJar resource (relative to the context path of the web application)
     */
    public String url(String webjarId, String path)
    {
        return url(webjarId, path, Collections.<String, Object>emptyMap());
    }

    /**
     * Creates an URL that can be used to load a resource (JavaScript, CSS, etc.) from a WebJar.
     *
     * @param webjarId the id of the WebJar that contains the resource; the format of the WebJar id is
     *            "groupId:artifactId" (e.g. org.xwiki.platform:xwiki-platform-job-webjar), where the groupId can be
     *            omitted if it is 'org.webjars' (i.e. "angular" translates to "org.webjars:angular")
     * @param path the path within the WebJar, starting from the version folder (e.g. you should pass just "angular.js"
     *            if the actual path is "META-INF/resources/webjars/angular/2.1.11/angular.js")
     * @param params additional query string parameters to add to the returned URL
     * @return the URL to load the WebJar resource (relative to the context path of the web application)
     */
    public String url(String webjarId, String path, Map<String, ?> params)
    {
        if (StringUtils.isEmpty(webjarId)) {
            return null;
        }

        String groupId = "org.webjars";
        String artifactId = webjarId;
        int groupSeparatorPosition = webjarId.indexOf(':');
        if (groupSeparatorPosition >= 0) {
            // A different group id.
            groupId = webjarId.substring(0, groupSeparatorPosition);
            artifactId = webjarId.substring(groupSeparatorPosition + 1);
        }

        Map<String, Object> urlParams = new LinkedHashMap<String, Object>();
        if (params != null) {
            urlParams.putAll(params);
        }

        Object version = urlParams.remove(VERSION);
        if (version == null) {
            version = getVersion(String.format("%s:%s", groupId, artifactId));
        }

        String fullPath = String.format("%s/%s/%s", artifactId, version, path);
        urlParams.put("value", fullPath);

        XWikiContext xcontext = xcontextProvider.get();
        XWikiURLFactory urlFactory = xcontext.getURLFactory();
        URL url = urlFactory.createURL("resources", "path", "webjars", escapeTool.url(urlParams), null, xcontext);
        return urlFactory.getURL(url, xcontext);
    }

    private String getVersion(String extensionId)
    {
        // Look for WebJars that are core extensions.
        Extension extension = coreExtensionRepository.getCoreExtension(extensionId);
        if (extension == null) {
            // Look for WebJars that are installed on the current wiki or the root namespace.
            String namespace = "wiki:" + xcontextProvider.get().getWikiId();
            extension = installedExtensionRepository.getInstalledExtension(extensionId, namespace);
            if (extension == null) {
                return null;
            }
        }
        return extension.getId().getVersion().getValue();
    }
}
