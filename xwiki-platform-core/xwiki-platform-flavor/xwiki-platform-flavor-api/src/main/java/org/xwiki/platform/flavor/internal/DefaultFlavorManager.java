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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.platform.flavor.FlavorFilter;
import org.xwiki.platform.flavor.FlavorManager;
import org.xwiki.platform.flavor.FlavorManagerException;
import org.xwiki.platform.flavor.FlavorQuery;

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
    private InstalledExtensionRepository installedRepository;
    
    @Inject
    private ConfigurationSource configurationSource;
    
    @Inject
    private ComponentManager componentManager;

    @Override
    public IterableResult<Extension> getFlavors(FlavorQuery query) throws FlavorManagerException
    {
        // Apply the flavor filters to the query
        try {
            List<FlavorFilter> filters = componentManager.getInstanceList(FlavorFilter.class);
            for (FlavorFilter filter : filters) {
                filter.addFilterToQuery(query);
            }
            return extensionRepositoryManager.search(query);
        } catch (ComponentLookupException e) {
            throw new FlavorManagerException("Failed to get the flavor filters.", e);
        }
    }

    @Override
    public ExtensionId getFlavorOfWiki(String wikiId)
    {
        String namespace = "wiki:" + wikiId;
        try {
            for (InstalledExtension extension
                : installedRepository.searchInstalledExtensions(namespace, new FlavorQuery())) {
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
        return configurationSource.getProperty("extension.oldflavors", Collections.<String>emptyList());
    }
}
