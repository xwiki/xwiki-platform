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
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

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

    /**
     * The default {@code groupId} for Maven projects that produce WebJars.
     */
    private static final String DEFAULT_WEBJAR_GROUP_ID = "org.webjars";

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
     * Used to get the id of the current wiki in order to determine the current extension namespace.
     */
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

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

        // Prefix the webjarId with a fake groupId just to make sure that the colon character (:) is not interpreted as
        // separator in the webjarId. This is required in order to ensure that the behavior of this method doesn't
        // change. Note that the groupdId is ignored if the WebJar version is specified so the fake groupId won't have
        // any effect.
        return url("fakeGroupId:" + parts[0], parts[2], Collections.singletonMap(VERSION, parts[1]));
    }

    /**
     * Creates an URL that can be used to load a resource (JavaScript, CSS, etc.) from a WebJar.
     * 
     * @param webjarId the id of the WebJar that contains the resource; the format of the WebJar id is
     *            {@code groupId:artifactId} (e.g. {@code org.xwiki.platform:xwiki-platform-job-webjar}), where the
     *            {@code groupId} can be omitted if it is {@link #DEFAULT_WEBJAR_GROUP_ID} (i.e. {@code angular}
     *            translates to {@code org.webjars:angular})
     * @param path the path within the WebJar, starting from the version folder (e.g. you should pass just
     *            {@code angular.js} if the actual path is {@code META-INF/resources/webjars/angular/2.1.11/angular.js})
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
     *            {@code groupId:artifactId} (e.g. {@code org.xwiki.platform:xwiki-platform-job-webjar}), where the
     *            {@code groupId} can be omitted if it is {@link #DEFAULT_WEBJAR_GROUP_ID} (i.e. {@code angular}
     *            translates to {@code org.webjars:angular})
     * @param path the path within the WebJar, starting from the version folder (e.g. you should pass just
     *            {@code angular.js} if the actual path is {@code META-INF/resources/webjars/angular/2.1.11/angular.js})
     * @param params additional query string parameters to add to the returned URL; there are two known (reserved)
     *            parameters: {@code version} (the WebJar version) and {@code evaluate} (a boolean parameter that
     *            specifies if the requested resource has Velocity code that needs to be evaluated); besides these you
     *            can pass whatever parameters you like (they will be taken into account or not depending on the
     *            resource)
     * @return the URL to load the WebJar resource (relative to the context path of the web application)
     */
    public String url(String webjarId, String path, Map<String, ?> params)
    {
        if (StringUtils.isEmpty(webjarId)) {
            return null;
        }

        String groupId = DEFAULT_WEBJAR_GROUP_ID;
        String artifactId = webjarId;
        int groupSeparatorPosition = webjarId.indexOf(':');
        if (groupSeparatorPosition >= 0) {
            // A different group id.
            groupId = webjarId.substring(0, groupSeparatorPosition);
            artifactId = webjarId.substring(groupSeparatorPosition + 1);
        }

        Map<String, Object> urlParams = new LinkedHashMap<>();
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
        // Note: we have to encode the query string since URLFactory will not do that currently (Actually it cannot do
        // this at the moment since it accepts a string representing the QueryString and it's thus not possible to
        // encode it automatically - we would need to pass a Map to allow for query string encoding done by the
        // URLFactory).
        URL url = urlFactory.createURL("resources", "path", "webjars", escapeTool.url(urlParams), null, xcontext);
        return urlFactory.getURL(url, xcontext);
    }

    private String getVersion(String extensionId)
    {
        // Look for WebJars that are core extensions.
        Extension extension = coreExtensionRepository.getCoreExtension(extensionId);
        if (extension == null) {
            // Look for WebJars that are installed on the current wiki or the root namespace.
            String namespace = "wiki:" + wikiDescriptorManager.getCurrentWikiId();
            extension = installedExtensionRepository.getInstalledExtension(extensionId, namespace);
            if (extension == null) {
                return null;
            }
        }
        return extension.getId().getVersion().getValue();
    }
}
