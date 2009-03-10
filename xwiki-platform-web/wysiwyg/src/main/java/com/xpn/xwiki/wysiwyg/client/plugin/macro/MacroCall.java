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
package com.xpn.xwiki.wysiwyg.client.plugin.macro;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores information about a macro call.
 * 
 * @version $Id$
 */
public class MacroCall
{
    /**
     * The prefix of the start macro comment node's value.
     */
    private static final String START_MACRO = "startmacro:";

    /**
     * The separator used to differentiate between macro name, macro arguments and macro content.
     */
    private static final String SEPARATOR = "|-|";

    /**
     * The name of the macro.
     */
    private String name;

    /**
     * The arguments passed to the macro.
     */
    private final Map<String, String> arguments = new HashMap<String, String>();

    /**
     * The content of the macro.
     */
    private String content;

    /**
     * Creates a new empty macro call.
     */
    public MacroCall()
    {
        name = "";
        content = "";
    }

    /**
     * Creates a new macro call from a start comment node's value.
     * 
     * @param startMacroComment the value of a start macro comment
     */
    public MacroCall(String startMacroComment)
    {
        // Extract macro name.
        int start = START_MACRO.length();
        int end = startMacroComment.indexOf(SEPARATOR, start);
        name = startMacroComment.substring(start, end);

        // Extract macro arguments.
        // Look for the first argument.
        start = end + SEPARATOR.length();
        int equalIndex = startMacroComment.indexOf('=', start);
        int separatorIndex = startMacroComment.indexOf(SEPARATOR, start);
        while (equalIndex < separatorIndex && equalIndex > 0) {
            String argumentName = startMacroComment.substring(start, equalIndex).trim();

            // Opening quote.
            start = startMacroComment.indexOf('"', equalIndex + 1) + 1;
            // Look for the closing quote.
            end = start;
            boolean escaped = false;
            while (escaped || startMacroComment.charAt(end) != '"') {
                escaped = !escaped && '\\' == startMacroComment.charAt(end);
                end++;
            }

            String argumentValue = startMacroComment.substring(start, end);
            arguments.put(argumentName, unescape(argumentValue));

            // Look for the next argument.
            start = end + 1;
            equalIndex = startMacroComment.indexOf('=', start);
            separatorIndex = startMacroComment.indexOf(SEPARATOR, start);
        }

        // Extract macro content.
        content = startMacroComment.substring(separatorIndex + SEPARATOR.length());
    }

    /**
     * @return the name of the macro
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of the macro to be called.
     * 
     * @param name a macro name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @param name the name of an argument
     * @return the value of the given argument
     */
    public String getArgument(String name)
    {
        return arguments.get(name);
    }

    /**
     * Sets the value of the specified argument.
     * 
     * @param name the name of the argument
     * @param value the value of the argument
     * @return the previous value of the given argument, or {@code null} if this argument was not specified
     */
    public String setArgument(String name, String value)
    {
        return arguments.put(name, value);
    }

    /**
     * Removes the specified argument from this macro call.
     * 
     * @param name the name of a macro parameter
     * @return the value previously associated with the specified argument
     */
    public String removeArgument(String name)
    {
        return arguments.remove(name);
    }

    /**
     * @return {@link #content}
     */
    public String getContent()
    {
        return content;
    }

    /**
     * Sets the content of the macro.
     * 
     * @param content the content of the macro
     */
    public void setContent(String content)
    {
        this.content = content;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuffer strBuff = new StringBuffer(START_MACRO);
        strBuff.append(getName());
        strBuff.append(SEPARATOR);
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            strBuff.append(entry.getKey());
            strBuff.append("=\"");
            strBuff.append(escape(entry.getValue()));
            strBuff.append("\" ");
        }
        strBuff.append(SEPARATOR);
        strBuff.append(getContent());

        return strBuff.toString();
    }

    /**
     * Escapes {@code \} and {@code "} symbols in the given string, which usually is the value entered by the user for
     * some macro parameter.
     * 
     * @param value the string to be escaped
     * @return the escaped string
     */
    private String escape(String value)
    {
        return value.replaceAll("([\\\\\\\"])", "\\\\$1");
    }

    /**
     * Unescapes {@code \} and {@code "} symbols in the given string before letting the user edit it.
     * 
     * @param value the string to be unescaped
     * @return the unescaped string
     */
    private String unescape(String value)
    {
        return value.replaceAll("\\\\([\\\\\\\"])", "$1");
    }
}
