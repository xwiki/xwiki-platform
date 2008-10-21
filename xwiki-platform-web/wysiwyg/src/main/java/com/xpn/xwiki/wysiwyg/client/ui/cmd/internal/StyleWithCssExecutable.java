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

import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.util.Document;

public class StyleWithCssExecutable extends AbstractExecutable
{
    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(Document, String)
     */
    public boolean execute(Document doc, String parameter)
    {
        boolean styleWithCSS = Boolean.valueOf(parameter);
        boolean success = execute(doc, Command.STYLE_WITH_CSS.toString(), String.valueOf(styleWithCSS));
        // useCSS command is deprecated and has opposite meaning
        success = success || execute(doc, "useCSS", String.valueOf(!styleWithCSS));
        return success;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#getParameter(Document)
     */
    public String getParameter(Document doc)
    {
        String parameter = getParameter(doc, Command.STYLE_WITH_CSS.toString());
        if (parameter == null) {
            parameter = getParameter(doc, "useCSS");
            if (parameter != null) {
                parameter = String.valueOf(!Boolean.valueOf(parameter));
            }
        }
        return parameter;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isEnabled(Document)
     */
    public boolean isEnabled(Document doc)
    {
        return isEnabled(doc, Command.STYLE_WITH_CSS.toString()) || isEnabled(doc, "useCSS");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isExecuted(Document)
     */
    public boolean isExecuted(Document doc)
    {
        return isExecuted(doc, Command.STYLE_WITH_CSS.toString()) || isExecuted(doc, "useCSS");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isSupported(Document)
     */
    public boolean isSupported(Document doc)
    {
        return isSupported(doc, Command.STYLE_WITH_CSS.toString()) || isSupported(doc, "useCSS");
    }
}
