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
package org.xwiki.index;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.model.internal.reference.converter.EntityReferenceConverter;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryParameter;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.script.SecurityAuthorizationScriptService;
import org.xwiki.security.authorization.script.internal.RightConverter;
import org.xwiki.security.internal.DefaultSecurityConfiguration;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.velocity.internal.XWikiDateTool;
import org.xwiki.velocity.tools.CollectionTool;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.velocity.tools.JSONTool;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of the AllAttachmentsResults.xml document.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@XWikiSyntax20ComponentList
@ComponentList({
    TemplateMacro.class,
    // SecurityScriptService
    SecurityScriptService.class,
    RightConverter.class,
    EntityReferenceConverter.class,
    SecurityAuthorizationScriptService.class,
    DefaultSecurityConfiguration.class,
    // ModelScriptService
    ModelScriptService.class,
    // XWikiDateTool
    XWikiDateTool.class
})
class AllAttachmentsResultsTest extends PageTest
{
    @Mock
    private QueryManagerScriptService queryService;

    @Mock
    private ScriptQuery query;

    private XWikiDateTool dateTool;

    @BeforeEach
    void setUp() throws Exception
    {
        setOutputSyntax(Syntax.PLAIN_1_0);
        registerVelocityTool("numbertool", new NumberTool());
        this.dateTool = this.componentManager.getInstance(XWikiDateTool.class);
        registerVelocityTool("datetool", this.dateTool);
        registerVelocityTool("escapetool", new EscapeTool());
        registerVelocityTool("mathtool", new MathTool());
        registerVelocityTool("stringtool", new StringUtils());
        registerVelocityTool("collectiontool", new CollectionTool());

        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryService);

        this.request.put("reqNo", "1");

