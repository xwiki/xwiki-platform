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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InvalidExtensionException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.LocalExtensionRepository;

/**
 * Default implementation of {@link LocalExtensionRepository}.
 * 
 * @version $Id$
 */
@Component
@Singleton
// TODO: make it threadsafe bulletproofs
public class DefaultLocalExtensionRepository implements LocalExtensionRepository, Initializable
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
     * Logging tool.
     */
    @Inject
    private Logger logger;

    /**
     * Used to read/write in the repository storage itself.
     */
    private DefaultLocalExtensionSerializer extensionSerializer;

    /**
     * The repository identifier.
     */
    private ExtensionRepositoryId repositoryId;

    /**
     * Local repository folder.
     */
    private File rootFolder;

    /**
     * the local extensions.
     */
    private Map<ExtensionId, LocalExtension> extensions = new ConcurrentHashMap<ExtensionId, LocalExtension>();

    /**
     * The local extensions grouped by ids.
     * <p>
     * <extension id, extensions>
     */
    private Map<String, List<LocalExtension>> extensionsById = new ConcurrentHashMap<String, List<LocalExtension>>();

    /**
     * The installed extensions backward dependencies.
     * <p>
     * <extension id, <namespace, extensions>>, used only for installed extensions
     */
    private Map<String, Map<String, Set<LocalExtension>>> backwardDependenciesMap =
        new ConcurrentHashMap<String, Map<String, Set<LocalExtension>>>();

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.rootFolder = this.configuration.getLocalRepository();

        this.repositoryId = new ExtensionRepositoryId("local", "xwiki", this.rootFolder.toURI());

        this.extensionSerializer = new DefaultLocalExtensionSerializer();

        loadExtensions();
    }

    /**
     * Load extension from repository storage.
     */
    private void loadExtensions()
    {
        // Load local extension from repository

        if (this.rootFolder.exists()) {
            FilenameFilter descriptorFilter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(".xed");
                }
            };

            for (File child : this.rootFolder.listFiles(descriptorFilter)) {
                if (!child.isDirectory()) {
                    try {
                        LocalExtension localExtension = loadDescriptor(child);

                        addLocalExtension(localExtension);
                    } catch (Exception e) {
                        this.logger.warn("Failed to load extension from file [" + child + "] in local repository", e);
                    }
                }
            }
        } else {
            this.rootFolder.mkdirs();
        }

        // Validate local extension

        Map<String, Set<String>> validatedExtension = new HashMap<String, Set<String>>();
        for (List<LocalExtension> extensionVersions : this.extensionsById.values()) {
            for (ListIterator<LocalExtension> it = extensionVersions.listIterator(extensionVersions.size()); it
                .hasPrevious();) {
                LocalExtension localExtension = it.previous();

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
    private void validateExtension(LocalExtension localExtension, Map<String, Set<String>> validatedExtensions)
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
    private void validateExtension(LocalExtension localExtension, Map<String, Set<String>> validatedExtensions,
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
    private void validateExtension(LocalExtension localExtension, Set<String> validatedExtensions, String namespace)
    {
        try {
            if (!localExtension.isInstalled(namespace)
                || this.coreExtensionRepository.exists(localExtension.getId().getId())) {
                // Impossible to overwrite core extensions
                ((DefaultLocalExtension) localExtension).setInstalled(false, namespace);
                return;
            }

            // Validate dependencies
            for (ExtensionDependency dependency : localExtension.getDependencies()) {
                boolean enabled = false;

                List<LocalExtension> dependencyVersions = this.extensionsById.get(dependency.getId());
                if (dependencyVersions != null) {
                    for (ListIterator<LocalExtension> it = dependencyVersions.listIterator(dependencyVersions.size()); it
                        .hasPrevious();) {
                        LocalExtension dependencyExtension = it.previous();

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
                    ((DefaultLocalExtension) localExtension).setInstalled(false, namespace);
                    return;
                }
            }

            // Complete local extension installation
            addLocalExtensionBackwardDependencies(localExtension, namespace);
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
            saveDescriptor(localExtension);
        } catch (Exception e) {
            throw new UninstallException("Failed to modify extension descriptor", e);
        }

        // Clean caches

        removeFromBackwardDependencies(localExtension);
    }

    /**
     * @param localExtension the extension to remove from backward dependencies map
     */
    private void removeFromBackwardDependencies(LocalExtension localExtension)
    {
        Collection<String> namespaces = localExtension.getNamespaces();

        if (namespaces == null) {
            this.backwardDependenciesMap.remove(localExtension.getId().getId());
        } else {
            Map<String, Set<LocalExtension>> namespaceBackwardDependencies =
                this.backwardDependenciesMap.get(localExtension.getId().getId());

            for (String namespace : namespaces) {
                namespaceBackwardDependencies.remove(namespace);
            }
        }
    }

    /**
     * Register a new local extension.
     * 
     * @param localExtension the new local extension
     */
    private void addLocalExtension(LocalExtension localExtension)
    {
        // extensions
        this.extensions.put(localExtension.getId(), localExtension);

        // versions
        List<LocalExtension> versions = this.extensionsById.get(localExtension.getId().getId());

        if (versions == null) {
            versions = new ArrayList<LocalExtension>();
            this.extensionsById.put(localExtension.getId().getId(), versions);

            versions.add(localExtension);
        } else {
            int index = 0;
            while (index < versions.size()
                && versionManager.compareVersions(localExtension.getId().getVersion(), versions.get(index).getId()
                    .getVersion()) > 0) {
                ++index;
            }

            versions.add(index, localExtension);
        }
    }

    /**
     * Register a new local extension on a specific namespace.
     * 
     * @param localExtension the local extension
     * @param namespace the namespace
     */
    private void addLocalExtension(DefaultLocalExtension localExtension, String namespace)
    {
        addLocalExtension(localExtension);

        localExtension.setInstalled(true, namespace);

        addLocalExtensionBackwardDependencies(localExtension, namespace);
    }

    /**
     * Register a newly installed extension in backward dependencies map.
     * 
     * @param localExtension the local extension to register
     * @param namespace the namespace
     */
    private void addLocalExtensionBackwardDependencies(LocalExtension localExtension, String namespace)
    {
        for (ExtensionDependency dependency : localExtension.getDependencies()) {
            Map<String, Set<LocalExtension>> namespaceBackwardDependencies =
                this.backwardDependenciesMap.get(dependency.getId());

            if (namespaceBackwardDependencies == null) {
                namespaceBackwardDependencies = new HashMap<String, Set<LocalExtension>>();
                this.backwardDependenciesMap.put(dependency.getId(), namespaceBackwardDependencies);
            }

            Set<LocalExtension> backwardDependencies = namespaceBackwardDependencies.get(namespace);
            if (backwardDependencies == null) {
                backwardDependencies = new HashSet<LocalExtension>();
                namespaceBackwardDependencies.put(namespace, backwardDependencies);
            }

            backwardDependencies.add(localExtension);
        }
    }

    /**
     * @return the repository folder
     */
    public File getRootFolder()
    {
        return this.rootFolder;
    }

    // Repository

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepository#resolve(org.xwiki.extension.ExtensionId)
     */
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        LocalExtension localExtension = this.extensions.get(extensionId);

        if (localExtension == null) {
            throw new ResolveException("Can't find extension [" + extensionId + "]");
        }

        return localExtension;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepository#exists(org.xwiki.extension.ExtensionId)
     */
    public boolean exists(ExtensionId extensionId)
    {
        return this.extensions.containsKey(extensionId);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepository#getId()
     */
    public ExtensionRepositoryId getId()
    {
        return this.repositoryId;
    }

    // LocalRepository

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.LocalExtensionRepository#getLocalExtensions()
     */
    public Collection<LocalExtension> getLocalExtensions()
    {
        return Collections.unmodifiableCollection(this.extensions.values());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.LocalExtensionRepository#getInstalledExtensions(java.lang.String)
     */
    public List<LocalExtension> getInstalledExtensions(String namespace)
    {
        List<LocalExtension> installedExtensions = new ArrayList<LocalExtension>(extensions.size());
        for (LocalExtension localExtension : this.extensions.values()) {
            if (localExtension.isInstalled(namespace)) {
                installedExtensions.add(localExtension);
            }
        }

        return installedExtensions;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.LocalExtensionRepository#getInstalledExtensions()
     */
    public List<LocalExtension> getInstalledExtensions()
    {
        List<LocalExtension> installedExtensions = new ArrayList<LocalExtension>(extensions.size());
        for (LocalExtension localExtension : this.extensions.values()) {
            if (localExtension.isInstalled()) {
                installedExtensions.add(localExtension);
            }
        }

        return installedExtensions;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.LocalExtensionRepository#getInstalledExtension(java.lang.String,
     *      java.lang.String)
     */
    public LocalExtension getInstalledExtension(String id, String namespace)
    {
        List<LocalExtension> versions = this.extensionsById.get(id);

        if (versions != null) {
            for (LocalExtension extension : versions) {
                if (extension.isInstalled(namespace)) {
                    return extension;
                }
            }
        }

        return null;
    }

    /**
     * Create a new local extension from a remote extension.
     * 
     * @param extension the extension to copy
     * @param dependency indicate if the extension is installed as dependency
     * @return the new local extension
     */
    private DefaultLocalExtension createExtension(Extension extension, boolean dependency)
    {
        DefaultLocalExtension localExtension = new DefaultLocalExtension(this, extension);

        localExtension.setDependency(dependency);

        localExtension.setFile(getExtensionFile(localExtension.getId(), localExtension.getType()));

        return localExtension;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.LocalExtensionRepository#countExtensions()
     */
    public int countExtensions()
    {
        return this.extensions.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.LocalExtensionRepository#installExtension(org.xwiki.extension.Extension,
     *      boolean, java.lang.String)
     */
    public LocalExtension installExtension(Extension extension, boolean dependency, String namespace)
        throws InstallException
    {
        DefaultLocalExtension localExtension = (DefaultLocalExtension) this.extensions.get(extension.getId());

        try {
            if (localExtension == null) {
                localExtension = createExtension(extension, dependency);
                extension.download(localExtension.getFile());
                addLocalExtension(localExtension, namespace);
                saveDescriptor(localExtension);
            } else if (!localExtension.isInstalled(namespace)) {
                addLocalExtension(localExtension, namespace);
                saveDescriptor(localExtension);
            }
        } catch (Exception e) {
            // TODO: clean

            throw new InstallException("Failed to save extensoin [" + extension + "] descriptor", e);
        }

        return localExtension;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.LocalExtensionRepository#uninstallExtension(org.xwiki.extension.LocalExtension,
     *      java.lang.String)
     */
    public void uninstallExtension(LocalExtension localExtension, String namespace) throws UninstallException
    {
        LocalExtension existingExtension = getInstalledExtension(localExtension.getId().getId(), namespace);

        if (existingExtension == localExtension) {
            uninstallLocalExtension((DefaultLocalExtension) localExtension, namespace);
        }
    }

    /***
     * Update the extension descriptor in the filesystem repository.
     * 
     * @param extension the local extension descriptor to save
     * @throws ParserConfigurationException error when trying to save the descriptor
     * @throws TransformerException error when trying to save the descriptor
     * @throws IOException error when trying to save the descriptor
     */
    private void saveDescriptor(LocalExtension extension) throws ParserConfigurationException, TransformerException,
        IOException
    {
        File file = getDescriptorFile(extension.getId());
        FileOutputStream fos = new FileOutputStream(file);

        try {
            this.extensionSerializer.saveDescriptor(extension, fos);
        } finally {
            fos.close();
        }
    }

    /**
     * @param id the extension identifier
     * @param type the extension type
     * @return the file containing the extension
     */
    private File getExtensionFile(ExtensionId id, String type)
    {
        return new File(getRootFolder(), getFileName(id, type));
    }

    /**
     * @param id the extension identifier
     * @return the file containing the extension descriptor
     */
    private File getDescriptorFile(ExtensionId id)
    {
        return new File(getRootFolder(), getFileName(id, "xed"));
    }

    /**
     * Get file path in the local extension repository.
     * 
     * @param id the extension id
     * @param fileExtension the file extension
     * @return the encoded file path
     */
    private String getFileName(ExtensionId id, String fileExtension)
    {
        String fileName = id.getId() + "-" + id.getVersion() + "." + fileExtension;
        try {
            return URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen

            return fileName;
        }
    }

    /**
     * Local extension descriptor from a file.
     * 
     * @param descriptor the descriptor file
     * @return the extension descriptor
     * @throws InvalidExtensionException error when trying to load extension descriptor
     */
    private LocalExtension loadDescriptor(File descriptor) throws InvalidExtensionException
    {
        FileInputStream fis;
        try {
            fis = new FileInputStream(descriptor);
        } catch (FileNotFoundException e) {
            throw new InvalidExtensionException("Failed to open descriptor for reading", e);
        }

        try {
            DefaultLocalExtension localExtension = this.extensionSerializer.loadDescriptor(this, fis);

            localExtension.setFile(getExtensionFile(localExtension.getId(), localExtension.getType()));

            if (!localExtension.getFile().exists()) {
                throw new InvalidExtensionException("Failed to load local extension [" + descriptor + "]: ["
                    + localExtension.getFile() + "] file does not exists");
            }

            return localExtension;
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                // TODO: log something
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.LocalExtensionRepository#getBackwardDependencies(java.lang.String,
     *      java.lang.String)
     */
    public Collection<LocalExtension> getBackwardDependencies(String id, String namespace) throws ResolveException
    {
        if (getInstalledExtension(id, namespace) == null) {
            throw new ResolveException("Extension [" + id + "] does is not installed");
        }

        Map<String, Set<LocalExtension>> namespaceBackwardDependenciesMap = this.backwardDependenciesMap.get(id);
        Set<LocalExtension> backwardDependencies =
            namespaceBackwardDependenciesMap != null ? namespaceBackwardDependenciesMap.get(id) : null;

        return backwardDependencies != null ? Collections.unmodifiableCollection(backwardDependencies) : Collections
            .<LocalExtension> emptyList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.LocalExtensionRepository#getBackwardDependencies(org.xwiki.extension.ExtensionId)
     */
    public Map<String, Collection<LocalExtension>> getBackwardDependencies(ExtensionId extensionId)
        throws ResolveException
    {
        Map<String, Collection<LocalExtension>> result;

        LocalExtension localExtension = (LocalExtension) resolve(extensionId);

        Collection<String> namespaces = localExtension.getNamespaces();

        Map<String, Set<LocalExtension>> backwardDependencies =
            this.backwardDependenciesMap.get(localExtension.getId().getId());
        if (backwardDependencies != null) {
            result = new HashMap<String, Collection<LocalExtension>>();
            for (Map.Entry<String, Set<LocalExtension>> entry : backwardDependencies.entrySet()) {
                if (namespaces == null || namespaces.contains(entry.getKey())) {
                    result.put(entry.getKey(), Collections.unmodifiableCollection(entry.getValue()));
                }
            }
        } else {
            result = Collections.emptyMap();
        }

        return result;
    }
}
