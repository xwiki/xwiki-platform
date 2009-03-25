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
package org.xwiki.rendering.internal.macro.toc;

import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.PlainTextBlockFilter;
import org.xwiki.rendering.renderer.LinkLabelGenerator;

/**
 * Used to filter the {@link HeaderBlock} title to generate the toc anchor.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
public class TocBlockFilter extends PlainTextBlockFilter
{
    /**
     * @param linkLabelGenerator generate link label.
     */
    public TocBlockFilter(LinkLabelGenerator linkLabelGenerator)
    {
        super(linkLabelGenerator);
    }

    /**
     * @param headerBlock the section title.
     * @return the filtered label to use in toc anchor link.
     */
    public List<Block> generateLabel(HeaderBlock headerBlock)
    {
        return headerBlock.clone(this).getChildren();
    }
}
