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
