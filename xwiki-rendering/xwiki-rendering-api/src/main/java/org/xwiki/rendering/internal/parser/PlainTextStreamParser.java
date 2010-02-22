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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Plain Text Parser to convert a text source into a events.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Component("plain/1.0")
public class PlainTextStreamParser implements StreamParser
{
    /**
     * The characters which are considered as "special" symbols for {@link org.xwiki.rendering.block.SpecialSymbolBlock}
     * .
     */
    public static final Pattern SPECIALSYMBOL_PATTERN = Pattern.compile("[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]");

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.Parser#getSyntax()
     */
    public Syntax getSyntax()
    {
        return Syntax.PLAIN_1_0;
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.StreamParser#parse(java.io.Reader, org.xwiki.rendering.listener.Listener)
     */
    public void parse(Reader source, Listener listener) throws ParseException
    {
        StringBuffer word = new StringBuffer();
        BufferedReader bufferedSource = new BufferedReader(source);
        int charAsInt;

        listener.beginParagraph(Listener.EMPTY_PARAMETERS);

        while ((charAsInt = readChar(bufferedSource)) != -1) {
            char c = (char) charAsInt;
            if (c == '\n') {
                if (word.length() > 0) {
                    listener.onWord(word.toString());
                }
                listener.onNewLine();

                word.setLength(0);
            } else if (c == '\r') {
                // Do nothing, skip it
            } else if (c == ' ') {
                if (word.length() > 0) {
                    listener.onWord(word.toString());
                }
                listener.onSpace();

                word.setLength(0);
            } else if (SPECIALSYMBOL_PATTERN.matcher(String.valueOf(c)).matches()) {
                if (word.length() > 0) {
                    listener.onWord(word.toString());
                }
                listener.onSpecialSymbol(c);

                word.setLength(0);
            } else {
                word.append(c);
            }
        }

        if (word.length() > 0) {
            listener.onWord(word.toString());
        }

        listener.endParagraph(Listener.EMPTY_PARAMETERS);
    }
}
