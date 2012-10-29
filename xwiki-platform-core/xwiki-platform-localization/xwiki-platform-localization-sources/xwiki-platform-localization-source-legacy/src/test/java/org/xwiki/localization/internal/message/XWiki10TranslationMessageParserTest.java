package org.xwiki.localization.internal.message;

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
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.annotation.MockingRequirement;

@MockingRequirement(value = XWiki10TranslationMessageParser.class, exceptions = Parser.class)
@ComponentList({PlainTextBlockParser.class, PlainTextStreamParser.class})
public class XWiki10TranslationMessageParserTest extends AbstractMockingComponentTestCase<TranslationMessageParser>
{
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
}
