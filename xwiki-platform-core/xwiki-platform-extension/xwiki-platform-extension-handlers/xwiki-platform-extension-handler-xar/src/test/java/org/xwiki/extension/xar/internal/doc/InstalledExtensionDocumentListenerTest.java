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

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.event.XarExtensionUninstalledEvent;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarPackage;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InstalledExtensionDocumentListener}.
 * 
 * @version $Id$
 * @since 11.10
 */
@ComponentTest
public class InstalledExtensionDocumentListenerTest
{
    @InjectMockComponents
    private InstalledExtensionDocumentListener listener;

    @MockComponent
    private InstalledExtensionDocumentTree tree;

    @MockComponent
    private Provider<Packager> packagerProvider;

    @MockComponent
    @Named(XarExtensionHandler.TYPE)
    private Provider<InstalledExtensionRepository> xarRepositoryProvider;

    @MockComponent
    private Provider<InstalledExtensionDocumentCustomizationDetector> customizationDetectorProvider;

    @MockComponent
    private Provider<WikiDescriptorManager> wikiDescriptorManagerProvider;

    @BeforeEach
    public void configure()
    {
        Packager packager = mock(Packager.class);
        when(this.packagerProvider.get()).thenReturn(packager);

        XarInstalledExtensionRepository installedXARs = mock(XarInstalledExtensionRepository.class);
        when(this.xarRepositoryProvider.get()).thenReturn(installedXARs);

        InstalledExtensionDocumentCustomizationDetector customizationDetector =
            mock(InstalledExtensionDocumentCustomizationDetector.class);
        when(this.customizationDetectorProvider.get()).thenReturn(customizationDetector);

        WikiDescriptorManager wikiDescriptorManager = mock(WikiDescriptorManager.class);
        when(this.wikiDescriptorManagerProvider.get()).thenReturn(wikiDescriptorManager);
    }

    @Test
    public void onDocumentUpdated()
    {
        DocumentReference alice = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference aliceFR = new DocumentReference(alice, Locale.FRENCH);
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(alice);
        when(document.getDocumentReferenceWithLocale()).thenReturn(aliceFR);

        // Content page.
        this.listener.onEvent(new DocumentUpdatedEvent(), document, null);
        verify(this.tree, never()).setCustomizedExtensionPage(eq(alice), any(Boolean.class));

        // Clean extension page.
        when(this.tree.isExtensionPage(alice)).thenReturn(true);
        this.listener.onEvent(new DocumentUpdatedEvent(), document, null);
        verify(this.tree).setCustomizedExtensionPage(aliceFR, false);

        // Customized extension page.
        when(this.customizationDetectorProvider.get().isCustomized(aliceFR)).thenReturn(true);
        this.listener.onEvent(new DocumentUpdatedEvent(), document, null);
        verify(this.tree).setCustomizedExtensionPage(aliceFR, true);
    }

    @Test
    public void onApplicationReady() throws Exception
    {
        when(this.wikiDescriptorManagerProvider.get().getCurrentWikiId()).thenReturn("test");

        XarInstalledExtension xarInstalledExtension = mock(XarInstalledExtension.class);
        when(this.xarRepositoryProvider.get().getInstalledExtensions("wiki:test"))
            .thenReturn(Collections.singleton(xarInstalledExtension));

        XarPackage xarPackage = mock(XarPackage.class);
        when(xarInstalledExtension.getXarPackage()).thenReturn(xarPackage);

        XarEntry xarEntry = mock(XarEntry.class);
        when(xarPackage.getEntries()).thenReturn(Collections.singleton(xarEntry));

        DocumentReference documentReference = new DocumentReference("test", "Some", "Page");
        DocumentReference documentReferenceWithLocale = new DocumentReference(documentReference, Locale.FRENCH);
        when(this.packagerProvider.get().getDocumentReferences(eq(Collections.singleton(xarEntry)),
            any(PackageConfiguration.class))).thenReturn(Collections.singletonList(documentReferenceWithLocale));
        when(this.customizationDetectorProvider.get().isCustomized(documentReferenceWithLocale)).thenReturn(true);

        this.listener.onEvent(new ApplicationReadyEvent(), null, null);

        verify(this.tree).addExtensionPage(documentReference);
        verify(this.tree).setCustomizedExtensionPage(documentReferenceWithLocale, true);
    }

    @Test
    public void onExtensionUninstalled() throws Exception
    {
        XarInstalledExtension xarInstalledExtension = mock(XarInstalledExtension.class);
        XarPackage xarPackage = mock(XarPackage.class);
        when(xarInstalledExtension.getXarPackage()).thenReturn(xarPackage);

        XarEntry firstXAREntry = mock(XarEntry.class, "first");
        XarEntry secondXAREntry = mock(XarEntry.class, "second");
        when(xarPackage.getEntries()).thenReturn(Arrays.asList(firstXAREntry, secondXAREntry));

        DocumentReference alice = new DocumentReference("test", "Users", "Alice");
        DocumentReference aliceWithLocale = new DocumentReference(alice, Locale.FRENCH);
        DocumentReference bob = new DocumentReference("test", "Users", "Bob");
        DocumentReference bobWithLocale = new DocumentReference(bob, Locale.FRENCH);
        when(this.packagerProvider.get().getDocumentReferences(eq(Arrays.asList(firstXAREntry, secondXAREntry)),
            any(PackageConfiguration.class))).thenReturn(Arrays.asList(aliceWithLocale, bobWithLocale));

        // Bob page is part of another extension.
        XarInstalledExtension otherXARInstalledExtension = mock(XarInstalledExtension.class, "other");
        when(((XarInstalledExtensionRepository) this.xarRepositoryProvider.get())
            .getXarInstalledExtensions(bobWithLocale)).thenReturn(Collections.singleton(otherXARInstalledExtension));

        ExtensionUninstalledEvent extensionUninstalledEvent =
            new ExtensionUninstalledEvent(new ExtensionId("org.xwiki.test:test"), "wiki:test");
        this.listener.onEvent(new XarExtensionUninstalledEvent(extensionUninstalledEvent), xarInstalledExtension, null);

        verify(this.tree).removeExtensionPage(alice);
        verify(this.tree, never()).removeExtensionPage(bob);
        verify(this.tree).setCustomizedExtensionPage(bobWithLocale, false);
    }
}
