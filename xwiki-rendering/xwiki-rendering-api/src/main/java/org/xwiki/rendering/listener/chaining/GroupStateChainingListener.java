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
package org.xwiki.rendering.listener.chaining;

import java.util.Map;

/**
 * Provides information on whether we're inside a group. Note that this listener is separated from the
 * {@link org.xwiki.rendering.listener.chaining.BlockStateChainingListener} class because we don't want this listener to
 * be stackable (since we need to create new instance of stackable listeners to reset states when we encounter
 * a begin group event but we also need to know we're inside a group).
 * 
 * @version $Id$
 * @since 1.8.3
 */
public class GroupStateChainingListener extends AbstractChainingListener
{
    private int groupDepth = 0;

    public GroupStateChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain);
    }

    public int getDocumentDepth()
    {
        return this.groupDepth;
    }

    public boolean isInGroup()
    {
        return this.groupDepth > 0;
    }

    // Events

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginGroup(Map)
     */
    @Override
    public void beginGroup(Map<String, String> parameters)
    {
        ++this.groupDepth;

        super.beginGroup(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endGroup(Map)
     */
    @Override
    public void endGroup(Map<String, String> parameters)
    {
        super.endGroup(parameters);

        --this.groupDepth;
    }
}
