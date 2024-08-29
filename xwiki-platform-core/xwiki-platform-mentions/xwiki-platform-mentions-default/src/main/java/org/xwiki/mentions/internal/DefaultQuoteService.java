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
package org.xwiki.mentions.internal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.text.StringUtils;

import static org.xwiki.rendering.block.Block.Axes.DESCENDANT;

/**
 * Default implementation of {@link QuoteService}.
 *
 * @version $Id$
 * @since 12.6
 */
@Component
@Singleton
public class DefaultQuoteService implements QuoteService
{
    @Inject
    @Named("plainmentions/1.0")
    private BlockRenderer renderer;

    @Override
    public Optional<String> extract(XDOM xdom, String anchorId)
    {
        List<Block> blockList = xdom.getBlocks(new ExactAnchorBlockMatcher(anchorId), DESCENDANT);

        Optional<Block> firstBlock;
        // Best effort selection of the mentions. In case of ambiguity we do not generation a quote 
        // for the provided anchorId.
        // Note that if xdom contains a single empty anchor, it is not ambiguous and a quote will
        // be extracted.
        if (blockList.size() == 1) {
            firstBlock = Optional.of(blockList.get(0));
        } else {
            firstBlock = Optional.empty();
        }

        return firstBlock
           .map(new RenderFunction())
           .map(it -> StringUtils.abbreviate(it, "...", 200));
    }

    private static class ExactAnchorBlockMatcher implements BlockMatcher
    {
        private final String anchorId;

        ExactAnchorBlockMatcher(String anchorId)
        {
            this.anchorId = anchorId;
        }

        @Override
        public boolean match(Block block)
        {
            boolean ret;
            if (block instanceof MacroBlock) {
                ret = ((MacroBlock) block).getId().equals("mention")
                    && Objects.equals(Objects.toString(block.getParameter("anchor"), ""), this.anchorId);
            } else {
                ret = false;
            }
            return ret;
        }
    }

    private final class RenderFunction implements Function<Block, String>
    {
        @Override
        public String apply(Block mentionBlock)
        {
            WikiPrinter printer = new DefaultWikiPrinter();
            Block parent = mentionBlock.getParent();
            Block block;
            if (parent instanceof XDOM) {
                // if the parent of the mention block is the XDOM, we instead select the previous and next  blocks
                block = new ParagraphBlock(
                    Stream.of(mentionBlock.getPreviousSibling(), mentionBlock, mentionBlock.getNextSibling())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                );
            } else {
                block = parent;
            }
            DefaultQuoteService.this.renderer.render(block, printer);
            return printer.toString();
        }
    }
}
