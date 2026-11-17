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

package com.xpn.xwiki.plugin.rightsmanager;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Test for the RightsManager.
 *
 * @version $Id$
 */
@OldcoreTest
@AllComponents
class RightsManagerTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    /**
     * Instance under test.
     */
    private RightsManager rights;

    /**
     * A reference to the document storing the rights object under test.
     */
    private DocumentReference rightsDocReference;

    @BeforeEach
    void beforeEach() throws Exception
    {
        rights = RightsManager.getInstance();

        rightsDocReference = new DocumentReference("xwiki", "Some", "Page");
        XWikiDocument rightsDocument = loadTestDocumentFromStore();

        rightsDocument.newXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS, oldcore.getXWikiContext());

        save(rightsDocument);
    }

    private void save(XWikiDocument rightsDocument) throws Exception
    {
        this.oldcore.getSpyXWiki().saveDocument(rightsDocument, this.oldcore.getXWikiContext());
        XWikiDocument savedRightsDocument = loadTestDocumentFromStore();
        doReturn(Collections.singletonList(savedRightsDocument)).when(this.oldcore.getMockStore()).searchDocuments(any(),
            any(), eq(this.oldcore.getXWikiContext()));
    }

    private XWikiDocument loadTestDocumentFromStore() throws Exception
    {
        return oldcore.getSpyXWiki().getDocument(rightsDocReference, oldcore.getXWikiContext());
    }

    // Tests

    @Test
    void replaceUserOrGroupFromAllRights() throws Exception
    {
        DocumentReference oldGroupName = new DocumentReference("xwiki", "XWiki", "GroupBefore");
        DocumentReference newGroupName = new DocumentReference("xwiki", "XWiki", "GroupAfter");
        String oldValue = "XWiki.GroupBefore,XWiki.Some\\,OtherGroup";
        String expectedValue = "XWiki.Some\\,OtherGroup,XWiki.GroupAfter";

        XWikiDocument rightsDocument = loadTestDocumentFromStore();
        BaseObject rightsObject = rightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        rightsObject.setLargeStringValue("groups", oldValue);
        save(rightsDocument);

        rights.replaceUserOrGroupFromAllRights(oldGroupName, newGroupName, false, oldcore.getXWikiContext());

        BaseObject updatedRightsObject = loadTestDocumentFromStore().getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        assertEquals(expectedValue, updatedRightsObject.getLargeStringValue("groups"));
    }

    @Test
    void removeUserOrGroupFromAllRights() throws Exception
    {
        String oldValue = "XWiki.SomeGroup,XWiki.GroupToRemove,XWiki.Some\\,OtherGroup";
        String expectedValue = "XWiki.SomeGroup,XWiki.Some\\,OtherGroup";

        XWikiDocument rightsDocument = loadTestDocumentFromStore();
        BaseObject rightsObject = rightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        rightsObject.setLargeStringValue("groups", oldValue);
        save(rightsDocument);

        rights.removeUserOrGroupFromAllRights("xwiki", "XWiki", "GroupToRemove", false, oldcore.getXWikiContext());

        BaseObject updatedRightsObject = loadTestDocumentFromStore().getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        assertEquals(expectedValue, updatedRightsObject.getLargeStringValue("groups"));
    }

    @Test
    void removeUserOrGroupFromAllRightsWithPrefixedReference() throws Exception
    {
        String oldValue = "xwiki:XWiki.UserToRemove,XWiki.SomeUser,XWiki.UserToRemove,xwiki:XWiki.UserToRemove";
        String expectedValue = "XWiki.SomeUser";

        XWikiDocument rightsDocument = loadTestDocumentFromStore();
        BaseObject rightsObject = rightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        rightsObject.setLargeStringValue("users", oldValue);
        save(rightsDocument);

        rights.removeUserOrGroupFromAllRights("xwiki", "XWiki", "UserToRemove", true, oldcore.getXWikiContext());

        BaseObject updatedRightsObject = loadTestDocumentFromStore().getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        assertEquals(expectedValue, updatedRightsObject.getLargeStringValue("users"));
        verify(this.oldcore.getMockStore()).searchDocuments(any(),
            eq(List.of("XWiki.XWikiRights", "XWiki.XWikiGlobalRights", "users", "%UserToRemove%")),
            eq(this.oldcore.getXWikiContext()));
    }

    @Test
    void removeUserOrGroupFromAllRightsInAnotherWiki() throws Exception
    {
        // A main wiki user stored the way the rights UI stores it in a subwiki, beside a subwiki user with the same
        // name which must be kept.
        String oldValue = "xwiki:XWiki.UserToRemove,UserToRemove,XWiki.UserToRemove,subwiki:XWiki.Other";
        String expectedValue = "UserToRemove,XWiki.UserToRemove,subwiki:XWiki.Other";

        this.rightsDocReference = new DocumentReference("subwiki", "XWiki", "XWikiPreferences");
        XWikiDocument rightsDocument = loadTestDocumentFromStore();
        rightsDocument.newXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS, this.oldcore.getXWikiContext())
            .setLargeStringValue("users", oldValue);
        save(rightsDocument);

        this.oldcore.getXWikiContext().setWikiId("subwiki");
        rights.removeUserOrGroupFromAllRights("xwiki", "XWiki", "UserToRemove", true, oldcore.getXWikiContext());

        BaseObject updatedRightsObject = loadTestDocumentFromStore().getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        assertEquals(expectedValue, updatedRightsObject.getLargeStringValue("users"));
        verify(this.oldcore.getMockStore()).searchDocuments(any(),
            eq(List.of("XWiki.XWikiRights", "XWiki.XWikiGlobalRights", "users", "%UserToRemove%")),
            eq(this.oldcore.getXWikiContext()));
    }

    @Test
    void removeLastUserOrGroupFromRights() throws Exception
    {
        XWikiDocument rightsDocument = loadTestDocumentFromStore();
        BaseObject allowObject = rightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        allowObject.setLargeStringValue("users", "XWiki.UserToRemove");
        allowObject.setIntValue("allow", 1);
        BaseObject denyObject =
            rightsDocument.newXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS, this.oldcore.getXWikiContext());
        denyObject.setLargeStringValue("users", "xwiki:XWiki.UserToRemove");
        denyObject.setIntValue("allow", 0);
        BaseObject unrelatedObject =
            rightsDocument.newXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS, this.oldcore.getXWikiContext());
        unrelatedObject.setIntValue("allow", 0);
        save(rightsDocument);

        this.rights.removeUserOrGroupFromAllRights("xwiki", "XWiki", "UserToRemove", true,
            this.oldcore.getXWikiContext());

        XWikiDocument updatedRightsDocument = loadTestDocumentFromStore();
        assertNull(updatedRightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS, 0));
        assertNull(updatedRightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS, 1));
        assertNotNull(updatedRightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS, 2));
    }

    @Test
    void removeUserOrGroupFromGlobalAndLocalRights() throws Exception
    {
        DocumentReference adminReference = new DocumentReference("xwiki", "XWiki", "Admin");
        this.oldcore.getXWikiContext().setUserReference(adminReference);

        XWikiDocument rightsDocument = loadTestDocumentFromStore();
        rightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS)
            .setLargeStringValue("users", "XWiki.UserToRemove,XWiki.Other");
        rightsDocument.newXObject(MockitoOldcore.RIGHTS_CLASS, this.oldcore.getXWikiContext())
            .setLargeStringValue("users", "XWiki.UserToRemove,XWiki.Other");
        save(rightsDocument);

        this.rights.removeUserOrGroupFromAllRights("xwiki", "XWiki", "UserToRemove", true,
            this.oldcore.getXWikiContext());

        XWikiDocument updatedRightsDocument = loadTestDocumentFromStore();
        assertEquals("XWiki.Other",
            updatedRightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS).getLargeStringValue("users"));
        assertEquals("XWiki.Other",
            updatedRightsDocument.getXObject(MockitoOldcore.RIGHTS_CLASS).getLargeStringValue("users"));
        assertEquals(adminReference, getOriginalMetadataAuthor(updatedRightsDocument));
    }

    @Test
    void replaceUserOrGroupFromAllRightsWithAnyReference() throws Exception
    {
        DocumentReference adminReference = new DocumentReference("xwiki", "XWiki", "Admin");
        this.oldcore.getXWikiContext().setUserReference(adminReference);

        XWikiDocument rightsDocument = loadTestDocumentFromStore();
        rightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS)
            .setLargeStringValue("users", "UserBefore,xwiki:XWiki.UserBefore,XWiki.Other");
        rightsDocument.newXObject(MockitoOldcore.RIGHTS_CLASS, this.oldcore.getXWikiContext())
            .setLargeStringValue("users", "XWiki.UserBefore");
        save(rightsDocument);

        this.rights.replaceUserOrGroupFromAllRights(new DocumentReference("xwiki", "XWiki", "UserBefore"),
            new DocumentReference("xwiki", "XWiki", "UserAfter"), true, this.oldcore.getXWikiContext());

        XWikiDocument updatedRightsDocument = loadTestDocumentFromStore();
        assertEquals("XWiki.Other,XWiki.UserAfter",
            updatedRightsDocument.getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS).getLargeStringValue("users"));
        assertEquals("XWiki.UserAfter",
            updatedRightsDocument.getXObject(MockitoOldcore.RIGHTS_CLASS).getLargeStringValue("users"));
        assertEquals(adminReference, getOriginalMetadataAuthor(updatedRightsDocument));
        verify(this.oldcore.getMockStore()).searchDocuments(any(),
            eq(List.of("XWiki.XWikiRights", "XWiki.XWikiGlobalRights", "users", "%UserBefore%")),
            eq(this.oldcore.getXWikiContext()));
    }

    private DocumentReference getOriginalMetadataAuthor(XWikiDocument document) throws Exception
    {
        UserReferenceSerializer<DocumentReference> serializer =
            this.oldcore.getMocker().getInstance(UserReferenceSerializer.TYPE_DOCUMENT_REFERENCE, "document");
        return serializer.serialize(document.getAuthors().getOriginalMetadataAuthor());
    }
}
