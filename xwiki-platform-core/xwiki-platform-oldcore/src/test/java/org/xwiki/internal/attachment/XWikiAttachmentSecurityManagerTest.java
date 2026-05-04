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
package org.xwiki.internal.attachment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.DownloadAction;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@OldcoreTest
class XWikiAttachmentSecurityManagerTest
{
    @InjectMockComponents
    private XWikiAttachmentSecurityManager securityManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private Container container;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Inject
    private AuthorizationManager authorizationManager;

    @Mock
    private Request request;

    @Mock
    private XWikiContext context;

    @BeforeEach
    void setup()
    {
        when(this.container.getRequest()).thenReturn(request);
        when(this.contextProvider.get()).thenReturn(context);
    }

    // Helpers for the tests
    private XWikiAttachment createAttachment() throws IOException
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        XWikiDocument document = new XWikiDocument(documentReference);
        XWikiAttachment filetxt = new XWikiAttachment(document, "file");
        filetxt.setContent(new ByteArrayInputStream("any content".getBytes(StandardCharsets.UTF_8)));
        filetxt.setDate(new Date());
        document.getAttachmentList().add(filetxt);
        return filetxt;
    }

    @Test
    void downloadWhenForce() throws XWikiException, IOException
    {
        XWikiAttachment attachment = createAttachment();
        when(this.request.getParameter("force-download")).thenReturn("1");
        assertTrue(this.securityManager.shouldBeDownloaded(attachment));
    }

    @Test
    void downloadWhenMimeTypeBlacklisted() throws Exception
    {
        XWikiAttachment attachment = createAttachment();
        attachment.setMimeType("image/png");
        this.oldcore.getConfigurationSource().setProperty(DownloadAction.BLACKLIST_PROPERTY,
            List.of("application/x-bzip", "image/png"));
        assertTrue(this.securityManager.shouldBeDownloaded(attachment));
    }

    @Test
    void downloadWhenMimeTypeBlacklistedAndAttachmentAddedByPRUser() throws Exception
    {
        XWikiAttachment attachment = createAttachment();
        attachment.setMimeType("image/png");
        DocumentReference prUser = new DocumentReference("xwiki", "XWiki", "PRUser");
        attachment.setAuthorReference(prUser);
        WikiReference wikiReference = new WikiReference("foo");
        when(context.getWikiReference()).thenReturn(wikiReference);

        this.oldcore.getConfigurationSource().setProperty(DownloadAction.BLACKLIST_PROPERTY,
            List.of("application/x-bzip", "image/png"));
        // Consider PR rights
        when(this.authorizationManager.hasAccess(Right.PROGRAM, prUser, wikiReference)).thenReturn(true);
        assertFalse(this.securityManager.shouldBeDownloaded(attachment));
    }

    @Test
    void downloadWhenMimeTypeForcedDownloadAndAttachmentAddedByPRUser() throws Exception
    {
        when(this.request.getParameter("force-download")).thenReturn("1");
        XWikiAttachment attachment = createAttachment();
        attachment.setMimeType("image/png");
        DocumentReference prUser = new DocumentReference("xwiki", "XWiki", "PRUser");
        attachment.setAuthorReference(prUser);
        WikiReference wikiReference = new WikiReference("foo");
        when(context.getWikiReference()).thenReturn(wikiReference);

        this.oldcore.getConfigurationSource().setProperty(DownloadAction.BLACKLIST_PROPERTY,
            Arrays.asList("application/x-bzip", "image/png"));
        // Consider PR rights
        when(this.authorizationManager.hasAccess(Right.PROGRAM, prUser, wikiReference)).thenReturn(true);
        assertTrue(this.securityManager.shouldBeDownloaded(attachment));
    }

    @Test
    void downloadWhenMimeTypeNotWhitelisted() throws Exception
    {
        XWikiAttachment attachment = createAttachment();
        attachment.setMimeType("image/png");
        this.oldcore.getConfigurationSource().setProperty(DownloadAction.WHITELIST_PROPERTY,
            List.of("application/x-bzip"));
        assertTrue(this.securityManager.shouldBeDownloaded(attachment));
    }

    @Test
    void downloadWhenMimeTypeNotWhitelistedButAddedByPRUser() throws Exception
    {
        XWikiAttachment attachment = createAttachment();
        attachment.setMimeType("image/png");
        DocumentReference prUser = new DocumentReference("xwiki", "XWiki", "PRUser");
        attachment.setAuthorReference(prUser);
        WikiReference wikiReference = new WikiReference("foo");
        when(context.getWikiReference()).thenReturn(wikiReference);

        this.oldcore.getConfigurationSource().setProperty(DownloadAction.WHITELIST_PROPERTY,
            List.of("application/x-bzip"));
        // Consider PR rights
        when(this.authorizationManager.hasAccess(Right.PROGRAM, prUser, wikiReference)).thenReturn(true);
        assertFalse(this.securityManager.shouldBeDownloaded(attachment));
    }
}