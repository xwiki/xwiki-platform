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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentRole;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.util.ErrorBlockGenerator;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Helper to execute Block based asynchronous renderer.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component(roles = DefaultBlockAsyncRenderer.class)
public class DefaultBlockAsyncRenderer extends AbstractBlockAsyncRenderer
{
    private static final String TM_FAILEDASYNC = "rendering.async.error.failed";

    private static final ParserUtils PARSERUTILS = new ParserUtils();

    @Inject
    private AsyncContext asyncContext;

    @Inject
    private ErrorBlockGenerator errorBlockGenerator;

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
    public Syntax getTargetSyntax()
    {
        return this.configuration.getTargetSyntax();
    }

    @Override
    public Block execute(boolean async, boolean cached) throws RenderingException
    {
        Block resultBlock;

        try {
            // Register the known involved references and components
            for (EntityReference reference : this.configuration.getReferences()) {
                this.asyncContext.useEntity(reference);
            }
            for (ComponentRole<?> role : this.configuration.getRoles()) {
                this.asyncContext.useComponent(role.getRoleType(), role.getRoleHint());
            }

            // Get the block to transform
            Block block = this.configuration.getBlock();

            // Make sure the source is inline if needed
            if (this.configuration.isInline()) {
                block = PARSERUTILS.convertToInline(block, true);
            }

            // Get the XDOM instance for the transformation context
            XDOM xdom = this.configuration.getXDOM();
            if (xdom == null) {
                Block rootBlock = block.getRoot();

                if (rootBlock instanceof XDOM) {
                    xdom = (XDOM) rootBlock;
                } else {
                    xdom = new XDOM(Collections.singletonList(rootBlock));
                }
            }

            ///////////////////////////////////////
            // Transformations

            resultBlock = tranform(xdom, block);
        } catch (Exception e) {
            // Display the error in the result
            resultBlock = new CompositeBlock(this.errorBlockGenerator.generateErrorBlocks(this.configuration.isInline(),
                TM_FAILEDASYNC, "Failed to execute asynchronous content", null, e));
        }

        return resultBlock;
    }

    private Block tranform(XDOM xdom, Block block) throws RenderingException
    {
        TransformationContext transformationContext =
            new TransformationContext(xdom, this.configuration.getDefaultSyntax(), this.configuration.isResricted());
        transformationContext.setTargetSyntax(this.configuration.getTargetSyntax());
        transformationContext.setId(this.configuration.getTransformationId());

        transform(block, transformationContext);

        // The result is often inserted in a bigger content so we remove the XDOM around it
        if (block instanceof XDOM) {
            return new MetaDataBlock(block.getChildren(), ((XDOM) block).getMetaData());
        }

        return block;
    }
}
