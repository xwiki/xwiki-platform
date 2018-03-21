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
package org.xwiki.annotation.internal.renderer;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.annotation.content.AlteredContent;
import org.xwiki.annotation.content.ContentAlterer;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.renderer.AbstractChainingPrintRenderer;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Plain text renderer that normalizes spaces in the printed text.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class PlainTextNormalizingChainingRenderer extends AbstractChainingPrintRenderer
{
    /**
     * Normalizer for the content serialized by this listener, to clean all texts such as protected strings in various
     * events.
     */
    private ContentAlterer textCleaner;

    /**
     * Flag to signal that the renderer is currently rendering whitespaces (has rendered the first one) and should not
     * append more. Starting true because we don't want to print beginning spaces.
     */
    private boolean isInWhitespace = true;

    /**
     * Flag to signal that this renderer has currently printed something. Cache for checking that serializing the
     * printer would return non zero characters, since serializing the printer at each step can be a bit too much.
     */
    private boolean hasPrinted;

    /**
     * Builds an abstract plain text normalizing renderer with the passed text cleaner.
     *
     * @param textCleaner the text cleaner used to normalize the texts produced by the events
     * @param listenerChain the listeners chain this listener is part of
     */
    public PlainTextNormalizingChainingRenderer(ContentAlterer textCleaner, ListenerChain listenerChain)
    {
        this.textCleaner = textCleaner;
        setListenerChain(listenerChain);
    }

    @Override
    public void onWord(String word)
    {
        printText(word);
    }

    @Override
    public void onSpecialSymbol(char symbol)
    {
        printText("" + symbol);
    }

    @Override
    public void onVerbatim(String content, boolean inline, Map<String, String> parameters)
    {
        if (!inline || Character.isWhitespace(content.charAt(0))) {
            // if there is a space right at the beginning of the verbatim string, or the verbatim string is block, we
            // need to print a space
            printSpace();
        }

        AlteredContent cleanedContent = textCleaner.alter(content);
        printText(cleanedContent.getContent().toString());

        if (!inline || Character.isWhitespace(content.charAt(content.length() - 1))) {
            // print a space after
            printSpace();
        }
    }

    @Override
    public void onRawText(String text, Syntax syntax)
    {
        if (StringUtils.isNotEmpty(text)) {
            // Similar approach to verbatim FTM. In the future, syntax specific cleaner could be used for various
            // syntaxes (which would do the great job for HTML, for example) normalize the protected string before
            // adding it to the plain text version
            if (Character.isWhitespace(text.charAt(0))) {
                // if there is a space right at the beginning of the raw text, we need to print a space
                printSpace();
            }

            AlteredContent cleanedContent = textCleaner.alter(text);
            printText(cleanedContent.getContent().toString());

            if (Character.isWhitespace(text.charAt(text.length() - 1))) {
                // if there is a space right at the end of the text, we need to print a space
                printSpace();
            }
        }
    }

    @Override
    public void onSpace()
    {
        printSpace();
    }

    /**
     * Print a space to the renderer's printer.
     */
    protected void printSpace()
    {
        // start printing whitespaces
        isInWhitespace = true;
    }

    /**
     * Prints a text to the renderer's printer.
     *
     * @param text the text to print
     */
    protected void printText(String text)
    {
        // if it's in whitespace and there was something printed before, print the remaining space, and then handle the
        // current text
        if (isInWhitespace && hasPrinted) {
            getPrinter().print(" ");
        }
        getPrinter().print(text);
        hasPrinted = true;
        isInWhitespace = false;
    }

    @Override
    public void onEmptyLines(int count)
    {
        if (count > 0) {
            printSpace();
        }
    }

    @Override
    public void onNewLine()
    {
        printSpace();
    }

    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        printSpace();
    }

    // all next events are block, so spaces need to be printed around

    @Override
    public void beginDefinitionDescription()
    {
        printSpace();
    }

    @Override
    public void endDefinitionDescription()
    {
        printSpace();
    }

    @Override
    public void beginDefinitionList(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endDefinitionList(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void beginDefinitionTerm()
    {
        printSpace();
    }

    @Override
    public void endDefinitionTerm()
    {
        printSpace();
    }

    @Override
    public void beginGroup(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endGroup(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void beginList(ListType type, Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endList(ListType type, Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void beginListItem()
    {
        printSpace();
    }

    @Override
    public void endListItem()
    {
        printSpace();
    }

    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void beginQuotationLine()
    {
        printSpace();
    }

    @Override
    public void endQuotationLine()
    {
        printSpace();
    }

    @Override
    public void beginTable(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endTable(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endTableRow(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void beginTableCell(Map<String, String> parameters)
    {
        printSpace();
    }

    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        printSpace();
    }
}
