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
package org.xwiki.rendering.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;

/**
 * Methods for helping in parsing.
 * 
 * @version $Id$
 * @since 1.7M1
 */
public class ParserUtils
{
    private static final Pattern SPECIALSYMBOL_PATTERN = Pattern.compile("[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]");

    /**
     * Parse a simple non wiki string to be able to insert it in the XDOM.
     * 
     * @param text the text to parse (pure text)
     * @return the list of {@link Block} ({@link WordBlock}, {@link SpaceBlock}, {@link NewLineBlock},
     *         {@link SpecialSymbolBlock}).
     * @since 1.7
     */
    public List<Block> parsePlainText(String text)
    {
        List<Block> blockList = new ArrayList<Block>();
        StringBuffer word = new StringBuffer();
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);

            if (c == '\n') {
                if (word.length() > 0) {
                    blockList.add(new WordBlock(word.toString()));
                }
                blockList.add(NewLineBlock.NEW_LINE_BLOCK);

                word.setLength(0);
            } else if (c == '\r') {
                continue;
            } else if (c == ' ') {
                if (word.length() > 0) {
                    blockList.add(new WordBlock(word.toString()));
                }
                blockList.add(SpaceBlock.SPACE_BLOCK);

                word.setLength(0);
            } else if (SPECIALSYMBOL_PATTERN.matcher(String.valueOf(c)).matches()) {
                if (word.length() > 0) {
                    blockList.add(new WordBlock(word.toString()));
                }
                blockList.add(new SpecialSymbolBlock(c));

                word.setLength(0);
            } else {
                word.append(c);
            }
        }

        if (word.length() > 0) {
            blockList.add(new WordBlock(word.toString()));
        }

        return blockList;
    }

    /**
     * Removes any top level paragraph since for example for the following use case we don't want an extra paragraph
     * block: <code>= hello {{velocity}}world{{/velocity}}</code>.
     * 
     * @param blocks the blocks to check and convert
     */
    public void removeTopLevelParagraph(List<Block> blocks)
    {
        // Remove any top level paragraph so that the result of a macro can be used inline for example.
        // We only remove the paragraph if there's only one top level element and if it's a paragraph.
        if ((blocks.size() == 1) && ParagraphBlock.class.isAssignableFrom(blocks.get(0).getClass())) {
            Block paragraphBlock = blocks.remove(0);
            blocks.addAll(0, paragraphBlock.getChildren());
        }
    }
}
