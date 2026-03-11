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
package com.xpn.xwiki.web;

import java.io.ByteArrayInputStream;

import javax.servlet.ServletOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.internal.filter.Importer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImportAction}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class ImportActionTest
{
    private static final String CSRF_TOKEN_VALUE = "validToken123";

    /**
     * Minimal valid empty ZIP file (22 bytes: End of Central Directory record only).
     */
    private static final byte[] MINIMAL_ZIP_BYTES =
        {0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private static final String TEST_XAR_NAME = "test.xar";

    private static final String RESUBMISSION_URL = "http://localhost/resubmit";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private ImportAction action;

    @MockComponent
    private CSRFToken csrfToken;

    @MockComponent
    private Importer importer;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWikiResponse response;

    @Mock
    private ServletOutputStream responseOutputStream;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(this.request);
        this.oldcore.getXWikiContext().setResponse(this.response);

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Import");
        XWikiDocument document = new XWikiDocument(documentReference);

        XWikiAttachment attachment = new XWikiAttachment(document, TEST_XAR_NAME);
        attachment.setContent(new ByteArrayInputStream(MINIMAL_ZIP_BYTES));
        document.getAttachmentList().add(attachment);

        this.oldcore.getXWikiContext().setDoc(document);

        when(this.oldcore.getMockRightService().hasWikiAdminRights(this.oldcore.getXWikiContext())).thenReturn(true);
        when(this.csrfToken.getToken()).thenReturn(CSRF_TOKEN_VALUE);
        when(this.csrfToken.isTokenValid(CSRF_TOKEN_VALUE)).thenReturn(true);
        when(this.csrfToken.getResubmissionURL()).thenReturn(RESUBMISSION_URL);
        when(this.response.encodeRedirectURL(RESUBMISSION_URL)).thenReturn(RESUBMISSION_URL);
        when(this.response.getOutputStream()).thenReturn(this.responseOutputStream);
    }

    @Test
    void importWithValidCSRFToken() throws Exception
    {
        when(this.request.get("name")).thenReturn(TEST_XAR_NAME);
        when(this.request.get("action")).thenReturn("import");
        when(this.request.getParameter("form_token")).thenReturn(CSRF_TOKEN_VALUE);

        String result = this.action.render(this.oldcore.getXWikiContext());

        assertEquals("admin", result);
        verify(this.csrfToken).isTokenValid(CSRF_TOKEN_VALUE);
        verify(this.importer).importXAR(any(), isNull(), any(), anyBoolean(), any());
    }

    @Test
    void importWithMissingCSRFToken() throws Exception
    {
        when(this.request.get("name")).thenReturn(TEST_XAR_NAME);
        when(this.request.get("action")).thenReturn("import");

        String result = this.action.render(this.oldcore.getXWikiContext());

        // Should return null (redirect to resubmission page) without importing
        assertNull(result);
        verify(this.csrfToken).isTokenValid(null);
        verify(this.csrfToken).getResubmissionURL();
        verify(this.importer, never()).importXAR(any(), any(), any(), anyBoolean(), any());
        verify(this.response).sendRedirect(RESUBMISSION_URL);
    }

    @Test
    void importWithInvalidCSRFToken() throws Exception
    {
        when(this.request.get("name")).thenReturn(TEST_XAR_NAME);
        when(this.request.get("action")).thenReturn("import");
        when(this.request.getParameter("form_token")).thenReturn("fakeToken");

        String result = this.action.render(this.oldcore.getXWikiContext());

        assertNull(result);
        verify(this.csrfToken).isTokenValid("fakeToken");
        verify(this.csrfToken).getResubmissionURL();
        verify(this.importer, never()).importXAR(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void getPackageInfosDoesNotRequireCSRFToken() throws Exception
    {
        when(this.request.get("name")).thenReturn(TEST_XAR_NAME);
        when(this.request.get("action")).thenReturn("getPackageInfos");

        this.action.render(this.oldcore.getXWikiContext());

        verify(this.csrfToken, never()).isTokenValid(any());
    }

    @Test
    void noActionDoesNotRequireCSRFToken() throws Exception
    {
        when(this.request.get("name")).thenReturn(TEST_XAR_NAME);

        String result = this.action.render(this.oldcore.getXWikiContext());

        assertNull(result);
        verify(this.csrfToken, never()).isTokenValid(any());
    }

    @Test
    void noNameReturnsAdmin() throws Exception
    {
        assertEquals("admin", this.action.render(this.oldcore.getXWikiContext()));
    }

    @Test
    void noAdminRightsReturnsException() throws Exception
    {
        when(this.oldcore.getMockRightService().hasWikiAdminRights(this.oldcore.getXWikiContext())).thenReturn(false);

        assertEquals("exception", this.action.render(this.oldcore.getXWikiContext()));
    }
}
