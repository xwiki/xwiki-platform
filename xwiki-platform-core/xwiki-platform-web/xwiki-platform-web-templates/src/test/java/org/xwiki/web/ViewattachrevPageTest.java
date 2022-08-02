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

import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the {@code viewattachrev.vm} template. Assert that the returned results are well-formed.
 *
 * @version $Id$
 */
class ViewattachrevPageTest extends PageTest
{
    private static final String VIEWATTACHREV = "viewattachrev.vm";

    @Mock
    private ScriptQuery query;

    @Mock
    private XWikiRecycleBinStoreInterface recycleBinStore;

    private TemplateManager templateManager;

    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.scriptContext = this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
            .getCurrentScriptContext();
    }

    @Test
    void escapeAttachmentName() throws Exception
    {
        XWikiDocument document =
            this.xwiki.getDocument(new DocumentReference("wiki", "space", "document"), this.context);

        XWikiAttachment attachment = new XWikiAttachment(document, "><img src=1 onerror=alert(1)>.jpg");
        attachment.setMimeType("image/jpeg");
        attachment.setArchive("");
        attachment.getAttachment_archive().updateArchive(this.context);

        this.scriptContext.setAttribute("attachment",
            new Attachment(document.newDocument(this.context), attachment, this.context), ScriptContext.GLOBAL_SCOPE);

        String result = this.templateManager.render(VIEWATTACHREV);

        assertFalse(result.contains("><img src=1 onerror=alert(1)>.jpg"));
        assertTrue(result.contains("&#62;&#60;img src=1 onerror=alert(1)&#62;.jpg"));
    }
}
