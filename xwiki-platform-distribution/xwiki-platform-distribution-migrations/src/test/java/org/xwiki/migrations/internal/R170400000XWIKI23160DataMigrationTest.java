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
package org.xwiki.migrations.internal;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.index.TaskManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.xar.XarEntry;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.migrations.internal.XWikiPropertiesMetaFieldCleanupTaskConsumer.TASK_NAME;

/**
 * Test of {@link R170400000XWIKI23160DataMigration}.
 *
 * @version $Id$
 */
@ComponentTest
class R170400000XWIKI23160DataMigrationTest
{
    private static final DocumentReference XWIKI_PREFERENCES_DOCUMENT_REFERENCE =
        new DocumentReference(new LocalDocumentReference(LOCAL_REFERENCE, Locale.ROOT),
            new WikiReference("xwiki"));

    @InjectMockComponents(role = HibernateDataMigration.class)
    private R170400000XWIKI23160DataMigration dataMigration;

    @MockComponent
    private Packager packager;

    @MockComponent
    private TaskManager taskManager;

    @MockComponent
    private Execution execution;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    private XarInstalledExtensionRepository xarInstalledExtensionRepository;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeComponent
    void componentsSetup() throws Exception
    {
        // We inject the InstalledExtensionRepository with in xar manually to control the concrete type of the component
        // as it's going to be cast in the tested code.
        this.xarInstalledExtensionRepository =
            this.componentManager.registerMockComponent(InstalledExtensionRepository.class, XarExtensionHandler.TYPE,
                XarInstalledExtensionRepository.class, true);
    }

