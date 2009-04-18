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

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.rendering.internal.renderer.XWikiSyntaxEscapeHandler;
import org.xwiki.rendering.renderer.XWikiSyntaxListenerChain;
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
    private XWikiSyntaxListenerChain listenerChain;

    private XWikiSyntaxEscapeHandler escapeHandler;

    private boolean escapeLastChar;

    private Pattern escapeFirstIfMatching;

    private static final Pattern VERBATIM_PATTERN = Pattern.compile("(\\{\\{\\{)|(\\}\\}\\})");

    public XWikiSyntaxEscapeWikiPrinter(WikiPrinter printer, XWikiSyntaxListenerChain listenerChain)
    {
        super(printer);

        this.escapeHandler = new XWikiSyntaxEscapeHandler();

        this.listenerChain = listenerChain;
    }

    @Override
    public void flush()
    {
        if (getBuffer().length() > 0) {
            this.escapeHandler.escape(getBuffer(), this.listenerChain, this.escapeLastChar, this.escapeFirstIfMatching);
            super.flush();
        }
        this.escapeLastChar = false;
        this.escapeFirstIfMatching = null;
    }

    public void printBeginBold()
    {
        boolean isOnNewLine =
            this.listenerChain.getTextOnNewLineStateChainingListener().isTextOnNewLine() && getBuffer().length() == 0;

        print("**");

        if (isOnNewLine) {
            this.escapeFirstIfMatching = XWikiSyntaxEscapeHandler.STARLISTEND_PATTERN;
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

    public void printVerbatimContent(String verbatimContent)
    {
        StringBuffer result = new StringBuffer();

        Stack<StringBuffer> subVerbatimStack = new Stack<StringBuffer>();
        boolean printEndVerbatim = false;

        Matcher matcher = VERBATIM_PATTERN.matcher(verbatimContent);
        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = verbatimContent.substring(currentIndex, matcher.start());

            if (printEndVerbatim) {
                if (before.startsWith("}")) {
                    result.append("~}~}~}");
                } else {
                    result.append("~}}}");
                }
            }

            if (subVerbatimStack.size() == 0) {
                result.append(before);
            } else {
                subVerbatimStack.peek().append(before);
            }

            if (matcher.group(1) != null) {
                subVerbatimStack.push(new StringBuffer());
            } else {
                if (subVerbatimStack.size() == 0) {
                    printEndVerbatim = true;
                } else {
                    StringBuffer subVerbatim = subVerbatimStack.pop();

                    if (subVerbatimStack.size() == 0) {
                        result.append("{{{");
                        result.append(subVerbatim);
                        result.append("}}}");
                    } else {
                        subVerbatimStack.peek().append("{{{");
                        subVerbatimStack.peek().append(subVerbatim);
                        subVerbatimStack.peek().append("}}}");
                    }
                }
            }
        }

        if (currentIndex == 0) {
            print(verbatimContent);
            return;
        }

        String end = verbatimContent.substring(currentIndex);

        if (printEndVerbatim) {
            if (end.length() == 0 || end.charAt(0) == '}') {
                result.append("~}~}~}");
            } else {
                result.append("~}}}");
            }
        }

        if (subVerbatimStack.size() > 0) {
            // Append remaining string
            subVerbatimStack.peek().append(end);

            // Escape not closed verbatim blocks
            while (subVerbatimStack.size() > 0) {
                StringBuffer subVerbatim = subVerbatimStack.pop();

                if (subVerbatimStack.size() == 0) {
                    if (subVerbatim.length() > 0 && subVerbatim.charAt(0) == '{') {
                        result.append("~{~{~{");
                    } else {
                        result.append("~{{{");
                    }
                    result.append(subVerbatim);
                } else {
                    if (subVerbatim.length() > 0 && subVerbatim.charAt(0) == '{') {
                        subVerbatimStack.peek().append("~{~{~{");
                    } else {
                        subVerbatimStack.peek().append("~{{{");
                    }
                    subVerbatimStack.peek().append(subVerbatim);
                }
            }
        } else {
            // Append remaining string
            result.append(end);
        }

        print(result.toString());
    }
}
