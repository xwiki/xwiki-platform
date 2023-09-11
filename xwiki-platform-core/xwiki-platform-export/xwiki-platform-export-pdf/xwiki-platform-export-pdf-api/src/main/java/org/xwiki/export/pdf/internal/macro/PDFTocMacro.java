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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.export.pdf.job.PDFExportJobStatus;
import org.xwiki.export.pdf.job.PDFExportJobStatus.DocumentRenderingResult;
import org.xwiki.export.pdf.macro.PDFTocMacroParameters;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.toc.TocTreeBuilder;
import org.xwiki.rendering.internal.macro.toc.TreeParameters;
import org.xwiki.rendering.internal.macro.toc.TreeParametersBuilder;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.toc.TocMacroParameters;
import org.xwiki.rendering.macro.toc.TocTreeBuilderFactory;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Used to generate the table of contents for the PDF export.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component
@Named("pdftoc")
@Singleton
public class PDFTocMacro extends AbstractMacro<PDFTocMacroParameters>
{
    /**
     * Used to retrieve the status of a finished job.
     */
    @Inject
    private JobStatusStore jobStatusStore;

    /**
     * Used to retrieve the status of the job currently being executed.
     */
    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    private TocTreeBuilder tocTreeBuilder;

    @Inject
    @Named("pdf")
    private TocTreeBuilderFactory tocTreeBuilderFactory;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public PDFTocMacro()
    {
        super("PDF Table of Contents", "Generates the table of contents for the PDF export.",
            PDFTocMacroParameters.class);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_INTERNAL));
    }

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        try {
            this.tocTreeBuilder = this.tocTreeBuilderFactory.build();
        } catch (ComponentLookupException e) {
            throw new InitializationException(String.format("Failed to initialize [%s]", TocTreeBuilder.class), e);
        }
    }

    @Override
    public List<Block> execute(PDFTocMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (parameters.getJobId() == null) {
            throw new MacroExecutionException("The mandatory job id parameter is missing.");
        }

        PDFExportJobStatus jobStatus = getJobStatus(parameters.getJobId());
        if (jobStatus != null && Objects.equals(jobStatus.getRequest().getUserReference(),
            this.documentAccessBridge.getCurrentUserReference())) {
            return generateToc(aggregateRenderingResults(jobStatus.getDocumentRenderingResults()),
                parameters.getDepth(), context);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    private PDFExportJobStatus getJobStatus(String jobIdString)
    {
        List<String> jobId = Arrays.asList(jobIdString.split("/"));

        JobStatus jobStatus;
        Job job = this.jobExecutor.getJob(jobId);
        if (job == null) {
            jobStatus = this.jobStatusStore.getJobStatus(jobId);
        } else {
            jobStatus = job.getStatus();
        }

        if (jobStatus instanceof PDFExportJobStatus) {
            return (PDFExportJobStatus) jobStatus;
        } else {
            return null;
        }
    }

    private XDOM aggregateRenderingResults(List<DocumentRenderingResult> renderingResults)
    {
        return new XDOM(renderingResults.stream().map(DocumentRenderingResult::getXDOM).map(XDOM::getChildren)
            .flatMap(List::stream).collect(Collectors.toList()));
    }

    private List<Block> generateToc(XDOM content, int depth, MacroTransformationContext context)
    {
        TreeParametersBuilder builder = new TreeParametersBuilder();
        TocMacroParameters parameters = new TocMacroParameters();
        parameters.setDepth(depth);
        TreeParameters treeParameters = builder.build(content, parameters, context);
        return this.tocTreeBuilder.build(treeParameters);
    }
}
