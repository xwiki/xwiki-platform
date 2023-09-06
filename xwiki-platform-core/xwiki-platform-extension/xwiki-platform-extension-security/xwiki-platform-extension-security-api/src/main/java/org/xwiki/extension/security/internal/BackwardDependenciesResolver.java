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
package org.xwiki.extension.security.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.text.XWikiToStringBuilder;

import static java.util.stream.Collectors.partitioningBy;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Resolves the backward dependencies of extensions, including transitive backward dependencies.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component(roles = BackwardDependenciesResolver.class)
@Singleton
public class BackwardDependenciesResolver
{
    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private Logger logger;

    /**
     * Returns a map representing explicitly installed backward dependencies of an extension, recursively. Meaning that,
     * if on two distinct namespaces, an extension is installed as a consequence of the installation of two separate
     * extensions, those two extensions will be returned.
     * <p>
     * Given the list below:
     * <ul>
     *     <li>
     *         E1/1.0 (namespace1)
     *         <ul>
     *             <li>
     *                 E2/1.0 (namespace1)
     *                 <ul>
     *                     E4/2.0 (namespace1)
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         E3/2.0 (namespace2)
     *         <ul>
     *             <li>
     *                 E4/2.0 (namespace2)
     *             </li>
     *         </ul>
     *     </li>
     * </ul>
     * The result when passing E3/2.0 as a parameter would contain E1/1.0 (namespace1) and E3/2.0 (namespace2). But not
     * E2/1.0 since it is only a dependency of E1/1.0.
     *
     * @param extensionId the identifier of the extension to get backward dependencies for
     * @return a map where the keys are the installed extensions that depend on the given extension, and the values are
     *     sets of namespaces of those dependencies
     */
    public Map<InstalledExtension, Set<String>> getExplicitlyInstalledBackwardDependencies(ExtensionId extensionId)
    {
        Map<Boolean, List<InstalledExtensionWithNamespace>> groupedExtensions = groupByIsDependency(extensionId);
        Set<InstalledExtensionWithNamespace> notDependency = new HashSet<>(groupedExtensions.get(false));
        while (true) {
            List<InstalledExtensionWithNamespace> installedExtensionWithNamespaces = groupedExtensions.get(true);
            if (installedExtensionWithNamespaces.isEmpty()) {
                break;
            }
            Map<Boolean, List<InstalledExtensionWithNamespace>> innerGroupedExtensions = new HashMap<>();
            for (InstalledExtensionWithNamespace installedExtensionWithNamespace : installedExtensionWithNamespaces) {
                innerGroupedExtensions.putAll(groupByIsDependency(installedExtensionWithNamespace));
            }
            groupedExtensions = innerGroupedExtensions;
            notDependency.addAll(groupedExtensions.get(false));
        }

        Map<InstalledExtension, Set<String>> map = new HashMap<>();
        for (InstalledExtensionWithNamespace installedExtensionWithNamespace : notDependency) {
            map.compute(installedExtensionWithNamespace.getInstalledExtension(), (installedExtension, namespaces) -> {
                if (namespaces == null) {
                    Set<String> newNamespaces = new HashSet<>();
                    newNamespaces.add(installedExtensionWithNamespace.getNamespace());
                    return newNamespaces;
                } else {
                    namespaces.add(installedExtensionWithNamespace.getNamespace());
                }
                return namespaces;
            });
        }

        return map;
    }

    /**
     * A tuple containing an {@link InstalledExtension} and a namespace.
     */
    private static final class InstalledExtensionWithNamespace
    {
        private final InstalledExtension installedExtension;

        private final String namespace;

        InstalledExtensionWithNamespace(InstalledExtension installedExtension, String namespace)
        {
            this.installedExtension = installedExtension;
            this.namespace = namespace;
        }

        public InstalledExtension getInstalledExtension()
        {
            return this.installedExtension;
        }

        public String getNamespace()
        {
            return this.namespace;
        }

        public boolean isDependency()
        {
            return this.installedExtension.isDependency(this.namespace);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            InstalledExtensionWithNamespace that = (InstalledExtensionWithNamespace) o;

            return new EqualsBuilder()
                .append(this.installedExtension, that.installedExtension)
                .append(this.namespace, that.namespace)
                .isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(17, 37)
                .append(this.installedExtension)
                .append(this.namespace)
                .toHashCode();
        }

        @Override
        public String toString()
        {
            ToStringBuilder builder = new XWikiToStringBuilder(this);
            builder.append("installedExtension=", this.installedExtension);
            builder.append("namespace=", this.namespace);

            return builder.toString();
        }
    }

    private Map<Boolean, List<InstalledExtensionWithNamespace>> groupByIsDependency(ExtensionId extensionId)
    {
        Map<String, Collection<InstalledExtension>> backwardDependencies;
        try {
            backwardDependencies = this.installedExtensionRepository.getBackwardDependencies(extensionId, true);
        } catch (ResolveException e) {
            this.logger.warn("Failed to load the list of backward dependencies of [{}]. Cause: [{}]", extensionId,
                getRootCauseMessage(e));
            backwardDependencies = Map.of();
        }
        return backwardDependencies.entrySet().stream()
            .flatMap(entry -> {
                Collection<InstalledExtension> installedExtensions = entry.getValue();
                String namespace = entry.getKey();
                return installedExtensions
                    .stream()
                    .map(installedExtension -> new InstalledExtensionWithNamespace(installedExtension, namespace));
            })
            .collect(partitioningBy(InstalledExtensionWithNamespace::isDependency));
    }

    private Map<Boolean, List<InstalledExtensionWithNamespace>> groupByIsDependency(
        InstalledExtensionWithNamespace installedExtensionWithNamespace)
    {
        Map<Boolean, List<InstalledExtensionWithNamespace>> groupedExtensions;
        ExtensionId extensionId = installedExtensionWithNamespace.getInstalledExtension().getId();
        // When the namespace is null, we look for all namespaces as an extension from a namespace can have a dependency
        // to an extension on the null namespace.
        // Since the reverse is false, we limit our search to a single namespace when the namespace is not null.
        if (installedExtensionWithNamespace.getNamespace() == null) {
            groupedExtensions = groupByIsDependency(extensionId);
        } else {
            Collection<InstalledExtension> backwardDependencies;
            try {
                backwardDependencies = this.installedExtensionRepository.getBackwardDependencies(
                    extensionId.getId(),
                    installedExtensionWithNamespace.getNamespace(), true);
            } catch (ResolveException e) {
                this.logger.warn(
                    "Failed to load the list of backward dependencies of [{}] on namespace [{}]. Cause: [{}]",
                    installedExtensionWithNamespace.getInstalledExtension(),
                    installedExtensionWithNamespace.getNamespace(),
                    getRootCauseMessage(e));
                backwardDependencies = List.of();
            }

            groupedExtensions = backwardDependencies.stream()
                .map(installedExtension -> new InstalledExtensionWithNamespace(installedExtension,
                    installedExtensionWithNamespace.getNamespace()))
                .collect(partitioningBy(InstalledExtensionWithNamespace::isDependency));
        }
        return groupedExtensions;
    }
}
