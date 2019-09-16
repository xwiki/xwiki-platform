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
package org.xwiki.rendering.async.internal.block;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Helper to execute Block based asynchronous renderer.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractBlockAsyncRenderer implements BlockAsyncRenderer
{
    @Inject
    @Named("context")
    protected Provider<ComponentManager> componentManagerProvider;

    @Inject
    protected TransformationManager transformationManager;

    @Inject
    @Named("macro")
    protected Transformation macroTransformation;

    @Inject
    protected RenderingContext renderingContext;

    protected void transform(Block block, TransformationContext transformationContext) throws TransformationException
    {
        if (isAsyncAllowed() || isCacheAllowed()) {
            this.transformationManager.performTransformations(block, transformationContext);
        } else {
            ((MutableRenderingContext) this.renderingContext).transformInContext(this.macroTransformation,
                transformationContext, block);
        }
    }

    /**
     * Removes any top level paragraph since for example for the following use case we don't want an extra paragraph
     * block: <code>= hello {{velocity}}world{{/velocity}}</code>.
     *
     * @param block the blocks to check and convert
     */
    protected Block removeTopLevelParagraph(Block block)
    {
        List<Block> blocks = block.getChildren();

        // Remove any top level paragraph so that the result of a macro can be used inline for example.
        // We only remove the paragraph if there's only one top level element and if it's a paragraph.
        if ((block.getChildren().size() == 1) && block.getChildren().get(0) instanceof ParagraphBlock) {
            Block paragraphBlock = blocks.remove(0);
            blocks.addAll(0, paragraphBlock.getChildren());

            return new CompositeBlock(blocks);
        }

        return block;
    }

    /**
     * @param block the block to render
     * @return the result of the block rendering
     * @throws RenderingException when failing to renderer the block
     * @since 11.8RC1
     */
    protected String render(Block block) throws RenderingException
    {
        BlockRenderer renderer;
        try {
            renderer =
                this.componentManagerProvider.get().getInstance(BlockRenderer.class, getTargetSyntax().toIdString());
        } catch (ComponentLookupException e) {
            throw new RenderingException("Failed to lookup renderer for syntax [" + getTargetSyntax() + "]", e);
        }

        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(block, printer);

        return printer.toString();
    }
}
