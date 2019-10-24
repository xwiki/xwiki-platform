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
package com.xpn.xwiki.user.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XWikiUser}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class XWikiUserTest
{
    @InjectMockitoOldcore
    private MockitoOldcore mockitoOldcore;

    @Mock
    private XWikiDocument userDocument;

    private DocumentReference userClassReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Foo");

    @BeforeEach
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        when(mockitoOldcore.getSpyXWiki().getDocument(userReference, mockitoOldcore.getXWikiContext()))
            .thenReturn(userDocument);
        when(userDocument.getDocumentReference()).thenReturn(userReference);
        when(userDocument.getDocumentReferenceWithLocale()).thenReturn(userReference);
        when(userDocument.clone()).thenReturn(userDocument);
        componentManager.registerMockComponent(ContextualLocalizationManager.class, "default");
    }

    @Test
    public void createWithNullReference()
    {
        XWikiUser user = new XWikiUser((DocumentReference) null);

        assertNull(user.getUserReference());
        assertEquals(XWikiRightService.GUEST_USER_FULLNAME, user.getFullName());
    }

    @Test
    public void createWithNullFullName()
    {
        XWikiUser user = new XWikiUser((String) null);

        assertNull(user.getUserReference());
        assertNull(user.getFullName());
    }

    @Test
    public void isDisabled()
    {
        XWikiUser user = new XWikiUser(userReference);
        when(userDocument.getIntValue(userClassReference, XWikiUser.ACTIVE_PROPERTY, 1)).thenReturn(1);
        assertFalse(user.isDisabled(mockitoOldcore.getXWikiContext()));

        when(userDocument.getIntValue(userClassReference, XWikiUser.ACTIVE_PROPERTY, 1)).thenReturn(0);
        assertTrue(user.isDisabled(mockitoOldcore.getXWikiContext()));

        user = new XWikiUser((DocumentReference) null);
        assertFalse(user.isDisabled(mockitoOldcore.getXWikiContext()));

        user = new XWikiUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        assertFalse(user.isDisabled(mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setDisabledFalseNormalUser() throws XWikiException
    {
        XWikiUser user = new XWikiUser(userReference);
        user.setDisabled(false, mockitoOldcore.getXWikiContext());
        verify(userDocument, times(1)).setIntValue(userClassReference, XWikiUser.ACTIVE_PROPERTY, 1);
        verify(mockitoOldcore.getSpyXWiki(), times(1))
            .saveDocument(same(userDocument), any(String.class), same(mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setDisabledTrueNormalUser() throws XWikiException
    {
        XWikiUser user = new XWikiUser(userReference);
        user.setDisabled(true, mockitoOldcore.getXWikiContext());
        verify(userDocument, times(1)).setIntValue(userClassReference, XWikiUser.ACTIVE_PROPERTY, 0);
        verify(mockitoOldcore.getSpyXWiki(), times(1))
            .saveDocument(same(userDocument), any(String.class), same(mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setDisabledGuestOrSuperadminUser() throws XWikiException
    {
        // With guest user we never save anything
        XWikiUser user = new XWikiUser((DocumentReference) null);
        user.setDisabled(true, mockitoOldcore.getXWikiContext());
        verify(userDocument, never())
            .setIntValue(same(userClassReference), any(String.class), any(Integer.class));
        verify(mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(mockitoOldcore.getXWikiContext()));

        user.setDisabled(false, mockitoOldcore.getXWikiContext());
        verify(userDocument, never())
            .setIntValue(same(userClassReference), any(String.class), any(Integer.class));
        verify(mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(mockitoOldcore.getXWikiContext()));

        // With superadmin user we never save anything
        user = new XWikiUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        user.setDisabled(true, mockitoOldcore.getXWikiContext());
        verify(userDocument, never())
            .setIntValue(same(userClassReference), any(String.class), any(Integer.class));
        verify(mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(mockitoOldcore.getXWikiContext()));

        user.setDisabled(false, mockitoOldcore.getXWikiContext());
        verify(userDocument, never())
            .setIntValue(same(userClassReference), any(String.class), any(Integer.class));
        verify(mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void isEmailChecked()
    {
        XWikiUser user = new XWikiUser(userReference);
        when(userDocument.getIntValue(userClassReference, XWikiUser.EMAIL_CHECKED_PROPERTY, 1)).thenReturn(1);
        assertTrue(user.isEmailChecked(mockitoOldcore.getXWikiContext()));

        when(userDocument.getIntValue(userClassReference, XWikiUser.EMAIL_CHECKED_PROPERTY, 1)).thenReturn(0);
        assertFalse(user.isEmailChecked(mockitoOldcore.getXWikiContext()));

        user = new XWikiUser((DocumentReference) null);
        assertTrue(user.isEmailChecked(mockitoOldcore.getXWikiContext()));

        user = new XWikiUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        assertTrue(user.isEmailChecked(mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setEmailCheckedFalseNormalUser() throws XWikiException
    {
        XWikiUser user = new XWikiUser(userReference);
        user.setEmailChecked(false, mockitoOldcore.getXWikiContext());
        verify(userDocument, times(1)).setIntValue(userClassReference, XWikiUser.EMAIL_CHECKED_PROPERTY, 0);
        verify(mockitoOldcore.getSpyXWiki(), times(1))
            .saveDocument(same(userDocument), any(String.class), same(mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setEmailCheckedTrueNormalUser() throws XWikiException
    {
        XWikiUser user = new XWikiUser(userReference);
        user.setEmailChecked(true, mockitoOldcore.getXWikiContext());
        verify(userDocument, times(1)).setIntValue(userClassReference, XWikiUser.EMAIL_CHECKED_PROPERTY, 1);
        verify(mockitoOldcore.getSpyXWiki(), times(1))
            .saveDocument(same(userDocument), any(String.class), same(mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setEmailCheckedGuestOrSuperadminUser() throws XWikiException
    {
        // With guest user we never save anything
        XWikiUser user = new XWikiUser((DocumentReference) null);
        user.setEmailChecked(true, mockitoOldcore.getXWikiContext());
        verify(userDocument, never())
            .setIntValue(same(userClassReference), any(String.class), any(Integer.class));
        verify(mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(mockitoOldcore.getXWikiContext()));

        user.setEmailChecked(false, mockitoOldcore.getXWikiContext());
        verify(userDocument, never())
            .setIntValue(same(userClassReference), any(String.class), any(Integer.class));
        verify(mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(mockitoOldcore.getXWikiContext()));

        // With superadmin user we never save anything
        user = new XWikiUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        user.setEmailChecked(true, mockitoOldcore.getXWikiContext());
        verify(userDocument, never())
            .setIntValue(same(userClassReference), any(String.class), any(Integer.class));
        verify(mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(mockitoOldcore.getXWikiContext()));

        user.setEmailChecked(false, mockitoOldcore.getXWikiContext());
        verify(userDocument, never())
            .setIntValue(same(userClassReference), any(String.class), any(Integer.class));
        verify(mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(mockitoOldcore.getXWikiContext()));
    }
}
