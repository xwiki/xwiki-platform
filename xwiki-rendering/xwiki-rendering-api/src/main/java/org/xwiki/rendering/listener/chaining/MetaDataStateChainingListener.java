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

import java.util.ListIterator;
import java.util.Stack;

import org.xwiki.rendering.listener.MetaData;

/**
 * Provides the accumulated MetaData for all the previous blocks.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class MetaDataStateChainingListener extends AbstractChainingListener
{
    /**
     * @see #getMetaData(String)
     */
    private Stack<MetaData> metaDataStack = new Stack<MetaData>();

    /**
     * @param listenerChain see {@link #getListenerChain()}
     */
    public MetaDataStateChainingListener(ListenerChain listenerChain)
    {
        setListenerChain(listenerChain);
    }

    /**
     * @param key the key for which to find the value
     * @return the accumulated MetaData during all the previous begin/endMetaData events, for the passed key
     */
    public Object getMetaData(String key)
    {
        Object result = null;
        if (!this.metaDataStack.isEmpty()) {
            ListIterator<MetaData> it = this.metaDataStack.listIterator(this.metaDataStack.size());
            while (it.hasPrevious()) {
                MetaData metaData = it.previous();
                result = metaData.getMetaData(key);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beginMetaData(MetaData metadata)
    {
        this.metaDataStack.push(metadata);
        super.beginMetaData(metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endMetaData(MetaData metadata)
    {
        super.endMetaData(metadata);
        this.metaDataStack.pop();
    }
}
