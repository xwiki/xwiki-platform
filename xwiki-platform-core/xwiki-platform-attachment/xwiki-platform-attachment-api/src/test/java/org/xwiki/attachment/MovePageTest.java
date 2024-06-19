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

import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.attachment.internal.DefaultAttachmentsManager;
import org.xwiki.attachment.internal.configuration.DefaultAttachmentConfiguration;
import org.xwiki.attachment.script.AttachmentScriptService;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.icon.IconManagerScriptServiceComponentList;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.internal.reference.converter.EntityReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.IconSetup;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test of the templates related to attachments move.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@SecurityScriptServiceComponentList
@IconManagerScriptServiceComponentList
@ComponentList({
    ModelScriptService.class,
    AttachmentScriptService.class,
    DefaultAttachmentConfiguration.class,
    DefaultAttachmentsManager.class,
    DocumentReferenceConverter.class,
    EntityReferenceConverter.class,
    TemplateScriptService.class
})
class MovePageTest extends PageTest
{
    private static final String MOVE_TEMPLATE = "attachment/move.vm";

    private static final DocumentReference DOC_REF = new DocumentReference("xwiki", "Space", "Page");

    private static final String ATTACHMENT_NAME = "attachment.txt";

    private TemplateManager templateManager;

    @Mock
    private CSRFTokenScriptService csrfScriptService;

    private ContextualAuthorizationManager contextualAuthorizationManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        // Initializes then environment for the icon extension.
        IconSetup.setUp(this, "/icons/default.iconset");
        this.componentManager.registerComponent(ScriptService.class, "csrf", this.csrfScriptService);
        this.componentManager.registerMockComponent(JobExecutor.class);
        when(this.csrfScriptService.isTokenValid(any(String.class))).thenReturn(true);
        this.contextualAuthorizationManager = this.componentManager.getInstance(ContextualAuthorizationManager.class);
    }

    @Test
    void moveViewNotAllowed() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(DOC_REF, this.context);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, DOC_REF)).thenReturn(false);
        document.setAttachment(ATTACHMENT_NAME, IOUtils.toInputStream("some content", Charset.defaultCharset()),
            this.context);
        this.xwiki.saveDocument(document, this.context);
        this.context.setDoc(document);
        this.request.put("attachment", ATTACHMENT_NAME);
        Document render = Jsoup.parse(this.templateManager.render(MOVE_TEMPLATE));
        assertEquals("error notallowed", render.getElementsByClass("xwikimessage").get(0).text());
    }

    @Test
    void move() throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, DOC_REF)).thenReturn(true);
        XWikiDocument document = this.xwiki.getDocument(DOC_REF, this.context);
        document.setAttachment(ATTACHMENT_NAME, IOUtils.toInputStream("some content", Charset.defaultCharset()),
            this.context);
        this.xwiki.saveDocument(document, this.context);
        this.context.setDoc(document);
        this.request.put("attachment", ATTACHMENT_NAME);
        Document render = Jsoup.parse(this.templateManager.render(MOVE_TEMPLATE));
        assertEquals("Space Page attachment.txt", render.getElementsByClass("breadcrumb").get(0).text());
        assertEquals(ATTACHMENT_NAME, render.getElementById("targetAttachmentNameTitle").val());
        assertEquals("xwiki:Space.Page", render.getElementsByAttributeValue("name", "sourceLocation").val());
        assertEquals(ATTACHMENT_NAME, render.getElementsByAttributeValue("name", "sourceAttachmentName").val());
        assertEquals("Space.Page", render.getElementById("targetLocation").getElementsByTag("option").get(0).val());
    }

    @Test
    void submitMoveTargetEditNotAllowed() throws Exception
    {
        DocumentReference sourceDocumentReference = new DocumentReference("xwiki", "Space", "Source");
        DocumentReference targetDocumentReference = new DocumentReference("xwiki", "Space", "Target");

        // Allow the user to edit the source document.
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, sourceDocumentReference)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, targetDocumentReference)).thenReturn(false);

        this.context.setDoc(this.xwiki.getDocument(sourceDocumentReference, this.context));
        XWikiDocument document =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Target"), this.context);
        this.xwiki.saveDocument(document, this.context);
        this.request.put("form_token", "a6DSv7pKWcPargoTvyx2Ww");
        this.request.put("async", "true");
        this.request.put("sourceLocation", "xwiki:Space.Source");
        this.request.put("sourceAttachmentName", "oldname.txt");
        this.request.put("autoRedirect", "true");
        this.request.put("targetAttachmentName", ATTACHMENT_NAME);
        this.request.put("targetLocation", "Space.Target");
        this.request.put("step", "2");

        Document render = Jsoup.parse(this.templateManager.render(MOVE_TEMPLATE));
        assertEquals("error: attachment.move.targetNotWritable",
            render.getElementsByClass("errormessage").get(0).text());
    }

    @Test
    void submitTargetAttachmentNameEmpty() throws Exception
    {
        this.context.setDoc(this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Source"), this.context));
        this.request.put("step", "2");
        this.request.put("form_token", "a6DSv7pKWcPargoTvyx2Ww");
        Document render = Jsoup.parse(this.templateManager.render(MOVE_TEMPLATE));
        assertEquals("error: attachment.move.emptyName", render.select(".errormessage").text());
    }

    @Test
    void submitMoveTargetAlreadyExists() throws Exception
    {
        DocumentReference sourceDocumentReference = new DocumentReference("xwiki", "Space", "Source");
        DocumentReference targetDocumentReference = new DocumentReference("xwiki", "Space", "Target\"'");
        this.context.setDoc(this.xwiki.getDocument(sourceDocumentReference, this.context));

        // Allow the user to edit the source and target documents.
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, sourceDocumentReference)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, targetDocumentReference)).thenReturn(true);
        XWikiDocument document =
            this.xwiki.getDocument(targetDocumentReference, this.context);
        document.setAttachment(ATTACHMENT_NAME, IOUtils.toInputStream("some content", Charset.defaultCharset()),
            this.context);
        this.xwiki.saveDocument(document, this.context);
        this.request.put("form_token", "a6DSv7pKWcPargoTvyx2Ww");
        this.request.put("async", "true");
        this.request.put("sourceWikiName", "xwiki");
        this.request.put("sourceSpaceName", "Space");
        this.request.put("sourcePageName", "Source");
        this.request.put("sourceAttachmentName", "oldname.txt");
        this.request.put("autoRedirect", "true");
        this.request.put("targetAttachmentName", ATTACHMENT_NAME);
        this.request.put("targetLocation", "Space.Target\"'");
        this.request.put("step", "2");

        Document render = Jsoup.parse(this.templateManager.render(MOVE_TEMPLATE));
        assertEquals("error: attachment.move.alreadyExists "
                + "[attachment.txt, Space.Target\"', /xwiki/bin/view/Space/Target%22%27]",
            render.getElementsByClass("errormessage").get(0).text());
    }
}
