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
package org.xwiki.attachment.internal;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import static com.xpn.xwiki.web.DownloadAction.ACTION_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link AttachmentsRedirectionFilter}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@ComponentTest
class AttachmentsRedirectionFilterTest
{
    public static final DocumentReference SOURCE_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    public static final AttachmentReference SOURCE_ATTACHMENT_REFERENCE =
        new AttachmentReference("file.txt", SOURCE_DOCUMENT_REFERENCE);

    public static final DocumentReference TARGET_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Target");

    public static final AttachmentReference TARGET_ATTACHMENT_REFERENCE =
        new AttachmentReference("file.txt", TARGET_DOCUMENT_REFERENCE);

    @InjectMockComponents
    private AttachmentsRedirectionFilter redirectionFilter;

    @MockComponent
    private ResourceReferenceManager resourceReferenceManager;

    @MockComponent
    private AttachmentsManager attachmentsManager;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiResponse response;

    @BeforeEach
    void setUp()
    {
        when(this.xWikiContext.getRequest()).thenReturn(this.request);
        when(this.xWikiContext.getResponse()).thenReturn(this.response);
        when(this.xWikiContext.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void redirectNotDownload() throws Exception
    {
        when(this.xWikiContext.getAction()).thenReturn("view");
        assertFalse(this.redirectionFilter.redirect(this.xWikiContext));
    }

    @Test
    void redirectNoRedirection() throws Exception
    {
        when(this.xWikiContext.getAction()).thenReturn(ACTION_NAME);
        when(this.resourceReferenceManager.getResourceReference())
            .thenReturn(new EntityResourceReference(SOURCE_ATTACHMENT_REFERENCE, null));
        when(this.attachmentsManager.getRedirection(SOURCE_ATTACHMENT_REFERENCE)).thenReturn(Optional.empty());
        assertFalse(this.redirectionFilter.redirect(this.xWikiContext));
    }

    @Test
    void redirect() throws Exception
    {
        when(this.xWikiContext.getAction()).thenReturn(ACTION_NAME);
        when(this.resourceReferenceManager.getResourceReference())
            .thenReturn(new EntityResourceReference(SOURCE_ATTACHMENT_REFERENCE, null));
        when(this.attachmentsManager.getRedirection(SOURCE_ATTACHMENT_REFERENCE)).thenReturn(
            Optional.of(TARGET_ATTACHMENT_REFERENCE));
        when(this.wiki.getURL(TARGET_ATTACHMENT_REFERENCE, "download", null, null, this.xWikiContext))
            .thenReturn("NEW_URL");
        assertTrue(this.redirectionFilter.redirect(this.xWikiContext));
        verify(this.response).sendRedirect("NEW_URL");
    }
}
