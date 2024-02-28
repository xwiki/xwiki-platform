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
package org.xwiki.extension.xar.internal.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.internal.installed.AbstractInstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.UnsupportedNamespaceException;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.XarHandlerUtils;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarEntryType;
import org.xwiki.xar.XarEntryTypeResolver;
import org.xwiki.xar.XarException;

/**
 * Local repository proxy for XAR extensions.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
@Named(XarExtensionHandler.TYPE)
public class XarInstalledExtensionRepository extends AbstractInstalledExtensionRepository<XarInstalledExtension>
    implements Initializable
{
    @Inject
    private transient InstalledExtensionRepository installedRepository;

    @Inject
    private transient XarEntryTypeResolver typeResolver;

    @Inject
    private EntityReferenceFactory referenceFactory;

    /**
     * Index used to find extensions owners of a document installed on a specific wiki.
     */
    private Map<DocumentReference, Collection<XarInstalledExtension>> documents = new ConcurrentHashMap<>();

    /**
     * Index used to find extensions owners of a document installed on root namespace.
     */
    private Map<LocalDocumentReference, Collection<XarInstalledExtension>> rootDocuments = new ConcurrentHashMap<>();

    @Override
    public void initialize() throws InitializationException
    {
        setDescriptor(new DefaultExtensionRepositoryDescriptor(XarExtensionHandler.TYPE, XarExtensionHandler.TYPE,
            this.installedRepository.getDescriptor().getURI()));

        loadExtensions();
    }

    void pagesRemoved(ExtensionId extensionId, String namespace) throws UnsupportedNamespaceException
    {
        pagesUpdated(extensionId, namespace, false);
    }

    void pagesAdded(ExtensionId extensionId, String namespace) throws UnsupportedNamespaceException
    {
        pagesUpdated(extensionId, namespace, true);
    }

    private void pagesUpdated(ExtensionId extensionId, String namespace, boolean add)
        throws UnsupportedNamespaceException
    {
        XarInstalledExtension installedExtension = (XarInstalledExtension) getInstalledExtension(extensionId);

        pagesUpdated(installedExtension, namespace, add);
    }

    private void pagesUpdated(XarInstalledExtension installedExtension, String namespace, boolean add)
        throws UnsupportedNamespaceException
    {
        if (installedExtension != null) {
            for (XarEntry xarEntry : installedExtension.getXarPackage().getEntries()) {
                if (namespace != null) {
                    DocumentReference reference = new DocumentReference(xarEntry,
                        new WikiReference(XarHandlerUtils.getWikiFromNamespace(namespace)));

                    synchronized (this.documents) {
                        Collection<XarInstalledExtension> referenceExtensions = this.documents.get(reference);
                        if (referenceExtensions != null || add) {
                            Set<XarInstalledExtension> newSet = referenceExtensions != null
                                ? new LinkedHashSet<>(referenceExtensions) : new LinkedHashSet<>();

                            if (add) {
                                newSet.add(installedExtension);
                            } else {
                                newSet.remove(installedExtension);
                            }

                            this.documents.put(this.referenceFactory.getReference(reference), newSet);
                        }
                    }
                } else {
                    synchronized (this.rootDocuments) {
                        Collection<XarInstalledExtension> referenceExtensions = this.rootDocuments.get(xarEntry);
                        if (referenceExtensions != null || add) {
                            Set<XarInstalledExtension> newSet = referenceExtensions != null
                                ? new LinkedHashSet<>(referenceExtensions) : new LinkedHashSet<>();

                            if (add) {
                                newSet.add(installedExtension);
                            } else {
                                newSet.remove(installedExtension);
                            }

                            this.rootDocuments.put(xarEntry, newSet);
                        }
                    }
                }
            }
        }
    }

    void updateCachedXarExtension(ExtensionId extensionId)
    {
        InstalledExtension installedExtension = this.installedRepository.getInstalledExtension(extensionId);

        if (installedExtension != null && XarExtensionHandler.TYPE.equals(installedExtension.getType())) {
            if (getInstalledExtension(installedExtension.getId()) == null) {
                try {
                    addCacheXarExtension(installedExtension);
                } catch (Exception e) {
                    this.logger.error("Failed to parse extension [{}]", installedExtension.getId(), e);
                }
            }
        } else {
            removeCachedXarExtension(extensionId);
        }
    }

    private XarInstalledExtension addCacheXarExtension(InstalledExtension installedExtension)
        throws IOException, XarException
    {
        XarInstalledExtension xarExtension = new XarInstalledExtension(installedExtension, this);

        addCachedExtension(xarExtension);

        return xarExtension;
    }

    protected void removeCachedXarExtension(ExtensionId extensionId)
    {
        XarInstalledExtension extension = (XarInstalledExtension) getInstalledExtension(extensionId);

        if (extension != null) {
            super.removeCachedExtension(extension);
        }
    }

    private void loadExtensions()
    {
        for (InstalledExtension localExtension : this.installedRepository.getInstalledExtensions()) {
            if (XarExtensionHandler.TYPE.equalsIgnoreCase(localExtension.getType())) {
                try {
                    // Add XAR extension to the cache
                    XarInstalledExtension xarInstalledExtension = addCacheXarExtension(localExtension);

                    // Add extension pages to the index
                    if (xarInstalledExtension.getNamespaces() == null) {
                        pagesUpdated(xarInstalledExtension, null, true);
                    } else {
                        for (String namespace : localExtension.getNamespaces()) {
                            pagesUpdated(xarInstalledExtension, namespace, true);
                        }
                    }
                } catch (Exception e) {
                    this.logger.error("Failed to parse extension [{}]", localExtension.getId(), e);

                    continue;
                }
            }
        }
    }

    /**
     * @param reference the reference of the document
     * @return the extension owners of the passed document
     * @since 8.1M2
     */
    public Collection<XarInstalledExtension> getXarInstalledExtensions(DocumentReference reference)
    {
        if (reference instanceof DocumentVersionReference versionReference) {
            if (versionReference.getVersion() instanceof ExtensionId extensionId) {
                if (extensionId != null) {
                    return Arrays.asList((XarInstalledExtension) getInstalledExtension(extensionId));
                }
            }
        }

        Collection<XarInstalledExtension> wikiExtensions = this.documents
            .get(reference.getLocale() == null ? new DocumentReference(reference, Locale.ROOT) : reference);
        Collection<XarInstalledExtension> rootExtensions =
            this.rootDocuments.get(reference.getLocalDocumentReference().getLocale() == null
                ? new LocalDocumentReference(reference.getLocalDocumentReference(), Locale.ROOT)
                : reference.getLocalDocumentReference());

        List<XarInstalledExtension> allExtensions = new ArrayList<>();

        if (wikiExtensions != null) {
            allExtensions.addAll(wikiExtensions);
        }

        if (rootExtensions != null) {
            allExtensions.addAll(rootExtensions);
        }

        return allExtensions;
    }

    /**
     * @param documentReference the reference of the document
     * @param right the right to check on the passed document
     * @return true if the passed right is allowed on the passed document. True unless explicitly indicated otherwise by
     *         a {@link XarEntryType}.
     * @since 10.3
     */
    public boolean isAllowed(DocumentReference documentReference, Right right)
    {
        Collection<XarInstalledExtension> extensions = getXarInstalledExtensions(documentReference);

        LocalDocumentReference localDocumentReference = documentReference.getLocalDocumentReference();

        // Make sure there is a locale
        if (localDocumentReference.getLocale() == null) {
            localDocumentReference = new LocalDocumentReference(localDocumentReference, Locale.ROOT);
        }

        for (XarInstalledExtension extension : extensions) {
            XarEntry entry = extension.getXarPackage().getEntry(localDocumentReference);

            XarEntryType type = this.typeResolver.resolve(entry, true);

            boolean allowed;
            if (Right.EDIT.equals(right)) {
                allowed = type.isEditAllowed();
            } else if (Right.DELETE.equals(right)) {
                allowed = type.isDeleteAllowed();
            } else {
                allowed = true;
            }

            if (!allowed) {
                return false;
            }
        }

        return true;
    }

    // InstalledExtensionRepository

    @Override
    public InstalledExtension getInstalledExtension(String id, String namespace)
    {
        InstalledExtension extension = this.installedRepository.getInstalledExtension(id, namespace);

        if (extension != null) {
            if (XarExtensionHandler.TYPE.equals(extension.getType())) {
                extension = this.extensions.get(extension.getId());
            } else {
                extension = null;
            }
        }

        return extension;
    }

    @Override
    public InstalledExtension installExtension(LocalExtension extension, String namespace, boolean dependency,
        Map<String, Object> properties) throws InstallException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void uninstallExtension(InstalledExtension extension, String namespace) throws UninstallException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<InstalledExtension> getBackwardDependencies(String id, String namespace, boolean withOptional)
        throws ResolveException
    {
        InstalledExtension extension = this.installedRepository.getInstalledExtension(id, namespace);

        return XarExtensionHandler.TYPE.equals(extension.getType())
            ? this.installedRepository.getBackwardDependencies(id, namespace, withOptional) : null;
    }

    @Override
    public Map<String, Collection<InstalledExtension>> getBackwardDependencies(ExtensionId extensionId,
        boolean withOptional) throws ResolveException
    {
        InstalledExtension extension = this.installedRepository.resolve(extensionId);

        return XarExtensionHandler.TYPE.equals(extension.getType())
            ? this.installedRepository.getBackwardDependencies(extensionId) : null;
    }
}