    @BeforeEach
    void setUp()
    {
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.executionContext.getProperty("xwikicontext")).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getWikiReference()).thenReturn(new WikiReference("xwiki"));
    }

    @Test
    void hibernateMigrateNoMatchingXAR() throws Exception
    {
        when(this.wiki.getDocument(XWIKI_PREFERENCES_DOCUMENT_REFERENCE, this.context)).thenReturn(
            mock(XWikiDocument.class));
        XarInstalledExtension xarInstalledExtension = mock(XarInstalledExtension.class);
        when(xarInstalledExtension.getId()).thenReturn(new ExtensionId("extension-id-0"));
        when(this.xarInstalledExtensionRepository.getXarInstalledExtensions(XWIKI_PREFERENCES_DOCUMENT_REFERENCE))
            .thenReturn(List.of(xarInstalledExtension));

        this.dataMigration.hibernateMigrate();

        verifyNoInteractions(this.taskManager);
    }

    @Test
    void hibernateMigrateNoXObject() throws Exception
    {
        WikiReference xwiki = new WikiReference("xwiki");
        XarEntry xarEntry = new XarEntry(XWIKI_PREFERENCES_DOCUMENT_REFERENCE.getLocalDocumentReference(),
            "XWiki/XWikiPreferences.xml");
        ExtensionId extensionId = new ExtensionId("org.xwiki.platform:xwiki-platform-distribution-ui-base");

        XWikiDocument xwikiPreferencesDocument = mock(XWikiDocument.class);
        XWikiDocument xwikiPreferencesDocumentFromXar = mock(XWikiDocument.class);
        XarInstalledExtension xarInstalledExtension = mock(XarInstalledExtension.class);

        when(this.wiki.getDocument(XWIKI_PREFERENCES_DOCUMENT_REFERENCE, this.context))
            .thenReturn(xwikiPreferencesDocument);
        when(xarInstalledExtension.getId()).thenReturn(extensionId);
        when(this.xarInstalledExtensionRepository.getXarInstalledExtensions(XWIKI_PREFERENCES_DOCUMENT_REFERENCE))
            .thenReturn(List.of(xarInstalledExtension));
        when(this.packager.getXWikiDocument(xwiki, xarEntry, xarInstalledExtension))
            .thenReturn(xwikiPreferencesDocumentFromXar);

        this.dataMigration.hibernateMigrate();

        verifyNoInteractions(this.taskManager);
    }

    @Test
    void hibernateMigrateDifferentValues() throws Exception
    {
        WikiReference xwiki = new WikiReference("xwiki");
        XarEntry xarEntry = new XarEntry(XWIKI_PREFERENCES_DOCUMENT_REFERENCE.getLocalDocumentReference(),
            "XWiki/XWikiPreferences.xml");
        ExtensionId extensionId = new ExtensionId("org.xwiki.platform:xwiki-platform-distribution-ui-base");
        BaseObject xwikiPreferencesXObject = new BaseObject();
        xwikiPreferencesXObject.setStringValue("meta", "a");
        BaseObject xwikiPreferencesXObjectFromXar = new BaseObject();
        xwikiPreferencesXObjectFromXar.setStringValue("meta", "b");
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");

        XWikiDocument xwikiPreferencesDocument = mock(XWikiDocument.class);
        XWikiDocument xwikiPreferencesDocumentFromXar = mock(XWikiDocument.class);
        XarInstalledExtension xarInstalledExtension = mock(XarInstalledExtension.class);

        when(this.wiki.getDocument(XWIKI_PREFERENCES_DOCUMENT_REFERENCE, this.context))
            .thenReturn(xwikiPreferencesDocument);
        when(xarInstalledExtension.getId()).thenReturn(extensionId);
        when(this.xarInstalledExtensionRepository.getXarInstalledExtensions(XWIKI_PREFERENCES_DOCUMENT_REFERENCE))
            .thenReturn(List.of(xarInstalledExtension));
        when(this.packager.getXWikiDocument(xwiki, xarEntry, xarInstalledExtension))
            .thenReturn(xwikiPreferencesDocumentFromXar);

        when(xwikiPreferencesDocument.getXObject(classReference)).thenReturn(xwikiPreferencesXObject);
        when(xwikiPreferencesDocumentFromXar.getXObject(classReference)).thenReturn(xwikiPreferencesXObjectFromXar);

        this.dataMigration.hibernateMigrate();

        verifyNoInteractions(this.taskManager);
    }

    @Test
    void hibernateMigrateSameValues() throws Exception
    {
        WikiReference xwiki = new WikiReference("xwiki");
        XarEntry xarEntry = new XarEntry(XWIKI_PREFERENCES_DOCUMENT_REFERENCE.getLocalDocumentReference(),
            "XWiki/XWikiPreferences.xml");
        ExtensionId extensionId = new ExtensionId("org.xwiki.platform:xwiki-platform-distribution-ui-base");
        BaseObject xwikiPreferencesXObject = new BaseObject();
        xwikiPreferencesXObject.setStringValue("meta", "a");
        BaseObject xwikiPreferencesXObjectFromXar = new BaseObject();
        xwikiPreferencesXObjectFromXar.setStringValue("meta", "a");
        LocalDocumentReference classReference = new LocalDocumentReference("XWiki", "XWikiPreferences");

        XWikiDocument xwikiPreferencesDocument = mock(XWikiDocument.class);
        XWikiDocument xwikiPreferencesDocumentFromXar = mock(XWikiDocument.class);
        XarInstalledExtension xarInstalledExtension = mock(XarInstalledExtension.class);

        when(this.wiki.getDocument(XWIKI_PREFERENCES_DOCUMENT_REFERENCE, this.context))
            .thenReturn(xwikiPreferencesDocument);
        when(xarInstalledExtension.getId()).thenReturn(extensionId);
        when(this.xarInstalledExtensionRepository.getXarInstalledExtensions(XWIKI_PREFERENCES_DOCUMENT_REFERENCE))
            .thenReturn(List.of(xarInstalledExtension));
        when(this.packager.getXWikiDocument(xwiki, xarEntry, xarInstalledExtension))
            .thenReturn(xwikiPreferencesDocumentFromXar);

        when(xwikiPreferencesDocument.getXObject(classReference)).thenReturn(xwikiPreferencesXObject);
        when(xwikiPreferencesDocumentFromXar.getXObject(classReference)).thenReturn(xwikiPreferencesXObjectFromXar);

        when(xwikiPreferencesDocument.getId()).thenReturn(42L);

        this.dataMigration.hibernateMigrate();

        verify(this.taskManager).addTask("xwiki", 42L, TASK_NAME);
    }
}
