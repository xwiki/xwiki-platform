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
package org.xwiki.localization.messagetool.internal;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.internal.parser.plain.PlainTextBlockParser;
import org.xwiki.rendering.internal.parser.plain.PlainTextStreamParser;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ComponentTest
@ComponentList({PlainTextBlockParser.class, PlainTextStreamParser.class})
class MessageToolTranslationMessageParserTest
{
    @InjectMockComponents
    private MessageToolTranslationMessageParser parser;

    @Test
    void messageEmpty()
    {
        TranslationMessage translationMessage = this.parser.parse("");

        assertEquals("", translationMessage.getRawSource());
        assertEquals(new CompositeBlock(), translationMessage.render(null, null));
    }

    @Test
    void messageSimple()
    {
        TranslationMessage translationMessage = this.parser.parse("word");

        assertEquals("word", translationMessage.getRawSource());
        assertEquals(new WordBlock("word"), translationMessage.render(null, null));
    }

    @Test
    void messageWithOneParameter()
    {
        TranslationMessage translationMessage = this.parser.parse("{0}");

        assertEquals("{0}", translationMessage.getRawSource());
        assertEquals(new WordBlock("word"), translationMessage.render(null, null, "word"));
    }

    @Test
    void messageWithExpectedParameter()
    {
        TranslationMessage translationMessage = this.parser.parse("{0}");

        assertEquals("{0}", translationMessage.getRawSource());
        assertEquals(
            new CompositeBlock(List.of(new SpecialSymbolBlock('{'), new WordBlock("0"),
                new SpecialSymbolBlock('}'))), translationMessage.render(null, null));
    }

    @Test
    void messageWithApostrophe()
    {
        TranslationMessage translationMessage = this.parser.parse("'");

        assertEquals("'", translationMessage.getRawSource());
        assertEquals(new SpecialSymbolBlock('\''), translationMessage.render(null, null));
    }

    @Test
    void messageWithEscapedParameter()
    {
        TranslationMessage translationMessage = this.parser.parse("'{0}");

        assertEquals("'{0}", translationMessage.getRawSource());
        assertEquals(
            new CompositeBlock(List.of(new SpecialSymbolBlock('{'), new WordBlock("0"),
                new SpecialSymbolBlock('}'))), translationMessage.render(null, null, "word"));
    }

    @Test
    void messageWithChoiceSyntax()
    {
        TranslationMessage translationMessage = this.parser.parse("{0,choice,0#choice1|0<choice2}");

        assertEquals("{0,choice,0#choice1|0<choice2}", translationMessage.getRawSource());
        assertEquals(new WordBlock("choice1"), translationMessage.render(null, null, 0));
        assertEquals(new WordBlock("choice2"), translationMessage.render(null, null, 42));
    }
}
