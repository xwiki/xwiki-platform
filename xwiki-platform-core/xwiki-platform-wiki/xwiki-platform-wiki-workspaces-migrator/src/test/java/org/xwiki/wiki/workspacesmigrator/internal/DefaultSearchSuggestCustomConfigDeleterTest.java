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
package org.xwiki.wiki.workspacesmigrator.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultSearchSuggestCustomConfigDeleterTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultSearchSuggestCustomConfigDeleter> mocker =
            new MockitoComponentMockingRule(DefaultSearchSuggestCustomConfigDeleter.class);

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
    }

    @Test
    public void deleteSearchSuggestCustomConfig() throws Exception
    {
        XWikiDocument searchSuggestConfigDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(new DocumentReference("mainWiki", "XWiki", "SearchSuggestConfig")),
                any(XWikiContext.class))).thenReturn(searchSuggestConfigDoc);
        BaseObject objConfig1 = mock(BaseObject.class);
        BaseObject objConfig2 = mock(BaseObject.class);
        BaseObject objConfig3 = mock(BaseObject.class);
        BaseObject objConfig4 = mock(BaseObject.class);
        BaseObject objConfig5 = mock(BaseObject.class);
        BaseObject objConfig6 = mock(BaseObject.class);
        List<BaseObject> objects = new ArrayList<BaseObject>();
        objects.add(objConfig1);
        objects.add(objConfig2);
        objects.add(objConfig3);
        // null objects can be present in the list
        objects.add(null);
        objects.add(objConfig4);
        objects.add(objConfig5);
        objects.add(objConfig6);
        when(searchSuggestConfigDoc.getXObjects(eq(new DocumentReference("mainWiki", "XWiki",
                "SearchSuggestSourceClass")))).thenReturn(objects);

        // Object 1
        when(objConfig1.getStringValue("name")).thenReturn("platform.workspace.searchSuggestSourceWorkspaces");
        when(objConfig1.getStringValue("engine")).thenReturn("solr");
        when(objConfig1.getStringValue("query")).thenReturn("class:XWiki.XWikiServerClass AND " +
                "propertyname:wikiprettyname AND propertyvalue__:(__INPUT__*)");
        when(objConfig1.getStringValue("url")).thenReturn("xwiki:WorkspaceManager.WorkspacesSuggestSolrService");

        // Object 2
        when(objConfig2.getStringValue("name")).thenReturn("Bad name");
        when(objConfig2.getStringValue("engine")).thenReturn("solr");
        when(objConfig2.getStringValue("query")).thenReturn("class:XWiki.XWikiServerClass AND " +
                "propertyname:wikiprettyname AND propertyvalue__:(__INPUT__*)");
        when(objConfig2.getStringValue("url")).thenReturn("xwiki:WorkspaceManager.WorkspacesSuggestSolrService");

        // Object 3
        when(objConfig3.getStringValue("name")).thenReturn("platform.workspace.searchSuggestSourceWorkspaces");
        when(objConfig3.getStringValue("engine")).thenReturn("lucene");
        when(objConfig3.getStringValue("query")).thenReturn("XWiki.XWikiServerClass.wikiprettyname:__INPUT__* AND " +
                "object:WorkspaceManager.WorkspaceClass");
        when(objConfig3.getStringValue("url")).thenReturn("xwiki:WorkspaceManager.WorkspacesSuggestLuceneService");

        // Object 4
        when(objConfig4.getStringValue("name")).thenReturn("platform.workspace.searchSuggestSourceWorkspaces");
        when(objConfig4.getStringValue("engine")).thenReturn(null);
        when(objConfig4.getStringValue("query")).thenReturn("XWiki.XWikiServerClass.wikiprettyname:__INPUT__* AND " +
                "object:WorkspaceManager.WorkspaceClass");
        when(objConfig4.getStringValue("url")).thenReturn("xwiki:WorkspaceManager.WorkspacesSuggestLuceneService");

        // Object 5
        when(objConfig5.getStringValue("name")).thenReturn("platform.workspace.searchSuggestSourceWorkspaces");
        when(objConfig5.getStringValue("engine")).thenReturn(null);
        when(objConfig5.getStringValue("query")).thenReturn("bad query");
        when(objConfig5.getStringValue("url")).thenReturn("xwiki:WorkspaceManager.WorkspacesSuggestLuceneService");

        // Object 6
        when(objConfig6.getStringValue("name")).thenReturn("platform.workspace.searchSuggestSourceWorkspaces");
        when(objConfig6.getStringValue("engine")).thenReturn(null);
        when(objConfig6.getStringValue("query")).thenReturn("XWiki.XWikiServerClass.wikiprettyname:__INPUT__* AND " +
                "object:WorkspaceManager.WorkspaceClass");
        when(objConfig6.getStringValue("url")).thenReturn("bad URL");

        // Run
        mocker.getComponentUnderTest().deleteSearchSuggestCustomConfig("mainWiki");

        // Verify that the good objects has been removed
        verify(searchSuggestConfigDoc).removeXObject(objConfig1);
        verify(searchSuggestConfigDoc).removeXObject(objConfig3);
        // Verify that the document have been saved
        verify(xwiki).saveDocument(searchSuggestConfigDoc,
                "Remove object previously introduced by WorkspaceManager.Install", xcontext);

    }

}
