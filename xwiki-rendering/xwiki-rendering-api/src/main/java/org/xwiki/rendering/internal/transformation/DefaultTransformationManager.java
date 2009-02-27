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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class DefaultTransformationManager implements TransformationManager, Composable, Initializable
{
    private ComponentManager componentManager;

    /**
     * Holds the list of transformations to apply. Injected by the Component Manager.
     */
    private List<Transformation> transformations = new ArrayList<Transformation>();

    /**
     * {@inheritDoc}
     * 
     * @see Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        List<Transformation> txs;
        try {
            txs = this.componentManager.lookupList(Transformation.ROLE);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to create Transformation cache", e);
        }
        Collections.sort(txs);
        this.transformations = txs;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.transformation.TransformationManager#performTransformations(org.xwiki.rendering.block.XDOM,
     *      org.xwiki.rendering.parser.Syntax)
     */
    public void performTransformations(XDOM dom, Syntax syntax) throws TransformationException
    {
        for (Transformation transformation : this.transformations) {
            transformation.transform(dom, syntax);
        }
    }
}
