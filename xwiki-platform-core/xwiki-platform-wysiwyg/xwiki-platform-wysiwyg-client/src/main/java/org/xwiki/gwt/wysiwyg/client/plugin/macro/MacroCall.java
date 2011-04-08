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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.EscapeUtils;

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
     * The values of the arguments passed to the macro.
     */
    private final Map<String, String> argumentValues = new HashMap<String, String>();

    /**
     * The case sensitive names of the arguments passed to the macro.
     */
    private final Map<String, String> argumentNames = new HashMap<String, String>();

    /**
     * The content of the macro.
     */
    private String content;

    /**
     * Creates a new empty macro call.
     */
    public MacroCall()
    {
    }

    /**
     * Creates a new macro call from a start comment node's value.
     * 
     * @param startMacroComment the value of a start macro comment
     */
    public MacroCall(String startMacroComment)
    {
        // Unescape the text of the start macro comment.
        String text = EscapeUtils.unescapeBackslash(startMacroComment);

        // Extract macro name.
        int start = START_MACRO.length();
        int end = text.indexOf(SEPARATOR, start);
        name = text.substring(start, end);

        // Extract macro arguments.
        // Look for the first argument.
        start = end + SEPARATOR.length();
        int equalIndex = text.indexOf('=', start);
        int separatorIndex = text.indexOf(SEPARATOR, start);
        while (equalIndex > 0 && (separatorIndex < 0 || equalIndex < separatorIndex)) {
            String argumentName = text.substring(start, equalIndex).trim();

            // Opening quote.
            start = text.indexOf('"', equalIndex + 1) + 1;
            // Look for the closing quote.
            end = start;
            boolean escaped = false;
            while (escaped || text.charAt(end) != '"') {
                escaped = !escaped && '\\' == text.charAt(end);
                end++;
            }

            setArgument(argumentName, EscapeUtils.unescapeBackslash(text.substring(start, end)));

            // Look for the next argument.
            start = end + 1;
            equalIndex = text.indexOf('=', start);
            separatorIndex = text.indexOf(SEPARATOR, start);
        }

        // Extract macro content, if there is any.
        if (separatorIndex >= 0) {
            content = text.substring(separatorIndex + SEPARATOR.length());
        }
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
        return argumentValues.get(name.toLowerCase());
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
        String id = name.toLowerCase();
        // If the name is case sensitive and it hasn't been set before, store it.
        // We don't overwrite argument names because we don't want to loose their Wiki syntax form over macro edit in
        // WYSIWYG mode. For instance, if a user writes {{toc sTaRt="1"/}} we should keep the case of the parameter name
        // over macro edit.
        if (!id.equals(name) && !argumentNames.containsKey(id)) {
            argumentNames.put(id, name);
        }
        return argumentValues.put(id, value);
    }

    /**
     * Removes the specified argument from this macro call.
     * 
     * @param name the name of a macro parameter
     * @return the value previously associated with the specified argument
     */
    public String removeArgument(String name)
    {
        String id = name.toLowerCase();
        argumentNames.remove(id);
        return argumentValues.remove(id);
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
        for (Map.Entry<String, String> entry : argumentValues.entrySet()) {
            String argumentName = argumentNames.get(entry.getKey());
            if (argumentName == null) {
                argumentName = entry.getKey();
            }
            strBuff.append(argumentName);
            strBuff.append("=\"");
            strBuff.append(escapeMacroParameterValue(entry.getValue()));
            strBuff.append("\" ");
        }
        if (content != null) {
            strBuff.append(SEPARATOR);
            strBuff.append(content);
        }

        return EscapeUtils.escapeComment(strBuff.toString());
    }

    /**
     * Escapes the quotes inside a macro parameter value.
     * 
     * @param value the string to be escaped
     * @return the escaped string
     */
    private String escapeMacroParameterValue(String value)
    {
        return value.replaceAll("([\\\\\\\"])", "\\\\$1");
    }
}
