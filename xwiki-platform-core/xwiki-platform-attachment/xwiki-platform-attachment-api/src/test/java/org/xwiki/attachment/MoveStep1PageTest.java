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
package org.xwiki.attachment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Test of {@code moveStep1.vm} template.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@SecurityScriptServiceComponentList
@ComponentList({
    TemplateScriptService.class
})
class MoveStep1PageTest extends PageTest
{
    private static final String MOVE_STEP1_TEMPLATE = "attachment/moveStep1.vm";

    private static final String ATTACHMENT_NAME = ">a<b>tt</b>achmen\"'t1.txt";

    private static final DocumentReference DOC_REF = new DocumentReference("xwiki", "Space", "Page");

    private TemplateManager templateManager;

    @Mock
    private CSRFTokenScriptService csrfScriptService;

    private VelocityManager velocityManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);
        this.componentManager.registerComponent(ScriptService.class, "csrf", this.csrfScriptService);
        when(this.csrfScriptService.getToken()).thenReturn("csrf_token");
    }

    @Test
    void escapedValues() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(DOC_REF, this.context);
        this.context.setDoc(document);

        this.velocityManager.getVelocityContext().put("attachment", new AttachmentReference(ATTACHMENT_NAME,
            document.getDocumentReference()));

        Document render = Jsoup.parse(this.templateManager.render(MOVE_STEP1_TEMPLATE));
        assertNotNull(render);
        assertEquals("cancel", render.select(".buttons .secondary.button").text());
        assertEquals("attachment.move.submit", render.select(".buttons input").attr("value"));
        assertEquals("attachment.move.autoRedirect.label", render.select("dt.autoRedirect label").text());
        assertEquals("attachment.move.autoRedirect.hint", render.select("dd.autoRedirect .xHint").text());
        assertEquals("attachment.move.newName.label", render.select("dt.targetAttachmentName label").text());
        assertEquals("attachment.move.newName.hint", render.select("dt.targetAttachmentName .xHint").text());
        assertEquals(ATTACHMENT_NAME, render.select("[name='sourceAttachmentName']").attr("value"));
        assertEquals(ATTACHMENT_NAME, render.select("#targetAttachmentNameTitle").attr("value"));
    }
}
