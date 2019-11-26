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
package org.xwiki.extension.xar.internal.doc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.event.XarExtensionInstalledEvent;
import org.xwiki.extension.xar.internal.event.XarExtensionUninstalledEvent;
import org.xwiki.extension.xar.internal.event.XarExtensionUpgradedEvent;
import org.xwiki.extension.xar.internal.handler.UnsupportedNamespaceException;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.XarHandlerUtils;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Keeps the {@link InstalledExtensionDocumentTree} up to date with respect to documents coming from installed XAR
 * extensions.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component
@Named(InstalledExtensionDocumentListener.HINT)
@Singleton
public class InstalledExtensionDocumentListener extends AbstractEventListener
{
    /**
     * The component hint.
     */
    public static final String HINT = "InstalledExtensionDocumentListener";

    @Inject
    private Logger logger;

    @Inject
    private InstalledExtensionDocumentTree tree;

    @Inject
    private Provider<Packager> packagerProvider;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private Provider<InstalledExtensionRepository> xarRepositoryProvider;

    @Inject
    private Provider<InstalledExtensionDocumentCustomizationDetector> customizationDetectorProvider;

    @Inject
    private Provider<WikiDescriptorManager> wikiDescriptorManagerProvider;

    /**
     * Default constructor.
     */
    public InstalledExtensionDocumentListener()
    {
        super(HINT, new ApplicationReadyEvent(), new WikiReadyEvent(), new DocumentUpdatedEvent(),
            new XarExtensionInstalledEvent(), new XarExtensionUpgradedEvent(), new XarExtensionUninstalledEvent());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentUpdatedEvent) {
            XWikiDocument document = (XWikiDocument) source;
            if (this.tree.isExtensionPage(document.getDocumentReference())) {
                updateCustomizedFlag(document.getDocumentReferenceWithLocale());
            }
        } else if (event instanceof ApplicationReadyEvent) {
            addExtensionDocuments(this.wikiDescriptorManagerProvider.get().getCurrentWikiId());
        } else if (event instanceof WikiReadyEvent) {
            addExtensionDocuments((String) source);
        } else if (event instanceof XarExtensionInstalledEvent) {
            addExtensionDocuments((ExtensionEvent) event, (InstalledExtension) source);
        } else if (event instanceof XarExtensionUninstalledEvent) {
            removeExtensionDocuments((ExtensionEvent) event, (InstalledExtension) source);
        } else if (event instanceof XarExtensionUpgradedEvent) {
            ((Collection<InstalledExtension>) data).stream().forEach(
                oldInstalledExtension -> removeExtensionDocuments((ExtensionEvent) event, oldInstalledExtension));
            addExtensionDocuments((ExtensionEvent) event, (InstalledExtension) source);
        }
    }

    private void updateCustomizedFlag(DocumentReference documentReferenceWithLocale)
    {
        this.tree.setCustomizedExtensionPage(documentReferenceWithLocale,
            this.customizationDetectorProvider.get().isCustomized(documentReferenceWithLocale));
    }

    private void addExtensionDocuments(String wiki)
    {
        // Add extension documents from the XAR extensions installed on the specified wiki.
        String namespace = "wiki:" + wiki;
        this.xarRepositoryProvider.get().getInstalledExtensions(namespace).stream()
            .forEach(installedExtension -> addExtensionDocuments(
                new XarExtensionInstalledEvent(new ExtensionInstalledEvent(installedExtension.getId(), namespace)),
                installedExtension));
    }

    private void addExtensionDocuments(ExtensionEvent extensionEvent, InstalledExtension installedExtension)
    {
        forEachExtensionDocument(extensionEvent, installedExtension, this::addExtensionDocument);
    }

    private void addExtensionDocument(DocumentReference documentReference)
    {
        this.tree.addExtensionPage(withoutLocale(documentReference));
        updateCustomizedFlag(documentReference);
    }

    private void removeExtensionDocuments(ExtensionEvent extensionEvent, InstalledExtension installedExtension)
    {
        forEachExtensionDocument(extensionEvent, installedExtension, this::removeExtensionDocument);
    }

    private void forEachExtensionDocument(ExtensionEvent extensionEvent, InstalledExtension installedExtension,
        Consumer<DocumentReference> action)
    {
        if (extensionEvent.hasNamespace() && installedExtension instanceof XarInstalledExtension) {
            getExtensionDocuments((XarInstalledExtension) installedExtension, extensionEvent.getNamespace()).stream()
                .forEach(action);
        }
    }

    private void removeExtensionDocument(DocumentReference documentReference)
    {
        if (getXarInstalledExtensions(documentReference).isEmpty()) {
            // There is no extension left to own the specified page.
            this.tree.removeExtensionPage(withoutLocale(documentReference));
        } else {
            // The specified page is part of another extension.
            updateCustomizedFlag(documentReference);
        }
    }

    private DocumentReference withoutLocale(DocumentReference documentReference)
    {
        return new DocumentReference(documentReference, (Locale) null);
    }

    private Collection<XarInstalledExtension> getXarInstalledExtensions(DocumentReference documentReference)
    {
        return ((XarInstalledExtensionRepository) this.xarRepositoryProvider.get())
            .getXarInstalledExtensions(documentReference);
    }

    private Set<DocumentReference> getExtensionDocuments(XarInstalledExtension xarInstalledExtension, String namespace)
    {
        try {
            PackageConfiguration configuration = new PackageConfiguration();
            configuration.setWiki(XarHandlerUtils.getWikiFromNamespace(namespace));
            return new HashSet<>(this.packagerProvider.get()
                .getDocumentReferences(xarInstalledExtension.getXarPackage().getEntries(), configuration));
        } catch (UnsupportedNamespaceException e) {
            this.logger.warn("Unsupported namespace [{}].", namespace);
        } catch (Exception e) {
            this.logger.error("Failed to retrieve the list of documents from the XAR package [{}].",
                xarInstalledExtension.getId(), e);
        }

        return Collections.emptySet();
    }
}
