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
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

/**
 * Test for the RightsManager
 *
 * @since 13.8
 * @version $Id$
 */
@OldcoreTest
@AllComponents
public class RightsManagerTest
{
    @InjectMockitoOldcore
    MockitoOldcore oldcore;

    /**
     * instance under test
     */
    private RightsManager rights;

    @BeforeEach
    protected void beforeEach() throws Exception
    {
        rights = RightsManager.getInstance();

        doReturn(true).when(oldcore.getSpyXWiki()).checkAccess(any(), any(), any());
    }

    @Test
    public void testRenameGroup() throws Exception
    {
        DocumentReference oldGroupName = new DocumentReference("xwiki", Collections.singletonList("XWiki"),
            "GroupBefore");
        DocumentReference newGroupName = new DocumentReference("xwiki", Collections.singletonList("XWiki"),
            "GroupAfter");
        String oldValue = "XWiki.GroupBefore,XWiki.Some\\,OtherGroup";
        String expectedValue = "XWiki.Some\\,OtherGroup,XWiki.GroupAfter";
        // ThreadLocal: only to have this final and use it in the answer
        final ThreadLocal<String> actualValue = new ThreadLocal<String>();

        XWikiDocument rightsDocument = createMockDocument();

        doReturn(Collections.singletonList(rightsDocument)).when(oldcore.getMockStore()).searchDocuments(any(), any(),
            eq(oldcore.getXWikiContext()));

        BaseObject rightsObject = Mockito.mock(BaseObject.class);
        doReturn(Collections.singletonList(rightsObject)).when(rightsDocument)
            .getXObjects(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        doReturn(oldValue).when(rightsObject).getLargeStringValue("groups");
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                actualValue.set((String) invocation.getArgument(1));
                return null;
            }
        }).when(rightsObject).setLargeStringValue(eq("groups"), any());

        rights.replaceUserOrGroupFromAllRights(oldGroupName, newGroupName, false, oldcore.getXWikiContext());

        assertEquals(expectedValue, actualValue.get());
    }

    @Test
    public void testRemoveGroup() throws Exception
    {
        String oldValue = "XWiki.SomeGroup,XWiki.GroupToRemove,XWiki.Some\\,OtherGroup";
        String expectedValue = "XWiki.SomeGroup,XWiki.Some\\,OtherGroup";
        // ThreadLocal: only to have this final and use it in the answer
        final ThreadLocal<String> actualValue = new ThreadLocal<String>();

        XWikiDocument rightsDocument = createMockDocument();

        doReturn(Collections.singletonList(rightsDocument)).when(oldcore.getMockStore()).searchDocuments(any(), any(),
            eq(oldcore.getXWikiContext()));

        BaseObject rightsObject = Mockito.mock(BaseObject.class);
        doReturn(Collections.singletonList(rightsObject)).when(rightsDocument)
            .getXObjects(MockitoOldcore.GLOBAL_RIGHTS_CLASS);
        doReturn(oldValue).when(rightsObject).getLargeStringValue("groups");
        doReturn("").when(rightsObject).getLargeStringValue("users");
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                actualValue.set((String) invocation.getArgument(1));
                return null;
            }
        }).when(rightsObject).setLargeStringValue(eq("groups"), any());

        rights.removeUserOrGroupFromAllRights("xwiki", "XWiki", "GroupToRemove", false, oldcore.getXWikiContext());

        assertEquals(expectedValue, actualValue.get());
    }

    /**
     * create a mock document that can be saved via the mock store
     *
     * @return the mocked document
     */
    private XWikiDocument createMockDocument()
    {
        XWikiDocument rightsDocument = Mockito.mock(XWikiDocument.class);

        // this seems to be necessary to save the document with the mock store
        DocumentReference rightsDocumentRef = new DocumentReference("xwiki", Collections.singletonList("Some"), "Page");
        DocumentReference rightsDocumentRefWithLocale = new DocumentReference(rightsDocumentRef, Locale.ROOT);
        doReturn(rightsDocumentRef).when(rightsDocument).getDocumentReference();
        doReturn(rightsDocumentRefWithLocale).when(rightsDocument).getDocumentReferenceWithLocale();
        doReturn(rightsDocument).when(rightsDocument).clone();
        doReturn(false).when(rightsDocument).isNew();

        oldcore.getDocuments().put(rightsDocumentRefWithLocale, rightsDocument);
        return rightsDocument;
    }
}

