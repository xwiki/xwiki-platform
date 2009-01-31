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
import org.xwiki.rendering.internal.renderer.state.XWikiSyntaxState;
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
    private XWikiSyntaxState state;

    private XWikiSyntaxEscapeHandler escapeHandler;

    private boolean escapeLastChar;

    private Pattern escapeFirstIfMatching;

    public XWikiSyntaxEscapeWikiPrinter(WikiPrinter printer, XWikiSyntaxState state)
    {
        super(printer);

        this.escapeHandler = new XWikiSyntaxEscapeHandler();

        this.state = state;
    }

    @Override
    public void flush()
    {
        this.escapeHandler.escape(getBuffer(), this.state, this.escapeLastChar, this.escapeFirstIfMatching);
        this.escapeLastChar = false;
        this.escapeFirstIfMatching = null;
        super.flush();
    }

    public void printBeginBold()
    {
        boolean isOnNewLine = this.state.getTextOnNewLineStateListener().isTextOnNewLine() && getBuffer().length() == 0;

        print("**");

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

        print("//");
    }

    public void printEndItalic()
    {
        print("//");
    }

    public void printInlineMacro(String xwikiSyntaxText)
    {
        // If the lookahead buffer is not empty and the last character is "{" then we need to escape it
        // since otherwise we would get "{{{" which could be confused for a verbatim block.
        if (getBuffer().length() > 0 && getBuffer().charAt(getBuffer().length() - 1) == '{') {
            this.escapeLastChar = true;
        }

        print(xwikiSyntaxText);
    }
}
