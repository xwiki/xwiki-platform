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
     * The name of the macro.
     */
    private String name;

    /**
     * The arguments passed to the macro.
     */
    private Map<String, String> arguments;

    /**
     * The content of the macro.
     */
    private String content;

    /**
     * Creates a new macro call from a start comment node's value.
     * 
     * @param startMacroComment the value of a start macro comment
     */
    public MacroCall(String startMacroComment)
    {
        String data = startMacroComment.substring("startmacro:".length());
        String[] parts = data.split("\\|\\-\\|", 3);

        name = parts[0];
        content = parts[2];

        arguments = new HashMap<String, String>();
        String[] args = parts[1].split("\\s+");
        for (int i = 0; i < args.length; i++) {
            String[] pair = args[i].split("=");
            String quotedValue = pair[1].trim();
            arguments.put(pair[0].trim(), quotedValue.substring(1, quotedValue.length() - 1));
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
}
