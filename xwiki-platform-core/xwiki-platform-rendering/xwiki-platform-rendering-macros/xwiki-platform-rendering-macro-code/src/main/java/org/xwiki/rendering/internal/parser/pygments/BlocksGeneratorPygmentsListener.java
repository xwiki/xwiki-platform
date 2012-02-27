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
package org.xwiki.rendering.internal.parser.pygments;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.python.core.PyNone;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;

/**
 * Transforms Pygments tokens into XWiki Rendering blocks. This class is overwritten in python an methods are called
 * from there.
 * 
 * @version $Id$
 * @since 1.7RC1
 */
public class BlocksGeneratorPygmentsListener implements PygmentsListener
{
    /**
     * The highlighted result block.
     */
    private List<Block> blocks = new ArrayList<Block>();

    /**
     * Used to convert Pygment token values into blocks.
     */
    private Parser plainTextParser;

    /**
     * @param plainTextParser the parser we'll use to parse Pygment token values into blocks
     */
    public BlocksGeneratorPygmentsListener(Parser plainTextParser)
    {
        this.plainTextParser = plainTextParser;
    }

    /**
     * @return the highlighted result block.
     */
    public List<Block> getBlocks()
    {
        return this.blocks;
    }

    @Override
    public void format(String tokenType, String value, Map<String, Object> style)
    {
        if (value == null || value.length() == 0) {
            return;
        }

        List<Block> blockList;

        if (StringUtils.isEmpty(value)) {
            blockList = Collections.emptyList();
        } else {
            try {
                blockList = this.plainTextParser.parse(new StringReader(value)).getChildren().get(0).getChildren();
            } catch (ParseException e) {
                // This shouldn't happen since the parser cannot throw an exception since the source is a memory
                // String.
                throw new RuntimeException("Failed to parse [" + value + "] as plain text.", e);
            }
        }

        if (!blockList.isEmpty()) {
            String styleParameter = formatStyle(style);

            FormatBlock formatBlock = null;
            if (styleParameter.length() > 0) {
                formatBlock = new FormatBlock(blockList, Format.NONE);
                formatBlock.setParameter("style", styleParameter);
                this.blocks.add(formatBlock);
            } else {
                blocks.addAll(blockList);
            }
        }
    }

    /**
     * Create css style from Pygments style map.
     * 
     * @param style the Pygments style map.
     * @return the css style.
     */
    protected String formatStyle(Map<String, Object> style)
    {
        StringBuffer styleOut = new StringBuffer();

        appendBold(styleOut, style);
        appendItalic(styleOut, style);
        appendUnderline(styleOut, style);

        appendColor(styleOut, style);
        appendBgColor(styleOut, style);
        appendBorder(styleOut, style);

        return styleOut.toString();
    }

    /**
     * Add css bold style to style property.
     * 
     * @param styleOut the {@link StringBuffer} to append to.
     * @param styles the Pygments style map.
     */
    protected void appendBold(StringBuffer styleOut, Map<String, Object> styles)
    {
        appendBoolean(styleOut, "bold", "font-weight: bold; ", styles);
    }

    /**
     * @param styleOut the {@link StringBuffer} to append to.
     * @param styles the Pygments style map.
     */
    protected void appendItalic(StringBuffer styleOut, Map<String, Object> styles)
    {
        appendBoolean(styleOut, "italic", "font-weight: italic; ", styles);
    }

    /**
     * @param styleOut the {@link StringBuffer} to append to.
     * @param styles the Pygments style map.
     */
    protected void appendUnderline(StringBuffer styleOut, Map<String, Object> styles)
    {
        appendBoolean(styleOut, "underline", "text-decoration: underline; ", styles);
    }

    /**
     * @param styleOut the {@link StringBuffer} to append to.
     * @param styles the Pygments style map.
     */
    protected void appendColor(StringBuffer styleOut, Map<String, Object> styles)
    {
        appendStringValue(styleOut, "color", "color: #{0}; ", styles);
    }

    /**
     * @param styleOut the {@link StringBuffer} to append to.
     * @param styles the Pygments style map.
     */
    protected void appendBgColor(StringBuffer styleOut, Map<String, Object> styles)
    {
        appendStringValue(styleOut, "bgcolor", "background-color: #{0}; ", styles);
    }

    /**
     * @param styleOut the {@link StringBuffer} to append to.
     * @param styles the Pygments style map.
     */
    protected void appendBorder(StringBuffer styleOut, Map<String, Object> styles)
    {
        appendStringValue(styleOut, "border", "border: 1px solid #{0}; ", styles);
    }

    /**
     * @param styleOut the {@link StringBuffer} to append to.
     * @param pyName the name of the Pygments property.
     * @param cssPattern the {@link MessageFormat} style pattern to append with the found value to the css
     *            {@link StringBuffer}.
     * @param styles the Pygments style map.
     */
    private void appendStringValue(StringBuffer styleOut, String pyName, String cssPattern, Map<String, Object> styles)
    {
        Object obj = styles.get(pyName);

        if (obj != null && !(obj instanceof PyNone)) {
            styleOut.append(MessageFormat.format(cssPattern, obj));
        }
    }

    /**
     * @param styleOut the {@link StringBuffer} to append to.
     * @param pyName the name of the Pygments property.
     * @param cssValue the css to append to provided @link StringBuffer}.
     * @param styles the Pygments style map.
     */
    private void appendBoolean(StringBuffer styleOut, String pyName, String cssValue, Map<String, Object> styles)
    {
        Object obj = styles.get(pyName);

        if (obj != null && !(obj instanceof PyNone)) {
            if (((Boolean) obj)) {
                styleOut.append(cssValue);
            }
        }
    }
}
