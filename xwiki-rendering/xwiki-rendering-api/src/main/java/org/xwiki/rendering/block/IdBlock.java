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
package org.xwiki.rendering.block;

import org.xwiki.rendering.listener.Listener;

/**
 * A reference/location in a page. In HTML for example this is called an Anchor. It allows pointing
 * to that location, for example in links.
 *
 * @version $Id$
 * @since 1.6M1
 * @see Listener#onId(String) 
 */
public class IdBlock extends AbstractBlock
{
    /**
     * The unique name for the reference/location.
     */
    private String name;

    /**
     * @param name the unique name for the reference/location.
     */
    public IdBlock(String name)
    {
        this.name = name;
    }

    /**
     * @return the reference/location name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractBlock#traverse(org.xwiki.rendering.listener.Listener)
     */
    public void traverse(Listener listener)
    {
        listener.onId(getName());
    }
}
