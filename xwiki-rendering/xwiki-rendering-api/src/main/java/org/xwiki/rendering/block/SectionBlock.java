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
import java.util.Map;

import org.xwiki.rendering.listener.Listener;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class SectionBlock extends AbstractFatherBlock
{
    /**
     * @param childBlocks the children of the section
     */
    public SectionBlock(List<Block> childBlocks)
    {
        super(childBlocks);
    }

    /**
     * @param childBlocks the children of the section
     * @param parameters the parameters of the section
     */
    public SectionBlock(List<Block> childBlocks, Map<String, String> parameters)
    {
        super(childBlocks, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.FatherBlock#before(org.xwiki.rendering.listener.Listener)
     */
    public void before(Listener listener)
    {
        listener.beginSection(getParameters());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.FatherBlock#after(org.xwiki.rendering.listener.Listener)
     */
    public void after(Listener listener)
    {
        listener.endSection(getParameters());
    }

    /**
     * @return the title block of the section.
     */
    public HeaderBlock getHeaderBlock()
    {
        HeaderBlock headerBlock = null;

        List<Block> children = getChildren();

        if (children.size() > 0) {
            Block firstChild = children.get(0);
            if (firstChild instanceof HeaderBlock) {
                headerBlock = (HeaderBlock) firstChild;
            }
        }

        return headerBlock;
    }
}
