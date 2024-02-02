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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.webjars.internal.WebjarUrlResolver;

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
    @Inject
    private WebjarUrlResolver webjarUrlResolver;

    /**
     * Creates an URL that can be used to load a resource (JavaScript, CSS, etc.) from a WebJar in the current wiki.
     *
     * @param resourceName the resource asked using the format {@code <webjarId>/<version>/<path/to/resource>} (e.g.
     *     {@code angular/2.1.11/angular.js"})
     * @return the computed URL
     */
    public String url(String resourceName)
    {
        return this.webjarUrlResolver.url(resourceName);
    }

    /**
     * Creates an URL that can be used to load a resource (JavaScript, CSS, etc.) from a WebJar in the current wiki.
     *
     * @param webjarId the id of the WebJar that contains the resource; the format of the WebJar id is
     *     {@code groupId:artifactId} (e.g. {@code org.xwiki.platform:xwiki-platform-job-webjar}), where the
     *     {@code groupId} can be omitted if it is {@link WebjarUrlResolver#DEFAULT_WEBJAR_GROUP_ID} (i.e.
     *     {@code angular} translates to {@code org.webjars:angular})
     * @param path the path within the WebJar, starting from the version folder (e.g., you should pass just
     *     {@code angular.js} if the actual path is {@code META-INF/resources/webjars/angular/2.1.11/angular.js})
     * @return the URL to load the WebJar resource (relative to the context path of the web application)
     */
    public String url(String webjarId, String path)
    {
        return this.webjarUrlResolver.url(webjarId, path);
    }

    /**
     * Creates an URL that can be used to load a resource (JavaScript, CSS, etc.) from a WebJar in the passed
     * namespace.
     *
     * @param webjarId the id of the WebJar that contains the resource; the format of the WebJar id is
     *     {@code groupId:artifactId} (e.g. {@code org.xwiki.platform:xwiki-platform-job-webjar}), where the
     *     {@code groupId} can be omitted if it is {@link WebjarUrlResolver#DEFAULT_WEBJAR_GROUP_ID} (i.e. {@code angular} translates to
     *     {@code org.webjars:angular})
     * @param namespace the namespace in which the webjars resources will be loaded from (e.g., for a wiki namespace
     *     you should use the format {@code wiki:<wikiId>}). If null then defaults to the current wiki namespace. And if
     *     the passed namespace doesn't exist, falls back to the main wiki namespace
     * @param path the path within the WebJar, starting from the version folder (e.g., you should pass just
     *     {@code angular.js} if the actual path is {@code META-INF/resources/webjars/angular/2.1.11/angular.js})
     * @return the URL to load the WebJar resource (relative to the context path of the web application)
     * @since 8.1M2
     */
    public String url(String webjarId, String namespace, String path)
    {
        return this.webjarUrlResolver.url(webjarId, namespace, path);
    }

    /**
     * Creates an URL that can be used to load a resource (JavaScript, CSS, etc.) from a WebJar.
     *
     * @param webjarId the id of the WebJar that contains the resource; the format of the WebJar id is
     *     {@code groupId:artifactId} (e.g. {@code org.xwiki.platform:xwiki-platform-job-webjar}), where the
     *     {@code groupId} can be omitted if it is {@link WebjarUrlResolver#DEFAULT_WEBJAR_GROUP_ID} (i.e. {@code angular} translates to
     *     {@code org.webjars:angular})
     * @param path the path within the WebJar, starting from the version folder (e.g., you should pass just
     *     {@code angular.js} if the actual path is {@code META-INF/resources/webjars/angular/2.1.11/angular.js})
     * @param params additional query string parameters to add to the returned URL; there are two known (reserved)
     *     parameters: {@code version} (the WebJar version) and {@code evaluate} (a boolean parameter that specifies if
     *     the requested resource has Velocity code that needs to be evaluated); besides these you can pass whatever
     *     parameters you like (they will be taken into account or not depending on the resource)
     * @return the URL to load the WebJar resource (relative to the context path of the web application)
     */
    public String url(String webjarId, String path, Map<String, ?> params)
    {
        return this.webjarUrlResolver.url(webjarId, path, params);
    }

    /**
     * Creates an URL that can be used to load a resource (JavaScript, CSS, etc.) from a WebJar in the passed
     * namespace.
     *
     * @param webjarId the id of the WebJar that contains the resource; the format of the WebJar id is
     *     {@code groupId:artifactId} (e.g. {@code org.xwiki.platform:xwiki-platform-job-webjar}), where the
     *     {@code groupId} can be omitted if it is {@link WebjarUrlResolver#DEFAULT_WEBJAR_GROUP_ID} (i.e. {@code angular} translates to
     *     {@code org.webjars:angular})
     * @param namespace the namespace in which the webjars resources will be loaded from (e.g., for a wiki namespace
     *     you should use the format {@code wiki:<wikiId>}). If null then defaults to the current wiki namespace. And if
     *     the passed namespace doesn't exist, falls back to the main wiki namespace
     * @param path the path within the WebJar, starting from the version folder (e.g., you should pass just
     *     {@code angular.js} if the actual path is {@code META-INF/resources/webjars/angular/2.1.11/angular.js})
     * @param params additional query string parameters to add to the returned URL; there are two known (reserved)
     *     parameters: {@code version} (the WebJar version) and {@code evaluate} (a boolean parameter that specifies if
     *     the requested resource has Velocity code that needs to be evaluated); besides these you can pass whatever
     *     parameters you like (they will be taken into account or not depending on the resource)
     * @return the URL to load the WebJar resource (relative to the context path of the web application)
     * @since 8.1M2
     */
    public String url(String webjarId, String namespace, String path, Map<String, ?> params)
    {
        return this.webjarUrlResolver.url(webjarId, namespace, path, params);
    }
}
