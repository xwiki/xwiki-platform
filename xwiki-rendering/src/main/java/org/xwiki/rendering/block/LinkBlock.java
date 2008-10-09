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

import java.util.List;

import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.Link;

/**
 * Represents a Link element in a page.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class LinkBlock extends AbstractFatherBlock
{
    /**
     * A link. See {@link Link} for more details.
     */
    private Link link;

    /**
     * If true then the link is a free standing URI directly in the text.
     */
    private boolean isFreeStandingURI;

    /**
     * @param childrenBlocks the nested children blocks
     * @param link the link
     */
    public LinkBlock(List<Block> childrenBlocks, Link link)
    {
        this(childrenBlocks, link, false);
    }

    /**
     * @param childrenBlocks the nested children blocks
     * @param link the link
     * @param isFreeStandingURI if true then the link is a free standing URI directly in the text
     */
    public LinkBlock(List<Block> childrenBlocks, Link link, boolean isFreeStandingURI)
    {
        super(childrenBlocks);
        this.link = link;
        this.isFreeStandingURI = isFreeStandingURI;
    }
    
    /**
     * @return the link
     * @see Link
     */
    public Link getLink()
    {
        return this.link;
    }

    /**
     * @return true if the link is a free standing URI directly in the text, false otherwise
     */
    public boolean isFreeStandingURI()
    {
        return this.isFreeStandingURI;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractFatherBlock#before(org.xwiki.rendering.listener.Listener)
     */
    public void before(Listener listener)
    {
        listener.beginLink(getLink(), isFreeStandingURI());
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractFatherBlock#after(org.xwiki.rendering.listener.Listener)  
     */
    public void after(Listener listener)
    {
        listener.endLink(getLink(), isFreeStandingURI());
    }
}
