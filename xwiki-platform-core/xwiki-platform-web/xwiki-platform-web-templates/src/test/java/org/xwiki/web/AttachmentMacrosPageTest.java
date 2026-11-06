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
package org.xwiki.web;

import javax.inject.Named;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.xwiki.icon.IconManagerScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the {@code attachment_macros.vm} template.
 *
 * @version $Id$
 * @since 18.10.0RC1
 * @since 18.4.8
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList(TemplateScriptService.class)
class AttachmentMacrosPageTest extends PageTest
{
    @MockComponent(classToMock = IconManagerScriptService.class)
    @Named("icon")
    private ScriptService iconManagerScriptService;

    @Test
    void deleteAttachmentModalCloseButtonHasLabel() throws Exception
    {
        XWikiDocument document =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        document.setContent("""
            {{velocity}}
            #template('attachment_macros.vm')
            {{html clean="false"}}#deleteAttachmentModal(){{/html}}
            {{/velocity}}""");
        document.setSyntax(Syntax.XWIKI_2_1);

        Element closeButton =
            Jsoup.parse(document.getRenderedContent(this.context)).selectFirst(".deleteAttachment button.close");
        assertNotNull(closeButton);
        assertEquals("core.modal.close", closeButton.attr("aria-label"));
    }
}
