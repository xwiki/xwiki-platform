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
package org.xwiki.mail.ui;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.mail.script.MailScriptService;
import org.xwiki.mail.script.MailStorageScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.SuccessMessageMacro;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code Mail.SendMailStatus}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@SecurityScriptServiceComponentList
@ComponentList({
    ErrorMessageMacro.class,
    SuccessMessageMacro.class,
    TestNoScriptMacro.class
})
class SendMailStatusPageTest extends PageTest
{
    private static final DocumentReference SEND_MAIL_STATUS_REFERENCE =
        new DocumentReference("xwiki", "Mail", "SendMailStatus");

    private static final WikiReference WIKI_REFERENCE = new WikiReference("xwiki");

    private MailStorageScriptService storageScriptService;

    private CSRFTokenScriptService csrfTokenScriptService;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        MailScriptService mailScriptService =
            componentManager.registerMockComponent(ScriptService.class, "mail", MailScriptService.class, false);
        this.storageScriptService = mock(MailStorageScriptService.class);
        when(mailScriptService.<ScriptService>get("storage")).thenReturn(this.storageScriptService);

        this.csrfTokenScriptService =
            componentManager.registerMockComponent(ScriptService.class, "csrf", CSRFTokenScriptService.class, false);

        // By default, the current user is an administrator of the wiki and the request has a valid CSRF token.
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.ADMIN, WIKI_REFERENCE))
            .thenReturn(true);
        when(this.csrfTokenScriptService.isTokenValid("validToken")).thenReturn(true);
    }

    private void putActionParameters(String action, String mailId, String formToken)
    {
        this.request.put(action, "1");
        this.request.put("mail_id", mailId);
        this.request.put("batch_id", "batchId");
        this.request.put("form_token", formToken);
    }

    @ParameterizedTest
    @ValueSource(strings = { "mailsendingaction_resend", "mailsendingaction_delete" })
    void actionIgnoredWhenNotWikiAdmin(String action) throws Exception
    {
        // Being admin of the current document (e.g. of its space) is not enough: the right is checked on the wiki.
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any(Right.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(0) == Right.ADMIN
                && !WIKI_REFERENCE.equals(invocation.getArgument(1)));

        putActionParameters(action, "mailId", "validToken");

        Document document = renderHTMLPage(SEND_MAIL_STATUS_REFERENCE);

        verifyNoInteractions(this.storageScriptService);
        assertNull(document.selectFirst(".box.successmessage"));
        assertNull(document.selectFirst(".box.errormessage"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "mailsendingaction_resend", "mailsendingaction_delete" })
    void actionRejectedWhenInvalidCSRFToken(String action) throws Exception
    {
        putActionParameters(action, "mailId", "invalidToken");

        Document document = renderHTMLPage(SEND_MAIL_STATUS_REFERENCE);

        verifyNoInteractions(this.storageScriptService);
        Element error = document.selectFirst(".box.errormessage");
        assertNotNull(error);
        assertEquals("mailSender.admin.invalidCSRF", error.text());
    }

    @Test
    void delete() throws Exception
    {
        putActionParameters("mailsendingaction_delete", "mailId", "validToken");

        Document document = renderHTMLPage(SEND_MAIL_STATUS_REFERENCE);

        verify(this.storageScriptService).delete("batchId", "mailId");
        Element success = document.selectFirst(".box.successmessage");
        assertNotNull(success);
        assertEquals("mailSender.admin.deleteSuccess", success.text());
    }

    @ParameterizedTest
    @ValueSource(strings = { "mailsendingaction_resend", "mailsendingaction_delete" })
    void errorDescriptionIsEscaped(String action) throws Exception
    {
        String mailId = "<strong>{{/html}}{{noscript /}}";
        Exception error = new Exception("Failed to load message (id [%s])".formatted(mailId));
        when(this.storageScriptService.getLastError()).thenReturn(error);

        putActionParameters(action, mailId, "validToken");

        Document document = renderHTMLPage(SEND_MAIL_STATUS_REFERENCE);

        Element errorDescription = document.selectFirst(".xwikirenderingerrordescription pre");
        assertNotNull(errorDescription);
        assertTrue(errorDescription.text().startsWith("java.lang.Exception: " + error.getMessage()),
            errorDescription.text());
    }
}
