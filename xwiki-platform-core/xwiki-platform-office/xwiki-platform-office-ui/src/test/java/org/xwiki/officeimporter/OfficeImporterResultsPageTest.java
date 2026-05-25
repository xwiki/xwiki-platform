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
package org.xwiki.officeimporter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.fileupload.FileItem;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.script.OfficeImporterScriptService;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.InfoMessageMacro;
import org.xwiki.rendering.internal.macro.message.WarningMessageMacro;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.script.WikiManagerScriptService;

import com.xpn.xwiki.plugin.fileupload.FileUploadPluginApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@code XWiki.OfficeImporterResults}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@ComponentList({
    InfoMessageMacro.class,
    WarningMessageMacro.class,
    ErrorMessageMacro.class,
    TestNoScriptMacro.class,
})
class OfficeImporterResultsPageTest extends PageTest
{
    private static final DocumentReference PAGE_REFERENCE = new DocumentReference("xwiki", "XWiki",
        "OfficeImporterResults");

    private static final DocumentReference MAIN_PAGE_REFERENCE = new DocumentReference("xwiki", "Main", "WebHome");

    private static final DocumentReference TARGET_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    private static final String FILE_NAME = "test.docx";

    private static final String SERIALIZED_TARGET_REFERENCE = "Space.Page]]{{noscript/}}";

    private static final String ERROR_MESSAGE = "error {{noscript/}}";

    @MockComponent(classToMock = ModelScriptService.class)
    @Named("model")
    private ScriptService modelScriptService;

    @MockComponent(classToMock = OfficeImporterScriptService.class)
    @Named("officeimporter")
    private ScriptService officeImporterScriptService;

    @MockComponent(classToMock = WikiManagerScriptService.class)
    @Named("wiki")
    private ScriptService wikiScriptService;

    @Mock
    private XDOMOfficeDocument xdomOfficeDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        this.context.setUserReference(new DocumentReference("xwiki", "XWiki", "User"));

        FileUploadPluginApi fileUploadPluginApi = mock();
        FileItem fileItem = mock();
        when(this.oldcore.getSpyXWiki().getPluginApi("fileupload", this.context)).thenReturn(fileUploadPluginApi);
        when(fileUploadPluginApi.getFileName("filePath")).thenReturn(FILE_NAME);
        when(fileUploadPluginApi.getFileItems()).thenReturn(List.of(fileItem));
        when(fileItem.getFieldName()).thenReturn("filePath");
        when(fileItem.getInputStream())
            .thenReturn(new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)));

        WikiDescriptor wikiDescriptor = mock();
        when(wikiDescriptor.getMainPageReference()).thenReturn(MAIN_PAGE_REFERENCE);
        when(((WikiManagerScriptService) this.wikiScriptService).getCurrentWikiDescriptor()).thenReturn(wikiDescriptor);
        when(((ModelScriptService) this.modelScriptService).resolveDocument(anyString()))
            .thenReturn(TARGET_DOCUMENT_REFERENCE);
        when(((ModelScriptService) this.modelScriptService).serialize(TARGET_DOCUMENT_REFERENCE))
            .thenReturn(SERIALIZED_TARGET_REFERENCE);
        when(((OfficeImporterScriptService) this.officeImporterScriptService)
            .officeToXDOM(any(), eq(FILE_NAME), eq(TARGET_DOCUMENT_REFERENCE), eq(false)))
            .thenReturn(this.xdomOfficeDocument);
        when(((OfficeImporterScriptService) this.officeImporterScriptService).getErrorMessage())
            .thenReturn(ERROR_MESSAGE);

        this.stubRequest.put("target", "Space.Page");
    }

    @Test
    void escapeResultDocumentLink() throws Exception
    {
        when(((OfficeImporterScriptService) this.officeImporterScriptService)
            .save(this.xdomOfficeDocument, TARGET_DOCUMENT_REFERENCE, true)).thenReturn(true);

        Document document = renderHTMLPage(PAGE_REFERENCE);

        List<String> links = document.select(".infomessage a").stream().map(Element::text).toList();
        assertEquals(2, links.size());
        assertEquals("[xe.officeimporter.results.result", links.get(0));
        assertEquals("xe.officeimporter.results.goback", links.get(1));
    }

    @Test
    void escapeErrorMessageWhenSaveFails() throws Exception
    {
        when(((OfficeImporterScriptService) this.officeImporterScriptService)
            .save(this.xdomOfficeDocument, TARGET_DOCUMENT_REFERENCE, true)).thenReturn(false);

        Document document = renderHTMLPage(PAGE_REFERENCE);

        assertEquals(ERROR_MESSAGE, document.selectFirst(".errormessage").text());
    }

    @Test
    void escapeWarningMessageWhenSplitSaveFails() throws Exception
    {
        this.stubRequest.put("splitDocument", "true");
        TargetDocumentDescriptor targetDocumentDescriptor =
            new TargetDocumentDescriptor(new DocumentReference("xwiki", "Space", "Child"), this.componentManager);
        XDOMOfficeDocument childOfficeDocument = mock();
        when(((OfficeImporterScriptService) this.officeImporterScriptService)
            .split(this.xdomOfficeDocument, new String[] {"1"}, null, false, TARGET_DOCUMENT_REFERENCE))
            .thenReturn(Map.of(targetDocumentDescriptor, childOfficeDocument));
        when(((OfficeImporterScriptService) this.officeImporterScriptService)
            .save(childOfficeDocument, targetDocumentDescriptor.getDocumentReference(), true)).thenReturn(false);

        Document document = renderHTMLPage(PAGE_REFERENCE);

        assertEquals(ERROR_MESSAGE, document.selectFirst(".warningmessage").text());
    }

    @Test
    void escapeErrorMessageWhenSplitFails() throws Exception
    {
        this.stubRequest.put("splitDocument", "true");
        when(((OfficeImporterScriptService) this.officeImporterScriptService)
            .split(this.xdomOfficeDocument, new String[] {"1"}, null, false, TARGET_DOCUMENT_REFERENCE))
            .thenReturn(null);

        Document document = renderHTMLPage(PAGE_REFERENCE);

        assertEquals(ERROR_MESSAGE, document.selectFirst(".errormessage").text());
    }

    @Test
    void escapeErrorMessageWhenImportFails() throws Exception
    {
        when(((OfficeImporterScriptService) this.officeImporterScriptService)
            .officeToXDOM(any(), eq(FILE_NAME), eq(TARGET_DOCUMENT_REFERENCE), eq(false)))
            .thenReturn(null);

        Document document = renderHTMLPage(PAGE_REFERENCE);

        assertEquals(ERROR_MESSAGE, document.selectFirst(".errormessage").text());
    }
}
