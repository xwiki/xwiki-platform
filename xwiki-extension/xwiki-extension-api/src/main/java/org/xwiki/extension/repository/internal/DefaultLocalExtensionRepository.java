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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
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
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.LocalExtensionRepository;

//TODO: make it threadsafe bulletproof
@Component
public class DefaultLocalExtensionRepository extends AbstractLogEnabled implements LocalExtensionRepository,
    Initializable
{
    @Requirement
    private ExtensionManagerConfiguration configuration;

    @Requirement
    private CoreExtensionRepository coreExtensionRepository;

    @Requirement
    private VersionManager versionManager;

    private DefaultLocalExtensionSerializer extensionSerializer;

    private ExtensionRepositoryId repositoryId;

    private File rootFolder;

    private Map<ExtensionId, LocalExtension> extensions = new ConcurrentHashMap<ExtensionId, LocalExtension>();

    /**
     * <extension id, extensions>
     */
    private Map<String, List<LocalExtension>> extensionsVersions =
        new ConcurrentHashMap<String, List<LocalExtension>>();

    /**
     * <extension id, <namespace, extensions>>, used only for installed extensions
     */
    private Map<String, Map<String, Set<LocalExtension>>> backwardDependenciesMap =
        new ConcurrentHashMap<String, Map<String, Set<LocalExtension>>>();

    public void initialize() throws InitializationException
    {
        this.rootFolder = this.configuration.getLocalRepository();

        this.repositoryId = new ExtensionRepositoryId("local", "xwiki", this.rootFolder.toURI());

        this.extensionSerializer = new DefaultLocalExtensionSerializer(this);

        loadExtensions();
    }

    private void loadExtensions()
    {
        // Load local extension from repository

        File rootFolder = getRootFolder();

        if (rootFolder.exists()) {
            FilenameFilter descriptorFilter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(".xed");
                }
            };

            for (File child : rootFolder.listFiles(descriptorFilter)) {
                if (!child.isDirectory()) {
                    try {
                        LocalExtension localExtension = loadDescriptor(child);

                        addLocalExtension(localExtension);
                    } catch (Exception e) {
                        getLogger().warn("Failed to load extension from file [" + child + "] in local repository", e);
                    }
                }
            }
        } else {
            rootFolder.mkdirs();
        }

        // Validate local extension

        Map<String, Set<String>> validatedExtension = new HashMap<String, Set<String>>();
        for (List<LocalExtension> extensionVersions : this.extensionsVersions.values()) {
            for (ListIterator<LocalExtension> it = extensionVersions.listIterator(extensionVersions.size()); it
                .hasPrevious();) {
                LocalExtension localExtension = it.previous();

                validateExtension(localExtension, validatedExtension);
            }
        }
    }

    private void validateExtension(LocalExtension localExtension, Map<String, Set<String>> validatedExtension)
    {
        if (localExtension.getNamespaces() == null) {
            validateExtension(localExtension, validatedExtension, null);
        } else {
            for (String namespace : localExtension.getNamespaces()) {
                validateExtension(localExtension, validatedExtension, namespace);
            }
        }
    }

    private void validateExtension(LocalExtension localExtension, Map<String, Set<String>> validatedExtension,
        String namespace)
    {
        Set<String> extensions = validatedExtension.get(namespace);
        if (extensions == null) {
            extensions = new HashSet<String>();
            validatedExtension.put(namespace, extensions);
        }

        if (!extensions.contains(localExtension.getId().getId())) {
            validateExtension(localExtension, extensions, namespace);
        }
    }

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

                List<LocalExtension> dependencyVersions = this.extensionsVersions.get(dependency.getId());
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

    private void addLocalExtension(LocalExtension localExtension)
    {
        // extensions
        this.extensions.put(localExtension.getId(), localExtension);

        // versions
        List<LocalExtension> extensionsVersions = this.extensionsVersions.get(localExtension.getId().getId());

        if (extensionsVersions == null) {
            extensionsVersions = new ArrayList<LocalExtension>();
            this.extensionsVersions.put(localExtension.getId().getId(), extensionsVersions);

            extensionsVersions.add(localExtension);
        } else {
            int index = 0;
            while (index < extensionsVersions.size()
                && versionManager.compareVersions(localExtension.getId().getVersion(), extensionsVersions.get(index)
                    .getId().getVersion()) > 0) {
                ++index;
            }

            extensionsVersions.add(index, localExtension);
        }
    }

    private void addLocalExtension(LocalExtension localExtension, String namespace)
    {
        addLocalExtension(localExtension);

        addLocalExtensionBackwardDependencies(localExtension, namespace);
    }

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

    public File getRootFolder()
    {
        return this.rootFolder;
    }

    // Repository

    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        LocalExtension localExtension = this.extensions.get(extensionId);

        if (localExtension == null) {
            throw new ResolveException("Can't find extension [" + extensionId + "]");
        }

        return localExtension;
    }

    public boolean exists(ExtensionId extensionId)
    {
        return this.extensions.containsKey(extensionId);
    }

    public ExtensionRepositoryId getId()
    {
        return this.repositoryId;
    }

    // LocalRepository

    public Collection<LocalExtension> getLocalExtensions()
    {
        return Collections.unmodifiableCollection(this.extensions.values());
    }

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

    public LocalExtension getInstalledExtension(String id, String namespace)
    {
        List<LocalExtension> versions = this.extensionsVersions.get(id);

        if (versions != null) {
            for (LocalExtension extension : versions) {
                if (extension.isInstalled(namespace)) {
                    return extension;
                }
            }
        }

        return null;
    }

    private LocalExtension createExtension(Extension extension, boolean dependency)
    {
        DefaultLocalExtension localExtension = new DefaultLocalExtension(this, extension);

        localExtension.setDependency(dependency);

        localExtension.setFile(getExtensionFile(localExtension.getId(), localExtension.getType()));

        return localExtension;
    }

    public int countExtensions()
    {
        return this.extensions.size();
    }

    public Collection< ? extends LocalExtension> getExtensions(int nb, int offset)
    {
        return new ArrayList<LocalExtension>(this.extensions.values()).subList(offset, offset + nb);
    }

    public LocalExtension installExtension(Extension extension, boolean dependency, String namespace)
        throws InstallException
    {
        LocalExtension localExtension = getInstalledExtension(extension.getId().getId(), namespace);

        if (localExtension == null || !extension.getId().getVersion().equals(localExtension.getId().getVersion())
            || (localExtension.getNamespaces() != null && !localExtension.getNamespaces().contains(namespace))) {
            localExtension = createExtension(extension, dependency);

            try {
                extension.download(localExtension.getFile());
                saveDescriptor(localExtension);
                addLocalExtension(localExtension, namespace);
            } catch (Exception e) {
                // TODO: clean

                throw new InstallException("Failed to download extension [" + extension + "]", e);
            }
        }

        return localExtension;
    }

    public void uninstallExtension(LocalExtension localExtension, String namespace) throws UninstallException
    {
        LocalExtension existingExtension = getInstalledExtension(localExtension.getId().getId(), namespace);

        if (existingExtension == localExtension) {
            uninstallLocalExtension((DefaultLocalExtension) localExtension, namespace);
        }
    }

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

    private File getExtensionFile(ExtensionId id, String type)
    {
        return new File(getRootFolder(), getFileName(id, type));
    }

    private File getDescriptorFile(ExtensionId id)
    {
        return new File(getRootFolder(), getFileName(id, "xed"));
    }

    private String getFileName(ExtensionId id, String extension)
    {
        String fileName = id.getId() + "-" + id.getVersion() + "." + extension;
        try {
            return URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen

            return fileName;
        }
    }
    
    private LocalExtension loadDescriptor(File descriptor) throws ParserConfigurationException, SAXException,
        IOException
    {
        FileInputStream fis = new FileInputStream(descriptor);

        try {
            DefaultLocalExtension localExtension = this.extensionSerializer.loadDescriptor(fis);

            localExtension.setFile(getExtensionFile(localExtension.getId(), localExtension.getType()));

            return localExtension;
        } finally {
            fis.close();
        }
    }

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
}
