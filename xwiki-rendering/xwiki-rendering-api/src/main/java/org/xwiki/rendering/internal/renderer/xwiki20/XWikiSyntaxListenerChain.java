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

import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.ConsecutiveNewLineStateChainingListener;
import org.xwiki.rendering.listener.chaining.GroupStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.LookaheadChainingListener;
import org.xwiki.rendering.listener.chaining.TextOnNewLineStateChainingListener;

/**
 * Provides convenient access to listeners in the chain used for
 * {@link XWikiSyntaxListenerChain}.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class XWikiSyntaxListenerChain extends ListenerChain
{
    /**
     * @return the stateful {@link LookaheadChainingListener} for this rendering session.
     */
    public LookaheadChainingListener getLookaheadChainingListener()
    {
        return (LookaheadChainingListener) getListener(LookaheadChainingListener.class);
    }

    /**
     * @return the stateful {@link BlockStateChainingListener} for this rendering session.
     */
    public BlockStateChainingListener getBlockStateChainingListener()
    {
        return (BlockStateChainingListener) getListener(BlockStateChainingListener.class);
    }

    /**
     * @return the stateful {@link ConsecutiveNewLineStateChainingListener} for this rendering session.
     */
    public ConsecutiveNewLineStateChainingListener getConsecutiveNewLineStateChainingListener()
    {
        return (ConsecutiveNewLineStateChainingListener) getListener(ConsecutiveNewLineStateChainingListener.class);
    }

    /**
     * @return the stateful {@link TextOnNewLineStateChainingListener} for this rendering session.
     */
    public TextOnNewLineStateChainingListener getTextOnNewLineStateChainingListener()
    {
        return (TextOnNewLineStateChainingListener) getListener(TextOnNewLineStateChainingListener.class);
    }

    /**
     * @return the stateful {@link GroupStateChainingListener} for this rendering session.
     */
    public GroupStateChainingListener getGroupStateChainingListener()
    {
        return (GroupStateChainingListener) getListener(GroupStateChainingListener.class);
    }
}
