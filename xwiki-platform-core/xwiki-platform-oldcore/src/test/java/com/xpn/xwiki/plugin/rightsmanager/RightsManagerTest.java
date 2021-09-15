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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

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

        oldcore.getSpyXWiki().saveDocument(rightsDocument, oldcore.getXWikiContext());

        XWikiDocument savedRightsDocument = loadTestDocumentFromStore();
        doReturn(Collections.singletonList(savedRightsDocument)).when(oldcore.getMockStore()).searchDocuments(any(),
            any(), eq(oldcore.getXWikiContext()));
    }

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
        oldcore.getSpyXWiki().saveDocument(rightsDocument, oldcore.getXWikiContext());

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
        oldcore.getSpyXWiki().saveDocument(rightsDocument, oldcore.getXWikiContext());

        rights.removeUserOrGroupFromAllRights("xwiki", "XWiki", "GroupToRemove", false, oldcore.getXWikiContext());

        BaseObject updatedRightsObject = loadTestDocumentFromStore().getXObject(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        assertEquals(expectedValue, updatedRightsObject.getLargeStringValue("groups"));
    }

    private XWikiDocument loadTestDocumentFromStore() throws Exception
    {
        return oldcore.getSpyXWiki().getDocument(rightsDocReference, oldcore.getXWikiContext());
    }
}
