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
package org.xwiki.extension.distribution.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Find the extensions which were installed as dependencies and are not needed anymore by any other extension.
 *
 * @version $Id$
 * @since 18.10.0RC1
 * @since 18.4.9
 * @since 17.10.16
 */
@Component(roles = OrphanedExtensionsResolver.class)
@Singleton
public class OrphanedExtensionsResolver
{
    @Inject
    private InstalledExtensionRepository installedRepository;

    @Inject
    private Provider<WikiDescriptorManager> wikiDescriptorManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Find the orphaned extensions of a wiki. The extensions installed on the root namespace are shared by all the
     * wikis, so they are only taken into account for the main wiki, where they are considered orphaned only when no
     * extension depends on them in any namespace.
     *
     * @param wiki the identifier of the wiki
     * @return the extensions installed as dependencies in the passed wiki and which are not needed anymore by any other
     *         extension
     */
    public List<InstalledExtension> getOrphanedExtensions(String wiki)
    {
        String namespace = new WikiNamespace(wiki).serialize();
        boolean mainWiki = this.wikiDescriptorManagerProvider.get().isMainWiki(wiki);

        List<InstalledExtension> orphanedExtensions = new ArrayList<>();
        for (InstalledExtension installedExtension : this.installedRepository.getInstalledExtensions(namespace)) {
            if (installedExtension.isDependency(namespace) && isOrphaned(installedExtension, namespace, mainWiki)) {
                orphanedExtensions.add(installedExtension);
            }
        }

        return orphanedExtensions;
    }

    private boolean isOrphaned(InstalledExtension installedExtension, String namespace, boolean mainWiki)
    {
        // The repository API does not forbid a null result (the XAR repository returns null for an extension it does
        // not handle): an unknown answer is never a reason to propose an uninstall, so it means "not orphaned".
        try {
            if (installedExtension.isInstalled(null)) {
                if (!mainWiki) {
                    return false;
                }

                Map<String, Collection<InstalledExtension>> backwardDependencies =
                    this.installedRepository.getBackwardDependencies(installedExtension.getId(), true);

                return backwardDependencies != null && backwardDependencies.isEmpty();
            } else {
                Collection<InstalledExtension> backwardDependencies = this.installedRepository
                    .getBackwardDependencies(installedExtension.getId().getId(), namespace, true);

                return backwardDependencies != null && backwardDependencies.isEmpty();
            }
        } catch (ResolveException e) {
            this.logger.warn(
                "Failed to resolve backward dependencies for extension id [{}] on namespace [{}]. Root cause is [{}]",
                installedExtension.getId().getId(), namespace, ExceptionUtils.getRootCauseMessage(e));

            return false;
        }
    }
}
