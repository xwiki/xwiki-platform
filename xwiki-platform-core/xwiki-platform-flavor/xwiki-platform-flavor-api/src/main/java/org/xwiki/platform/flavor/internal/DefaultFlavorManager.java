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
package org.xwiki.platform.flavor.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.internal.RepositoryUtils;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.platform.flavor.FlavorManager;
import org.xwiki.platform.flavor.FlavorQuery;
import org.xwiki.properties.ConverterManager;

/**
 * Default implementation of {@link org.xwiki.platform.flavor.FlavorManager}.
 * 
 * @version $Id$
 * @since 7.1M2
 */
@Component
@Singleton
public class DefaultFlavorManager implements FlavorManager
{
    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    @Inject
    private LocalExtensionRepository localRepository;

    @Inject
    private InstalledExtensionRepository installedRepository;

    @Inject
    private CoreExtensionRepository coreRepository;

    @Inject
    private ConverterManager converter;

    @Inject
    private ConfigurationSource configurationSource;

    @Inject
    private Logger logger;

    @Override
    @Deprecated
    public IterableResult<Extension> getFlavors(FlavorQuery query) throws SearchException
    {
        return searchFlavors(query);
    }

    @Override
    public IterableResult<Extension> searchFlavors(FlavorQuery query) throws SearchException
    {
        IterableResult<Extension> result = null;

        // Search local flavors
        try {
            result = RepositoryUtils.appendSearchResults(result, this.localRepository.search(query));
        } catch (SearchException e) {
            this.logger.error("Failed to search in local repository", e);
        }

        // Search remote flavors
        result = RepositoryUtils.appendSearchResults(result, this.extensionRepositoryManager.search(query));

        return result;
    }

    @Override
    public Collection<ExtensionId> getKnownFlavors()
    {
        Collection<ExtensionId> flavors = new LinkedHashSet<>();

        // Get flavors from environment extension
        CoreExtension extension = this.coreRepository.getEnvironmentExtension();
        if (extension != null) {
            String flavorsString = extension.getProperty("xwiki.extension.knownFlavors");
            flavorsString = flavorsString.replaceAll("[\r\n]", "");
            flavors.addAll(this.converter.convert(ExtensionId.TYPE_LIST, flavorsString));
        }

        // TODO: Get flavors from configuration
        // flavors.addAll(configurationSource.getProperty("extension.flavor.known",
        // Collections.<String>emptyList()));

        return flavors;
    }

    @Override
    public ExtensionId getFlavorOfWiki(String wikiId)
    {
        String namespace = "wiki:" + wikiId;
        try {
            for (InstalledExtension extension : installedRepository.searchInstalledExtensions(namespace,
                new FlavorQuery())) {
                // Don't consider a dependency as the top level flavor, because a flavor can be a combination of other
                // flavors
                if (!extension.isDependency(namespace)) {
                    // There should be only one flavor per wiki
                    return extension.getId();
                }
            }
        } catch (SearchException e) {
            // It should never happen with the local repository
        }

        // If nothing has been found, look for extensions that was not tagged as flavors but that are in the list of
        // old flavors
        for (String oldFlavor : getExtensionsConsideredAsFlavors()) {
            InstalledExtension installedExtension = installedRepository.getInstalledExtension(oldFlavor, namespace);
            if (installedExtension != null) {
                return installedExtension.getId();
            }
        }

        // It seems there is no known UI on this wiki
        return null;
    }

    /**
     * @return the list of old extensions that can be considered as flavors even if they are not tagged
     */
    private Collection<String> getExtensionsConsideredAsFlavors()
    {
        return this.configurationSource.getProperty("extension.oldflavors", Collections.<String>emptyList());
    }
}
