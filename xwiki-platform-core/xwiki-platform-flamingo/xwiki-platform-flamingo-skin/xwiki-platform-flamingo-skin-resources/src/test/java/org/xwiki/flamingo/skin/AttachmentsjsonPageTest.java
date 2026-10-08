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
package org.xwiki.flamingo.skin;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.internal.XWikiDateTool;
import org.xwiki.velocity.tools.JSONTool;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@code attachmentsjson.vm} template of the Flamingo skin.
 *
 * @version $Id$
 */
@ComponentList({
    XWikiDateTool.class,
    ModelScriptService.class
})
@SecurityScriptServiceComponentList
class AttachmentsjsonPageTest extends PageTest
{
    private static final Path TEMPLATE = Path.of("src/main/resources/flamingo/attachmentsjson.vm");

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    @Mock
    private QueryManagerScriptService queryService;

    @Mock
    private ScriptQuery query;

    private TemplateManager templateManager;

    private JSONTool jsonTool;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryService);

        this.jsonTool = spy(new JSONTool());
        registerVelocityTool("jsontool", this.jsonTool);

        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.addFilter(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValue(anyString(), any())).thenReturn(this.query);

        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any(), any())).thenReturn(true);

        this.context.setAction("get");
    }

    @Test
    void attachmentSavedDuringTheRequest() throws Exception
    {
        // The document loaded at the beginning of the request doesn't have the attachment saved afterwards, which
        // the query returns.
        XWikiDocument requestDocument = saveDocumentWithAttachments("first.txt");
        this.context.setDoc(requestDocument);
        saveDocumentWithAttachments("first.txt", "second.txt");
        mockQueryResults("first.txt", "second.txt");

        Map<String, Object> result = renderTemplate();

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertEquals(List.of("first.txt", "second.txt"), rows.stream().map(row -> row.get("id")).toList());
        assertTrue(((String) rows.get(1).get("filesize")).contains("data-size=\"10\""),
            () -> "Unexpected file size: " + rows.get(1).get("filesize"));
        assertEquals(2, result.get("returnedrows"));
    }

    @Test
    void attachmentDeletedAfterTheQuery() throws Exception
    {
        this.context.setDoc(saveDocumentWithAttachments("first.txt"));
        mockQueryResults("first.txt", "deleted.txt");

        Map<String, Object> result = renderTemplate();

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertEquals(List.of("first.txt"), rows.stream().map(row -> row.get("id")).toList());
        assertEquals(1, result.get("returnedrows"));
    }

    private XWikiDocument saveDocumentWithAttachments(String... fileNames) throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(DOCUMENT_REFERENCE, this.context).clone();
        for (String fileName : fileNames) {
            if (document.getAttachment(fileName) == null) {
                XWikiAttachment attachment = new XWikiAttachment(document, fileName);
                attachment.setContent(new ByteArrayInputStream(fileName.getBytes(StandardCharsets.UTF_8)));
                document.setAttachment(attachment);
            }
        }
        this.xwiki.saveDocument(document, this.context);
        return this.xwiki.getDocument(DOCUMENT_REFERENCE, this.context).clone();
    }

    private void mockQueryResults(String... fileNames) throws Exception
    {
        List<AttachmentReference> references = List.of(fileNames).stream()
            .map(fileName -> new AttachmentReference(fileName, DOCUMENT_REFERENCE)).toList();
        when(this.query.execute()).thenReturn((List) references);
        when(this.query.count()).thenReturn((long) references.size());
    }

    private Map<String, Object> renderTemplate() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.templateManager.render(this.templateManager.createStringTemplate("attachmentsjson.vm",
            Files.readString(TEMPLATE, StandardCharsets.UTF_8), (DocumentReference) null, (DocumentReference) null),
            writer);

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(this.jsonTool).serialize(argument.capture());
        return (Map<String, Object>) argument.getValue();
    }
}
