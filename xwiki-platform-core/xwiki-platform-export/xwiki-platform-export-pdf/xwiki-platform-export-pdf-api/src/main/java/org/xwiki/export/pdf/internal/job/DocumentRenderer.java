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
package org.xwiki.export.pdf.internal.job;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.export.pdf.job.PDFExportJobStatus.DocumentRenderingResult;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;

/**
 * Component used to render documents.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component(roles = DocumentRenderer.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentRenderer
{
    /**
     * The parameter used to mark header blocks that correspond to document titles. In other words, this marks the
     * beginning of a new document when multiple documents are exported.
     */
    public static final String PARAMETER_DOCUMENT_REFERENCE = "data-xwiki-document-reference";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    @Named("configured")
    private DocumentDisplayer documentDisplayer;

    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Used to generate unique identifiers across multiple rendered documents.
     */
    private PDFExportIdGenerator idGenerator = new PDFExportIdGenerator();

    /**
     * Renders the specified document.
     * 
     * @param documentReference the document to render
     * @param withTitle {@code true} to render also the document title (before the document content), {@code false}
     *            otherwise
     * @return the rendering result
     * @throws Exception if rendering the specified document fails
     */
    public DocumentRenderingResult render(DocumentReference documentReference, boolean withTitle) throws Exception
    {
        Syntax targetSyntax = this.renderingContext.getTargetSyntax();

        DocumentDisplayerParameters parameters = new DocumentDisplayerParameters();
        // Each document is rendered in a separate execution context, as if it was the current document on the context,
        // otherwise relative references used in the document content are badly resolved.
        parameters.setExecutionContextIsolated(true);
        parameters.setTransformationContextIsolated(true);
        parameters.setTransformationContextRestricted(false);
        parameters.setContentTranslated(false);
        parameters.setTargetSyntax(targetSyntax);
        // Use the same id generator while rendering all the documents included in a PDF export in order to ensure that
        // the generated identifiers are unique across the aggregated content (as if all the exported documents are
        // included in the same parent document).
        parameters.setIdGenerator(this.idGenerator);

        XDOM xdom = display(getDocument(documentReference), parameters, withTitle);
        String html = renderXDOM(xdom, targetSyntax);
        return new DocumentRenderingResult(documentReference, xdom, html, this.idGenerator.resetLocalIds());
    }

    private DocumentModelBridge getDocument(DocumentReference documentReference) throws Exception
    {
        // If the current document is included in the PDF export then we use the document instance from the XWiki
        // context in order to match what the user sees in view mode (e.g. the user might be looking at a document
        // revision).
        DocumentModelBridge currentDocument = this.documentAccessBridge.getCurrentDocument();
        if (currentDocument != null && documentReference.equals(currentDocument.getDocumentReference())) {
            return currentDocument;
        } else {
            return this.documentAccessBridge.getTranslatedDocumentInstance(documentReference);
        }
    }

    private XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters, boolean withTitle)
    {
        XDOM xdom = this.documentDisplayer.display(document, parameters);

        if (withTitle) {
            parameters.setTitleDisplayed(true);
            XDOM titleXDOM = this.documentDisplayer.display(document, parameters);
            String documentReference = this.entityReferenceSerializer.serialize(document.getDocumentReference());
            // Generate an id so that we can target this heading from the table of contents.
            String id = xdom.getIdGenerator().generateUniqueId("H", documentReference);
            HeaderBlock title = new HeaderBlock(titleXDOM.getChildren(), HeaderLevel.LEVEL1, id);
            // Mark the beginning of a new document when multiple documents are rendered.
            title.setParameter(PARAMETER_DOCUMENT_REFERENCE, documentReference);
            if (xdom.getChildren().isEmpty()) {
                xdom.addChild(title);
            } else {
                xdom.insertChildBefore(title, xdom.getChildren().get(0));
            }
        }

        return xdom;
    }

    private String renderXDOM(XDOM xdom, Syntax targetSyntax) throws Exception
    {
        BlockRenderer renderer =
            this.contextComponentManager.getInstance(BlockRenderer.class, targetSyntax.toIdString());
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(xdom, printer);
        return printer.toString();
    }
}
