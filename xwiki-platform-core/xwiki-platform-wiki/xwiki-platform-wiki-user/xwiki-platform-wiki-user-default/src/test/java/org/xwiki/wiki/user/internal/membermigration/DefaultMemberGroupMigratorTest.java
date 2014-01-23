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
package org.xwiki.wiki.user.internal.membermigration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryFilter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultMemberGroupMigrator}.
 *
 * @version $Id$
 * @since 5.4RC1
 */
public class DefaultMemberGroupMigratorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMemberGroupMigrator> mocker =
            new MockitoComponentMockingRule(DefaultMemberGroupMigrator.class);

    private WikiDescriptorManager wikiDescriptorManager;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null,
                DocumentReferenceResolver.class, String.class));
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));

        mocker.registerMockComponent(QueryFilter.class, "unique");

        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");

        xcontext = mock(XWikiContext.class);
        xwiki = mock(XWiki.class);

        when(xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
    }

    @Test
    public void migrateGroups() throws Exception
    {
        String userLocal1 = "XWiki.UserLocal1";
        String userLocal2 = "XWiki.UserLocal2";
        String userGlobal1 = "xwiki:XWiki.UserGlobal1";
        String userGlobal2 = "xwiki:XWiki.UserGlobal2";

        DocumentReference userLocalRef1 = new DocumentReference("subwiki", "XWiki", "UserLocal1");
        DocumentReference userLocalRef2 = new DocumentReference("subwiki", "XWiki", "UserLocal2");
        DocumentReference userGlobalRef1 = new DocumentReference("xwiki", "XWiki", "UserGlobal1");
        DocumentReference userGlobalRef2 = new DocumentReference("xwiki", "XWiki", "UserGlobal2");

        BaseObject member1 = mock(BaseObject.class);
        when(member1.getStringValue("member")).thenReturn(userLocal1);
        BaseObject member2 = mock(BaseObject.class);
        when(member2.getStringValue("member")).thenReturn(userLocal2);
        BaseObject member3 = mock(BaseObject.class);
        when(member3.getStringValue("member")).thenReturn(userGlobal1);
        BaseObject member4 = mock(BaseObject.class);
        when(member4.getStringValue("member")).thenReturn(userGlobal2);
        BaseObject member5 = mock(BaseObject.class);
        when(member5.getStringValue("member")).thenReturn("");
        List<BaseObject> membersOfAllGroup = new ArrayList<BaseObject>();
        membersOfAllGroup.add(member1);
        membersOfAllGroup.add(null);
        membersOfAllGroup.add(member2);
        membersOfAllGroup.add(member3);
        membersOfAllGroup.add(member4);
        membersOfAllGroup.add(member5);

        WikiReference wikiReference = new WikiReference("subwiki");
        when(documentReferenceResolver.resolve(eq(userLocal1), eq(wikiReference))).thenReturn(userLocalRef1);
        when(documentReferenceResolver.resolve(eq(userLocal2), eq(wikiReference))).thenReturn(userLocalRef2);
        when(documentReferenceResolver.resolve(eq(userGlobal1), eq(wikiReference))).thenReturn(userGlobalRef1);
        when(documentReferenceResolver.resolve(eq(userGlobal2), eq(wikiReference))).thenReturn(userGlobalRef2);

        DocumentReference allGroupRef = new DocumentReference("subwiki", "XWiki", "XWikiAllGroup");
        DocumentReference memberGroupRef = new DocumentReference("subwiki", "XWiki", "XWikiGlobalMemberGroup");

        XWikiDocument allGroupDoc = mock(XWikiDocument.class);
        XWikiDocument memberGroupDoc = mock(XWikiDocument.class);

        when(xwiki.getDocument(eq(allGroupRef), any(XWikiContext.class))).thenReturn(allGroupDoc);
        when(xwiki.getDocument(eq(memberGroupRef), any(XWikiContext.class))).thenReturn(memberGroupDoc);

        DocumentReference memberClass = new DocumentReference("subwiki", "XWiki", "XWikiGroups");
        when(allGroupDoc.getXObjects(eq(memberClass))).thenReturn(membersOfAllGroup);

        // User Global 2 is already a member of GlobalMemberGroup;
        List<BaseObject> membersOfGlobalMemberGroup = new ArrayList<BaseObject>();
        membersOfGlobalMemberGroup.add(member4);
        when(memberGroupDoc.getXObjects(eq(memberClass))).thenReturn(membersOfGlobalMemberGroup);

        BaseObject newObject = mock(BaseObject.class);
        when(memberGroupDoc.newXObject(eq(memberClass), any(XWikiContext.class))).thenReturn(newObject);

        // Test
        mocker.getComponentUnderTest().migrateGroups("subwiki");

        // Verify

        // The user GlobalUser1 has been added
        verify(newObject).setStringValue(eq("member"), eq(userGlobal1));
        // The user GlobalUser2 has not been added becaise it was already there
        verify(newObject, never()).setStringValue(eq("member"), eq(userGlobal2));
        // The local users has not been added
        verify(newObject, never()).setStringValue(eq("member"), eq(userLocal1));
        verify(newObject, never()).setStringValue(eq("member"), eq(userLocal2));
        // The document has been saved
        verify(xwiki).saveDocument(memberGroupDoc, "[UPGRADE] Add all global users who are members "
                + "of this wiki in this group.", xcontext);

        // Ony Global users have been removed from XWikiAllGroup
        verify(allGroupDoc).removeXObject(member3);
        verify(allGroupDoc).removeXObject(member4);
        // But the local users have not been removed
        verify(allGroupDoc, never()).removeXObject(member1);
        verify(allGroupDoc, never()).removeXObject(member2);
        // The document has been saved
        verify(xwiki).saveDocument(allGroupDoc, "[UPGRADE] Remove all global users from this group.", xcontext);
    }

    @Test
    public void migrateGroupsException() throws Exception
    {
        Exception exception = new XWikiException();
        DocumentReference allGroupRef = new DocumentReference("subwiki", "XWiki", "XWikiAllGroup");
        when(xwiki.getDocument(eq(allGroupRef), any(XWikiContext.class))).thenThrow(exception);

        // Test
        boolean exceptionCaught = false;
        try {
            mocker.getComponentUnderTest().migrateGroups("subwiki");
        } catch (DataMigrationException e) {
            exceptionCaught = true;
            assertEquals("Failed to migrate groups in the wiki [subwiki].", e.getMessage());
        }

        assertTrue(exceptionCaught);
    }
}