        this.context.setAction("get");
    }

    @Test
    void listAttachmentsWithoutFilters() throws Exception
    {
        initDefaultQueryMocks();

        DocumentReference templatesDocumentReference =
            new DocumentReference("xwiki", asList("Help", "Templates"), "WebHome");
        DocumentReference applicationsDocumentReference =
            new DocumentReference("xwiki", asList("Help", "Applications"), "WebHome");
        AttachmentReference administrationMp4Attachment =
            new AttachmentReference("Administration.mp4", templatesDocumentReference);
        AttachmentReference administrationWebmAttachment =
            new AttachmentReference("Administration.webm", templatesDocumentReference);
        AttachmentReference articlePngAttachment = new AttachmentReference("article.png", templatesDocumentReference);
        AttachmentReference awmMp4Attachment = new AttachmentReference("AWM.mp4", applicationsDocumentReference);
        when(this.query.execute()).thenReturn(asList(
            administrationMp4Attachment,
            administrationWebmAttachment,
            articlePngAttachment,
            awmMp4Attachment
        ));

        // Initialize the attachments.
        addAttachment("Administration.mp4", administrationMp4Attachment);
        addAttachment("article.png", articlePngAttachment);
        addAttachment("AWM.mp4", awmMp4Attachment);

        when(this.query.count()).thenReturn(100L);

        // Change the visibility of the second attachment to false.
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(same(Right.VIEW),
            eq(administrationWebmAttachment))).thenReturn(false);

        Map<String, Object> response = renderPage();

        verify(this.queryService).hql(" order by lower(attachment.filename) asc, attachment.filename asc");

        assertEquals(100L, response.get("totalrows"));
        assertEquals(4, response.get("returnedrows"));
        assertEquals(1, response.get("offset"));
        assertEquals(1, response.get("reqNo"));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
        assertEquals(4, rows.size());

        Map<String, Object> row0 = rows.get(0);
        Map<String, Object> row1 = rows.get(1);
        Map<String, Object> row2 = rows.get(2);
        Map<String, Object> row3 = rows.get(3);

        assertTrue((Boolean) row0.get("acclev"));
        assertFalse((Boolean) row1.get("acclev"));
        assertTrue((Boolean) row2.get("acclev"));
        assertTrue((Boolean) row3.get("acclev"));

        assertFullName((String) row0.get("fullName"), "Help Templates", "/xwiki/bin/view/Help/",
            "/xwiki/bin/view/Help/Templates/");
        Document row1FullNameDocument = Jsoup.parse((String) row1.get("fullName"));
        assertEquals("Help / Templates", row1FullNameDocument.text());
        assertEquals(0, row1FullNameDocument.getElementsByClass("space").size());
        assertFullName((String) row2.get("fullName"), "Help Templates", "/xwiki/bin/view/Help/",
            "/xwiki/bin/view/Help/Templates/");
        assertFullName((String) row3.get("fullName"), "Help Applications", "/xwiki/bin/view/Help/",
            "/xwiki/bin/view/Help/Applications/");

        // doc.fullName is expected to always be equal to fullName.
        assertEquals(row0.get("fullName"), row0.get("doc.fullName"));
        assertEquals(row1.get("fullName"), row1.get("doc.fullName"));
        assertEquals(row2.get("fullName"), row2.get("doc.fullName"));
        assertEquals(row3.get("fullName"), row3.get("doc.fullName"));

        assertMimeType(row0, "video/mp4");
        assertFalse(row1.containsKey("mimeType"));
        assertMimeType(row2, "image/png");
        assertMimeType(row3, "video/mp4");
    }

    @Test
    void filterByTimestampDateRange() throws Exception
    {
        initDefaultQueryMocks();
        this.request.put("date", "1607295600000-1632347999999");

        renderPage();

        verify(this.queryService).hql("where attachment.date >= :attachment_date_start "
            + "and attachment.date <= :attachment_date_end "
            + "order by lower(attachment.filename) asc, attachment.filename asc");
        verify(this.query).bindValue("attachment_date_start", this.dateTool.toDate(1607295600000L));
        verify(this.query).bindValue("attachment_date_end", this.dateTool.toDate(1632347999999L));
    }

    @Test
    void filterByISO8601DateRange() throws Exception
    {
        initDefaultQueryMocks();
        this.request.put("date", "2021-09-22T00:00:00+02:00/2021-09-22T23:59:59+02:00");

        renderPage();

        verify(this.queryService).hql("where attachment.date >= :attachment_date_start "
            + "and attachment.date <= :attachment_date_end "
            + "order by lower(attachment.filename) asc, attachment.filename asc");
        verify(this.query).bindValue("attachment_date_start",
            this.dateTool.toDate("iso_tz", "2021-09-22T00:00:00+02:00"));
        verify(this.query).bindValue("attachment_date_end",
            this.dateTool.toDate("iso_tz", "2021-09-22T23:59:59+02:00"));
    }

    private void initDefaultQueryMocks() throws QueryException
    {
        when(this.queryService.hql(any())).thenReturn(this.query);
        when(this.query.addFilter(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValue(anyString(), any())).thenReturn(this.query);
        QueryParameter queryParameter = mock(QueryParameter.class);
        when(this.query.bindValue(anyString())).thenReturn(queryParameter);
        when(queryParameter.literal(any())).thenReturn(queryParameter);
        when(queryParameter.anyChars()).thenReturn(queryParameter);
        when(queryParameter.query()).thenReturn(this.query);
    }

    private void assertMimeType(Map<String, Object> row, String expectedMimeType)
    {
        assertEquals(expectedMimeType,
            Jsoup.parse((String) row.get("mimeType")).getElementsByClass("attachmentMimeType").get(0)
                .attr("data-type"));
    }

    private void assertFullName(String fullNameContent, String expectedText, String expectedFirst,
        String expectedSecondLink)
    {
        Document fullNameDocument = Jsoup.parse(fullNameContent);
        assertEquals(expectedText, fullNameDocument.text());
        Elements spaces = fullNameDocument.getElementsByClass("space");
        assertEquals(expectedFirst, spaces.get(0).child(0).attr("href"));
        assertEquals(expectedSecondLink, spaces.get(1).child(0).attr("href"));
    }

    private void addAttachment(String filename, AttachmentReference attachment)
        throws XWikiException, IOException
    {
        XWikiDocument document = this.xwiki.getDocument(attachment, this.context);
        document.setAttachment(filename,
            AllAttachmentsResultsTest.class.getResourceAsStream("/AllAttachmentsResults/" + filename), this.context);
        this.xwiki.saveDocument(document, this.context);
    }

    private Map<String, Object> renderPage() throws Exception
    {
        JSONTool jsonTool = mock(JSONTool.class);
        registerVelocityTool("jsontool", jsonTool);

        renderPage(new DocumentReference("xwiki", "XWiki", "AllAttachmentsResults"));

        ArgumentCaptor<Map<String, Object>> argument = ArgumentCaptor.forClass(Map.class);
        verify(jsonTool).serialize(argument.capture());

        return argument.getValue();
    }
}
