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
package org.xwiki.extension.repository.internal;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.extension.repository.AbstractExtensionRepository;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepositoryException;

/**
 * Default implementation of {@link LocalExtensionRepository}.
 * 
 * @version $Id$
 */
@Component
@Singleton
// TODO: make it threadsafe bulletproofs
public class DefaultLocalExtensionRepository extends AbstractExtensionRepository implements LocalExtensionRepository,
    Initializable
{
    /**
     * Used to get repository path.
     */
    @Inject
    private ExtensionManagerConfiguration configuration;

    /**
     * Used to check for existing core extensions.
     */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * Used to compare extensions versions when upgrading.
     */
    @Inject
    private VersionManager versionManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    private ComponentManager componentManager;

    /**
     * Used to manipulate filesystem repository storage.
     */
    private ExtensionStorage storage;

    /**
     * the local extensions.
     */
    private Map<ExtensionId, DefaultLocalExtension> extensions =
        new ConcurrentHashMap<ExtensionId, DefaultLocalExtension>();

    /**
     * The local extensions grouped by ids.
     * <p>
     * <extension id, extensions>
     */
    private Map<String, List<DefaultLocalExtension>> extensionsById =
        new ConcurrentHashMap<String, List<DefaultLocalExtension>>();

    /**
     * The installed extensions.
     * <p>
     * <feature, <namespace, extension>>
     */
    private Map<String, Map<String, DefaultInstalledExtension>> installedExtensions =
        new ConcurrentHashMap<String, Map<String, DefaultInstalledExtension>>();

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.storage = new ExtensionStorage(this, this.configuration.getLocalRepository(), this.componentManager);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to intialize local extension storage", e);
        }

        setId(new ExtensionRepositoryId("local", "xwiki", this.storage.getRootFolder().toURI()));

        this.storage.loadExtensions();

        // Validate local extension

        Map<String, Set<String>> validatedExtension = new HashMap<String, Set<String>>();
        for (List<DefaultLocalExtension> extensionVersions : this.extensionsById.values()) {
            for (ListIterator<DefaultLocalExtension> it = extensionVersions.listIterator(extensionVersions.size()); it
                .hasPrevious();) {
                DefaultLocalExtension localExtension = it.previous();

                validateExtension(localExtension, validatedExtension);
            }
        }
    }

    /**
     * Check extension validity and set it as not installed if not.
     * 
     * @param localExtension the extension to validate
     * @param validatedExtensions the already validated extensions
     */
    private void validateExtension(DefaultLocalExtension localExtension, Map<String, Set<String>> validatedExtensions)
    {
        if (localExtension.getNamespaces() == null) {
            validateExtension(localExtension, validatedExtensions, null);
        } else {
            for (String namespace : localExtension.getNamespaces()) {
                validateExtension(localExtension, validatedExtensions, namespace);
            }
        }
    }

    /**
     * Check extension validity against a specific namespace and set it as not installed if not.
     * 
     * @param localExtension the extension to validate
     * @param validatedExtensions the already validated extensions
     * @param namespace the namespace
     */
    private void validateExtension(DefaultLocalExtension localExtension, Map<String, Set<String>> validatedExtensions,
        String namespace)
    {
        Set<String> validatedExtensionsNamespace = validatedExtensions.get(namespace);
        if (validatedExtensionsNamespace == null) {
            validatedExtensionsNamespace = new HashSet<String>();
            validatedExtensions.put(namespace, validatedExtensionsNamespace);
        }

        if (!validatedExtensionsNamespace.contains(localExtension.getId().getId())) {
            validateExtension(localExtension, validatedExtensionsNamespace, namespace);
        }
    }

    /**
     * Check extension validity against a specific namespace and set it as not installed if not.
     * 
     * @param localExtension the extension to validate
     * @param validatedExtensions already validated extensions
     * @param namespace the namespace
     */
    private void validateExtension(DefaultLocalExtension localExtension, Set<String> validatedExtensions,
        String namespace)
    {
        try {
            if (!localExtension.isInstalled(namespace)
                || this.coreExtensionRepository.exists(localExtension.getId().getId())) {
                // Impossible to overwrite core extensions
                localExtension.setInstalled(false, namespace);
                return;
            }

            // Validate dependencies
            for (ExtensionDependency dependency : localExtension.getDependencies()) {
                boolean enabled = false;

                List<DefaultLocalExtension> dependencyVersions = this.extensionsById.get(dependency.getId());
                if (dependencyVersions != null) {
                    for (ListIterator<DefaultLocalExtension> it =
                        dependencyVersions.listIterator(dependencyVersions.size()); it.hasPrevious();) {
                        DefaultLocalExtension dependencyExtension = it.previous();

                        if (!validatedExtensions.contains(dependency.getId())) {
                            validateExtension(dependencyExtension, validatedExtensions, namespace);
                        }

                        if (dependencyExtension.isInstalled(namespace)) {
                            enabled = true;
                            break;
                        }
                    }
                } else {
                    enabled = this.coreExtensionRepository.exists(dependency.getId());
                }

                if (!enabled) {
                    localExtension.setInstalled(false, namespace);
                    return;
                }
            }

            // Complete local extension installation
            addInstalledExtension(localExtension, namespace);
        } finally {
            validatedExtensions.add(localExtension.getId().getId());
        }
    }

    /**
     * Uninstall provided extension.
     * 
     * @param localExtension the extension to uninstall
     * @param namespace the namespace
     * @throws UninstallException error when trying to uninstall extension
     * @see #uninstallExtension(LocalExtension, String)
     */
    private void uninstallLocalExtension(DefaultLocalExtension localExtension, String namespace)
        throws UninstallException
    {
        localExtension.setInstalled(false, namespace);

        try {
            this.storage.saveDescriptor(localExtension);
        } catch (Exception e) {
            throw new UninstallException("Failed to modify extension descriptor", e);
        }

        // Clean caches

        if (namespace == null) {
            this.installedExtensions.remove(localExtension.getId().getId());
        } else {
            Map<String, DefaultInstalledExtension> namespaceInstalledExtension =
                this.installedExtensions.get(localExtension.getId().getId());

            namespaceInstalledExtension.remove(namespace);
        }

        removeFromBackwardDependencies(localExtension, namespace);
    }

    /**
     * Install provided extension.
     * 
     * @param localExtension the extension to install
     * @param namespace the namespace
     * @throws InstallException error when trying to uninstall extension
     * @see #installExtension(LocalExtension, String)
     */
    private void installLocalExtension(DefaultLocalExtension localExtension, String namespace) throws InstallException
    {
        localExtension.setInstalled(true, namespace);

        try {
            this.storage.saveDescriptor(localExtension);
        } catch (Exception e) {
            throw new InstallException("Failed to modify extension descriptor", e);
        }

        // Update caches

        addInstalledExtension(localExtension, namespace);
    }

    private void removeFromBackwardDependencies(DefaultLocalExtension localExtension, String namespace)
    {
        // Clean provided extension dependencies backward dependencies
        for (ExtensionDependency dependency : localExtension.getDependencies()) {
            DefaultInstalledExtension installedExtension =
                getInstalledExtensionFromCache(dependency.getId(), namespace);

            if (installedExtension.getBackwardDependencies().remove(localExtension)) {
                // That should never happen so lets log it
                this.logger.warn("Extension [" + localExtension + "] was not regisistered as backward dependency of ["
                    + installedExtension.getExtension() + "]");
            }
        }
    }

    /**
     * Register a new local extension.
     * 
     * @param localExtension the new local extension
     */
    protected void addLocalExtension(DefaultLocalExtension localExtension)
    {
        // extensions
        this.extensions.put(localExtension.getId(), localExtension);

        // versions
        List<DefaultLocalExtension> versions = this.extensionsById.get(localExtension.getId().getId());

        if (versions == null) {
            versions = new ArrayList<DefaultLocalExtension>();
            this.extensionsById.put(localExtension.getId().getId(), versions);

            versions.add(localExtension);
        } else {
            int index = 0;
            while (index < versions.size()
                && this.versionManager.compareVersions(localExtension.getId().getVersion(), versions.get(index).getId()
                    .getVersion()) > 0) {
                ++index;
            }

            versions.add(index, localExtension);
        }
    }

    /**
     * Register a newly installed extension in backward dependencies map.
     * 
     * @param localExtension the local extension to register
     * @param namespace the namespace
     * @return the installed extension informations
     */
    private DefaultInstalledExtension addInstalledExtension(DefaultLocalExtension localExtension, String namespace)
    {
        // Register the extension in the installed extensions for the provided namespace
        DefaultInstalledExtension installedExtension =
            getInstalledExtensionFromCache(localExtension.getId().getId(), namespace, localExtension);

        // Add virtual extensions
        for (String feature : localExtension.getFeatures()) {
            getInstalledExtensionFromCache(feature, namespace, localExtension);
        }

        // Add backward dependencies
        for (ExtensionDependency dependency : localExtension.getDependencies()) {
            if (!this.coreExtensionRepository.exists(dependency.getId())) {
                // Get the extension for the dependency feature for the provided namespace
                DefaultLocalExtension dependencyLocalExtension =
                    (DefaultLocalExtension) getInstalledExtension(dependency.getId(), namespace);

                // Make sure to register backward dependency on the right namespace
                DefaultInstalledExtension dependencyInstalledExtension =
                    getInstalledExtensionFromCache(dependency.getId(), namespace, dependencyLocalExtension);

                dependencyInstalledExtension.addBackwardDependency(localExtension);
            }
        }

        return installedExtension;
    }

    /**
     * Get extension registered as installed for the provided feature and namespace or can register it if provided.
     * <p>
     * Only look at provide namespace and does take into account inheritance.
     * 
     * @param feature the feature provided by the extension
     * @param namespace the namespace where the extension is installed
     * @param localExtension the extension
     * @return the installed extension informations
     */
    private DefaultInstalledExtension getInstalledExtensionFromCache(String feature, String namespace,
        DefaultLocalExtension localExtension)
    {
        Map<String, DefaultInstalledExtension> installedExtensionsForFeature = this.installedExtensions.get(feature);

        if (installedExtensionsForFeature == null) {
            installedExtensionsForFeature = new HashMap<String, DefaultInstalledExtension>();
            this.installedExtensions.put(feature, installedExtensionsForFeature);
        }

        DefaultInstalledExtension installedExtension = installedExtensionsForFeature.get(namespace);
        if (installedExtension == null) {
            installedExtension = new DefaultInstalledExtension(localExtension, feature, namespace);
            installedExtensionsForFeature.put(namespace, installedExtension);
        }

        return installedExtension;
    }

    /**
     * Get extension registered as installed for the provided feature and namespace (including on root namespace).
     * 
     * @param feature the feature provided by the extension
     * @param namespace the namespace where the extension is installed
     * @return the installed extension informations
     */
    private DefaultInstalledExtension getInstalledExtensionFromCache(String feature, String namespace)
    {
        Map<String, DefaultInstalledExtension> installedExtensionsForFeature = this.installedExtensions.get(feature);

        if (installedExtensionsForFeature == null) {
            return null;
        }

        DefaultInstalledExtension installedExtension = installedExtensionsForFeature.get(namespace);

        if (installedExtension == null && namespace != null) {
            installedExtension = getInstalledExtensionFromCache(feature, null);
        }

        return installedExtension;
    }

    // Repository

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        LocalExtension localExtension = this.extensions.get(extensionId);

        if (localExtension == null) {
            throw new ResolveException("Can't find extension [" + extensionId + "]");
        }

        return localExtension;
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        return this.extensions.containsKey(extensionId);
    }

    // LocalRepository

    @Override
    public Collection<LocalExtension> getLocalExtensions()
    {
        return Collections.<LocalExtension> unmodifiableCollection(this.extensions.values());
    }

    @Override
    public List<LocalExtension> getInstalledExtensions(String namespace)
    {
        List<LocalExtension> result = new ArrayList<LocalExtension>(this.extensions.size());
        for (LocalExtension localExtension : this.extensions.values()) {
            if (localExtension.isInstalled(namespace)) {
                result.add(localExtension);
            }
        }

        return result;
    }

    @Override
    public List<LocalExtension> getInstalledExtensions()
    {
        List<LocalExtension> result = new ArrayList<LocalExtension>(this.extensions.size());
        for (LocalExtension localExtension : this.extensions.values()) {
            if (localExtension.isInstalled()) {
                result.add(localExtension);
            }
        }

        return result;
    }

    @Override
    public LocalExtension getInstalledExtension(String feature, String namespace)
    {
        DefaultInstalledExtension installedExtension = getInstalledExtensionFromCache(feature, namespace);

        if (installedExtension != null) {
            return installedExtension.getExtension();
        }

        return null;
    }

    /**
     * Create a new local extension from a remote extension.
     * 
     * @param extension the extension to copy
     * @return the new local extension
     */
    private DefaultLocalExtension createExtension(Extension extension)
    {
        DefaultLocalExtension localExtension = new DefaultLocalExtension(this, extension);

        localExtension.setFile(this.storage.getExtensionFile(localExtension.getId(), localExtension.getType()));

        return localExtension;
    }

    @Override
    public int countExtensions()
    {
        return this.extensions.size();
    }

    @Override
    public LocalExtension storeExtension(Extension extension) throws LocalExtensionRepositoryException
    {
        DefaultLocalExtension localExtension = this.extensions.get(extension.getId());

        if (localExtension == null) {
            try {
                localExtension = createExtension(extension);

                // Store extension in the local repository
                FileOutputStream fos = FileUtils.openOutputStream(localExtension.getFile().getFile());
                try {
                    InputStream is = extension.getFile().openStream();
                    try {
                        IOUtils.copy(is, fos);
                    } finally {
                        is.close();
                    }
                } finally {
                    fos.close();
                }
                this.storage.saveDescriptor(localExtension);

                // Cache extension
                addLocalExtension(localExtension);
            } catch (Exception e) {
                // TODO: clean

                throw new LocalExtensionRepositoryException("Failed to save extensoin [" + extension + "] descriptor",
                    e);
            }
        } else {
            throw new LocalExtensionRepositoryException("Extension [" + extension
                + "] already exists in local repository");
        }

        return localExtension;
    }

    @Override
    public void removeExtension(LocalExtension extension) throws ResolveException
    {
        LocalExtension localExtension = (LocalExtension) resolve(extension.getId());

        this.storage.removeExtension(localExtension);
    }

    @Override
    public void installExtension(LocalExtension extension, String namespace, boolean dependency)
        throws InstallException
    {
        DefaultLocalExtension localExtension = this.extensions.get(extension.getId());

        if (localExtension != null) {
            if (extension.isInstalled(namespace)) {
                if (localExtension.isDependency() == dependency) {
                    throw new InstallException("The extension [" + localExtension
                        + "] is already installed on namespace [" + namespace + "]");
                }

                localExtension.setDependency(dependency);
            } else {
                if (dependency || localExtension.getProperty(LocalExtension.PKEY_DEPENDENCY) == null) {
                    localExtension.setDependency(dependency);
                }

                installLocalExtension(localExtension, namespace);
            }
        } else {
            // Should be a very rare use case since we explicitly ask for a LocalExtension
            throw new InstallException("The extension [" + extension + "] need to be stored first");
        }
    }

    @Override
    public void uninstallExtension(LocalExtension localExtension, String namespace) throws UninstallException
    {
        LocalExtension existingExtension = getInstalledExtension(localExtension.getId().getId(), namespace);

        if (existingExtension == localExtension) {
            uninstallLocalExtension((DefaultLocalExtension) localExtension, namespace);
        }
    }

    @Override
    public Collection<LocalExtension> getBackwardDependencies(String feature, String namespace) throws ResolveException
    {
        if (getInstalledExtension(feature, namespace) == null) {
            throw new ResolveException("Extension [" + feature + "] is not installed on namespace [" + namespace + "]");
        }

        Map<String, DefaultInstalledExtension> installedExtensionsByFeature = this.installedExtensions.get(feature);
        if (installedExtensionsByFeature != null) {
            DefaultInstalledExtension installedExtension = installedExtensionsByFeature.get(namespace);

            if (installedExtension != null) {
                Set<DefaultLocalExtension> backwardDependencies = installedExtension.getBackwardDependencies();

                // copy the list to allow use cases like uninstalling all backward dependencies without getting a
                // concurrent issue on the list
                return backwardDependencies != null ? new ArrayList<LocalExtension>(backwardDependencies) : Collections
                    .<LocalExtension> emptyList();
            }
        }

        return Collections.<LocalExtension> emptyList();
    }

    @Override
    public Map<String, Collection<LocalExtension>> getBackwardDependencies(ExtensionId extensionId)
        throws ResolveException
    {
        Map<String, Collection<LocalExtension>> result;

        DefaultLocalExtension localExtension = (DefaultLocalExtension) resolve(extensionId);

        Collection<String> namespaces = localExtension.getNamespaces();

        Map<String, DefaultInstalledExtension> installedExtensionsByFeature =
            this.installedExtensions.get(localExtension.getId().getId());

        if (installedExtensionsByFeature != null) {
            result = new HashMap<String, Collection<LocalExtension>>();
            for (DefaultInstalledExtension installedExtension : installedExtensionsByFeature.values()) {
                if ((namespaces == null || namespaces.contains(installedExtension.getNamespace()))
                    && !installedExtension.getBackwardDependencies().isEmpty()) {
                    // copy the list to allow use cases like uninstalling all backward dependencies without getting a
                    // concurrent issue on the list
                    result.put(installedExtension.getNamespace(),
                        new ArrayList<LocalExtension>(installedExtension.getBackwardDependencies()));
                }
            }
        } else {
            result = Collections.emptyMap();
        }

        return result;
    }
}
