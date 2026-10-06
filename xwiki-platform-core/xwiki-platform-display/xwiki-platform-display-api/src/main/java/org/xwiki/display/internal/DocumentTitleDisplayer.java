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
package org.xwiki.display.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.util.ErrorBlockGenerator;

/**
 * Displays the title of a document. If the title is not specified, extracts the document title from the first heading
 * in the document content that has the level less than or equal to {@link DisplayConfiguration#getTitleHeadingDepth()}.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("title")
@Singleton
public class DocumentTitleDisplayer extends AbstractDocumentTitleDisplayer
{
    /**
     * The key under which the document whose content author is used to check the script and programming rights is
     * stored in the XWiki context.
     */
    private static final String SECURE_DOCUMENT = "sdoc";

    /**
     * The component used to perform the rendering transformations on the title extracted from the document content.
     */
    @Inject
    private TransformationManager transformationManager;

    /**
     * The display configuration.
     */
    @Inject
    private DisplayConfiguration displayConfiguration;

    @Inject
    private Execution execution;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ErrorBlockGenerator errorBlockGenerator;

    @Override
    protected XDOM extractTitleFromContent(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        // Note: Ideally we should apply transformations on the document's returned XDOM here since macros could
        // generate headings for example or some other transformations could modify headings. However we don't do this
        // at the moment since it would be too costly to do so. In the future we will even probably remove the feature
        // of generating the title from the content.
        XDOM contentXDOM = document.getPreparedXDOM();
        List<HeaderBlock> blocks =
            contentXDOM.getBlocks(new ClassBlockMatcher(HeaderBlock.class), Block.Axes.DESCENDANT);
        if (!blocks.isEmpty()) {
            HeaderBlock heading = blocks.get(0);
            // Check the heading depth after which we should return null if no heading was found.
            if (heading.getLevel().getAsInt() <= this.displayConfiguration.getTitleHeadingDepth()) {
                // Keep the meta data of the content, in particular the source, so that the macros of the heading are
                // executed as if they were executed as part of the content they are taken from.
                XDOM headingXDOM = new XDOM(List.of(heading), contentXDOM.getMetaData());
                try {
                    TransformationContext txContext =
                        new TransformationContext(headingXDOM, document.getSyntax(),
                            parameters.isTransformationContextRestricted() || document.isRestricted());
                    txContext.setTargetSyntax(parameters.getTargetSyntax());
                    txContext.setId(this.entityReferenceSerializer.serialize(document.getDocumentReference()));
                    performTransformations(document, headingXDOM, txContext);

                    // Don't use a rendering error as the title of the document. The error is already displayed in the
                    // content of the document, and plain text contexts like the browser tab title, the breadcrumb or
                    // the navigation tree would display the whole error description. Fall back to the document name
                    // instead.
                    if (this.errorBlockGenerator.containsError(headingXDOM)) {
                        return null;
                    }

                    Block headingBlock = headingXDOM.getChildren().size() > 0 ? headingXDOM.getChildren().get(0) : null;
                    if (headingBlock instanceof HeaderBlock) {
                        return new XDOM(headingBlock.getChildren());
                    }
                } catch (TransformationException e) {
                    getLogger().warn("Failed to extract title from document content.");
                }
            }
        }
        return null;
    }

    /**
     * Execute the transformations on the heading in the context of the document the heading has been extracted from,
     * i.e., with that document as secure document, so that the macros of the heading are executed the same way as
     * when they are executed as part of the content of that document.
     *
     * @param document the document the heading is extracted from
     * @param headingXDOM the heading to transform
     * @param txContext the transformation context to use
     * @throws TransformationException when the transformations failed
     */
    private void performTransformations(DocumentModelBridge document, XDOM headingXDOM,
        TransformationContext txContext) throws TransformationException
    {
        Map<Object, Object> xwikiContext = getXWikiContextMap();

        if (xwikiContext == null) {
            this.transformationManager.performTransformations(headingXDOM, txContext);
            return;
        }

        Object previousSecureDocument = xwikiContext.put(SECURE_DOCUMENT, document);
        try {
            this.transformationManager.performTransformations(headingXDOM, txContext);
        } finally {
            if (previousSecureDocument != null) {
                xwikiContext.put(SECURE_DOCUMENT, previousSecureDocument);
            } else {
                xwikiContext.remove(SECURE_DOCUMENT);
            }
        }
    }

    /**
     * @return the XWiki context map, {@code null} when there is no XWiki context
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> getXWikiContextMap()
    {
        ExecutionContext executionContext = this.execution.getContext();

        return executionContext != null ? (Map<Object, Object>) executionContext.getProperty("xwikicontext") : null;
    }
}
