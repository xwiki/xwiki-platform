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
package org.xwiki.rendering.internal.parser;

import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.component.annotation.Component;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Plain Text Parser to convert a text source into a XDOM object.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Component("plain/1.0")
public class PlainTextParser implements Parser
{
    /**
     * The characters which are considered as "special" symbols for {@link SpecialSymbolBlock}.
     */
    private static final Pattern SPECIALSYMBOL_PATTERN = Pattern.compile("[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]");

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.parser.Parser#getSyntax()
     */
    public Syntax getSyntax()
    {
        return Syntax.PLAIN_1_0;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.parser.Parser#parse(java.io.Reader)
     */
    public XDOM parse(Reader source) throws ParseException
    {
        List<Block> blockList = new ArrayList<Block>();
        StringBuffer word = new StringBuffer();
        BufferedReader bufferedSource = new BufferedReader(source);
        int charAsInt;

        while ((charAsInt = readChar(bufferedSource)) != -1) {
            char c = (char) charAsInt;
            if (c == '\n') {
                if (word.length() > 0) {
                    blockList.add(new WordBlock(word.toString()));
                }
                blockList.add(NewLineBlock.NEW_LINE_BLOCK);

                word.setLength(0);
            } else if (c == '\r') {
                // Do nothing, skip it
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

        return new XDOM(blockList);
    }

    /**
     * Read a single char from an Reader source.
     *
     * @param source the input to read from
     * @return the char read
     * @throws ParseException in case of reading error
     */
    private int readChar(Reader source) throws ParseException
    {
        int c;

        try {
            c = source.read();
        } catch (IOException e) {
            throw new ParseException("Failed to read input source", e);
        }

        return c;
    }
}
