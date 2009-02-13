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
package org.xwiki.rendering.renderer;

import org.xwiki.rendering.internal.renderer.XWikiSyntaxImageRenderer;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.ConsecutiveNewLineStateChainingListener;
import org.xwiki.rendering.listener.chaining.DocumentStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.LookaheadChainingListener;
import org.xwiki.rendering.listener.chaining.TextOnNewLineStateChainingListener;

/**
 * Provides convenient access to listeners in the chain used for the {@link XWikiSyntaxImageRenderer}.
 *  
 * @version $Id$
 * @since 1.8RC1
 */
public class XWikiSyntaxListenerChain extends ListenerChain
{
    public LookaheadChainingListener getLookaheadChainingListener()
    {
        return (LookaheadChainingListener) getListener(LookaheadChainingListener.class);
    }

    public BlockStateChainingListener getBlockStateChainingListener()
    {
        return (BlockStateChainingListener) getListener(BlockStateChainingListener.class);
    }

    public ConsecutiveNewLineStateChainingListener getConsecutiveNewLineStateChainingListener()
    {
        return (ConsecutiveNewLineStateChainingListener) getListener(ConsecutiveNewLineStateChainingListener.class);
    }

    public TextOnNewLineStateChainingListener getTextOnNewLineStateChainingListener()
    {
        return (TextOnNewLineStateChainingListener) getListener(TextOnNewLineStateChainingListener.class);
    }
    
    public DocumentStateChainingListener getDocumentStateChainingListener()
    {
        return (DocumentStateChainingListener) getListener(DocumentStateChainingListener.class);
    }
}
