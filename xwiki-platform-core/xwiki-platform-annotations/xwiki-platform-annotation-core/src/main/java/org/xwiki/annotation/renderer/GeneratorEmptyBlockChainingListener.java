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
package org.xwiki.annotation.renderer;

import org.xwiki.rendering.listener.chaining.EmptyBlockChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;

/**
 * Empty block chaining listener to push in the chain before the generator listener. It adds no special functionality to
 * the {@link EmptyBlockChainingListener}, it's just a new type so that we can add two such listeners, for different
 * purposes at different positions in the chain.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class GeneratorEmptyBlockChainingListener extends EmptyBlockChainingListener
{
    /**
     * Builds a new empty block chaining listener to push in the passed chain.
     *
     * @param listenerChain the chain to push the listener in
     */
    public GeneratorEmptyBlockChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain);
    }
}
