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
package org.xwiki.wiki.user.internal;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@AllComponents
public class WikiUserFromWorkspaceMigrationTest
{
    @Rule
    public MockitoComponentMockingRule<WikiUserFromWorkspaceMigration> mocker =
            new MockitoComponentMockingRule(WikiUserFromWorkspaceMigration.class, HibernateDataMigration.class,
                    "R530000WikiUserFromWorkspaceMigration");

    private MockitoOldcoreRule oldcore;

    private WikiDescriptorManager wikiDescriptorManager;

    private WikiUserConfigurationHelper wikiUserConfigurationHelper;

    private Execution execution;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        oldcore = new MockitoOldcoreRule(this.mocker);
        //Utils.setComponentManager(mocker);

        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        wikiUserConfigurationHelper = mocker.getInstance(WikiUserConfigurationHelper.class);
        execution = mock(Execution.class);
        mocker.registerComponent(Execution.class, execution);
        xcontext = oldcore.getXWikiContext();
        xwiki = oldcore.getMockXWiki();

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xcontext);
        //when(xcontext.getWiki()).thenReturn(oldcore.getMockXWiki());
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
    }

    @Test
    public void restoreDeletedDocuments() throws Exception
    {
        XWikiDocument workspaceInstallDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(new DocumentReference("mainWiki", "WorkspaceManager", "Install")),
                any(XWikiContext.class))).thenReturn(workspaceInstallDoc);
        XWikiAttachment xeXar = mock(XWikiAttachment.class);
        when(workspaceInstallDoc.getAttachment("workspace-template.xar")).thenReturn(xeXar);
        when(xeXar.getContentInputStream(any(XWikiContext.class))).thenReturn(
                getClass().getResourceAsStream("/test-restore-documents.xar"));

        XWikiDocument documentToRestore = mock(XWikiDocument.class);
        //when(xwiki.getDocument(eq(new DocumentReference("wikiid1", "XWiki", "AdminRegistrationSheet")),
        //                any(XWikiContext.class))).thenReturn(documentToRestore);

        this.mocker.getComponentUnderTest().restoreDeletedDocuments("wikiid1");

        verify(documentToRestore).fromXML(any(InputStream.class));
        //verify(xwiki).saveDocument(documentToRestore, xcontext);
    }


}
