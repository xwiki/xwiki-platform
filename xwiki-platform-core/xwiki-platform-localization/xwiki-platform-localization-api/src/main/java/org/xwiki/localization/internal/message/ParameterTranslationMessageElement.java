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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.message.TranslationMessageElement;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.util.ParserUtils;

/**
 * A {@link TranslationMessageElement} resolved based on a passed parameter.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public class ParameterTranslationMessageElement implements TranslationMessageElement
{
    /**
     * Used to clean the result of plain text parser.
     */
    private static final ParserUtils PARSERUTILS = new ParserUtils();

    /**
     * The index of the parameter to return.
     */
    private int index;

    /**
     * Used to parse the String content.
     */
    private Parser plainParser;

    /**
     * @param index the index of the paramater to return
     * @param plainParser used to parse the String content
     */
    public ParameterTranslationMessageElement(int index, Parser plainParser)
    {
        this.index = index;
        this.plainParser = plainParser;
    }

    @Override
    public Block render(Locale locale, Collection<TranslationBundle> bundles, Object... parameters)
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
