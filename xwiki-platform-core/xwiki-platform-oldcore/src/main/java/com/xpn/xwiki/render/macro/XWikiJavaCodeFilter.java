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
package com.xpn.xwiki.render.macro;

import org.radeox.macro.code.DefaultRegexCodeFormatter;
import org.radeox.macro.code.SourceCodeFormatter;

/**
 * Redefinition of the Java code formatter ({@code org.radeox.macro.code.JavaCodeFilter}) from Radeox. This was needed
 * because of the inefficient regular expression for detecting string literals, which caused exponential rendering times
 * for certain inputs.
 * 
 * @version $Id$
 */
public class XWikiJavaCodeFilter extends DefaultRegexCodeFormatter implements SourceCodeFormatter
{
    /**
     * Regular expression for matching java keywords.
     */
    private static final String KEYWORDS =
        "\\b(abstract|break|byvalue|case|cast|catch|const|continue|default|do|else|extends|"
            + "false|final|finally|for|future|generic|goto|if|implements|import|inner|instanceof|interface|"
            + "native|new|null|operator|outer|package|private|protected|public|rest|return|static|super|switch|"
            + "synchronized|this|throw|throws|transient|true|try|var|volatile|while)\\b";

    /**
     * Regular expression for matching popular class names.
     */
    private static final String OBJECTS =
        "\\b(Boolean|Byte|Character|Class|ClassLoader|Cloneable|Compiler|Double|Float|Integer|Long|Math|"
            + "Number|Object|Process|Runnable|Runtime|SecurityManager|Short|String|StringBuffer|System|"
            + "Thread|ThreadGroup|Void|boolean|char|byte|short|int|long|float|double)\\b";

    /**
     * Regular expression for matching string literals. Fixed to prevent exponential explosion after an unclosed quote.
     */
    private static final String QUOTES = "\"(([^\"\\\\]|\\.)*+)\"";

    /**
     * Default constructor, adds filters for java keywords, class names and string literals.
     */
    public XWikiJavaCodeFilter()
    {
        super(QUOTES, "<span class=\"java-quote\">\"$1\"</span>");
        addRegex(KEYWORDS, "<span class=\"java-keyword\">$1</span>");
        addRegex(OBJECTS, "<span class=\"java-object\">$1</span>");
    }

    @Override
    public String getName()
    {
        return "java";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority()
    {
        // Return a larger priority, so that our code formatter replaces the default formatter provided by Radeox.
        return 20;
    }
}
