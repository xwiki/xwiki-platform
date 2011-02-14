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
package org.xwiki.rendering.transformation;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Performs a transformation on a XDOM (i.e. a tree of {@link org.xwiki.rendering.block.Block}. This used for example
 * for transforming Macro Blocks into other Blocks corresponding to the execution of the Macros. Another example of
 * transformation would be looking for all words that have an entry on Wikipedia and adding links to them.
 * 
 * @version $Id$
 * @since 1.5M2
 */
@ComponentRole
public interface Transformation extends Comparable<Transformation>
{
    /**
     * The priority of execution relative to the other transformations. The lowest values have the highest priorities
     * and execute first. For example a Transformation with a priority of 100 will execute before one with a priority of
     * 500.
     * 
     * @return the execution priority
     */
    int getPriority();

    /**
     * Transform the passed XDOM and modifies it.
     * 
     * @param dom the AST representing the content in Blocks
     * @param syntax the Syntax of the content
     * @throws TransformationException if the transformation fails for any reason
     * @deprecated since 2.4M1 use {@link #transform(Block, TransformationContext)} instead
     */
    @Deprecated
    void transform(XDOM dom, Syntax syntax) throws TransformationException;

    /**
     * Transform the passed XDOM and modifies it.
     * 
     * @param block the block to transform (can be an {@link XDOM})
     * @param context the context of the transformation process (syntax, transformation id, etc)
     * @throws TransformationException if the transformation fails for any reason
     * @since 2.4M1
     */
    void transform(Block block, TransformationContext context) throws TransformationException;
}
