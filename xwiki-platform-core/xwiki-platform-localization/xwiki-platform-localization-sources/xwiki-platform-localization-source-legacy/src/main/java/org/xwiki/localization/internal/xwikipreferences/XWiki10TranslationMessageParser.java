package org.xwiki.localization.internal.xwikipreferences;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.internal.message.BlockTranslationMessage;
import org.xwiki.localization.internal.message.MessageFormatTranslationMessage;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.util.ParserUtils;

@Component
@Singleton
@Named("xwikil10n/1.0")
public class XWiki10TranslationMessageParser implements TranslationMessageParser
{
    private static final ParserUtils PARSERUTILS = new ParserUtils();

    @Inject
    @Named("plain/1.0")
    private Parser plainParser;

    @Override
    public TranslationMessage parse(String messsage)
    {
        TranslationMessage translationMessage;
        if (messsage.contains("{0}")) {
            translationMessage = new MessageFormatTranslationMessage(new MessageFormat(messsage), this.plainParser);
        } else {
            Block block;
            try {
                List<Block> blocks = this.plainParser.parse(new StringReader(messsage)).getChildren();

                PARSERUTILS.removeTopLevelParagraph(blocks);

                if (blocks.size() == 0) {
                    block = new CompositeBlock();
                } else if (blocks.size() == 1) {
                    block = blocks.get(0);
                } else {
                    block = new CompositeBlock(blocks);
                }
            } catch (ParseException e) {
                // Should never happen since plain text parser cannot fail
                block = new CompositeBlock();
            }

            translationMessage = new BlockTranslationMessage(messsage, block);
        }

        return translationMessage;
    }
}
