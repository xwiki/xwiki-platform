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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReferenceResolver;

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

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Mock
    private XWikiDocument userDocument;

    @Mock
    private DocumentAuthors authors;

    private final DocumentReference userClassReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");

    private final DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Foo");

    @BeforeEach
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        when(this.mockitoOldcore.getSpyXWiki().getDocument(this.userReference, this.mockitoOldcore.getXWikiContext()))
            .thenReturn(this.userDocument);
        when(this.userDocument.getDocumentReference()).thenReturn(this.userReference);
        when(this.userDocument.getDocumentReferenceWithLocale()).thenReturn(this.userReference);
        when(this.userDocument.getAuthors()).thenReturn(this.authors);
        when(this.userDocument.clone()).thenReturn(this.userDocument);
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
        XWikiUser user = new XWikiUser(this.userReference);
        when(this.userDocument.getIntValue(this.userClassReference, XWikiUser.ACTIVE_PROPERTY, 1)).thenReturn(1);
        assertFalse(user.isDisabled(this.mockitoOldcore.getXWikiContext()));

        when(this.userDocument.getIntValue(this.userClassReference, XWikiUser.ACTIVE_PROPERTY, 1)).thenReturn(0);
        assertTrue(user.isDisabled(this.mockitoOldcore.getXWikiContext()));

        user = new XWikiUser((DocumentReference) null);
        assertFalse(user.isDisabled(this.mockitoOldcore.getXWikiContext()));

        user = new XWikiUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        assertFalse(user.isDisabled(this.mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setDisabledFalseNormalUser() throws XWikiException
    {
        XWikiUser user = new XWikiUser(this.userReference);
        user.setDisabled(false, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, times(1)).setIntValue(this.userClassReference, XWikiUser.ACTIVE_PROPERTY, 1);
        verify(this.mockitoOldcore.getSpyXWiki(), times(1))
            .saveDocument(same(this.userDocument), any(String.class), same(this.mockitoOldcore.getXWikiContext()));
        verify(this.authors, times(1)).setOriginalMetadataAuthor(any());
        verify(this.authors, never()).setEffectiveMetadataAuthor(any());
    }

    @Test
    public void setDisabledTrueNormalUser() throws XWikiException
    {
        XWikiUser user = new XWikiUser(this.userReference);
        user.setDisabled(true, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, times(1)).setIntValue(this.userClassReference, XWikiUser.ACTIVE_PROPERTY, 0);
        verify(this.mockitoOldcore.getSpyXWiki(), times(1))
            .saveDocument(same(this.userDocument), any(String.class), same(this.mockitoOldcore.getXWikiContext()));
        verify(this.authors, times(1)).setOriginalMetadataAuthor(any());
        verify(this.authors, never()).setEffectiveMetadataAuthor(any());
    }

    @Test
    public void setDisabledGuestOrSuperadminUser() throws XWikiException
    {
        // With guest user we never save anything
        XWikiUser user = new XWikiUser((DocumentReference) null);
        user.setDisabled(true, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, never())
            .setIntValue(same(this.userClassReference), any(String.class), any(Integer.class));
        verify(this.mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(this.mockitoOldcore.getXWikiContext()));

        user.setDisabled(false, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, never())
            .setIntValue(same(this.userClassReference), any(String.class), any(Integer.class));
        verify(this.mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(this.mockitoOldcore.getXWikiContext()));

        // With superadmin user we never save anything
        user = new XWikiUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        user.setDisabled(true, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, never())
            .setIntValue(same(this.userClassReference), any(String.class), any(Integer.class));
        verify(this.mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(this.mockitoOldcore.getXWikiContext()));

        user.setDisabled(false, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, never())
            .setIntValue(same(this.userClassReference), any(String.class), any(Integer.class));
        verify(this.mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(this.mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void isEmailChecked()
    {
        XWikiUser user = new XWikiUser(this.userReference);
        when(this.userDocument.getIntValue(this.userClassReference, XWikiUser.EMAIL_CHECKED_PROPERTY, 1)).thenReturn(1);
        assertTrue(user.isEmailChecked(this.mockitoOldcore.getXWikiContext()));

        when(this.userDocument.getIntValue(this.userClassReference, XWikiUser.EMAIL_CHECKED_PROPERTY, 1)).thenReturn(0);
        assertFalse(user.isEmailChecked(this.mockitoOldcore.getXWikiContext()));

        user = new XWikiUser((DocumentReference) null);
        assertTrue(user.isEmailChecked(this.mockitoOldcore.getXWikiContext()));

        user = new XWikiUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        assertTrue(user.isEmailChecked(this.mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setEmailCheckedFalseNormalUser() throws XWikiException
    {
        XWikiUser user = new XWikiUser(this.userReference);
        user.setEmailChecked(false, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, times(1)).setIntValue(this.userClassReference, XWikiUser.EMAIL_CHECKED_PROPERTY, 0);
        verify(this.mockitoOldcore.getSpyXWiki(), times(1))
            .saveDocument(same(this.userDocument), any(String.class), same(this.mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setEmailCheckedTrueNormalUser() throws XWikiException
    {
        XWikiUser user = new XWikiUser(this.userReference);
        user.setEmailChecked(true, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, times(1)).setIntValue(this.userClassReference, XWikiUser.EMAIL_CHECKED_PROPERTY, 1);
        verify(this.mockitoOldcore.getSpyXWiki(), times(1))
            .saveDocument(same(this.userDocument), any(String.class), same(this.mockitoOldcore.getXWikiContext()));
    }

    @Test
    public void setEmailCheckedGuestOrSuperadminUser() throws XWikiException
    {
        // With guest user we never save anything
        XWikiUser user = new XWikiUser((DocumentReference) null);
        user.setEmailChecked(true, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, never())
            .setIntValue(same(this.userClassReference), any(String.class), any(Integer.class));
        verify(this.mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(this.mockitoOldcore.getXWikiContext()));

        user.setEmailChecked(false, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, never())
            .setIntValue(same(this.userClassReference), any(String.class), any(Integer.class));
        verify(this.mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(this.mockitoOldcore.getXWikiContext()));

        // With superadmin user we never save anything
        user = new XWikiUser(XWikiRightService.SUPERADMIN_USER_FULLNAME);
        user.setEmailChecked(true, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, never())
            .setIntValue(same(this.userClassReference), any(String.class), any(Integer.class));
        verify(this.mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(this.mockitoOldcore.getXWikiContext()));

        user.setEmailChecked(false, this.mockitoOldcore.getXWikiContext());
        verify(this.userDocument, never())
            .setIntValue(same(this.userClassReference), any(String.class), any(Integer.class));
        verify(this.mockitoOldcore.getSpyXWiki(), never())
            .saveDocument(any(XWikiDocument.class), any(String.class), same(this.mockitoOldcore.getXWikiContext()));
    }
}
