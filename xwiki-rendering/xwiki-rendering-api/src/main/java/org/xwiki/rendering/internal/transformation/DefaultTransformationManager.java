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
package org.xwiki.rendering.internal.transformation;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Calls all existing transformations (executed by priority) on an existing XDOM object to generate a new transformed
 * XDOM.
 * 
 * @version $Id$
 * @since 1.5M2
 */
@Component
public class DefaultTransformationManager implements TransformationManager
{
    /**
     * Used to get the ordered list of transformations to execute.
     */
    @Requirement
    private RenderingConfiguration configuration;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.transformation.TransformationManager#performTransformations(org.xwiki.rendering.block.XDOM,
     *      org.xwiki.rendering.syntax.Syntax)
     */
    public void performTransformations(XDOM dom, Syntax syntax) throws TransformationException
    {
        performTransformations(dom, new TransformationContext(dom, syntax));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.transformation.TransformationManager#performTransformations(org.xwiki.rendering.block.Block,
     *      org.xwiki.rendering.transformation.TransformationContext)
     */
    public void performTransformations(Block block, TransformationContext context) throws TransformationException
    {
        for (Transformation transformation : this.configuration.getTransformations()) {
            transformation.transform(block, context);
        }
    }
}
