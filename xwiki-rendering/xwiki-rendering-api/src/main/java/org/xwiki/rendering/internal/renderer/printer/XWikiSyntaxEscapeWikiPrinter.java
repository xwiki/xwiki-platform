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
package org.xwiki.rendering.internal.renderer.printer;

import java.util.regex.Pattern;

import org.xwiki.rendering.internal.renderer.XWikiSyntaxEscapeHandler;
import org.xwiki.rendering.internal.renderer.state.BlockStateListener;
import org.xwiki.rendering.internal.renderer.state.TextOnNewLineStateListener;
import org.xwiki.rendering.renderer.printer.LookaheadWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * A Wiki printer that knows how to escape characters that would otherwise mean something different in XWiki wiki
 * syntax. For example if we have "**" as special symbols (and not as a Bold Format block) we need to escape them to
 * "~*~*" as otherwise they'd be considered bold after being rendered.
 * 
 * @version $Id$
 * @since 1.7
 */
public class XWikiSyntaxEscapeWikiPrinter extends LookaheadWikiPrinter
{
    private XWikiSyntaxEscapeHandler escapeHandler;

    private boolean escapeLastChar;

    private Pattern escapeFirstIfMatching;

    private BlockStateListener blockListener;

    private TextOnNewLineStateListener textListener;

    public XWikiSyntaxEscapeWikiPrinter(WikiPrinter printer)
    {
        this(printer, null, null);
    }

    public XWikiSyntaxEscapeWikiPrinter(WikiPrinter printer, BlockStateListener blockListener, 
        TextOnNewLineStateListener textListener)
    {
        super(printer);
        this.escapeHandler = new XWikiSyntaxEscapeHandler();
        this.blockListener = blockListener;
        this.textListener = textListener;
    }

    public void setBlockListener(BlockStateListener blockListener)
    {
        this.blockListener = blockListener;
    }

    public void setTextListener(TextOnNewLineStateListener textListener)
    {
        this.textListener = textListener;
    }

    public BlockStateListener getBlockListener()
    {
        return this.blockListener;
    }

    public TextOnNewLineStateListener getTextListener()
    {
        return this.textListener;
    }

    public void printBeginBold()
    {
        boolean isOnNewLine = getTextListener().isTextOnNewLine() && getBuffer().length() == 0;

        super.print("**");

        if (isOnNewLine) {
            this.escapeFirstIfMatching = XWikiSyntaxEscapeHandler.SPACE_PATTERN;
        }
    }

    public void printBeginItalic()
    {
        // If the lookahead buffer is not empty and the last character is ":" then we need to escape it
        // since otherwise we would get "://" which could be confused for a URL.
        if (getBuffer().length() > 0 && getBuffer().charAt(getBuffer().length() - 1) == ':') {
            this.escapeLastChar = true;
        }

        super.print("//");
    }

    public void printEndItalic()
    {
        super.print("//");
    }

    public void printInlineMacro(String xwikiSyntaxText)
    {
        // If the lookahead buffer is not empty and the last character is "{" then we need to escape it
        // since otherwise we would get "{{{" which could be confused for a verbatim block.
        if (getBuffer().length() > 0 && getBuffer().charAt(getBuffer().length() - 1) == '{') {
            this.escapeLastChar = true;
        }

        super.print(xwikiSyntaxText);
    }

    @Override
    public void flush()
    {
        this.escapeHandler.escape(getBuffer(), getBlockListener(), getTextListener(), this.escapeLastChar, 
            this.escapeFirstIfMatching);
        this.escapeLastChar = false;
        this.escapeFirstIfMatching = null;
        super.flush();
    }
}
