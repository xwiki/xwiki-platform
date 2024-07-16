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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;

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
     * The component used to perform the rendering transformations on the title extracted from the document content.
     */
    @Inject
    private TransformationManager transformationManager;

    /**
     * The display configuration.
     */
    @Inject
    private DisplayConfiguration displayConfiguration;

    @Override
    protected XDOM extractTitleFromContent(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        // Note: Ideally we should apply transformations on the document's returned XDOM here since macros could
        // generate headings for example or some other transformations could modify headings. However we don't do this
        // at the moment since it would be too costly to do so. In the future we will even probably remove the feature
        // of generating the title from the content.
        List<HeaderBlock> blocks =
            document.getPreparedXDOM().getBlocks(new ClassBlockMatcher(HeaderBlock.class), Block.Axes.DESCENDANT);
        if (!blocks.isEmpty()) {
            HeaderBlock heading = blocks.get(0);
            // Check the heading depth after which we should return null if no heading was found.
            if (heading.getLevel().getAsInt() <= displayConfiguration.getTitleHeadingDepth()) {
                XDOM headingXDOM = new XDOM(Collections.<Block> singletonList(heading));
                try {
                    TransformationContext txContext =
                        new TransformationContext(headingXDOM, document.getSyntax(),
                            parameters.isTransformationContextRestricted() || document.isRestricted());
                    txContext.setTargetSyntax(parameters.getTargetSyntax());
                    transformationManager.performTransformations(headingXDOM, txContext);

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
}
