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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentRole;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;

/**
 * Helper to execute Block based asynchronous renderer.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component(roles = DefaultBlockAsyncRenderer.class)
public class DefaultBlockAsyncRenderer extends AbstractBlockAsyncRenderer
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    private AsyncContext asyncContext;

    private BlockAsyncRendererConfiguration configuration;

    /**
     * @param configuration the configuration of the renderer
     */
    public void initialize(BlockAsyncRendererConfiguration configuration)
    {
        this.configuration = configuration;
    }

    @Override
    public List<String> getId()
    {
        return this.configuration.getId();
    }

    @Override
    public boolean isAsyncAllowed()
    {
        return this.configuration.isAsyncAllowed();
    }

    @Override
    public boolean isCacheAllowed()
    {
        return this.configuration.isCacheAllowed();
    }

    @Override
    public boolean isInline()
    {
        return this.configuration.isInline();
    }

    @Override
    public BlockAsyncRendererResult render(boolean async, boolean cached) throws RenderingException
    {
        // Register the known involved references and components
        for (EntityReference reference : this.configuration.getReferences()) {
            this.asyncContext.useEntity(reference);
        }
        for (ComponentRole<?> role : this.configuration.getRoles()) {
            this.asyncContext.useComponent(role.getRoleType(), role.getRoleHint());
        }

        Block block = this.configuration.getBlock();
        XDOM xdom;
        if (block instanceof XDOM) {
            xdom = (XDOM) block;
        } else {
            Block rootBlock = block.getRoot();

            if (rootBlock instanceof XDOM) {
                xdom = (XDOM) rootBlock;
            } else {
                xdom = new XDOM(Collections.singletonList(rootBlock));
            }
        }

        ///////////////////////////////////////
        // Transformations

        Block resultBlock = tranform(xdom, block);

        ///////////////////////////////////////
        // Rendering

        String resultString = null;

        if (async || cached) {
            Syntax targetSyntax = this.configuration.getTargetSyntax();
            BlockRenderer renderer;
            try {
                renderer = this.componentManager.get().getInstance(BlockRenderer.class, targetSyntax.toIdString());
            } catch (ComponentLookupException e) {
                throw new RenderingException("Failed to lookup renderer for syntax [" + targetSyntax + "]", e);
            }

            WikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(resultBlock, printer);

            resultString = printer.toString();
        }

        return new BlockAsyncRendererResult(resultString, resultBlock);
    }

    private Block tranform(XDOM xdom, Block block) throws RenderingException
    {
        TransformationContext transformationContext =
            new TransformationContext(xdom, this.configuration.getDefaultSyntax(), false);
        transformationContext.setTargetSyntax(this.configuration.getTargetSyntax());
        transformationContext.setId(this.configuration.getTransformationId());

        if (this.configuration.isAuthorReferenceSet()) {
            transform(block, transformationContext, this.configuration.getAuthorReference(),
                this.configuration.getSourceReference());
        } else {
            transform(block, transformationContext);
        }

        // The result is often inserted in a bigger content so we remove the XDOM around it
        if (block instanceof XDOM) {
            return new MetaDataBlock(block.getChildren(), ((XDOM) block).getMetaData());
        }

        return block;
    }
}
