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
package org.xwiki.office.viewer.script;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.artofsolving.jodconverter.document.DocumentFormat;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.office.viewer.OfficeViewer;
import org.xwiki.office.viewer.OfficeViewerScriptService;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Default implementation of {@link OfficeViewerScriptService}.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@Component
@Named("officeviewer")
@Singleton
public class DefaultOfficeViewerScriptService implements OfficeViewerScriptService
{
    /**
     * The key used to save on the execution context the exception caught during office document view.
     */
    private static final String OFFICE_VIEW_EXCEPTION = "officeView.caughtException";

    /**
     * The component used to view office documents.
     */
    @Inject
    private OfficeViewer officeViewer;

    /**
     * The component used to retrieve the office document converter, which knows the supported media types.
     * 
     * @see #isMimeTypeSupported(String)
     */
    @Inject
    private OfficeServer officeServer;

    /**
     * Used to lookup various {@link BlockRenderer} implementations based on the output syntax.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Reference to the current execution context, used to save the exception caught during office document view.
     */
    @Inject
    private Execution execution;

    /**
     * The component used to check access rights on the document holding the office attachment to be viewed.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to perform the XDOM transformations.
     */
    @Inject
    private TransformationManager transformationManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public Exception getCaughtException()
    {
        return (Exception) this.execution.getContext().getProperty(OFFICE_VIEW_EXCEPTION);
    }

    @Override
    public String view(AttachmentReference attachmentReference)
    {
        Map<String, String> parameters = Collections.emptyMap();
        return view(attachmentReference, parameters);
    }

    @Override
    public String view(AttachmentReference attachmentReference, Map<String, String> parameters)
    {
        // Clear previous caught exception.
        this.execution.getContext().removeProperty(OFFICE_VIEW_EXCEPTION);
        try {
            DocumentReference documentReference = attachmentReference.getDocumentReference();
            // Check whether current user has view rights on the document containing the attachment.
            if (!this.documentAccessBridge.isDocumentViewable(documentReference)) {
                throw new RuntimeException("Inadequate privileges.");
            }

            // Create the view and render the result.
            Syntax fromSyntax = this.documentAccessBridge.getTranslatedDocumentInstance(documentReference).getSyntax();
            Syntax toSyntax = Syntax.XHTML_1_0;
            return render(this.officeViewer.createView(attachmentReference, parameters), fromSyntax, toSyntax);
        } catch (Exception e) {
            // Save caught exception.
            this.execution.getContext().setProperty(OFFICE_VIEW_EXCEPTION, e);
            this.logger.error("Failed to view office document: " + attachmentReference, e);
            return null;
        }
    }

    @Override
    public boolean isMimeTypeSupported(String mimeType)
    {
        return isConversionSupported(mimeType, "text/html");
    }

    /**
     * Use this method to check if the unidirectional conversion from a document format (input media type) to another
     * document format (output media type) is supported by this converter.
     * 
     * @param inputMediaType the media type of the input document
     * @param outputMediaType the media type of the output document
     * @return {@code true} if a document can be converted from the input media type to the output media type,
     *         {@code false} otherwise
     */
    private boolean isConversionSupported(String inputMediaType, String outputMediaType)
    {
        OfficeConverter converter = this.officeServer.getConverter();
        if (converter != null) {
            DocumentFormat inputFormat = converter.getFormatRegistry().getFormatByMediaType(inputMediaType);
            DocumentFormat outputFormat = converter.getFormatRegistry().getFormatByMediaType(outputMediaType);
            return inputFormat != null && outputFormat != null
                && outputFormat.getStoreProperties(inputFormat.getInputFamily()) != null;
        } else {
            return false;
        }
    }

    /**
     * Renders the given XDOM into specified syntax.
     * 
     * @param xdom the {@link XDOM} to be rendered
     * @param fromSyntax the syntax for which to perform the transformations
     * @param toSyntax expected output syntax
     * @return string holding the result of rendering
     * @throws Exception if an error occurs during rendering
     */
    private String render(XDOM xdom, Syntax fromSyntax, Syntax toSyntax) throws Exception
    {
        // Perform the transformations. This is required for office presentations which use the gallery macro to display
        // the slide images.
        TransformationContext context = new TransformationContext(xdom, fromSyntax);
        context.setTargetSyntax(toSyntax);
        this.transformationManager.performTransformations(xdom, context);

        WikiPrinter printer = new DefaultWikiPrinter();
        BlockRenderer renderer = this.componentManager.getInstance(BlockRenderer.class, toSyntax.toIdString());
        renderer.render(xdom, printer);
        return printer.toString();
    }
}
