package org.xwiki.localization.message;

import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.xwiki.localization.Bundle;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.util.ParserUtils;

public class ParameterTranslationMessageElement implements TranslationMessageElement
{
    private static final ParserUtils PARSERUTILS = new ParserUtils();

    private int index;

    private Parser plainParser;

    public ParameterTranslationMessageElement(int index, Parser plainParser)
    {
        this.index = index;
    }

    @Override
    public Block render(Locale locale, Collection<Bundle> bundles, Object... parameters)
    {
        Object parameter = parameters[index];

        Block block;
        if (parameter instanceof Block) {
            block = (Block) parameter;
        } else if (parameter != null) {
            try {
                XDOM xdom = this.plainParser.parse(new StringReader(parameter.toString()));

                List<Block> blocks = xdom.getChildren();
                PARSERUTILS.removeTopLevelParagraph(blocks);
                if (blocks.isEmpty()) {
                    block = null;
                } else if (blocks.size() == 1) {
                    block = blocks.get(0);
                } else {
                    block = new CompositeBlock(blocks);
                }
            } catch (ParseException e) {
                // make the #render fail instead ?
                block = null;
            }
        } else {
            block = null;
        }

        return block;
    }
}
