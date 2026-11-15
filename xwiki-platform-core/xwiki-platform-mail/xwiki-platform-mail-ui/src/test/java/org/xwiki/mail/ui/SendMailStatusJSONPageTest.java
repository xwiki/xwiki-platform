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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.script.MailScriptService;
import org.xwiki.mail.script.MailStorageScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code Mail.SendMailStatusJSON}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@SecurityScriptServiceComponentList
class SendMailStatusJSONPageTest extends PageTest
{
    private static final DocumentReference SEND_MAIL_STATUS_JSON_REFERENCE =
        new DocumentReference("xwiki", "Mail", "SendMailStatusJSON");

    private static final WikiReference WIKI_REFERENCE = new WikiReference("xwiki");

    private MailStorageScriptService storageScriptService;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        MailScriptService mailScriptService =
            componentManager.registerMockComponent(ScriptService.class, "mail", MailScriptService.class, false);
        this.storageScriptService = mock(MailStorageScriptService.class);
        when(mailScriptService.<ScriptService>get("storage")).thenReturn(this.storageScriptService);

        CSRFTokenScriptService csrfTokenScriptService =
            componentManager.registerMockComponent(ScriptService.class, "csrf", CSRFTokenScriptService.class, false);
        when(csrfTokenScriptService.getToken()).thenReturn("token");

        this.request.put("outputSyntax", "plain");
        setOutputSyntax(Syntax.PLAIN_1_0);
    }

    @Test
    void mailStatusesHiddenWhenNotWikiAdmin() throws Exception
    {
        // Being admin of the current document (e.g. of its space) is not enough: the right is checked on the wiki.
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any(Right.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(0) == Right.ADMIN
                && !WIKI_REFERENCE.equals(invocation.getArgument(1)));

        Map<?, ?> result = renderJSON();

        // The mail storage script service only checks the author of the page, who is an administrator: the page has to
        // check the current user itself.
        verifyNoInteractions(this.storageScriptService);
        assertEquals(0, result.get("totalrows"));
        assertEquals(List.of(), result.get("rows"));
    }

    @Test
    void mailStatusesWhenWikiAdmin() throws Exception
    {
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.ADMIN, WIKI_REFERENCE))
            .thenReturn(true);
        MailStatus status = new MailStatus();
        status.setBatchId("batchId");
        status.setMessageId("messageId");
        status.setState("send_error");
        status.setDate(new Date());
        when(this.storageScriptService.count(anyMap())).thenReturn(1L);
        when(this.storageScriptService.load(anyMap(), anyInt(), anyInt(), any(), anyBoolean()))
            .thenReturn(List.of(status));

        Map<?, ?> result = renderJSON();

        assertEquals(1, result.get("totalrows"));
        List<?> rows = (List<?>) result.get("rows");
        assertEquals(1, rows.size());
        Map<?, ?> row = (Map<?, ?>) rows.get(0);
        assertEquals("batchId", row.get("batchId"));
        // The resend and delete actions carry the CSRF token checked by Mail.SendMailStatus.
        String actions = (String) row.get("actions");
        assertTrue(actions.contains("name='form_token' value='token'"), actions);
    }

    private Map<?, ?> renderJSON() throws Exception
    {
        return new ObjectMapper().readValue(renderPage(SEND_MAIL_STATUS_JSON_REFERENCE), Map.class);
    }
}
