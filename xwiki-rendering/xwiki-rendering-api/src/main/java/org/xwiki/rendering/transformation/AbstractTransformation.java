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

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Code common to all Transformation and base implementation of priorities (see {@link Transformation#getPriority()}).
 * 
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractTransformation extends AbstractLogEnabled implements Transformation
{
    /**
     * Execution order priority. Transformations with smaller values are executed sooner. Injected by the Component
     * Manager.
     */
    private int priority = 1000;

    /**
     * {@inheritDoc}
     * 
     * @see Transformation#getPriority()
     */
    public int getPriority()
    {
        return this.priority;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Transformation#compareTo(Object)
     */
    public int compareTo(Transformation transformation)
    {
        return getPriority() - transformation.getPriority();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.transformation.Transformation#transform(org.xwiki.rendering.block.XDOM ,
     *      org.xwiki.rendering.syntax.Syntax)
     */
    public void transform(XDOM dom, Syntax syntax) throws TransformationException
    {
        transform(dom, new TransformationContext(dom, syntax));
    }
}
