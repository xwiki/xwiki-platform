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
package org.xwiki.export.pdf.internal.macro;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.export.pdf.internal.job.DocumentRenderer;
import org.xwiki.export.pdf.internal.job.PDFExportJob;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobStatus;
import org.xwiki.export.pdf.job.PDFExportJobStatus.DocumentRenderingResult;
import org.xwiki.export.pdf.macro.PDFTocMacroParameters;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.toc.DefaultTocEntriesResolver;
import org.xwiki.rendering.internal.renderer.event.EventBlockRenderer;
import org.xwiki.rendering.internal.renderer.event.EventRenderer;
import org.xwiki.rendering.internal.renderer.event.EventRendererFactory;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PDFTocMacro}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({
    EventBlockRenderer.class,
    EventRendererFactory.class,
    EventRenderer.class,
    PdfTocTreeBuilderFactory.class,
    DefaultTocEntriesResolver.class
})
class PDFTocMacroTest
{
    @InjectMockComponents
    private PDFTocMacro pdfTocMacro;

    @MockComponent
    private JobStatusStore jobStatusStore;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    @Named("plain/1.0")
    private Parser plainTextParser;

    @MockComponent
    private LinkLabelGenerator linkLabelGenerator;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManager;

    @Inject
    @Named("event/1.0")
    private BlockRenderer eventRenderer;

    private PDFTocMacroParameters parameters = new PDFTocMacroParameters();

    @Mock
    private MacroTransformationContext context;

    private List<String> jobId = Arrays.asList("export", "pdf", "1654");

    private PDFExportJobRequest request = new PDFExportJobRequest();

    private PDFExportJobStatus jobStatus = new PDFExportJobStatus(PDFExportJob.JOB_TYPE, this.request, null, null);

    private DocumentReference aliceReference = new DocumentReference("test", "Users", "Alice");

    @BeforeComponent
    void setup() throws Exception
    {
        when(this.contextComponentManager.get()).thenReturn(this.componentManager);
    }

    @BeforeEach
    void configure()
    {
        this.parameters.setJobId(StringUtils.join(this.jobId, "/"));
        this.request.setUserReference(this.aliceReference);

        DocumentReference firstPageReference = new DocumentReference("test", "First", "Page");
        XDOM firstXDOM = new XDOM(Collections.singletonList(
            new HeaderBlock(Collections.singletonList(new WordBlock("First Heading")), HeaderLevel.LEVEL1)));
        this.jobStatus.getDocumentRenderingResults()
            .add(new DocumentRenderingResult(firstPageReference, firstXDOM, "first HTML"));

        DocumentReference secondPageReference = new DocumentReference("test", "Second", "Page");
        XDOM secondXDOM = new XDOM(Collections.singletonList(
            new HeaderBlock(Collections.singletonList(new WordBlock("Second Heading")), HeaderLevel.LEVEL2)));
        this.jobStatus.getDocumentRenderingResults()
            .add(new DocumentRenderingResult(secondPageReference, secondXDOM, "second HTML"));
    }

    @Test
    void executeWithoutJobId()
    {
        try {
            this.pdfTocMacro.execute(new PDFTocMacroParameters(), null, null);
            fail();
        } catch (MacroExecutionException e) {
            assertEquals("The mandatory job id parameter is missing.", e.getMessage());
        }
    }

    @Test
    void executeWithoutJobStatus() throws Exception
    {
        assertEquals(0, this.pdfTocMacro.execute(this.parameters, null, this.context).size());
    }

    @Test
    void executeWithUnexpectedJobStatus() throws Exception
    {
        JobStatus unexpectedJobStatus = mock(JobStatus.class);
        when(this.jobStatusStore.getJobStatus(jobId)).thenReturn(unexpectedJobStatus);
        assertEquals(0, this.pdfTocMacro.execute(this.parameters, null, this.context).size());
        verify(this.jobExecutor).getJob(jobId);
    }

    @Test
    void executeWithDifferentUser() throws Exception
    {
        when(this.jobStatusStore.getJobStatus(jobId)).thenReturn(this.jobStatus);
        assertEquals(0, this.pdfTocMacro.execute(this.parameters, null, this.context).size());
    }

    @Test
    void execute() throws Exception
    {
        when(this.jobStatusStore.getJobStatus(jobId)).thenReturn(this.jobStatus);
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(this.aliceReference);

        List<Block> output = this.pdfTocMacro.execute(this.parameters, null, this.context);

        List<String> events = Arrays.asList(
            "beginList [BULLETED] [[class]=[wikitoc]]",
            "beginListItem",
            "beginLink [Typed = [true] Type = [doc]] [false]",
            "onWord [First Heading]",
            "endLink [Typed = [true] Type = [doc]] [false]",
            "beginList [BULLETED]",
            "beginListItem",
            "beginLink [Typed = [true] Type = [doc]] [false]",
            "onWord [Second Heading]",
            "endLink [Typed = [true] Type = [doc]] [false]",
            "endListItem",
            "endList [BULLETED]",
            "endListItem",
            "endList [BULLETED] [[class]=[wikitoc]]",
            ""
        );
        assertBlockEvents(StringUtils.join(events, "\n"), output.get(0));
    }

    @Test
    void executeWithDepth() throws Exception
    {
        when(this.jobStatusStore.getJobStatus(jobId)).thenReturn(this.jobStatus);
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(this.aliceReference);

        this.parameters.setDepth(1);
        List<Block> output = this.pdfTocMacro.execute(this.parameters, null, this.context);

        List<String> events = Arrays.asList(
            "beginList [BULLETED] [[class]=[wikitoc]]",
            "beginListItem",
            "beginLink [Typed = [true] Type = [doc]] [false]",
            "onWord [First Heading]",
            "endLink [Typed = [true] Type = [doc]] [false]",
            "endListItem",
            "endList [BULLETED] [[class]=[wikitoc]]",
            ""
        );
        assertBlockEvents(StringUtils.join(events, "\n"), output.get(0));
    }

    @Test
    void executeWithDocumentTitles() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("test", "Some", "Page");
        HeaderBlock header =
            new HeaderBlock(Collections.singletonList(new WordBlock("Document Title")), HeaderLevel.LEVEL1);
        header.setParameter(DocumentRenderer.PARAMETER_DOCUMENT_REFERENCE, "test:Some.Page");
        XDOM xdom = new XDOM(
            Arrays.asList(header, new ParagraphBlock(Collections.singletonList(new WordBlock("document content")))));
        this.jobStatus.getDocumentRenderingResults().clear();
        this.jobStatus.getDocumentRenderingResults()
            .add(new DocumentRenderingResult(documentReference, xdom, "rendered content"));

        when(this.jobStatusStore.getJobStatus(jobId)).thenReturn(this.jobStatus);
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(this.aliceReference);

        List<Block> output = this.pdfTocMacro.execute(this.parameters, null, this.context);

        List<String> events = Arrays.asList(
            "beginList [BULLETED] [[class]=[wikitoc]]",
            "beginListItem [[data-xwiki-document-reference]=[test:Some.Page]]",
            "beginLink [Typed = [true] Type = [doc]] [false]",
            "onWord [Document Title]",
            "endLink [Typed = [true] Type = [doc]] [false]",
            "endListItem [[data-xwiki-document-reference]=[test:Some.Page]]",
            "endList [BULLETED] [[class]=[wikitoc]]",
            ""
        );
        assertBlockEvents(StringUtils.join(events, "\n"), output.get(0));
    }

    private void assertBlockEvents(String expected, Block block)
    {
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        this.eventRenderer.render(block, printer);
        assertEquals(expected, printer.toString());
    }
}
