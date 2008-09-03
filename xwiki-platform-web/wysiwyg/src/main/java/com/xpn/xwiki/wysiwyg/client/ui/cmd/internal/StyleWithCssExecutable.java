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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import com.google.gwt.user.client.Element;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

public class StyleWithCssExecutable extends AbstractExecutable
{
    public boolean execute(Element target, String parameter)
    {
        boolean styleWithCSS = Boolean.valueOf(parameter);
        boolean success = execute(target, Command.STYLE_WITH_CSS.toString(), String.valueOf(styleWithCSS));
        // useCSS command is deprecated and has opposite meaning
        success = success || execute(target, "useCSS", String.valueOf(!styleWithCSS));
        return success;
    }

    public String getParameter(Element target)
    {
        String parameter = getParameter(target, Command.STYLE_WITH_CSS.toString());
        if (parameter == null) {
            parameter = getParameter(target, "useCSS");
            if (parameter != null) {
                parameter = String.valueOf(!Boolean.valueOf(parameter));
            }
        }
        return parameter;
    }

    public boolean isEnabled(Element target)
    {
        return isEnabled(target, Command.STYLE_WITH_CSS.toString()) || isEnabled(target, "useCSS");
    }

    public boolean isExecuted(Element target)
    {
        return isExecuted(target, Command.STYLE_WITH_CSS.toString()) || isExecuted(target, "useCSS");
    }

    public boolean isSupported(Element target)
    {
        return isSupported(target, Command.STYLE_WITH_CSS.toString()) || isSupported(target, "useCSS");
    }
}
