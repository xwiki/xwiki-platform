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
package org.xwiki.invitation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
import org.xwiki.rendering.internal.macro.message.InfoMessageMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@code Invitation.InvitationMailClass}.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@ComponentList({
    InfoMessageMacro.class,
    TestNoScriptMacro.class,
    // Start - Required in addition of RenderingScriptServiceComponentList
    DefaultExtendedRenderingConfiguration.class,
    RenderingConfigClassDocumentConfigurationSource.class,
    // End - Required in additional of RenderingScriptServiceComponentList
})
class InvitationMailClassPageTest extends PageTest
{
    private static final DocumentReference INVITATION_MAIL_CLASS_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "Invitation", "InvitationMailClass");

    @Test
    void escapeInfoMessageInternalDocumentParameter() throws Exception
    {
        XWikiDocument invitationGuestActionsDocument = loadPage(INVITATION_MAIL_CLASS_DOCUMENT_REFERENCE);

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(
            this.xwiki.getDocument(new DocumentReference("xwiki", "]] {{noscript/}}", "Page"), this.context));

        Document document = Jsoup.parse(invitationGuestActionsDocument.getRenderedContent(this.context));
        Element infomessage = document.selectFirst(".infomessage");
        assertEquals("xe.invitation.internalDocument []] {{noscript/}}.WebHome]", infomessage.text());
    }
}
