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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.security.authorization.AuthorExecutor;

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
    protected TransformationManager transformationManager;

    @Inject
    @Named("macro")
    protected Transformation macroTransformation;

    @Inject
    protected AuthorExecutor authorExecutor;

    @Inject
    protected RenderingContext renderingContext;

    protected void transform(Block block, TransformationContext transformationContext, DocumentReference author)
        throws TransformationException
    {
        try {
            this.authorExecutor.call(() -> {
                transform(block, transformationContext);

                return null;
            }, author);
        } catch (Exception e) {
            throw new TransformationException("Failed to execute transformations", e);
        }
    }

    protected void transform(Block block, TransformationContext transformationContext) throws TransformationException
    {
        if (isAsyncAllowed() || isCacheAllowed()) {
            this.transformationManager.performTransformations(block, transformationContext);
        } else {
            ((MutableRenderingContext) this.renderingContext).transformInContext(this.macroTransformation,
                transformationContext, block);
        }
    }
}
