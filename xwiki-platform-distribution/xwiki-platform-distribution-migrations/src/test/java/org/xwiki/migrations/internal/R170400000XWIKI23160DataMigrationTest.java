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

import java.util.Locale;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.index.TaskManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository installedExtensionRepository;

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

    @BeforeEach
    void setUp()
    {
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.executionContext.getProperty("xwikicontext")).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getWikiReference()).thenReturn(new WikiReference("xwiki"));
    }

    @Test
    void hibernateMigrate() throws Exception
    {
        when(this.wiki.getDocument(XWIKI_PREFERENCES_DOCUMENT_REFERENCE, this.context)).thenReturn(
            mock(XWikiDocument.class));

        this.dataMigration.hibernateMigrate();
    }
}
