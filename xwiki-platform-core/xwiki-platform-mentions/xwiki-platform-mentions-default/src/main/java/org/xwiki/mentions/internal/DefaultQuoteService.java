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

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import static org.xwiki.rendering.block.Block.Axes.DESCENDANT;

/**
 * Default implementation of {@link QuoteService}.
 *
 * @version $Id$
 * @since 12.6RC1
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
        return Optional.<Block>ofNullable(xdom.getFirstBlock(mentionsBlocksMatcher(anchorId), DESCENDANT))
                   .map(new RenderFunction());
    }

    private BlockMatcher mentionsBlocksMatcher(String anchorId)
    {
        return block -> {
            boolean ret;
            if (block instanceof MacroBlock) {
                ret = ((MacroBlock) block).getId().equals("mention")
                          && Optional.ofNullable(block.getParameter("anchor"))
                                 .map(it -> it.equals(anchorId)).orElse(false);
            } else {
                ret = false;
            }
            return ret;
        };
    }

    private class RenderFunction implements Function<Block, String>
    {
        @Override
        public String apply(Block block)
        {
            WikiPrinter printer = new DefaultWikiPrinter();
            DefaultQuoteService.this.renderer.render(block.getParent(), printer);
            return printer.toString();
        }
    }
}
