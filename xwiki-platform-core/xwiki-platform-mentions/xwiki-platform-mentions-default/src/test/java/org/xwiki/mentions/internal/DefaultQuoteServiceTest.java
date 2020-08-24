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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test of {@link DefaultQuoteService}.
 *
 * @version $Id$
 * @since 12.6
 */
@ComponentTest
class DefaultQuoteServiceTest
{
    @InjectMockComponents
    private DefaultQuoteService quoteService;

    @MockComponent
    @Named("plainmentions/1.0")
    private BlockRenderer renderer;

    @Test
    void extractNoMentionFound()
    {
        Optional<String> actual = this.quoteService.extract(new XDOM(emptyList()), "anchorId");

        verify(this.renderer, never()).render(any(Block.class), any(WikiPrinter.class));

        assertFalse(actual.isPresent());
    }

    @Test
    void extractOneMention()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("anchor", "anchorId");
        MacroBlock mentionMacro = new MacroBlock("mention", parameters, false);
        ParagraphBlock paragraphBlock =
            new ParagraphBlock(asList(new RawBlock("hello", Syntax.XWIKI_2_1), mentionMacro));
        XDOM xdom = new XDOM(singletonList(paragraphBlock));

        Optional<String> actual = this.quoteService.extract(xdom, "anchorId");

        verify(this.renderer).render(eq(paragraphBlock), any(WikiPrinter.class));

        assertTrue(actual.isPresent());
    }
}