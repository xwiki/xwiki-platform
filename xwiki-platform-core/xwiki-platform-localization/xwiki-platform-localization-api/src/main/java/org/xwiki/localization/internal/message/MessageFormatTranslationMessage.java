package org.xwiki.localization.internal.message;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.xwiki.localization.Bundle;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.util.ParserUtils;

public class MessageFormatTranslationMessage implements TranslationMessage
{
    private static final ParserUtils PARSERUTILS = new ParserUtils();

    private MessageFormat messageFormat;

    private Parser plainParser;

    public MessageFormatTranslationMessage(MessageFormat messageFormat, Parser plainParser)
    {
        this.messageFormat = messageFormat;
        this.plainParser = plainParser;
    }

    @Override
    public Block render(Locale locale, Collection<Bundle> bundles, Object... parameters)
    {
        String result = this.messageFormat.format(parameters);

        Block block;

        try {
            List<Block> blocks = this.plainParser.parse(new StringReader(result)).getChildren();

            PARSERUTILS.removeTopLevelParagraph(blocks);

            if (blocks.size() == 0) {
                block = null;
            } else if (blocks.size() == 1) {
                block = blocks.get(0);
            } else {
                block = new CompositeBlock(blocks);
            }
        } catch (ParseException e) {
            // Should never happen since plain text parser cannot fail
            block = null;
        }

        return block;
    }

    @Override
    public String getRawSource()
    {
        return this.messageFormat.toPattern();
    }
}
