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
package org.xwiki.user.rest.internal.document;

import java.net.URI;

import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.stubbing.Answer;
import org.xwiki.mail.EmailAddressObfuscator;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;
import org.xwiki.user.rest.model.jaxb.User;
import org.xwiki.user.rest.model.jaxb.UserSummary;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the behavior of {@link DocumentUserReferenceModelSerializer}.
 *
 * @version $Id$
 * @since 18.2.0RC1
 */
@OldcoreTest
class DocumentUserReferenceModelSerializerTest
{
    @InjectMockComponents
    private DocumentUserReferenceModelSerializer documentUserReferenceModelSerializer;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    private GeneralMailConfiguration mailConfiguration;

    @MockComponent
    private EmailAddressObfuscator emailAddressObfuscator;

    @Captor
    private ArgumentCaptor<InternetAddress> emailAddressCaptor;

    private XWikiContext xcontext;

    @BeforeEach
    void setUp(MockitoOldcore oldcore)
    {
        this.xcontext = oldcore.getXWikiContext();

        when(this.emailAddressObfuscator.obfuscate(this.emailAddressCaptor.capture()))
            .then((Answer<String>) invocationOnMock -> this.emailAddressCaptor.getValue().getAddress() + " OBFUSCATED");

        doReturn("avatar.default.url")
            .when(oldcore.getSpyXWiki()).getSkinFile("icons/xwiki/noavatar.png", this.xcontext);

        when(this.mailConfiguration.shouldObfuscate()).thenReturn(true);
    }

    @Test
    void testUserSummary() throws Exception
    {
        URI baseUri = URI.create("localhost.uri");
        String userId = "xwiki:XWiki.testuser";

        UserSummary userSummary =
            this.documentUserReferenceModelSerializer.toRestUserSummary(baseUri, userId, createTestUser());

        assertEquals(userId, userSummary.getId());
        assertEquals("First", userSummary.getFirstName());
        assertEquals("LAST", userSummary.getLastName());
        assertEquals("view.url", userSummary.getXwikiRelativeUrl());
        assertEquals("view.external.url", userSummary.getXwikiAbsoluteUrl());
        assertEquals("avatar.mycoolavatar.png.url", userSummary.getAvatarUrl());
        assertTrue(userSummary.isGlobal());
    }

    @Test
    void testGuestUser() throws Exception
    {
        URI baseUri = URI.create("localhost.uri");

        UserProperties guestUserReference = mock(UserProperties.class);
        when(guestUserReference.getFirstName()).thenReturn("Guest");
        when(this.userPropertiesResolver.resolve(GuestUserReference.INSTANCE)).thenReturn(guestUserReference);

        User user =
            this.documentUserReferenceModelSerializer.toRestUser(baseUri, "", GuestUserReference.INSTANCE, false);

        assertEquals("XWiki.XWikiGuest", user.getId());
        assertEquals("Guest", user.getFirstName());
        assertEquals("Guest", user.getDisplayName());
        assertEquals("avatar.default.url", user.getAvatarUrl());
        assertTrue(user.isGlobal());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUser(boolean preferences) throws Exception
    {
        URI baseUri = URI.create("localhost.uri");
        String userId = "xwiki:XWiki.testuser";

        User user =
            this.documentUserReferenceModelSerializer.toRestUser(baseUri, userId, createTestUser(), preferences);

        assertEquals(userId, user.getId());
        assertEquals("First", user.getFirstName());
        assertEquals("LAST", user.getLastName());
        assertEquals("view.url", user.getXwikiRelativeUrl());
        assertEquals("view.external.url", user.getXwikiAbsoluteUrl());
        assertEquals("test@email.tld OBFUSCATED", user.getEmail());
        assertEquals("avatar.mycoolavatar.png.url", user.getAvatarUrl());
        assertEquals("My company", user.getCompany());
        assertEquals("Hello World!", user.getAbout());
        assertEquals("0123456789", user.getPhone());
        assertEquals("42 Street Name\n11111 City Name\nCountry Name", user.getAddress());
        assertEquals("http://myblog.url/", user.getBlog());
        assertEquals("http://myblog.url/rss", user.getBlogFeed());
        assertTrue(user.isGlobal());

        if (!preferences) {
            assertNull(user.getPreferences());
        } else {
            assertTrue(user.getPreferences().isDisplayHiddenDocuments());
            assertTrue(user.getPreferences().isAdvanced());
            assertEquals("WYSIWYG", user.getPreferences().getEditor());
            assertEquals("Europe/Paris", user.getPreferences().getTimezone());
            assertEquals("Yes", user.getPreferences().getUnderlineLinks());
        }
    }

    private UserReference createTestUser() throws Exception
    {
        DocumentReference testUser = new DocumentReference("xwiki", "XWiki", "testuser");
        UserReference testUserReference = new DocumentUserReference(testUser, true);

        UserProperties testUserProperties = mock(UserProperties.class);
        when(testUserProperties.getFirstName()).thenReturn("First");
        when(testUserProperties.getLastName()).thenReturn("LAST");
        when(testUserProperties.getEmail()).thenReturn(new InternetAddress("test@email.tld"));
        when(testUserProperties.displayHiddenDocuments()).thenReturn(true);
        when(testUserProperties.getProperty("avatar")).thenReturn("mycoolavatar.png");
        when(testUserProperties.getProperty("company")).thenReturn("My company");
        when(testUserProperties.getProperty("comment")).thenReturn("Hello World!");
        when(testUserProperties.getProperty("phone")).thenReturn("0123456789");
        when(testUserProperties.getProperty("address")).thenReturn("42 Street Name\n11111 City Name\nCountry Name");
        when(testUserProperties.getProperty("blog")).thenReturn("http://myblog.url/");
        when(testUserProperties.getProperty("blogfeed")).thenReturn("http://myblog.url/rss");
        when(testUserProperties.getProperty("underline")).thenReturn("Yes");
        when(testUserProperties.getProperty("timezone")).thenReturn("Europe/Paris");
        when(testUserProperties.getProperty("editor")).thenReturn("WYSIWYG");
        when(testUserProperties.getProperty("usertype")).thenReturn("Advanced");
        when(this.userPropertiesResolver.resolve(testUserReference)).thenReturn(testUserProperties);

        XWikiDocument testUserDocument = mock(XWikiDocument.class);
        when(testUserDocument.getDocumentReference()).thenReturn(testUser);
        when(testUserDocument.getURL("view", this.xcontext)).thenReturn("view.url");
        when(testUserDocument.getExternalURL("view", this.xcontext)).thenReturn("view.external.url");
        doReturn(testUserDocument).when(this.xcontext.getWiki()).getDocument(testUser, this.xcontext);

        AttachmentReference testAvatarReference = new AttachmentReference("mycoolavatar.png", testUser);
        XWikiAttachment testAvatarAttachment = mock(XWikiAttachment.class);
        when(testAvatarAttachment.getReference()).thenReturn(testAvatarReference);
        when(testUserDocument.getAttachment(testAvatarReference.getName())).thenReturn(testAvatarAttachment);
        doReturn("avatar.mycoolavatar.png.url").when(this.xcontext.getWiki())
            .getURL(testAvatarReference, this.xcontext);

        return testUserReference;
    }
}
