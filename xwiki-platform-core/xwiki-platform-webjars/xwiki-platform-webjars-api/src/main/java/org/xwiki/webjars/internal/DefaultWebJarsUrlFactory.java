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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.webjars.WebJarsUrlFactory;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation of {@link WebJarsUrlFactory}.
 *
 * @version $Id$
 * @since 17.3.0RC1
 */
@Component
@Singleton
public class DefaultWebJarsUrlFactory implements WebJarsUrlFactory
{
    private static final String RESOURCE_SEPARATOR = "/";

    /**
     * The name of the parameter that specifies the WebJar version.
     */
    private static final String VERSION = "version";

    /**
     * The name of the parameter that specifies the wiki in which the resource is located. If not specified, then the
     * wiki used will be the current wiki.
     */
    private static final String WIKI = "wiki";

    @Inject
    private Logger logger;

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

    @Inject
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> defaultResourceReferenceSerializer;

    @Override
    public String url(String resourceName)
    {
        if (StringUtils.isEmpty(resourceName)) {
            return null;
        }

        String[] parts = resourceName.split(RESOURCE_SEPARATOR, 3);
        if (parts.length < 3) {
            this.logger.warn("Invalid webjar resource name [{}]. Expected format is 'webjarId/version/path'",
                resourceName);
            return null;
        }

        // Prefix the webjarId with a fake groupId just to make sure that the colon character (:) is not interpreted as
        // separator in the webjarId. This is required to ensure that the behavior of this method doesn't change. Note
        // that the groupdId is ignored if the WebJar version is specified so the fake groupId won't have any effect.
        return url("fakeGroupId:" + parts[0], null, parts[2],
            Collections.singletonMap(VERSION, parts[1]));
    }

    @Override
    public String url(String webjarId, String path)
    {
        return url(webjarId, null, path, null);
    }

    @Override
    public String url(String webjarId, String namespace, String path)
    {
        return url(webjarId, namespace, path, null);
    }

    @Override
    public String url(String webjarId, String path, Map<String, ?> params)
    {
        // For backward-compatibility reasons, we still support passing the target wiki in parameters
        String namespace = null;
        if (params != null) {
            // For backward-compatibility reasons, we still support passing the target wiki in parameters
            String wikiId = (String) params.get(WIKI);
            if (!StringUtils.isEmpty(wikiId)) {
                namespace = constructNamespace(wikiId);
            }
        }

        return url(webjarId, namespace, path, params);
    }

    @Override
    public String url(String webjarId, String namespace, String path, Map<String, ?> params)
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
        String extensionId = String.format("%s:%s", groupId, artifactId);

        Map<String, Object> urlParams = new LinkedHashMap<>();
        if (params != null) {
            urlParams.putAll(params);
        }

        // For backward-compatibility reasons, we still support passing the target wiki in parameters. However, we need
        // to remove it from the params if that's the case since we don't want to output a URL with the wiki id in the
        // query string (since the namespace is now part of the URL).
        urlParams.remove(WIKI);

        // Construct a WebJarsResourceReference so that we can serialize it!
        WebJarsResourceReference resourceReference;
        String version = (String) urlParams.remove(VERSION);
        if (version == null) {
            // Try to determine the version based on the extensions that are currently installed or provided by default.
            Extension extension = getExtension(extensionId, namespace);
            if (extension != null) {
                // Generate the URL based on the found extension
                resourceReference = getResourceReference(getArtifactId(extension.getId().getId()),
                    extension.getId().getVersion().getValue(), namespace, path, urlParams);
            } else {
                // Fallback on a URL which does not include the version
                resourceReference = getResourceReference(artifactId, null, namespace, path, urlParams);
            }
        } else {
            // The version was explicitly indicated
            resourceReference = getResourceReference(artifactId, version, namespace, path, urlParams);
        }

        ExtendedURL extendedURL;
        try {
            extendedURL = this.defaultResourceReferenceSerializer.serialize(resourceReference);
        } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
            this.logger.warn("Error while serializing WebJar URL for id [{}], path = [{}]. Root cause = [{}]", webjarId,
                path, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }

        return extendedURL.serialize();
    }

    private String getArtifactId(String extensionId)
    {
        String artifactId = extensionId;
        int groupSeparatorPosition = extensionId.indexOf(':');
        if (groupSeparatorPosition >= 0) {
            artifactId = extensionId.substring(groupSeparatorPosition + 1);
        }

        return artifactId;
    }

    private WebJarsResourceReference getResourceReference(String artifactId, String version, String namespace,
        String path, Map<String, Object> urlParams)
    {
        List<String> segments = new ArrayList<>();
        segments.add(artifactId);
        // Don't include the version if it's not specified and there's no installed/core extension that matches the
        // given WebJar id.
        if (version != null) {
            segments.add(version);
        }
        segments.addAll(Arrays.asList(path.split(RESOURCE_SEPARATOR)));

        // When a JavaScript resource is loaded using RequireJS the resource URL must not include the ".js" suffix (by
        // default) if the URL is relative and doesn't have a query string. Before XWIKI-10881 (Introduce a proper
        // "webjars" type instead of reusing the "bin" type) all WebJar URLs had a query string (the resource path), so
        // we were forced to specify the ".js" suffix when using RequireJS. The resource path is currently no longer
        // part of the query string, and thus the ".js" suffix must now be omitted (otherwise RequireJS will ask for
        // ".js.js"), unless the resource has parameters (e.g., the resource is evaluated). In order to preserve
        // backwards compatibility with existing extensions and also in order to fix this mess (the developer doesn't
        // know when to put the ".js" suffix and when not), we have decided to add a fake query string if the ".js"
        // suffix is specified and there is no query string (thus preventing RequireJS from requesting ".js.js").
        if (path.endsWith(".js") && urlParams.isEmpty()) {
            urlParams.put("r", "1");
        }

        WebJarsResourceReference resourceReference =
            new WebJarsResourceReference(resolveNamespace(namespace), segments);
        for (Map.Entry<String, Object> parameterEntry : urlParams.entrySet()) {
            resourceReference.addParameter(parameterEntry.getKey(), parameterEntry.getValue());
        }

        return resourceReference;
    }

    private Extension getExtension(String extensionId, String namespace)
    {
        // Look for WebJars that are core extensions.
        Extension extension = this.coreExtensionRepository.getCoreExtension(extensionId);
        if (extension == null) {
            // Look for WebJars that are installed on the passed namespace (if defined), the current wiki or the main
            // wiki.
            String selectedNamespace = resolveNamespace(namespace);
            extension = this.installedExtensionRepository.getInstalledExtension(extensionId, selectedNamespace);
            if (extension == null) {
                // Fallback by looking in the main wiki
                selectedNamespace = constructNamespace(this.wikiDescriptorManager.getMainWikiId());
                extension = this.installedExtensionRepository.getInstalledExtension(extensionId, selectedNamespace);
            }
        }

        return extension;
    }

    private String resolveNamespace(String namespace)
    {
        String resolvedNamespace;
        if (StringUtils.isNotEmpty(namespace)) {
            resolvedNamespace = namespace;
        } else {
            resolvedNamespace = constructNamespace(getCurrentWikiId());
        }
        return resolvedNamespace;
    }

    private String getCurrentWikiId()
    {
        return this.wikiDescriptorManager.getCurrentWikiId();
    }

    private String constructNamespace(String wikiId)
    {
        return String.format("wiki:%s", wikiId);
    }
}
