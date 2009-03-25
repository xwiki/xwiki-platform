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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.renderer.LinkLabelGenerator;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Used to filter plain text blocks.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class PlainTextBlockFilter implements BlockFilter
{
    /**
     * The set of valid Block classes as toc item content.
     */
    private static final Set<Class< ? extends Block>> VALID_PLAINTEXT_BLOCKS =
        new HashSet<Class< ? extends Block>>(Arrays.<Class< ? extends Block>> asList(WordBlock.class, SpaceBlock.class,
            SpecialSymbolBlock.class));

    /**
     * Used to generate link label from link reference.
     */
    private ParserUtils parseUtils = new ParserUtils();

    /**
     * Generate link label.
     */
    private LinkLabelGenerator linkLabelGenerator;

    /**
     * @param linkLabelGenerator generate link label.
     */
    public PlainTextBlockFilter(LinkLabelGenerator linkLabelGenerator)
    {
        this.linkLabelGenerator = linkLabelGenerator;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.BlockFilter#filter(org.xwiki.rendering.block.Block)
     */
    public List<Block> filter(Block block)
    {
        if (VALID_PLAINTEXT_BLOCKS.contains(block.getClass())) {
            return Collections.singletonList(block);
        } else if (block.getClass() == LinkBlock.class && ((LinkBlock) block).getChildren().size() == 0) {
            Link link = ((LinkBlock) block).getLink();
            if (link.getType() == LinkType.DOCUMENT) {
                return this.parseUtils.parseInlineNonWiki(this.linkLabelGenerator.generate(link));
            } else {
                return this.parseUtils.parseInlineNonWiki(link.getReference());
            }
        } else {
            return Collections.emptyList();
        }
    }
}
