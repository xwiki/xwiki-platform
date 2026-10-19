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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.mail.script.MailScriptService;
import org.xwiki.mail.script.MailStorageScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code Mail.SendMailStatus}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TestNoScriptMacro.class
})
class SendMailStatusPageTest extends PageTest
{
    private static final DocumentReference SEND_MAIL_STATUS_REFERENCE =
        new DocumentReference("xwiki", "Mail", "SendMailStatus");

    private MailStorageScriptService storageScriptService;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        MailScriptService mailScriptService =
            componentManager.registerMockComponent(ScriptService.class, "mail", MailScriptService.class, false);
        this.storageScriptService = mock(MailStorageScriptService.class);
        when(mailScriptService.<ScriptService>get("storage")).thenReturn(this.storageScriptService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "mailsendingaction_resend", "mailsendingaction_delete" })
    void errorDescriptionIsEscaped(String action) throws Exception
    {
        String mailId = "<strong>{{/html}}{{noscript /}}";
        Exception error = new Exception("Failed to load message (id [%s])".formatted(mailId));
        when(this.storageScriptService.getLastError()).thenReturn(error);

        this.request.put(action, "1");
        this.request.put("mail_id", mailId);
        this.request.put("batch_id", "batchId");

        Document document = renderHTMLPage(SEND_MAIL_STATUS_REFERENCE);

        Element errorDescription = document.selectFirst(".xwikirenderingerrordescription pre");
        assertNotNull(errorDescription);
        assertTrue(errorDescription.text().startsWith("java.lang.Exception: " + error.getMessage()),
            errorDescription.text());
    }
}
