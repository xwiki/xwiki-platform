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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.internal.parser.plain.PlainTextBlockParser;
import org.xwiki.rendering.internal.parser.plain.PlainTextStreamParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.jmock.annotation.MockingRequirement;

@MockingRequirement(value = MessageToolTranslationMessageParser.class, exceptions = Parser.class)
@ComponentList({PlainTextBlockParser.class, PlainTextStreamParser.class})
public class MessageToolTranslationMessageParserTest extends AbstractMockingComponentTestCase<TranslationMessageParser>
{
    @Test
    public void messageEmpty() throws ComponentLookupException
    {
        TranslationMessage translationMessage = getMockedComponent().parse("");

        Assert.assertEquals("", translationMessage.getRawSource());
        Assert.assertEquals(new CompositeBlock(), translationMessage.render(null, null));
    }

    @Test
    public void messageSimple() throws ComponentLookupException
    {
        TranslationMessage translationMessage = getMockedComponent().parse("word");

        Assert.assertEquals("word", translationMessage.getRawSource());
        Assert.assertEquals(new WordBlock("word"), translationMessage.render(null, null));
    }

    @Test
    public void messageWithOneParameter() throws ComponentLookupException
    {
        TranslationMessage translationMessage = getMockedComponent().parse("{0}");

        Assert.assertEquals("{0}", translationMessage.getRawSource());
        Assert.assertEquals(new WordBlock("word"), translationMessage.render(null, null, "word"));
    }

    @Test
    public void messageWithExpectedParameter() throws ComponentLookupException
    {
        TranslationMessage translationMessage = getMockedComponent().parse("{0}");

        Assert.assertEquals("{0}", translationMessage.getRawSource());
        Assert.assertEquals(
            new CompositeBlock(Arrays.<Block> asList(new SpecialSymbolBlock('{'), new WordBlock("0"),
                new SpecialSymbolBlock('}'))), translationMessage.render(null, null));
    }

    @Test
    public void messageWithApostrophe() throws ComponentLookupException
    {
        TranslationMessage translationMessage = getMockedComponent().parse("'");

        Assert.assertEquals("'", translationMessage.getRawSource());
        Assert.assertEquals(new SpecialSymbolBlock('\''), translationMessage.render(null, null));
    }

    @Test
    public void messageWithEscapedParameter() throws ComponentLookupException
    {
        TranslationMessage translationMessage = getMockedComponent().parse("'{0}");

        Assert.assertEquals("'{0}", translationMessage.getRawSource());
        Assert.assertEquals(
            new CompositeBlock(Arrays.<Block> asList(new SpecialSymbolBlock('{'), new WordBlock("0"),
                new SpecialSymbolBlock('}'))), translationMessage.render(null, null, "word"));
    }
    
    @Test
    public void messageWithChoiceSyntax() throws ComponentLookupException
    {
        TranslationMessage translationMessage = getMockedComponent().parse("{0,choice,0#choice1|0<choice2}");

        Assert.assertEquals("{0,choice,0#choice1|0<choice2}", translationMessage.getRawSource());
        Assert.assertEquals(new WordBlock("choice1"), translationMessage.render(null, null, 0));
        Assert.assertEquals(new WordBlock("choice2"), translationMessage.render(null, null, 42));
    }

}
