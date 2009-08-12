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
import java.io.StringReader;

import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.renderer.LinkLabelGenerator;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.ParseException;

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
     * A parser that knows how to parse plain text; this is used to transform link labels into plain text.
     */
    private Parser plainTextParser;

    /**
     * Generate link label.
     */
    private LinkLabelGenerator linkLabelGenerator;

    /**
     * @param plainTextParser a plain text parser used to transform link labels into plain text
     * @param linkLabelGenerator generate link label.
     * @since 2.0M3
     */
    public PlainTextBlockFilter(Parser plainTextParser, LinkLabelGenerator linkLabelGenerator)
    {
        this.plainTextParser = plainTextParser;
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
        } else if (block.getClass() == LinkBlock.class && block.getChildren().size() == 0) {
            Link link = ((LinkBlock) block).getLink();

            try {
                if (link.getType() == LinkType.DOCUMENT) {
                    return this.plainTextParser.parse(
                        new StringReader(this.linkLabelGenerator.generate(link))).getChildren();
                } else {
                    return this.plainTextParser.parse(new StringReader(link.getReference())).getChildren();
                }
            } catch (ParseException e) {
                // This shouldn't happen since the parser cannot throw an exception since the source is a memory
                // String.
                throw new RuntimeException("Failed to parse link label as plain text", e);
            }
        } else {
            return Collections.emptyList();
        }
    }
}
