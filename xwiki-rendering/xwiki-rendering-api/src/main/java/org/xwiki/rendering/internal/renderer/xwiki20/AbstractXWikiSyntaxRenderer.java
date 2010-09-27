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
package org.xwiki.rendering.internal.renderer.xwiki20;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.ChainingListener;
import org.xwiki.rendering.listener.chaining.ConsecutiveNewLineStateChainingListener;
import org.xwiki.rendering.listener.chaining.GroupStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.LookaheadChainingListener;
import org.xwiki.rendering.renderer.AbstractChainingPrintRenderer;

/**
 * XWiki Syntax Renderer implementation common to XWiki Syntax versions greater than 2.0 (X>iki Syntax 2.0,
 * XWiki Syntax 2.1, etc).
 *  
 * @version $Id$
 * @since 2.5M2
 */
public abstract class AbstractXWikiSyntaxRenderer extends AbstractChainingPrintRenderer implements Initializable
{
    /**
     * Allows extending classes to choose which implementation to use.
     * 
     * @param chain the rendering chain, see {@link org.xwiki.rendering.listener.chaining.ListenerChain}
     * @return the XWiki Syntax renderer containing the implementation to use for handling the listener's events
     */
    protected abstract ChainingListener createXWikiSyntaxChainingRenderer(ListenerChain chain);

    /**
     * {@inheritDoc}
     *
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        ListenerChain chain = new XWikiSyntaxListenerChain();
        setListenerChain(chain);

        // Construct the listener chain in the right order. Listeners early in the chain are called before listeners
        // placed later in the chain. This chain allows using several listeners that make it easier
        // to write the XWiki Syntax chaining listener, for example for saving states (are we in a list, in a
        // paragraph, are we starting a new line, etc).
        chain.addListener(this);
        chain.addListener(new LookaheadChainingListener(chain, 2));
        chain.addListener(new GroupStateChainingListener(chain));
        chain.addListener(new BlockStateChainingListener(chain));
        chain.addListener(new ConsecutiveNewLineStateChainingListener(chain));
        chain.addListener(createXWikiSyntaxChainingRenderer(chain));
    }
}
