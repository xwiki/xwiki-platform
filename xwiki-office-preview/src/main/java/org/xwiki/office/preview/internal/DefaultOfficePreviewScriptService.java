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
package org.xwiki.office.preview.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.office.preview.OfficePreviewBuilder;
import org.xwiki.office.preview.OfficePreviewScriptService;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Default implementation of {@link OfficePreviewScriptService}.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@Component("officepreview")
public class DefaultOfficePreviewScriptService extends AbstractLogEnabled implements OfficePreviewScriptService
{
    /**
     * The list of supported mime types, i.e. the mime types that can be previewed.
     */
    private static final List<String> SUPPORTED_MIME_TYPES =
        Arrays.asList("application/msword", "application/powerpoint", "application/vnd.ms-powerpoint",
            "application/vnd.ms-excel", "application/vnd.oasis.opendocument",
            "application/vnd.oasis.opendocument.text", "application/vnd.oasis.opendocument.graphics",
            "application/vnd.oasis.opendocument.presentation", "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.chart", "application/vnd.oasis.opendocument.formula");

    /**
     * The key used to save on the execution context the exception caught during office document preview.
     */
    private static final String OFFICE_PREVIEW_EXCEPTION = "officePreview.caughtException";

    /**
     * The component used to preview office documents.
     */
    @Requirement
    private OfficePreviewBuilder officePreviewBuilder;

    /**
     * Used to lookup various {@link OfficePreviewBuilder} implementations based on the office file format.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Reference to the current execution context, used to save the exception caught during office document preview.
     */
    @Requirement
    private Execution execution;

    /**
     * The component used to check access rights on the document holding the office attachment to be previewed.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * {@inheritDoc}
     * 
     * @see OfficePreviewScriptService#getCaughtException()
     */
    public Exception getCaughtException()
    {
        return (Exception) execution.getContext().getProperty(OFFICE_PREVIEW_EXCEPTION);
    }

    /**
     * {@inheritDoc}
     * 
     * @see OfficePreviewScriptService#preview(AttachmentReference)
     */
    public String preview(AttachmentReference attachmentReference)
    {
        Map<String, String> parameters = Collections.emptyMap();
        return preview(attachmentReference, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see OfficePreviewScriptService#preview(AttachmentReference, java.util.Map)
     */
    public String preview(AttachmentReference attachmentReference, Map<String, String> parameters)
    {
        // Clear previous caught exception.
        execution.getContext().removeProperty(OFFICE_PREVIEW_EXCEPTION);
        try {
            // Check whether current user has view rights on the document containing the attachment.
            if (!documentAccessBridge.isDocumentViewable(attachmentReference.getDocumentReference())) {
                throw new RuntimeException("Inadequate privileges.");
            }

            // Build the preview and render the result.
            return render(officePreviewBuilder.build(attachmentReference, parameters), "xhtml/1.0");
        } catch (Exception e) {
            // Save caught exception.
            execution.getContext().setProperty(OFFICE_PREVIEW_EXCEPTION, e);
            getLogger().error("Failed to preview office document: " + attachmentReference, e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see OfficePreviewScriptService#isMimeTypeSupported(String)
     */
    public boolean isMimeTypeSupported(String mimeType)
    {
        return SUPPORTED_MIME_TYPES.contains(mimeType);
    }

    /**
     * Renders the given block into specified syntax.
     * 
     * @param block {@link Block} to be rendered
     * @param syntaxId expected output syntax
     * @return string holding the result of rendering
     * @throws Exception if an error occurs during rendering
     */
    private String render(Block block, String syntaxId) throws Exception
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        BlockRenderer renderer = componentManager.lookup(BlockRenderer.class, syntaxId);
        renderer.render(block, printer);
        return printer.toString();
    }
}
