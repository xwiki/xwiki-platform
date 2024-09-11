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
package org.xwiki.sharepage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Page test for the {@code shareinline} template.
 *
 * @version $Id$
 */
@ComponentList({
    TemplateScriptService.class
})
class ShareInlineTemplateTest extends PageTest
{
    /**
     * Verify that when no "from" address is specified, one is constructed automatically, based on the server name.
     */
    @Test
    void shareByEmailWhenNoFromAddress() throws Exception
    {
        // Log in (since the template checks that a user is logged in)
        this.oldcore.getXWikiContext().setUserReference(new DocumentReference("xwiki", "XWiki", "SomeUser"));

        // Simulate that we're using the shareinline template to send the emails
        this.request.put("send", "1");

        // Simulate the server name
        this.request.setServerName("localhost");

        // Simulate a target recipient to send the share page to, specified using an email address
        this.request.put("target", "john@doe.com");

        // Register a fake MailSenderPlugin in which we assert the "from" parameter for the mail sending call.
        this.oldcore.getSpyXWiki().getPluginManager().addPlugin("mailsender", TestableMailSenderPlugin.class.getName(),
            this.oldcore.getXWikiContext());
        TestableMailSenderPlugin plugin =
            (TestableMailSenderPlugin) this.oldcore.getSpyXWiki().getPluginManager().getPlugin("mailsender");

        // This is the test, triggered by the call to templateManager.render()
        plugin.setExpectations("=?UTF-8?Q?SomeUser?= <noreply@localhost>");

        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        String result = templateManager.render("shareinline.vm");

        assertTrue(result.contains("<div class=\"box infomessage\">core.viewers.share.send.success [john]</div>"));
    }

    @Test
    void displayEmailErrorWithSpecialChars() throws Exception
    {
        // Log in (since the template checks that a user is logged in)
        this.oldcore.getXWikiContext().setUserReference(new DocumentReference("xwiki", "XWiki", "SomeUser"));

        // Simulate that we're using the shareinline template to send the emails
        this.request.put("send", "1");
        // Simulate an unknown target recipient to send the share page to, to produce an error message.
        // The '@' is required in order to make it considered as a mail.
        this.request.put("target", "<strong>hello</strong>@");

        this.request.put("message", "Test message");

        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);

        Document document = Jsoup.parse(templateManager.render("shareinline.vm"));

        assertEquals("error: core.viewers.share.send.error "
                + "[<strong>hello</strong>, core.viewers.share.error.serverError]",
            document.selectFirst(".errormessage").text());
    }
}
